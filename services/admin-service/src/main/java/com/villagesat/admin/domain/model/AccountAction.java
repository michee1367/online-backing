package com.villagesat.admin.domain.model;

import java.time.Instant;
import java.util.UUID;

public class AccountAction {

    private UUID id;
    private UUID targetUserId;
    private ActionType actionType;
    private UUID performedBy;
    private String reason;
    private Instant createdAt;

    public AccountAction() {}

    public AccountAction(UUID id, UUID targetUserId, ActionType actionType, UUID performedBy, String reason, Instant createdAt) {
        this.id = id;
        this.targetUserId = targetUserId;
        this.actionType = actionType;
        this.performedBy = performedBy;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTargetUserId() { return targetUserId; }
    public void setTargetUserId(UUID targetUserId) { this.targetUserId = targetUserId; }
    public ActionType getActionType() { return actionType; }
    public void setActionType(ActionType actionType) { this.actionType = actionType; }
    public UUID getPerformedBy() { return performedBy; }
    public void setPerformedBy(UUID performedBy) { this.performedBy = performedBy; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
