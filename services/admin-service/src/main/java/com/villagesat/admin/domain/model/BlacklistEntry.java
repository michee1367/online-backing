package com.villagesat.admin.domain.model;

import java.time.Instant;
import java.util.UUID;

public class BlacklistEntry {

    private UUID id;
    private EntityType entityType;
    private String entityValue;
    private String reason;
    private UUID createdBy;
    private Instant createdAt;
    private Instant expiresAt;
    private boolean active;

    public BlacklistEntry() {}

    public BlacklistEntry(UUID id, EntityType entityType, String entityValue, String reason, UUID createdBy, Instant createdAt, Instant expiresAt, boolean active) {
        this.id = id;
        this.entityType = entityType;
        this.entityValue = entityValue;
        this.reason = reason;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.active = active;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public EntityType getEntityType() { return entityType; }
    public void setEntityType(EntityType entityType) { this.entityType = entityType; }
    public String getEntityValue() { return entityValue; }
    public void setEntityValue(String entityValue) { this.entityValue = entityValue; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
