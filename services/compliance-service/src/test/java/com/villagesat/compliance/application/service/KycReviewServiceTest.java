package com.villagesat.compliance.application.service;

import com.villagesat.compliance.domain.model.KycSubmission;
import com.villagesat.compliance.domain.port.out.KycEventPublisher;
import com.villagesat.compliance.domain.port.out.KycSubmissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.villagesat.compliance.support.ComplianceTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KycReviewServiceTest {

    @Mock
    KycSubmissionRepository submissionRepository;

    @Mock
    KycEventPublisher eventPublisher;

    @InjectMocks
    KycReviewService reviewService;

    @Test
    void approve_inReviewSubmission_setsApprovedAndPublishes() {
        KycSubmission inReview = inReviewSubmission();
        when(submissionRepository.findById(SUBMISSION_ID)).thenReturn(Optional.of(inReview));
        when(submissionRepository.save(any(), isNull())).thenAnswer(inv -> inv.getArgument(0));

        KycSubmission result = reviewService.approve(SUBMISSION_ID, REVIEWER_ID, "Documents conformes");

        assertThat(result.status()).isEqualTo(KycSubmission.KycStatus.APPROVED);
        assertThat(result.reviewedBy()).isEqualTo(REVIEWER_ID);
        assertThat(result.reviewNotes()).isEqualTo("Documents conformes");
        assertThat(result.reviewedAt()).isNotNull();
        verify(eventPublisher).publishApproved(result);
    }

    @Test
    void approve_pendingSubmission_alsoSucceeds() {
        KycSubmission pending = pendingSubmission();
        when(submissionRepository.findById(SUBMISSION_ID)).thenReturn(Optional.of(pending));
        when(submissionRepository.save(any(), isNull())).thenAnswer(inv -> inv.getArgument(0));

        KycSubmission result = reviewService.approve(SUBMISSION_ID, REVIEWER_ID, "OK");

        assertThat(result.status()).isEqualTo(KycSubmission.KycStatus.APPROVED);
        verify(eventPublisher).publishApproved(result);
    }

    @Test
    void reject_inReviewSubmission_setsRejectedAndPublishes() {
        KycSubmission inReview = inReviewSubmission();
        when(submissionRepository.findById(SUBMISSION_ID)).thenReturn(Optional.of(inReview));
        when(submissionRepository.save(any(), isNull())).thenAnswer(inv -> inv.getArgument(0));

        KycSubmission result = reviewService.reject(SUBMISSION_ID, REVIEWER_ID, "Document illisible");

        assertThat(result.status()).isEqualTo(KycSubmission.KycStatus.REJECTED);
        assertThat(result.reviewedBy()).isEqualTo(REVIEWER_ID);
        assertThat(result.reviewNotes()).isEqualTo("Document illisible");
        verify(eventPublisher).publishRejected(result);
    }

    @Test
    void approve_nonExistentSubmission_throwsNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(submissionRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.approve(unknownId, REVIEWER_ID, "OK"))
                .isInstanceOf(KycService.ComplianceException.class)
                .hasMessageContaining("introuvable");
    }

    @Test
    void approve_alreadyApprovedSubmission_throwsInvalidStatus() {
        KycSubmission approved = approvedSubmission(1);
        when(submissionRepository.findById(SUBMISSION_ID)).thenReturn(Optional.of(approved));

        assertThatThrownBy(() -> reviewService.approve(SUBMISSION_ID, REVIEWER_ID, "OK"))
                .isInstanceOf(KycService.ComplianceException.class)
                .hasMessageContaining("ne peut plus être revue");
    }

    @Test
    void reject_alreadyRejectedSubmission_throwsInvalidStatus() {
        KycSubmission rejected = submission(KycSubmission.KycStatus.REJECTED, 1, new java.math.BigDecimal("0.30"));
        when(submissionRepository.findById(SUBMISSION_ID)).thenReturn(Optional.of(rejected));

        assertThatThrownBy(() -> reviewService.reject(SUBMISSION_ID, REVIEWER_ID, "motif"))
                .isInstanceOf(KycService.ComplianceException.class)
                .hasMessageContaining("ne peut plus être revue");
    }

    @Test
    void listPendingReview_returnsInReviewSubmissions() {
        List<KycSubmission> expected = List.of(inReviewSubmission());
        when(submissionRepository.findByStatus(KycSubmission.KycStatus.IN_REVIEW)).thenReturn(expected);

        List<KycSubmission> result = reviewService.listPendingReview();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(KycSubmission.KycStatus.IN_REVIEW);
    }

    @Test
    void listPendingReview_emptyList() {
        when(submissionRepository.findByStatus(KycSubmission.KycStatus.IN_REVIEW)).thenReturn(List.of());

        List<KycSubmission> result = reviewService.listPendingReview();

        assertThat(result).isEmpty();
    }
}
