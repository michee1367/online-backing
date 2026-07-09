package com.villagesat.auth.adapter.out.persistence;

import com.villagesat.auth.adapter.out.persistence.entity.MfaSecretEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MfaSecretJpaRepository extends JpaRepository<MfaSecretEntity, UUID> {
}
