package com.villagesat.audit.adapter.in.web;

import com.villagesat.audit.domain.model.AuditEntry;
import com.villagesat.audit.domain.model.AuditSearchCriteria;
import com.villagesat.audit.domain.port.in.AuditUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
@PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER')")
public class AuditController {

    private final AuditUseCase auditUseCase;

    public AuditController(AuditUseCase auditUseCase) {
        this.auditUseCase = auditUseCase;
    }

    @GetMapping("/search")
    public ResponseEntity<List<AuditEntry>> search(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {

        AuditSearchCriteria criteria = new AuditSearchCriteria();
        criteria.setEntityType(entityType);
        criteria.setEntityId(entityId);
        criteria.setUserId(userId);
        criteria.setEventType(eventType);
        criteria.setFrom(from);
        criteria.setTo(to);

        return ResponseEntity.ok(auditUseCase.search(criteria));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditEntry> getById(@PathVariable UUID id) {
        return auditUseCase.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
