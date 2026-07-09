package com.villagesat.admin.adapter.out.persistence;

import com.villagesat.admin.adapter.out.persistence.entity.SystemConfigEntity;
import com.villagesat.admin.domain.model.SystemConfig;
import com.villagesat.admin.domain.port.out.SystemConfigRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class SystemConfigRepositoryAdapter implements SystemConfigRepository {

    private final SystemConfigJpaRepository jpaRepository;

    public SystemConfigRepositoryAdapter(SystemConfigJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<SystemConfig> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<SystemConfig> findByKey(String key) {
        return jpaRepository.findByKey(key).map(this::toDomain);
    }

    @Override
    public SystemConfig save(SystemConfig config) {
        SystemConfigEntity entity = toEntity(config);
        return toDomain(jpaRepository.save(entity));
    }

    private SystemConfigEntity toEntity(SystemConfig config) {
        SystemConfigEntity entity = new SystemConfigEntity();
        entity.setKey(config.getKey());
        entity.setValue(config.getValue());
        entity.setDescription(config.getDescription());
        entity.setUpdatedBy(config.getUpdatedBy());
        entity.setUpdatedAt(config.getUpdatedAt());
        return entity;
    }

    private SystemConfig toDomain(SystemConfigEntity entity) {
        return new SystemConfig(
                entity.getKey(),
                entity.getValue(),
                entity.getDescription(),
                entity.getUpdatedBy(),
                entity.getUpdatedAt()
        );
    }
}
