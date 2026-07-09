package com.villagesat.wallet.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Agrégat Wallet — entité domaine pure, sans dépendance framework.
 */
public record Wallet(
        UUID id,
        UUID userId,
        String accountNumber,
        String currency,
        WalletType type,
        String label,
        WalletStatus status,
        int kycLevel,
        BigDecimal dailyLimit,
        BigDecimal monthlyLimit,
        Instant createdAt,
        long version
) {
    public enum WalletType { PERSONAL, BUSINESS, ESCROW, SAVINGS }
    public enum WalletStatus { ACTIVE, FROZEN, CLOSED }

    public Wallet freeze() {
        if (status == WalletStatus.CLOSED) {
            throw new IllegalStateException("Cannot freeze a closed wallet");
        }
        return new Wallet(id, userId, accountNumber, currency, type, label,
                WalletStatus.FROZEN, kycLevel, dailyLimit, monthlyLimit, createdAt, version);
    }

    public boolean isOperational() {
        return status == WalletStatus.ACTIVE;
    }

    /** Applique les plafonds transactionnels alignés sur le niveau KYC. */
    public Wallet applyKycLevel(int level) {
        WalletKycLimits limits = WalletKycLimits.forLevel(level);
        return new Wallet(id, userId, accountNumber, currency, type, label, status, level,
                limits.dailyLimit(), limits.monthlyLimit(), createdAt, version);
    }
}
