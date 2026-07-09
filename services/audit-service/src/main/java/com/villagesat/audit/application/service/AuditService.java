package com.villagesat.audit.application.service;

import com.villagesat.audit.domain.model.AuditEntry;
import com.villagesat.audit.domain.model.AuditSearchCriteria;
import com.villagesat.audit.domain.port.in.AuditUseCase;
import com.villagesat.audit.domain.port.out.AuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuditService implements AuditUseCase {

    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Override
    public AuditEntry logEvent(AuditEntry entry) {
        if (entry.getId() == null) {
            entry.setId(UUID.randomUUID());
        }
        if (entry.getTimestamp() == null) {
            entry.setTimestamp(Instant.now());
        }
        return auditRepository.save(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEntry> search(AuditSearchCriteria criteria) {
        return auditRepository.search(criteria);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEntry> getByEntityId(String entityType, String entityId) {
        return auditRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuditEntry> getById(UUID id) {
        return auditRepository.findById(id);
    }
}
