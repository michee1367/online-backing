package com.villagesat.transaction.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Transaction(
        UUID id,
        UUID idempotencyKey,
        TransactionType type,
        TransactionStatus status,
        UUID sourceWalletId,
        UUID destWalletId,
        BigDecimal amount,
        BigDecimal feeAmount,
        String currency,
        String description,
        String externalReference,
        UUID initiatedBy,
        Integer fraudScore,
        Instant createdAt,
        Instant completedAt,
        String failedReason,
        long version
) {
    public enum TransactionType {
        INTERNAL_TRANSFER, EXTERNAL_TRANSFER, DEPOSIT, WITHDRAWAL, PAYMENT, FEE
    }

    public enum TransactionStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, REVERSED, CANCELLED
    }

    public static Transaction createInternalTransfer(
            UUID idempotencyKey, UUID sourceWalletId, UUID destWalletId,
            BigDecimal amount, BigDecimal fee, String currency,
            String description, String externalReference, UUID initiatedBy) {
        return new Transaction(
                UUID.randomUUID(), idempotencyKey, TransactionType.INTERNAL_TRANSFER,
                TransactionStatus.PENDING, sourceWalletId, destWalletId,
                amount, fee, currency, description, externalReference , initiatedBy,
                null, Instant.now(), null, null, 0L);
    }

    public Transaction markProcessing() {
        return new Transaction(id, idempotencyKey, type, TransactionStatus.PROCESSING,
                sourceWalletId, destWalletId, amount, feeAmount, currency, description,externalReference,
                initiatedBy, fraudScore, createdAt, completedAt, failedReason, version);
    }

    public Transaction complete() {
        return new Transaction(id, idempotencyKey, type, TransactionStatus.COMPLETED,
                sourceWalletId, destWalletId, amount, feeAmount, currency, description, externalReference,
                initiatedBy, fraudScore, createdAt, Instant.now(), failedReason, version);
    }

    public Transaction fail(String reason) {
        return new Transaction(id, idempotencyKey, type, TransactionStatus.FAILED,
                sourceWalletId, destWalletId, amount, feeAmount, currency, description, externalReference,
                initiatedBy, fraudScore, createdAt, Instant.now(), reason, version);
    }
}
