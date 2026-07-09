package com.villagesat.user.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record UserProfile(
        UUID userId,
        LocalDate dateOfBirth,
        String addressLine1,
        String addressCity,
        String addressCountry,
        String preferredLanguage,
        String timezone,
        String avatarUrl,
        Map<String, Object> metadata,
        Instant updatedAt
) {
    public static UserProfile defaultProfile(UUID userId) {
        return new UserProfile(userId, null, null, null, null, "fr",
                "Africa/Kinshasa", null, Map.of(), Instant.now());
    }
}
