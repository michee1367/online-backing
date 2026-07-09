package com.villagesat.transaction.adapter.out.persistence.mapper;

import com.villagesat.transaction.adapter.out.persistence.entity.TransactionEntity;
import com.villagesat.transaction.adapter.out.persistence.entity.TransactionId;
import com.villagesat.transaction.domain.model.Transaction;

public final class TransactionMapper {

    private TransactionMapper() {}

    public static Transaction toDomain(TransactionEntity entity) {
        return new Transaction(
                entity.getId().getId(),
                entity.getIdempotencyKey(),
                Transaction.TransactionType.valueOf(entity.getType().name()),
                Transaction.TransactionStatus.valueOf(entity.getStatus().name()),
                entity.getSourceWalletId(),
                entity.getDestWalletId(),
                entity.getAmount(),
                entity.getFeeAmount(),
                entity.getCurrency(),
                entity.getDescription(),
                entity.getInitiatedBy(),
                entity.getFraudScore() != null ? entity.getFraudScore().intValue() : null,
                entity.getId().getCreatedAt(),
                entity.getCompletedAt(),
                entity.getFailedReason(),
                entity.getVersion() != null ? entity.getVersion() : 0L
        );
    }

    public static TransactionEntity toEntity(Transaction transaction) {
        TransactionEntity entity = new TransactionEntity();
        entity.setId(new TransactionId(transaction.id(), transaction.createdAt()));
        entity.setIdempotencyKey(transaction.idempotencyKey());
        entity.setType(TransactionEntity.TransactionTypeEntity.valueOf(transaction.type().name()));
        entity.setStatus(TransactionEntity.TransactionStatusEntity.valueOf(transaction.status().name()));
        entity.setSourceWalletId(transaction.sourceWalletId());
        entity.setDestWalletId(transaction.destWalletId());
        entity.setAmount(transaction.amount());
        entity.setFeeAmount(transaction.feeAmount());
        entity.setCurrency(transaction.currency());
        entity.setDescription(transaction.description());
        entity.setInitiatedBy(transaction.initiatedBy());
        if (transaction.fraudScore() != null) {
            entity.setFraudScore(transaction.fraudScore().shortValue());
        }
        entity.setCompletedAt(transaction.completedAt());
        entity.setFailedReason(transaction.failedReason());
        if (transaction.version() > 0) {
            entity.setVersion(transaction.version());
            entity.setNewEntity(false);
        }
        return entity;
    }

    public static void updateEntity(TransactionEntity entity, Transaction transaction) {
        entity.setStatus(TransactionEntity.TransactionStatusEntity.valueOf(transaction.status().name()));
        entity.setSourceWalletId(transaction.sourceWalletId());
        entity.setDestWalletId(transaction.destWalletId());
        entity.setAmount(transaction.amount());
        entity.setFeeAmount(transaction.feeAmount());
        entity.setCurrency(transaction.currency());
        entity.setDescription(transaction.description());
        entity.setInitiatedBy(transaction.initiatedBy());
        if (transaction.fraudScore() != null) {
            entity.setFraudScore(transaction.fraudScore().shortValue());
        }
        entity.setCompletedAt(transaction.completedAt());
        entity.setFailedReason(transaction.failedReason());
    }
}
