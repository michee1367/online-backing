package com.villagesat.user.domain.model;

import java.time.Instant;
import java.util.UUID;

public record DataExportRequest(
        UUID id,
        UUID userId,
        ExportStatus status,
        String downloadUrl,
        Instant requestedAt,
        Instant completedAt
) {
    public enum ExportStatus { PENDING, PROCESSING, COMPLETED, FAILED }
}
