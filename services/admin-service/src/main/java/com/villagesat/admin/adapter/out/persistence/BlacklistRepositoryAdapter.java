package com.villagesat.admin.adapter.out.persistence;

import com.villagesat.admin.adapter.out.persistence.entity.BlacklistEntryEntity;
import com.villagesat.admin.domain.model.BlacklistEntry;
import com.villagesat.admin.domain.model.EntityType;
import com.villagesat.admin.domain.port.out.BlacklistRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class BlacklistRepositoryAdapter implements BlacklistRepository {

    private final BlacklistJpaRepository jpaRepository;

    public BlacklistRepositoryAdapter(BlacklistJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public BlacklistEntry save(BlacklistEntry entry) {
        BlacklistEntryEntity entity = toEntity(entry);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<BlacklistEntry> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<BlacklistEntry> findAllActive() {
        return jpaRepository.findByActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deactivate(UUID id) {
        jpaRepository.deactivateById(id);
    }

    private BlacklistEntryEntity toEntity(BlacklistEntry entry) {
        BlacklistEntryEntity entity = new BlacklistEntryEntity();
        entity.setId(entry.getId());
        entity.setEntityType(entry.getEntityType().name());
        entity.setEntityValue(entry.getEntityValue());
        entity.setReason(entry.getReason());
        entity.setCreatedBy(entry.getCreatedBy());
        entity.setCreatedAt(entry.getCreatedAt());
        entity.setExpiresAt(entry.getExpiresAt());
        entity.setActive(entry.isActive());
        return entity;
    }

    private BlacklistEntry toDomain(BlacklistEntryEntity entity) {
        return new BlacklistEntry(
                entity.getId(),
                EntityType.valueOf(entity.getEntityType()),
                entity.getEntityValue(),
                entity.getReason(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.isActive()
        );
    }
}
