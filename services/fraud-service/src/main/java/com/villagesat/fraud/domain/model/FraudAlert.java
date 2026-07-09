package com.villagesat.fraud.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FraudAlert(
        UUID id,
        UUID userId,
        UUID transactionId,
        int score,
        FraudAction action,
        List<String> reasons,
        List<String> rulesFired,
        AlertStatus status,
        Instant createdAt,
        Instant resolvedAt,
        UUID resolvedBy,
        String resolutionNote,
        long version
) {
    public FraudAlert resolve(AlertStatus resolution, UUID resolver, String note) {
        return new FraudAlert(
                id, userId, transactionId, score, action, reasons, rulesFired,
                resolution, createdAt, Instant.now(), resolver, note, version
        );
    }

    public static FraudAlert create(UUID userId, UUID transactionId, FraudScoreResult result) {
        return new FraudAlert(
                UUID.randomUUID(), userId, transactionId,
                result.score(), result.action(), result.reasons(), result.rulesFired(),
                AlertStatus.OPEN, Instant.now(), null, null, null, 0L
        );
    }
}
