package com.villagesat.compliance.adapter.out.persistence;

import com.villagesat.compliance.adapter.out.persistence.entity.ScreeningEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ScreeningJpaRepository extends JpaRepository<ScreeningEntity, UUID> {
}
