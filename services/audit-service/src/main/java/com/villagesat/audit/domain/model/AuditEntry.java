package com.villagesat.audit.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class AuditEntry {

    private UUID id;
    private String serviceName;
    private String eventType;
    private UUID userId;
    private String entityType;
    private String entityId;
    private String action;
    private String oldValue;
    private String newValue;
    private String ipAddress;
    private Instant timestamp;
    private Map<String, String> metadata;

    public AuditEntry() {}

    public AuditEntry(UUID id, String serviceName, String eventType, UUID userId, String entityType,
                      String entityId, String action, String oldValue, String newValue,
                      String ipAddress, Instant timestamp, Map<String, String> metadata) {
        this.id = id;
        this.serviceName = serviceName;
        this.eventType = eventType;
        this.userId = userId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.ipAddress = ipAddress;
        this.timestamp = timestamp;
        this.metadata = metadata;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }
    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
}
