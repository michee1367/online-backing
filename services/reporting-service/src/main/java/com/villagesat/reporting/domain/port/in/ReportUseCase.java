package com.villagesat.reporting.domain.port.in;

import com.villagesat.reporting.domain.model.ReportRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportUseCase {

    ReportRequest requestReport(ReportRequest command);

    Optional<ReportRequest> getReport(UUID id);

    List<ReportRequest> listReports(UUID userId);

    Optional<String> downloadReport(UUID id);
}
