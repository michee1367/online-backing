package com.villagesat.reporting.adapter.in.web;

import com.villagesat.reporting.domain.model.ReportRequest;
import com.villagesat.reporting.domain.port.in.ReportUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportUseCase reportUseCase;

    public ReportController(ReportUseCase reportUseCase) {
        this.reportUseCase = reportUseCase;
    }

    @PostMapping("/request")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReportRequest> requestReport(@Valid @RequestBody ReportRequest command,
                                                       @AuthenticationPrincipal Jwt jwt) {
        command.setUserId(UUID.fromString(jwt.getSubject()));
        return ResponseEntity.status(HttpStatus.CREATED).body(reportUseCase.requestReport(command));
    }

    @GetMapping
    public ResponseEntity<List<ReportRequest>> listReports(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(reportUseCase.listReports(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReportRequest> getReport(@PathVariable UUID id) {
        return reportUseCase.getReport(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadReport(@PathVariable UUID id) {
        return reportUseCase.downloadReport(id)
                .map(url -> ResponseEntity.ok(Map.of("downloadUrl", url)))
                .orElse(ResponseEntity.notFound().build());
    }
}
