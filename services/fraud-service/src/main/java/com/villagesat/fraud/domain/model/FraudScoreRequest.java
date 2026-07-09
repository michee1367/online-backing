package com.villagesat.fraud.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FraudScoreRequest(
        UUID userId,
        UUID walletId,
        BigDecimal amount,
        String currency,
        String ipAddress,
        String deviceId,
        Instant timestamp
) {
    public FraudScoreRequest {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}
