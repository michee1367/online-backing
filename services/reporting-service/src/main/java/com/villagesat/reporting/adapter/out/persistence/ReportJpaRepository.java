package com.villagesat.reporting.adapter.out.persistence;

import com.villagesat.reporting.adapter.out.persistence.entity.ReportRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportJpaRepository extends JpaRepository<ReportRequestEntity, UUID> {

    List<ReportRequestEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
