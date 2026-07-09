package com.villagesat.compliance.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record Screening(
        UUID id,
        UUID userId,
        UUID kycSubmissionId,
        ScreeningType screeningType,
        ScreeningResult result,
        String provider,
        Map<String, Object> details,
        Instant screenedAt
) {
    public enum ScreeningType { PEP, SANCTIONS, ADVERSE_MEDIA }
    public enum ScreeningResult { CLEAR, MATCH, REVIEW }
}
