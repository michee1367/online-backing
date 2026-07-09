package com.villagesat.fraud.adapter.in.web;

import com.villagesat.fraud.domain.model.AlertStatus;
import com.villagesat.fraud.domain.model.FraudAlert;
import com.villagesat.fraud.domain.port.in.FraudAlertUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fraud/alerts")
@Tag(name = "Fraud Alerts", description = "Gestion des alertes de fraude")
public class FraudAlertController {

    private final FraudAlertUseCase alertUseCase;

    public FraudAlertController(FraudAlertUseCase alertUseCase) {
        this.alertUseCase = alertUseCase;
    }

    @GetMapping
    @PreAuthorize("hasRole('FRAUD_ANALYST')")
    @Operation(summary = "Lister les alertes de fraude par statut")
    public List<FraudAlertResponse> getAlerts(
            @RequestParam(defaultValue = "OPEN") AlertStatus status) {
        return alertUseCase.getAlerts(status).stream()
                .map(FraudAlertResponse::from)
                .toList();
    }

    @PostMapping("/{alertId}/resolve")
    @PreAuthorize("hasRole('FRAUD_ANALYST')")
    @Operation(summary = "Résoudre une alerte de fraude")
    public ResponseEntity<FraudAlertResponse> resolveAlert(
            @PathVariable UUID alertId,
            @Valid @RequestBody ResolveAlertRequest request) {
        FraudAlert resolved = alertUseCase.resolveAlert(
                alertId, request.resolution(), request.resolvedBy(), request.note());
        return ResponseEntity.ok(FraudAlertResponse.from(resolved));
    }

    public record ResolveAlertRequest(
            @NotNull AlertStatus resolution,
            @NotNull UUID resolvedBy,
            @Size(max = 500) String note
    ) {}

    public record FraudAlertResponse(
            UUID id,
            UUID userId,
            UUID transactionId,
            int score,
            String action,
            List<String> reasons,
            List<String> rulesFired,
            String status,
            Instant createdAt,
            Instant resolvedAt,
            UUID resolvedBy,
            String resolutionNote
    ) {
        static FraudAlertResponse from(FraudAlert a) {
            return new FraudAlertResponse(
                    a.id(), a.userId(), a.transactionId(), a.score(),
                    a.action().name(), a.reasons(), a.rulesFired(),
                    a.status().name(), a.createdAt(), a.resolvedAt(),
                    a.resolvedBy(), a.resolutionNote()
            );
        }
    }
}
