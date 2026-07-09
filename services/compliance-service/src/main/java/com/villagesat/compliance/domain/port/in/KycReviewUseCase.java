package com.villagesat.compliance.domain.port.in;

import com.villagesat.compliance.domain.model.KycSubmission;

import java.util.List;
import java.util.UUID;

public interface KycReviewUseCase {

    KycSubmission approve(UUID submissionId, UUID reviewerId, String notes);

    KycSubmission reject(UUID submissionId, UUID reviewerId, String notes);

    List<KycSubmission> listPendingReview();
}
