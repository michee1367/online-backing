package com.villagesat.compliance.domain.port.out;

import com.villagesat.compliance.domain.model.KycSubmission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KycSubmissionRepository {

    KycSubmission save(KycSubmission submission, byte[] encryptedDocumentNumber);

    Optional<KycSubmission> findById(UUID id);

    Optional<KycSubmission> findLatestApprovedByUserId(UUID userId);

    Optional<KycSubmission> findLatestByUserId(UUID userId);

    boolean hasPendingSubmission(UUID userId, int targetLevel);

    List<KycSubmission> findByStatus(KycSubmission.KycStatus status);
}
