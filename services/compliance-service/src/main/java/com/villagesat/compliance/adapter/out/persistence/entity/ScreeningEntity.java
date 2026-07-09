package com.villagesat.compliance.adapter.out.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "screenings", schema = "compliance")
public class ScreeningEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "kyc_submission_id")
    private UUID kycSubmissionId;

    @Column(name = "screening_type", nullable = false, length = 30)
    private String screeningType;

    @Column(nullable = false, length = 20)
    private String result;

    @Column(length = 50)
    private String provider;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Column(name = "screened_at", nullable = false)
    private Instant screenedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getKycSubmissionId() { return kycSubmissionId; }
    public void setKycSubmissionId(UUID kycSubmissionId) { this.kycSubmissionId = kycSubmissionId; }
    public String getScreeningType() { return screeningType; }
    public void setScreeningType(String screeningType) { this.screeningType = screeningType; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
    public Instant getScreenedAt() { return screenedAt; }
    public void setScreenedAt(Instant screenedAt) { this.screenedAt = screenedAt; }
}
