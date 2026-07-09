package com.villagesat.wallet.application.service;

import com.villagesat.wallet.domain.model.Balance;
import com.villagesat.wallet.domain.model.InsufficientFundsException;
import com.villagesat.wallet.domain.model.Wallet;
import com.villagesat.wallet.domain.port.in.BalanceUseCase;
import com.villagesat.wallet.domain.port.out.BalanceRepository;
import com.villagesat.wallet.domain.port.out.LedgerRepository;
import com.villagesat.wallet.domain.port.out.TransactionLimitPort;
import com.villagesat.wallet.domain.port.out.WalletEventPublisher;
import com.villagesat.wallet.domain.port.out.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Transactional
public class BalanceService implements BalanceUseCase {

    private final WalletRepository walletRepository;
    private final BalanceRepository balanceRepository;
    private final LedgerRepository ledgerRepository;
    private final TransactionLimitPort transactionLimitPort;
    private final WalletEventPublisher eventPublisher;

    public BalanceService(WalletRepository walletRepository,
                          BalanceRepository balanceRepository,
                          LedgerRepository ledgerRepository,
                          TransactionLimitPort transactionLimitPort,
                          WalletEventPublisher eventPublisher) {
        this.walletRepository = walletRepository;
        this.balanceRepository = balanceRepository;
        this.ledgerRepository = ledgerRepository;
        this.transactionLimitPort = transactionLimitPort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public Balance getBalance(UUID walletId, UUID userId) {
        walletRepository.findByIdAndUserId(walletId, userId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));
        return balanceRepository.findByWalletId(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Balance debit(UUID walletId, UUID transactionId, BigDecimal amount, String description) {
        // 💡 ÉTAPE 1 : Sécurité Idempotence
        // On vérifie si cette transaction spécifique a déjà été traitée par le passé.
        if (ledgerRepository.existsByTransactionIdAndEntryType(transactionId, "DEBIT")) {
            // Si oui, le réseau a coupé au retour lors de la tentative précédente.
            // On ne fait rien, on renvoie juste le solde actuel de manière sécurisée.
            return balanceRepository.findByWalletId(walletId)
                    .orElseThrow(() -> new WalletNotFoundException(walletId));
        }
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));

        if (!wallet.isOperational()) {
            throw new WalletFrozenException(walletId);
        }

        transactionLimitPort.validateDebitWithinLimits(wallet, transactionId, amount);

        Balance balance = balanceRepository.findByWalletIdForUpdate(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));

        Balance updated;
        try {
            updated = balance.debit(amount);
        } catch (InsufficientFundsException e) {
            throw e;
        }

        balanceRepository.save(updated);
        ledgerRepository.appendEntry(walletId, transactionId, "DEBIT", amount,
                updated.balance(), description);
        eventPublisher.publishBalanceUpdated(walletId, updated);
        return updated;
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Balance credit(UUID walletId, UUID transactionId, BigDecimal amount, String description) {
        // 💡 ÉTAPE 1 : Sécurité Idempotence pour le crédit
        if (ledgerRepository.existsByTransactionIdAndEntryType(transactionId, "CREDIT")) {
            return balanceRepository.findByWalletId(walletId)
                    .orElseThrow(() -> new WalletNotFoundException(walletId));
        }
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));

        if (wallet.status() == Wallet.WalletStatus.CLOSED) {
            throw new WalletClosedException(walletId);
        }

        Balance balance = balanceRepository.findByWalletIdForUpdate(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));

        Balance updated = balance.credit(amount);
        balanceRepository.save(updated);
        ledgerRepository.appendEntry(walletId, transactionId, "CREDIT", amount,
                updated.balance(), description);
        eventPublisher.publishBalanceUpdated(walletId, updated);
        return updated;
    }

    public static class WalletNotFoundException extends RuntimeException {
        public WalletNotFoundException(UUID walletId) {
            super("Wallet not found: " + walletId);
        }
    }

    public static class WalletFrozenException extends RuntimeException {
        public WalletFrozenException(UUID walletId) {
            super("Wallet is frozen: " + walletId);
        }
    }

    public static class WalletClosedException extends RuntimeException {
        public WalletClosedException(UUID walletId) {
            super("Wallet is closed: " + walletId);
        }
    }
}
