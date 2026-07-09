package com.villagesat.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

public record AuthSession(
        UUID id,
        UUID userId,
        String refreshTokenHash,
        String deviceFingerprint,
        String ipAddress,
        String userAgent,
        boolean mfaVerified,
        Instant expiresAt,
        Instant revokedAt,
        Instant createdAt
) {
    public boolean isActive() {
        return revokedAt == null && expiresAt.isAfter(Instant.now());
    }

    public AuthSession withMfaVerified() {
        return new AuthSession(id, userId, refreshTokenHash, deviceFingerprint, ipAddress,
                userAgent, true, expiresAt, revokedAt, createdAt);
    }

    public AuthSession revoke() {
        return new AuthSession(id, userId, refreshTokenHash, deviceFingerprint, ipAddress,
                userAgent, mfaVerified, expiresAt, Instant.now(), createdAt);
    }
}
