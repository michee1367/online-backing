package com.villagesat.banking.adapter.in.web.dto;

import com.villagesat.banking.domain.model.BankTransfer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BankTransferResponse(
        UUID id,
        UUID userId,
        UUID walletId,
        UUID linkedAccountId,
        String transferType,
        BigDecimal amount,
        String currency,
        String status,
        String swiftCode,
        String reference,
        String externalRef,
        Instant createdAt,
        Instant completedAt,
        String failedReason
) {
    public static BankTransferResponse from(BankTransfer transfer) {
        return new BankTransferResponse(
                transfer.getId(), transfer.getUserId(), transfer.getWalletId(),
                transfer.getLinkedAccountId(),
                transfer.getTransferType().name(),
                transfer.getAmount(), transfer.getCurrency(),
                transfer.getStatus().name(), transfer.getSwiftCode(),
                transfer.getReference(), transfer.getExternalRef(),
                transfer.getCreatedAt(), transfer.getCompletedAt(),
                transfer.getFailedReason()
        );
    }
}
