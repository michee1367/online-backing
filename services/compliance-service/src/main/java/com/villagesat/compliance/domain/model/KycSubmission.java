package com.villagesat.compliance.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record KycSubmission(
        UUID id,
        UUID userId,
        int targetLevel,
        DocumentType documentType,
        String documentFrontKey,
        String documentBackKey,
        String selfieKey,
        KycStatus status,
        String reviewNotes,
        UUID reviewedBy,
        String providerRef,
        BigDecimal riskScore,
        Instant submittedAt,
        Instant reviewedAt
) {
    public enum DocumentType {
        NATIONAL_ID, PASSPORT, DRIVERS_LICENSE, RESIDENCE_PERMIT
    }

    public enum KycStatus {
        PENDING, IN_REVIEW, APPROVED, REJECTED, EXPIRED
    }

    public KycSubmission approve(UUID reviewerId, String notes) {
        return new KycSubmission(id, userId, targetLevel, documentType, documentFrontKey,
                documentBackKey, selfieKey, KycStatus.APPROVED, notes, reviewerId, providerRef,
                riskScore, submittedAt, Instant.now());
    }

    public KycSubmission reject(UUID reviewerId, String notes) {
        return new KycSubmission(id, userId, targetLevel, documentType, documentFrontKey,
                documentBackKey, selfieKey, KycStatus.REJECTED, notes, reviewerId, providerRef,
                riskScore, submittedAt, Instant.now());
    }

    public KycSubmission withStatus(KycStatus status, BigDecimal riskScore, String providerRef) {
        return new KycSubmission(id, userId, targetLevel, documentType, documentFrontKey,
                documentBackKey, selfieKey, status, reviewNotes, reviewedBy, providerRef,
                riskScore, submittedAt, reviewedAt);
    }
}
