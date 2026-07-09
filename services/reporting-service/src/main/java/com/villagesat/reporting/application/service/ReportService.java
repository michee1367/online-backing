package com.villagesat.reporting.application.service;

import com.villagesat.reporting.domain.model.ReportRequest;
import com.villagesat.reporting.domain.model.ReportStatus;
import com.villagesat.reporting.domain.port.in.ReportUseCase;
import com.villagesat.reporting.domain.port.out.ReportGeneratorPort;
import com.villagesat.reporting.domain.port.out.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ReportService implements ReportUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final ReportRepository reportRepository;
    private final ReportGeneratorPort reportGeneratorPort;

    public ReportService(ReportRepository reportRepository, ReportGeneratorPort reportGeneratorPort) {
        this.reportRepository = reportRepository;
        this.reportGeneratorPort = reportGeneratorPort;
    }

    @Override
    public ReportRequest requestReport(ReportRequest command) {
        command.setId(UUID.randomUUID());
        command.setStatus(ReportStatus.PENDING);
        command.setCreatedAt(Instant.now());
        command.setVersion(0L);

        ReportRequest saved = reportRepository.save(command);
        generateAsync(saved);
        return saved;
    }

    @Async
    protected void generateAsync(ReportRequest request) {
        try {
            request.setStatus(ReportStatus.GENERATING);
            reportRepository.save(request);

            String fileUrl = reportGeneratorPort.generate(request);

            request.setFileUrl(fileUrl);
            request.setStatus(ReportStatus.READY);
            request.setCompletedAt(Instant.now());
            reportRepository.save(request);
        } catch (Exception e) {
            log.error("Report generation failed for id={}: {}", request.getId(), e.getMessage(), e);
            request.setStatus(ReportStatus.FAILED);
            request.setFailedReason(e.getMessage());
            reportRepository.save(request);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReportRequest> getReport(UUID id) {
        return reportRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportRequest> listReports(UUID userId) {
        return reportRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> downloadReport(UUID id) {
        return reportRepository.findById(id)
                .filter(r -> r.getStatus() == ReportStatus.READY)
                .map(ReportRequest::getFileUrl);
    }
}
