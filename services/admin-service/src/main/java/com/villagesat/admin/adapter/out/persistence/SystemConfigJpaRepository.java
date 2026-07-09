package com.villagesat.admin.adapter.out.persistence;

import com.villagesat.admin.adapter.out.persistence.entity.SystemConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemConfigJpaRepository extends JpaRepository<SystemConfigEntity, String> {

    Optional<SystemConfigEntity> findByKey(String key);
}
