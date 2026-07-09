package com.villagesat.reporting.adapter.out.generator;

import com.villagesat.reporting.domain.model.ReportRequest;
import com.villagesat.reporting.domain.port.out.ReportGeneratorPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SimulatedReportGenerator implements ReportGeneratorPort {

    private static final Logger log = LoggerFactory.getLogger(SimulatedReportGenerator.class);

    @Override
    public String generate(ReportRequest request) {
        log.info("Generating {} report in {} format for user {}",
                request.getReportType(), request.getFormat(), request.getUserId());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Report generation interrupted", e);
        }

        String filename = String.format("reports/%s/%s_%s.%s",
                request.getUserId(),
                request.getReportType().name().toLowerCase(),
                UUID.randomUUID().toString().substring(0, 8),
                request.getFormat().name().toLowerCase());

        return "https://storage.villagesat.com/" + filename;
    }
}
