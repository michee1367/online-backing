package com.villagesat.compliance.application.service;

import com.villagesat.compliance.config.ComplianceProperties;
import com.villagesat.compliance.domain.model.*;
import com.villagesat.compliance.domain.port.in.KycUseCase;
import com.villagesat.compliance.domain.port.out.*;
import com.villagesat.compliance.adapter.out.crypto.DocumentEncryptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class KycService implements KycUseCase {

    private final KycSubmissionRepository submissionRepository;
    private final AmlScreeningPort amlScreening;
    private final IdentityVerificationPort identityVerification;
    private final KycEventPublisher eventPublisher;
    private final DocumentEncryptionService encryptionService;
    private final ComplianceProperties properties;

    public KycService(KycSubmissionRepository submissionRepository,
                      AmlScreeningPort amlScreening,
                      IdentityVerificationPort identityVerification,
                      KycEventPublisher eventPublisher,
                      DocumentEncryptionService encryptionService,
                      ComplianceProperties properties) {
        this.submissionRepository = submissionRepository;
        this.amlScreening = amlScreening;
        this.identityVerification = identityVerification;
        this.eventPublisher = eventPublisher;
        this.encryptionService = encryptionService;
        this.properties = properties;
    }

    @Override
    public KycSubmission submit(SubmitKycCommand command) {
        validateLevel(command.level());

        if (submissionRepository.hasPendingSubmission(command.userId(), command.level())) {
            throw new ComplianceException("KYC_PENDING_EXISTS",
                    "Une demande KYC est déjà en cours pour ce niveau");
        }

        KycSubmission.KycStatus initialStatus = KycSubmission.KycStatus.PENDING;
        BigDecimal riskScore = BigDecimal.ZERO;
        String providerRef = null;

        var screenings = amlScreening.screenUser(command.userId(), null, command.documentNumber());
        boolean sanctionsMatch = screenings.stream()
                .anyMatch(s -> s.screeningType() == Screening.ScreeningType.SANCTIONS
                        && s.result() == Screening.ScreeningResult.MATCH);

        if (sanctionsMatch) {
            KycSubmission rejected = persistSubmission(command, initialStatus, riskScore, providerRef,
                    KycSubmission.KycStatus.REJECTED, "Rejet automatique — correspondance liste sanctions");
            eventPublisher.publishRejected(rejected);
            throw new ComplianceException("KYC_SANCTIONS_MATCH",
                    "Vérification impossible : correspondance liste de sanctions");
        }

        var verification = identityVerification.verify(new IdentityVerificationPort.VerificationRequest(
                command.userId(), command.documentType(),
                command.documentFrontKey(), command.selfieKey()));

        riskScore = verification.score();
        providerRef = verification.providerRef();

        KycSubmission submission = persistSubmission(command, initialStatus, riskScore, providerRef,
                determineStatus(riskScore, verification.documentValid()), null);

        eventPublisher.publishSubmitted(submission);

        if (submission.status() == KycSubmission.KycStatus.APPROVED) {
            eventPublisher.publishApproved(submission);
        } else if (submission.status() == KycSubmission.KycStatus.REJECTED) {
            eventPublisher.publishRejected(submission);
        }

        return submission;
    }

    @Override
    @Transactional(readOnly = true)
    public KycStatusSummary getStatus(UUID userId) {
        var approved = submissionRepository.findLatestApprovedByUserId(userId);
        if (approved.isPresent()) {
            KycSubmission s = approved.get();
            return new KycStatusSummary(
                    s.targetLevel(),
                    s.status().name(),
                    s.reviewedAt(),
                    KycLimits.forLevel(s.targetLevel()),
                    s.reviewNotes()
            );
        }

        var latest = submissionRepository.findLatestByUserId(userId);
        if (latest.isPresent()) {
            KycSubmission s = latest.get();
            int effectiveLevel = s.status() == KycSubmission.KycStatus.APPROVED ? s.targetLevel() : 0;
            return new KycStatusSummary(
                    effectiveLevel,
                    s.status().name(),
                    s.reviewedAt(),
                    KycLimits.forLevel(effectiveLevel),
                    s.reviewNotes()
            );
        }

        return new KycStatusSummary(0, "NOT_SUBMITTED", null, KycLimits.forLevel(0), null);
    }

    private KycSubmission persistSubmission(SubmitKycCommand command, KycSubmission.KycStatus ignored,
                                            BigDecimal riskScore, String providerRef,
                                            KycSubmission.KycStatus status, String reviewNotes) {
        KycSubmission submission = new KycSubmission(
                UUID.randomUUID(),
                command.userId(),
                command.level(),
                KycSubmission.DocumentType.valueOf(command.documentType()),
                command.documentFrontKey(),
                command.documentBackKey(),
                command.selfieKey(),
                status,
                reviewNotes,
                null,
                providerRef,
                riskScore,
                Instant.now(),
                status == KycSubmission.KycStatus.APPROVED || status == KycSubmission.KycStatus.REJECTED
                        ? Instant.now() : null
        );

        byte[] encryptedDoc = command.documentNumber() != null
                ? encryptionService.encrypt(command.documentNumber().getBytes())
                : null;

        return submissionRepository.save(submission, encryptedDoc);
    }

    private KycSubmission.KycStatus determineStatus(BigDecimal score, boolean documentValid) {
        if (!documentValid || score.compareTo(properties.autoRejectThreshold()) < 0) {
            return KycSubmission.KycStatus.REJECTED;
        }
        if (score.compareTo(properties.autoApproveThreshold()) >= 0) {
            return KycSubmission.KycStatus.APPROVED;
        }
        if (score.compareTo(properties.manualReviewThreshold()) >= 0) {
            return KycSubmission.KycStatus.IN_REVIEW;
        }
        return KycSubmission.KycStatus.PENDING;
    }

    private void validateLevel(int level) {
        if (level < 1 || level > 3) {
            throw new ComplianceException("KYC_INVALID_LEVEL", "Le niveau KYC doit être entre 1 et 3");
        }
    }

    public static class ComplianceException extends RuntimeException {
        private final String code;

        public ComplianceException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() { return code; }
    }
}
