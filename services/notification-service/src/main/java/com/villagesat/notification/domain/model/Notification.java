package com.villagesat.notification.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record Notification(
        UUID id,
        UUID userId,
        Channel channel,
        String templateCode,
        String recipientAddress,
        String subject,
        String body,
        Status status,
        Priority priority,
        Map<String, String> metadata,
        Instant createdAt,
        Instant sentAt,
        String failedReason
) {
    public enum Channel { SMS, EMAIL, PUSH }
    public enum Status { PENDING, SENT, FAILED, DELIVERED }
    public enum Priority { LOW, NORMAL, HIGH, CRITICAL }

    public Notification markSent(Instant at) {
        return new Notification(id, userId, channel, templateCode, recipientAddress, subject,
                body, Status.SENT, priority, metadata, createdAt, at, null);
    }

    public Notification markFailed(String reason) {
        return new Notification(id, userId, channel, templateCode, recipientAddress, subject,
                body, Status.FAILED, priority, metadata, createdAt, null, reason);
    }
}
