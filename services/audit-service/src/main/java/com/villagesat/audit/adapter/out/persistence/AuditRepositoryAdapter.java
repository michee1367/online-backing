package com.villagesat.audit.adapter.out.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.villagesat.audit.adapter.out.persistence.entity.AuditEntryEntity;
import com.villagesat.audit.domain.model.AuditEntry;
import com.villagesat.audit.domain.model.AuditSearchCriteria;
import com.villagesat.audit.domain.port.out.AuditRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class AuditRepositoryAdapter implements AuditRepository {

    private final AuditJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    public AuditRepositoryAdapter(AuditJpaRepository jpaRepository, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public AuditEntry save(AuditEntry entry) {
        AuditEntryEntity entity = toEntity(entry);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<AuditEntry> search(AuditSearchCriteria criteria) {
        Specification<AuditEntryEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getEntityType() != null) {
                predicates.add(cb.equal(root.get("entityType"), criteria.getEntityType()));
            }
            if (criteria.getEntityId() != null) {
                predicates.add(cb.equal(root.get("entityId"), criteria.getEntityId()));
            }
            if (criteria.getUserId() != null) {
                predicates.add(cb.equal(root.get("userId"), criteria.getUserId()));
            }
            if (criteria.getEventType() != null) {
                predicates.add(cb.equal(root.get("eventType"), criteria.getEventType()));
            }
            if (criteria.getFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), criteria.getFrom()));
            }
            if (criteria.getTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), criteria.getTo()));
            }

            query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return jpaRepository.findAll(spec).stream().map(this::toDomain).toList();
    }

    @Override
    public List<AuditEntry> findByEntityTypeAndEntityId(String entityType, String entityId) {
        return jpaRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<AuditEntry> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    private AuditEntryEntity toEntity(AuditEntry entry) {
        AuditEntryEntity entity = new AuditEntryEntity();
        entity.setId(entry.getId());
        entity.setServiceName(entry.getServiceName());
        entity.setEventType(entry.getEventType());
        entity.setUserId(entry.getUserId());
        entity.setEntityType(entry.getEntityType());
        entity.setEntityId(entry.getEntityId());
        entity.setAction(entry.getAction());
        entity.setOldValue(entry.getOldValue());
        entity.setNewValue(entry.getNewValue());
        entity.setIpAddress(entry.getIpAddress());
        entity.setCreatedAt(entry.getTimestamp());
        try {
            entity.setMetadata(entry.getMetadata() != null ? objectMapper.writeValueAsString(entry.getMetadata()) : "{}");
        } catch (Exception e) {
            entity.setMetadata("{}");
        }
        return entity;
    }

    private AuditEntry toDomain(AuditEntryEntity entity) {
        Map<String, String> metadata = new HashMap<>();
        try {
            if (entity.getMetadata() != null) {
                metadata = objectMapper.readValue(entity.getMetadata(), new TypeReference<>() {});
            }
        } catch (Exception ignored) {}

        return new AuditEntry(
                entity.getId(),
                entity.getServiceName(),
                entity.getEventType(),
                entity.getUserId(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getAction(),
                entity.getOldValue(),
                entity.getNewValue(),
                entity.getIpAddress(),
                entity.getCreatedAt(),
                metadata
        );
    }
}
