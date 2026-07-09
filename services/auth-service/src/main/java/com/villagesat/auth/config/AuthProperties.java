package com.villagesat.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "villagesat.auth")
public record AuthProperties(
        String mfaIssuer,
        String encryptionKey,
        int sessionRefreshDays,
        int maxSessionsPerUser,
        int loginMaxAttempts,
        int loginLockMinutes
) {}
