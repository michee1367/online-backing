package com.villagesat.user.domain.model;

import java.time.Instant;
import java.util.UUID;

public record User(
        UUID id,
        String email,
        String phone,
        String firstName,
        String lastName,
        String countryCode,
        int kycLevel,
        UserStatus status,
        UUID tenantId,
        UUID keycloakId,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        long version
) {
    public enum UserStatus {
        PENDING_VERIFICATION, ACTIVE, SUSPENDED, FROZEN, CLOSED
    }

    public boolean isActive() {
        return deletedAt == null && status != UserStatus.CLOSED;
    }

    public User withProfileNames(String firstName, String lastName) {
        return new User(id, email, phone, firstName, lastName, countryCode, kycLevel, status,
                tenantId, keycloakId, createdAt, Instant.now(), deletedAt, version);
    }

    public User withKycLevel(int kycLevel) {
        return new User(id, email, phone, firstName, lastName, countryCode, kycLevel, status,
                tenantId, keycloakId, createdAt, Instant.now(), deletedAt, version);
    }

    public User withStatus(UserStatus status) {
        return new User(id, email, phone, firstName, lastName, countryCode, kycLevel, status,
                tenantId, keycloakId, createdAt, Instant.now(), deletedAt, version);
    }
}
