package com.villagesat.compliance.domain.model;

import java.time.Instant;

public record KycStatusSummary(
        int level,
        String status,
        Instant approvedAt,
        KycLimits limits,
        String reviewNotes
) {}
