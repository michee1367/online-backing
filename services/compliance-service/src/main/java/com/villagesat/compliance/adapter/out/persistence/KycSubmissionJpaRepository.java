package com.villagesat.compliance.adapter.out.persistence;

import com.villagesat.compliance.adapter.out.persistence.entity.KycSubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KycSubmissionJpaRepository extends JpaRepository<KycSubmissionEntity, UUID> {

    Optional<KycSubmissionEntity> findFirstByUserIdAndStatusOrderByReviewedAtDesc(UUID userId, String status);

    Optional<KycSubmissionEntity> findFirstByUserIdOrderBySubmittedAtDesc(UUID userId);

    boolean existsByUserIdAndTargetLevelAndStatusIn(UUID userId, int targetLevel, List<String> statuses);

    List<KycSubmissionEntity> findByStatus(String status);
}
