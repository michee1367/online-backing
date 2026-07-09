package com.villagesat.user.adapter.out.persistence;

import com.villagesat.user.adapter.out.persistence.entity.DataExportRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DataExportJpaRepository extends JpaRepository<DataExportRequestEntity, UUID> {
}
