package com.villagesat.admin.domain.model;

import java.time.Instant;
import java.util.UUID;

public class SystemConfig {

    private String key;
    private String value;
    private String description;
    private UUID updatedBy;
    private Instant updatedAt;

    public SystemConfig() {}

    public SystemConfig(String key, String value, String description, UUID updatedBy, Instant updatedAt) {
        this.key = key;
        this.value = value;
        this.description = description;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
