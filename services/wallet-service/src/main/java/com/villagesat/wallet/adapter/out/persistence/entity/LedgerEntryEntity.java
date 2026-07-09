package com.villagesat.wallet.adapter.out.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries", schema = "wallets")
public class LedgerEntryEntity implements Persistable<LedgerEntryId> {

    @EmbeddedId
    private LedgerEntryId id;

    @Transient
    private boolean newEntity = true;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "entry_type", nullable = false, length = 10)
    private String entryType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "entry_sequence", insertable = false, updatable = false)
    private Long entrySequence;

    @Column(length = 255)
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = new LedgerEntryId(UUID.randomUUID(), Instant.now());
        } else if (id.getCreatedAt() == null) {
            id.setCreatedAt(Instant.now());
        }
    }

    @PostPersist
    @PostLoad
    void markPersisted() {
        newEntity = false;
    }

    @Override
    public LedgerEntryId getId() { return id; }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    public void setId(LedgerEntryId id) { this.id = id; }
    public UUID getWalletId() { return walletId; }
    public void setWalletId(UUID walletId) { this.walletId = walletId; }
    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }
    public String getEntryType() { return entryType; }
    public void setEntryType(String entryType) { this.entryType = entryType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
    public Long getEntrySequence() { return entrySequence; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
