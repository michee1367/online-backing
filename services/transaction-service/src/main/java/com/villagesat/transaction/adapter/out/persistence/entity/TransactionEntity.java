package com.villagesat.transaction.adapter.out.persistence.entity;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions", schema = "transactions")
public class TransactionEntity implements Persistable<TransactionId> {

    @EmbeddedId
    private TransactionId id;

    @Transient
    private boolean newEntity = true;

    @Column(name = "idempotency_key", nullable = false)
    private UUID idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionTypeEntity type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatusEntity status;

    @Column(name = "source_wallet_id")
    private UUID sourceWalletId;

    @Column(name = "dest_wallet_id")
    private UUID destWalletId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "fee_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal feeAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 255)
    private String description;

    @Column(name = "external_reference", length = 255)
    private String externalReference;

    @Column(name = "initiated_by", nullable = false)
    private UUID initiatedBy;

    @Column(name = "fraud_score")
    private Short fraudScore;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failed_reason", length = 255)
    private String failedReason;

    @Version
    private Long version;

    public enum TransactionTypeEntity {
        INTERNAL_TRANSFER, EXTERNAL_TRANSFER, DEPOSIT, WITHDRAWAL, PAYMENT, FEE
    }

    public enum TransactionStatusEntity {
        PENDING, PROCESSING, COMPLETED, FAILED, REVERSED, CANCELLED
    }

    @PrePersist
    void onCreate() {
        if (id != null && id.getCreatedAt() == null) {
            id.setCreatedAt(Instant.now());
        }
    }

    @PostPersist
    @PostLoad
    void markPersisted() {
        newEntity = false;
    }

    @Override
    public TransactionId getId() { return id; }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    public void setNewEntity(boolean newEntity) { this.newEntity = newEntity; }
    public void setId(TransactionId id) { this.id = id; }
    public UUID getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(UUID idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public TransactionTypeEntity getType() { return type; }
    public void setType(TransactionTypeEntity type) { this.type = type; }
    public TransactionStatusEntity getStatus() { return status; }
    public void setStatus(TransactionStatusEntity status) { this.status = status; }
    public UUID getSourceWalletId() { return sourceWalletId; }
    public void setSourceWalletId(UUID sourceWalletId) { this.sourceWalletId = sourceWalletId; }
    public UUID getDestWalletId() { return destWalletId; }
    public void setDestWalletId(UUID destWalletId) { this.destWalletId = destWalletId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getExternalReference() { return externalReference; }
    public void setExternalReference(String externalReference) { this.externalReference = description; }
    public UUID getInitiatedBy() { return initiatedBy; }
    public void setInitiatedBy(UUID initiatedBy) { this.initiatedBy = initiatedBy; }
    public Short getFraudScore() { return fraudScore; }
    public void setFraudScore(Short fraudScore) { this.fraudScore = fraudScore; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public String getFailedReason() { return failedReason; }
    public void setFailedReason(String failedReason) { this.failedReason = failedReason; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
