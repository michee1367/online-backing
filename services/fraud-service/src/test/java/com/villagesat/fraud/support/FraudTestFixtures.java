package com.villagesat.fraud.support;

import com.villagesat.fraud.domain.model.FraudScoreRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

public final class FraudTestFixtures {

    public static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID WALLET_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private FraudTestFixtures() {}

    public static FraudScoreRequest lowAmountRequest() {
        return new FraudScoreRequest(
                USER_ID, WALLET_ID, new BigDecimal("500"), "CDF",
                "192.168.1.1", "device-1",
                ZonedDateTime.of(2026, 5, 26, 10, 0, 0, 0, ZoneOffset.UTC).toInstant()
        );
    }

    public static FraudScoreRequest highAmountRequest() {
        return new FraudScoreRequest(
                USER_ID, WALLET_ID, new BigDecimal("15000"), "CDF",
                "192.168.1.1", "device-1",
                ZonedDateTime.of(2026, 5, 26, 10, 0, 0, 0, ZoneOffset.UTC).toInstant()
        );
    }

    public static FraudScoreRequest largeTransferRequest() {
        return new FraudScoreRequest(
                USER_ID, WALLET_ID, new BigDecimal("75000"), "USD",
                "10.0.0.1", "device-2",
                ZonedDateTime.of(2026, 5, 26, 2, 30, 0, 0, ZoneOffset.UTC).toInstant()
        );
    }

    public static FraudScoreRequest crossBorderRequest() {
        return new FraudScoreRequest(
                USER_ID, WALLET_ID, new BigDecimal("500"), "USD",
                "192.168.1.1", "device-1",
                ZonedDateTime.of(2026, 5, 26, 10, 0, 0, 0, ZoneOffset.UTC).toInstant()
        );
    }

    public static FraudScoreRequest unusualHourRequest() {
        return new FraudScoreRequest(
                USER_ID, WALLET_ID, new BigDecimal("500"), "CDF",
                "192.168.1.1", "device-1",
                ZonedDateTime.of(2026, 5, 26, 3, 0, 0, 0, ZoneOffset.UTC).toInstant()
        );
    }
}
