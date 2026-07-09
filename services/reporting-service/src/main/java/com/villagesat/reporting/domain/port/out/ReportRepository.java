package com.villagesat.reporting.domain.port.out;

import com.villagesat.reporting.domain.model.ReportRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportRepository {

    ReportRequest save(ReportRequest request);

    Optional<ReportRequest> findById(UUID id);

    List<ReportRequest> findByUserId(UUID userId);
}
