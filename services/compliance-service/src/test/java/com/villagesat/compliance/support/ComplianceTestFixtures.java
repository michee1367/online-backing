package com.villagesat.compliance.support;

import com.villagesat.compliance.domain.model.KycSubmission;
import com.villagesat.compliance.domain.model.Screening;
import com.villagesat.compliance.domain.port.in.KycUseCase.SubmitKycCommand;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ComplianceTestFixtures {

    public static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    public static final UUID REVIEWER_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440000");
    public static final UUID SUBMISSION_ID = UUID.fromString("770e8400-e29b-41d4-a716-446655440000");

    private ComplianceTestFixtures() {}

    public static SubmitKycCommand submitCommand(int level) {
        return new SubmitKycCommand(
                USER_ID, level, "NATIONAL_ID", "DOC-123456",
                "front-key", "back-key", "selfie-key"
        );
    }

    public static KycSubmission submission(KycSubmission.KycStatus status, int level, BigDecimal riskScore) {
        return new KycSubmission(
                SUBMISSION_ID, USER_ID, level,
                KycSubmission.DocumentType.NATIONAL_ID,
                "front-key", "back-key", "selfie-key",
                status, null, null, "sim-provider",
                riskScore, Instant.now(),
                status == KycSubmission.KycStatus.APPROVED || status == KycSubmission.KycStatus.REJECTED
                        ? Instant.now() : null
        );
    }

    public static KycSubmission pendingSubmission() {
        return submission(KycSubmission.KycStatus.PENDING, 1, new BigDecimal("0.60"));
    }

    public static KycSubmission inReviewSubmission() {
        return submission(KycSubmission.KycStatus.IN_REVIEW, 1, new BigDecimal("0.80"));
    }

    public static KycSubmission approvedSubmission(int level) {
        return submission(KycSubmission.KycStatus.APPROVED, level, new BigDecimal("0.97"));
    }

    public static List<Screening> clearScreenings() {
        Instant now = Instant.now();
        return List.of(
                new Screening(UUID.randomUUID(), USER_ID, null,
                        Screening.ScreeningType.PEP, Screening.ScreeningResult.CLEAR,
                        "test-provider", Map.of(), now),
                new Screening(UUID.randomUUID(), USER_ID, null,
                        Screening.ScreeningType.SANCTIONS, Screening.ScreeningResult.CLEAR,
                        "test-provider", Map.of(), now),
                new Screening(UUID.randomUUID(), USER_ID, null,
                        Screening.ScreeningType.ADVERSE_MEDIA, Screening.ScreeningResult.CLEAR,
                        "test-provider", Map.of(), now)
        );
    }

    public static List<Screening> sanctionsMatchScreenings() {
        Instant now = Instant.now();
        return List.of(
                new Screening(UUID.randomUUID(), USER_ID, null,
                        Screening.ScreeningType.PEP, Screening.ScreeningResult.CLEAR,
                        "test-provider", Map.of(), now),
                new Screening(UUID.randomUUID(), USER_ID, null,
                        Screening.ScreeningType.SANCTIONS, Screening.ScreeningResult.MATCH,
                        "test-provider", Map.of(), now),
                new Screening(UUID.randomUUID(), USER_ID, null,
                        Screening.ScreeningType.ADVERSE_MEDIA, Screening.ScreeningResult.CLEAR,
                        "test-provider", Map.of(), now)
        );
    }
}
