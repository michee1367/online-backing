package com.villagesat.transaction.application.service;

import com.villagesat.common.idempotency.IdempotencyService;
import com.villagesat.common.security.SecurityUtils;
import com.villagesat.transaction.adapter.out.wallet.WalletClient;
import com.villagesat.transaction.application.service.TransferService.DuplicateTransactionException;
import com.villagesat.transaction.application.service.TransferService.FraudBlockedException;
import com.villagesat.transaction.application.service.TransferService.TransferResult;
import com.villagesat.transaction.domain.model.Transaction;
import com.villagesat.transaction.domain.port.out.FraudScoringPort;
import com.villagesat.transaction.domain.port.out.TransactionEventPublisher;
import com.villagesat.transaction.domain.port.out.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class TransferService {

    private static final BigDecimal TRANSFER_FEE_RATE = new BigDecimal("0.01"); // 1%
    private static final BigDecimal MIN_FEE = new BigDecimal("100.00");

    private final TransactionRepository transactionRepository;
    private final WalletClient walletClient;
    private final FraudScoringPort fraudScoringPort;
    private final TransactionEventPublisher eventPublisher;
    private final IdempotencyService idempotencyService;
    private final FeeCalculator feeCalculator;

    public TransferService(TransactionRepository transactionRepository,
                           WalletClient walletClient,
                           FraudScoringPort fraudScoringPort,
                           TransactionEventPublisher eventPublisher,
                           IdempotencyService idempotencyService,
                           FeeCalculator feeCalculator) {
        this.transactionRepository = transactionRepository;
        this.walletClient = walletClient;
        this.fraudScoringPort = fraudScoringPort;
        this.eventPublisher = eventPublisher;
        this.idempotencyService = idempotencyService;
        this.feeCalculator = feeCalculator;
    }

    @Transactional
    public TransferResult executeTransfer(TransferCommand command) {
        // 1. Idempotence check
        var cached = idempotencyService.getCachedResponse(command.idempotencyKey(), TransferResult.class);
        if (cached.isPresent()) {
            return cached.get();
        }

        if (!idempotencyService.tryAcquireLock(command.idempotencyKey())) {
            throw new DuplicateTransactionException(command.idempotencyKey());
        }

        UUID userId = SecurityUtils.getCurrentUserId();

        // 2. Fraud scoring (sync, timeout 200ms)
        var fraudResult = fraudScoringPort.score(new FraudScoringPort.FraudRequest(
                userId, command.sourceWalletId(), command.amount(), command.currency()));
        if (fraudResult.action() == FraudScoringPort.FraudAction.BLOCK) {
            throw new FraudBlockedException(fraudResult.score());
        }

        // 3. Calculate fee
        BigDecimal fee = feeCalculator.calculateTransferFee(command.amount());
        BigDecimal totalDebit = command.amount().add(fee);

        // 4. Create pending transaction
        Transaction transaction = Transaction.createInternalTransfer(
                command.idempotencyKey(), command.sourceWalletId(), command.destinationWalletId(),
                command.amount(), fee, command.currency(), command.description(), command.externalReference(), userId);

        transaction = transactionRepository.save(transaction.markProcessing());

        try {
            // 5. Debit source (includes fee)
            walletClient.debit(command.sourceWalletId(), transaction.id(), totalDebit,
                    "Transfer to " + command.destinationWalletId());

            // 6. Credit destination
            walletClient.credit(command.destinationWalletId(), transaction.id(), command.amount(),
                    "Transfer from " + command.sourceWalletId());

            // 7. Complete transaction
            transaction = transactionRepository.save(transaction.complete());

            TransferResult result = TransferResult.from(transaction);
            idempotencyService.storeResponse(command.idempotencyKey(), result);
            eventPublisher.publishTransactionCompleted(transaction);

            return result;
        } catch (Exception e) {
            transactionRepository.save(transaction.fail(e.getMessage()));
            throw e;
        }
    }

    public record TransferCommand(
            UUID idempotencyKey,
            UUID sourceWalletId,
            UUID destinationWalletId,
            BigDecimal amount,
            String currency,
            String externalReference,
            String description
    ) {}

    public record TransferResult(
            UUID transactionId,
            String status,
            String amount,
            String fee,
            String totalDebited,
            String externalReference,
            Instant completedAt
    ) {
        static TransferResult from(Transaction t) {
            return new TransferResult(t.id(), t.status().name(), t.amount().toPlainString(),
                    t.feeAmount().toPlainString(),
                    t.amount().add(t.feeAmount()).toPlainString(), t.externalReference(), t.completedAt());
        }
    }

    public static class DuplicateTransactionException extends RuntimeException {
        public DuplicateTransactionException(UUID key) {
            super("Transaction already in progress or completed for key: " + key);
        }
    }

    public static class FraudBlockedException extends RuntimeException {
        public FraudBlockedException(int score) {
            super("Transaction blocked by fraud detection. Score: " + score);
        }
    }
}
