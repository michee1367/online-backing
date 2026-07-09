package com.villagesat.fraud.adapter.out.persistence;

import com.villagesat.fraud.adapter.out.persistence.entity.FraudAlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FraudAlertJpaRepository extends JpaRepository<FraudAlertEntity, UUID> {

    List<FraudAlertEntity> findByStatusOrderByCreatedAtDesc(String status);

    List<FraudAlertEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
