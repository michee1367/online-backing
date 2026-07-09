package com.villagesat.compliance.application.service;

import com.villagesat.compliance.domain.model.KycSubmission;
import com.villagesat.compliance.domain.port.in.KycReviewUseCase;
import com.villagesat.compliance.domain.port.out.KycEventPublisher;
import com.villagesat.compliance.domain.port.out.KycSubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class KycReviewService implements KycReviewUseCase {

    private final KycSubmissionRepository submissionRepository;
    private final KycEventPublisher eventPublisher;

    public KycReviewService(KycSubmissionRepository submissionRepository,
                            KycEventPublisher eventPublisher) {
        this.submissionRepository = submissionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public KycSubmission approve(UUID submissionId, UUID reviewerId, String notes) {
        KycSubmission submission = getReviewableSubmission(submissionId);
        KycSubmission approved = submissionRepository.save(
                submission.approve(reviewerId, notes), null);
        eventPublisher.publishApproved(approved);
        return approved;
    }

    @Override
    public KycSubmission reject(UUID submissionId, UUID reviewerId, String notes) {
        KycSubmission submission = getReviewableSubmission(submissionId);
        KycSubmission rejected = submissionRepository.save(
                submission.reject(reviewerId, notes), null);
        eventPublisher.publishRejected(rejected);
        return rejected;
    }

    @Override
    @Transactional(readOnly = true)
    public List<KycSubmission> listPendingReview() {
        return submissionRepository.findByStatus(KycSubmission.KycStatus.IN_REVIEW);
    }

    private KycSubmission getReviewableSubmission(UUID submissionId) {
        KycSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new KycService.ComplianceException("KYC_NOT_FOUND", "Soumission KYC introuvable"));

        if (submission.status() != KycSubmission.KycStatus.IN_REVIEW
                && submission.status() != KycSubmission.KycStatus.PENDING) {
            throw new KycService.ComplianceException("KYC_INVALID_STATUS",
                    "Cette soumission ne peut plus être revue (statut: " + submission.status() + ")");
        }
        return submission;
    }
}
