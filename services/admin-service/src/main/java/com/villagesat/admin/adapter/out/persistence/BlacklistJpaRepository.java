package com.villagesat.admin.adapter.out.persistence;

import com.villagesat.admin.adapter.out.persistence.entity.BlacklistEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface BlacklistJpaRepository extends JpaRepository<BlacklistEntryEntity, UUID> {

    List<BlacklistEntryEntity> findByActiveTrueOrderByCreatedAtDesc();

    @Modifying
    @Query("UPDATE BlacklistEntryEntity b SET b.active = false WHERE b.id = :id")
    void deactivateById(UUID id);
}
