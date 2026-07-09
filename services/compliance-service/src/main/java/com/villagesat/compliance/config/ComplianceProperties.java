package com.villagesat.compliance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "villagesat.compliance")
public record ComplianceProperties(
        String encryptionKey,
        BigDecimal autoApproveThreshold,
        BigDecimal manualReviewThreshold,
        BigDecimal autoRejectThreshold
) {}
