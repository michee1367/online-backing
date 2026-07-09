package com.villagesat.reporting.domain.port.out;

import com.villagesat.reporting.domain.model.ReportRequest;

public interface ReportGeneratorPort {

    String generate(ReportRequest request);
}
