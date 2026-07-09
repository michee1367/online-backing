package com.villagesat.auth.domain.model;

import java.util.UUID;

public record RegisteredUser(
        UUID userId,
        String email,
        int kycLevel,
        String status,
        boolean verificationSent
) {}
