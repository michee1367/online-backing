package com.villagesat.audit.domain.port.in;

import com.villagesat.audit.domain.model.AuditEntry;
import com.villagesat.audit.domain.model.AuditSearchCriteria;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditUseCase {

    AuditEntry logEvent(AuditEntry entry);

    List<AuditEntry> search(AuditSearchCriteria criteria);

    List<AuditEntry> getByEntityId(String entityType, String entityId);

    Optional<AuditEntry> getById(UUID id);
}
