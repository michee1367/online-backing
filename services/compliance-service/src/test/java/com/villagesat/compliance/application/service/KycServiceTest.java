package com.villagesat.compliance.application.service;

import com.villagesat.compliance.config.ComplianceProperties;
import com.villagesat.compliance.domain.model.KycStatusSummary;
import com.villagesat.compliance.domain.model.KycSubmission;
import com.villagesat.compliance.domain.port.in.KycUseCase.SubmitKycCommand;
import com.villagesat.compliance.domain.port.out.AmlScreeningPort;
import com.villagesat.compliance.domain.port.out.IdentityVerificationPort;
import com.villagesat.compliance.domain.port.out.IdentityVerificationPort.VerificationResult;
import com.villagesat.compliance.domain.port.out.KycEventPublisher;
import com.villagesat.compliance.domain.port.out.KycSubmissionRepository;
import com.villagesat.compliance.adapter.out.crypto.DocumentEncryptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static com.villagesat.compliance.support.ComplianceTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KycServiceTest {

    @Mock
    KycSubmissionRepository submissionRepository;

    @Mock
    AmlScreeningPort amlScreening;

    @Mock
    IdentityVerificationPort identityVerification;

    @Mock
    KycEventPublisher eventPublisher;

    @Mock
    DocumentEncryptionService encryptionService;

    @Mock
    ComplianceProperties properties;

    @InjectMocks
    KycService kycService;

    @Test
    void submit_withHighScore_autoApproves() {
        SubmitKycCommand command = submitCommand(1);
        stubDefaults(command);
        when(identityVerification.verify(any()))
                .thenReturn(new VerificationResult(new BigDecimal("0.97"), "sim-ref", true));
        when(properties.autoApproveThreshold()).thenReturn(new BigDecimal("0.95"));
        when(properties.autoRejectThreshold()).thenReturn(new BigDecimal("0.40"));

        KycSubmission result = kycService.submit(command);

        assertThat(result.status()).isEqualTo(KycSubmission.KycStatus.APPROVED);
        assertThat(result.riskScore()).isEqualByComparingTo("0.97");

        verify(eventPublisher).publishSubmitted(any());
        verify(eventPublisher).publishApproved(any());
        verify(eventPublisher, never()).publishRejected(any());
    }

    @Test
    void submit_withMediumScore_setsInReview() {
        SubmitKycCommand command = submitCommand(1);
        stubDefaults(command);
        when(identityVerification.verify(any()))
                .thenReturn(new VerificationResult(new BigDecimal("0.80"), "sim-ref", true));
        when(properties.autoApproveThreshold()).thenReturn(new BigDecimal("0.95"));
        when(properties.manualReviewThreshold()).thenReturn(new BigDecimal("0.70"));
        when(properties.autoRejectThreshold()).thenReturn(new BigDecimal("0.40"));

        KycSubmission result = kycService.submit(command);

        assertThat(result.status()).isEqualTo(KycSubmission.KycStatus.IN_REVIEW);
        verify(eventPublisher).publishSubmitted(any());
        verify(eventPublisher, never()).publishApproved(any());
    }

    @Test
    void submit_withLowScore_rejects() {
        SubmitKycCommand command = submitCommand(2);
        stubDefaults(command);
        when(identityVerification.verify(any()))
                .thenReturn(new VerificationResult(new BigDecimal("0.30"), "sim-ref", true));
        when(properties.autoApproveThreshold()).thenReturn(new BigDecimal("0.95"));
        when(properties.autoRejectThreshold()).thenReturn(new BigDecimal("0.40"));

        KycSubmission result = kycService.submit(command);

        assertThat(result.status()).isEqualTo(KycSubmission.KycStatus.REJECTED);
        verify(eventPublisher).publishSubmitted(any());
        verify(eventPublisher).publishRejected(any());
    }

    @Test
    void submit_withInvalidDocument_rejects() {
        SubmitKycCommand command = submitCommand(1);
        stubDefaults(command);
        when(identityVerification.verify(any()))
                .thenReturn(new VerificationResult(new BigDecimal("0.80"), "sim-ref", false));
        when(properties.autoRejectThreshold()).thenReturn(new BigDecimal("0.40"));

        KycSubmission result = kycService.submit(command);

        assertThat(result.status()).isEqualTo(KycSubmission.KycStatus.REJECTED);
    }

    @Test
    void submit_whenSanctionsMatch_throwsAndPublishesRejected() {
        SubmitKycCommand command = submitCommand(1);
        when(submissionRepository.hasPendingSubmission(command.userId(), command.level())).thenReturn(false);
        when(amlScreening.screenUser(eq(command.userId()), isNull(), eq(command.documentNumber())))
                .thenReturn(sanctionsMatchScreenings());
        when(submissionRepository.save(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> kycService.submit(command))
                .isInstanceOf(KycService.ComplianceException.class)
                .hasMessageContaining("sanctions");

        verify(eventPublisher).publishRejected(any());
        verify(eventPublisher, never()).publishApproved(any());
    }

    @Test
    void submit_whenPendingExists_throwsConflict() {
        SubmitKycCommand command = submitCommand(1);
        when(submissionRepository.hasPendingSubmission(command.userId(), command.level())).thenReturn(true);

        assertThatThrownBy(() -> kycService.submit(command))
                .isInstanceOf(KycService.ComplianceException.class)
                .hasMessageContaining("déjà en cours");
    }

    @Test
    void submit_invalidLevel0_throws() {
        SubmitKycCommand command = new SubmitKycCommand(
                USER_ID, 0, "NATIONAL_ID", "DOC-123", "front", "back", "selfie");

        assertThatThrownBy(() -> kycService.submit(command))
                .isInstanceOf(KycService.ComplianceException.class)
                .hasMessageContaining("entre 1 et 3");
    }

    @Test
    void submit_invalidLevel4_throws() {
        SubmitKycCommand command = new SubmitKycCommand(
                USER_ID, 4, "NATIONAL_ID", "DOC-123", "front", "back", "selfie");

        assertThatThrownBy(() -> kycService.submit(command))
                .isInstanceOf(KycService.ComplianceException.class)
                .hasMessageContaining("entre 1 et 3");
    }

    @Test
    void getStatus_withApprovedSubmission_returnsApprovedLevel() {
        KycSubmission approved = approvedSubmission(2);
        when(submissionRepository.findLatestApprovedByUserId(USER_ID)).thenReturn(Optional.of(approved));

        KycStatusSummary status = kycService.getStatus(USER_ID);

        assertThat(status.level()).isEqualTo(2);
        assertThat(status.status()).isEqualTo("APPROVED");
        assertThat(status.limits()).isNotNull();
        assertThat(status.limits().dailyTransferLimit()).isEqualByComparingTo("5000");
    }

    @Test
    void getStatus_withPendingSubmission_returnsL0Limits() {
        KycSubmission pending = pendingSubmission();
        when(submissionRepository.findLatestApprovedByUserId(USER_ID)).thenReturn(Optional.empty());
        when(submissionRepository.findLatestByUserId(USER_ID)).thenReturn(Optional.of(pending));

        KycStatusSummary status = kycService.getStatus(USER_ID);

        assertThat(status.level()).isZero();
        assertThat(status.status()).isEqualTo("PENDING");
        assertThat(status.limits().dailyTransferLimit()).isEqualByComparingTo("200000");
    }

    @Test
    void getStatus_noSubmission_returnsNotSubmitted() {
        when(submissionRepository.findLatestApprovedByUserId(USER_ID)).thenReturn(Optional.empty());
        when(submissionRepository.findLatestByUserId(USER_ID)).thenReturn(Optional.empty());

        KycStatusSummary status = kycService.getStatus(USER_ID);

        assertThat(status.level()).isZero();
        assertThat(status.status()).isEqualTo("NOT_SUBMITTED");
        assertThat(status.limits().dailyTransferLimit()).isEqualByComparingTo("200000");
    }

    @Test
    void submit_pendingScore_setsPendingStatus() {
        SubmitKycCommand command = submitCommand(1);
        stubDefaults(command);
        when(identityVerification.verify(any()))
                .thenReturn(new VerificationResult(new BigDecimal("0.55"), "sim-ref", true));
        when(properties.autoApproveThreshold()).thenReturn(new BigDecimal("0.95"));
        when(properties.manualReviewThreshold()).thenReturn(new BigDecimal("0.70"));
        when(properties.autoRejectThreshold()).thenReturn(new BigDecimal("0.40"));

        KycSubmission result = kycService.submit(command);

        assertThat(result.status()).isEqualTo(KycSubmission.KycStatus.PENDING);
        verify(eventPublisher).publishSubmitted(any());
        verify(eventPublisher, never()).publishApproved(any());
        verify(eventPublisher, never()).publishRejected(any());
    }

    private void stubDefaults(SubmitKycCommand command) {
        when(submissionRepository.hasPendingSubmission(command.userId(), command.level())).thenReturn(false);
        when(amlScreening.screenUser(eq(command.userId()), isNull(), eq(command.documentNumber())))
                .thenReturn(clearScreenings());
        when(submissionRepository.save(any(), any())).thenAnswer(inv -> inv.getArgument(0));
        when(encryptionService.encrypt(any())).thenReturn(new byte[]{1, 2, 3});
    }
}
