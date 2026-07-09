package com.villagesat.wallet.support;

import com.villagesat.wallet.domain.model.Wallet;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class WalletTestFixtures {

    public static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    private WalletTestFixtures() {}

    public static Wallet wallet(UUID id, int kycLevel, BigDecimal daily, BigDecimal monthly) {
        return new Wallet(
                id,
                USER_ID,
                "123456",
                "CDF",
                Wallet.WalletType.PERSONAL,
                "Test",
                Wallet.WalletStatus.ACTIVE,
                kycLevel,
                daily,
                monthly,
                Instant.parse("2025-01-01T00:00:00Z"),
                0L
        );
    }

    public static Wallet l0Wallet(UUID id) {
        return wallet(id, 0, new BigDecimal("200000"), new BigDecimal("2000000"));
    }
}
