package com.villagesat.auth.domain.model;

public record AuthTokens(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType
) {}
