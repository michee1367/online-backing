package com.villagesat.reporting.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class ReportRequest {

    private UUID id;
    private UUID userId;
    private ReportType reportType;
    private Map<String, String> parameters;
    private ReportStatus status;
    private String fileUrl;
    private ReportFormat format;
    private Instant createdAt;
    private Instant completedAt;
    private String failedReason;
    private Long version;

    public ReportRequest() {}

    public ReportRequest(UUID id, UUID userId, ReportType reportType, Map<String, String> parameters,
                         ReportStatus status, String fileUrl, ReportFormat format,
                         Instant createdAt, Instant completedAt, String failedReason, Long version) {
        this.id = id;
        this.userId = userId;
        this.reportType = reportType;
        this.parameters = parameters;
        this.status = status;
        this.fileUrl = fileUrl;
        this.format = format;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.failedReason = failedReason;
        this.version = version;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public ReportType getReportType() { return reportType; }
    public void setReportType(ReportType reportType) { this.reportType = reportType; }
    public Map<String, String> getParameters() { return parameters; }
    public void setParameters(Map<String, String> parameters) { this.parameters = parameters; }
    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public ReportFormat getFormat() { return format; }
    public void setFormat(ReportFormat format) { this.format = format; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public String getFailedReason() { return failedReason; }
    public void setFailedReason(String failedReason) { this.failedReason = failedReason; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
