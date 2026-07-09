package com.villagesat.wallet.adapter.in.web;

import com.villagesat.wallet.adapter.out.persistence.LedgerEntryJpaRepository;
import com.villagesat.wallet.adapter.out.persistence.entity.LedgerEntryEntity;
import com.villagesat.wallet.adapter.out.transaction.TransactionServiceClient;
import com.villagesat.common.security.SecurityUtils;
import com.villagesat.wallet.domain.model.Wallet;
import com.villagesat.wallet.domain.port.in.WalletUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Wallet Transactions", description = "Historique et transferts P2P (mobile)")
public class WalletTransactionController {

    private final WalletUseCase walletUseCase;
    private final LedgerEntryJpaRepository ledgerRepository;
    private final TransactionServiceClient transactionServiceClient;

    public WalletTransactionController(WalletUseCase walletUseCase,
                                       LedgerEntryJpaRepository ledgerRepository,
                                       TransactionServiceClient transactionServiceClient) {
        this.walletUseCase = walletUseCase;
        this.ledgerRepository = ledgerRepository;
        this.transactionServiceClient = transactionServiceClient;
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Transfert P2P interne", description = "Relais vers transaction-service. Requiert Idempotency-Key.")
    public ResponseEntity<String> transfer(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("Idempotency-Key") java.util.UUID idempotencyKey,
            @RequestBody String body) {
        return transactionServiceClient.forwardTransfer(authorization, idempotencyKey, body);
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Historique des opérations d'un wallet")
    public TransactionPageResponse listByWallet(
            @RequestParam("walletId") UUID walletId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Wallet wallet = walletUseCase.getWallet(walletId, userId);

        int pageSize = Math.min(Math.max(size, 1), 100);
        var pageable = PageRequest.of(Math.max(page, 0), pageSize);
        var entries = ledgerRepository.findByWalletIdOrderByEntrySequenceDesc(walletId, pageable);

        List<TransactionItemResponse> content = entries.getContent().stream()
                .map(e -> toItem(e, wallet.currency()))
                .toList();

        return new TransactionPageResponse(
                content, entries.getNumber(), entries.getSize(), entries.getTotalElements());
    }

    private static TransactionItemResponse toItem(LedgerEntryEntity entry, String currency) {
        Instant createdAt = entry.getId() != null ? entry.getId().getCreatedAt() : Instant.now();
        return new TransactionItemResponse(
                entry.getTransactionId(),
                entry.getWalletId(),
                entry.getEntryType(),
                "P2P",
                entry.getAmount().toPlainString(),
                currency,
                "COMPLETED",
                entry.getDescription(),
                createdAt
        );
    }

    public record TransactionItemResponse(
            UUID transactionId,
            UUID walletId,
            String type,
            String category,
            String amount,
            String currency,
            String status,
            String description,
            Instant createdAt
    ) {}

    public record TransactionPageResponse(
            List<TransactionItemResponse> content,
            int page,
            int size,
            long totalElements
    ) {}
}
