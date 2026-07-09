package com.villagesat.compliance.adapter.out.persistence;

import com.villagesat.compliance.adapter.out.persistence.mapper.ComplianceMapper;
import com.villagesat.compliance.domain.model.KycSubmission;
import com.villagesat.compliance.domain.port.out.KycSubmissionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class KycSubmissionRepositoryAdapter implements KycSubmissionRepository {

    private final KycSubmissionJpaRepository jpaRepository;

    public KycSubmissionRepositoryAdapter(KycSubmissionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public KycSubmission save(KycSubmission submission, byte[] encryptedDocumentNumber) {
        return ComplianceMapper.toDomain(
                jpaRepository.save(ComplianceMapper.toEntity(submission, encryptedDocumentNumber)));
    }

    @Override
    public Optional<KycSubmission> findById(UUID id) {
        return jpaRepository.findById(id).map(ComplianceMapper::toDomain);
    }

    @Override
    public Optional<KycSubmission> findLatestApprovedByUserId(UUID userId) {
        return jpaRepository.findFirstByUserIdAndStatusOrderByReviewedAtDesc(userId, "APPROVED")
                .map(ComplianceMapper::toDomain);
    }

    @Override
    public Optional<KycSubmission> findLatestByUserId(UUID userId) {
        return jpaRepository.findFirstByUserIdOrderBySubmittedAtDesc(userId).map(ComplianceMapper::toDomain);
    }

    @Override
    public boolean hasPendingSubmission(UUID userId, int targetLevel) {
        return jpaRepository.existsByUserIdAndTargetLevelAndStatusIn(
                userId, targetLevel, List.of("PENDING", "IN_REVIEW"));
    }

    @Override
    public List<KycSubmission> findByStatus(KycSubmission.KycStatus status) {
        return jpaRepository.findByStatus(status.name()).stream()
                .map(ComplianceMapper::toDomain).toList();
    }
}
