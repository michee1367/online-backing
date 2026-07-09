package com.villagesat.compliance.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "kyc_submissions", schema = "compliance")
public class KycSubmissionEntity {

    @Id
    @GeneratedValue // Laisse Postgres gérer le gen_random_uuid() par défaut
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "target_level", nullable = false)
    private short targetLevel; // Mappe le SMALLINT (valeurs entre 0 et 3)

    @Column(name = "document_type", nullable = false, length = 30)
    private String documentType;

    @Column(name = "document_number_enc")
    private byte[] documentNumberEnc; // Mappe le type BYTEA pour les données chiffrées

    @Column(name = "document_front_key", length = 500)
    private String documentFrontKey;

    @Column(name = "document_back_key", length = 500)
    private String documentBackKey;

    @Column(name = "selfie_key", length = 500)
    private String selfieKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusEntity status;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "provider_ref", length = 100)
    private String providerRef;

    @Column(name = "risk_score", precision = 5, scale = 2)
    private BigDecimal riskScore; // Mappe le NUMERIC(5,2)

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    // --- ENUM INTERNE pour le statut KYC ---
    public enum StatusEntity {
        PENDING, IN_REVIEW, APPROVED, REJECTED, EXPIRED
    }

    // --- Cycle de vie JPA pour les valeurs par défaut ---
    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = StatusEntity.PENDING;
        }
        if (this.submittedAt == null) {
            this.submittedAt = LocalDateTime.now();
        }
    }

    //region Getters and Setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public short getTargetLevel() { return targetLevel; }
    public void setTargetLevel(short targetLevel) { this.targetLevel = targetLevel; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType ) { this.documentType = documentType; }

    public byte[] getDocumentNumberEnc() { return documentNumberEnc; }
    public void setDocumentNumberEnc(byte[] documentNumberEnc) { this.documentNumberEnc = documentNumberEnc; }

    public String getDocumentFrontKey() { return documentFrontKey; }
    public void setDocumentFrontKey(String documentFrontKey) { this.documentFrontKey = documentFrontKey; }

    public String getDocumentBackKey() { return documentBackKey; }
    public void setDocumentBackKey(String documentBackKey) { this.documentBackKey = documentBackKey; }

    public String getSelfieKey() { return selfieKey; }
    public void setSelfieKey(String selfieKey) { this.selfieKey = selfieKey; }

    public StatusEntity getStatus() { return status; }
    public void setStatus(StatusEntity status) { this.status = status; }

    public String getReviewNotes() { return reviewNotes; }
    public void setReviewNotes(String reviewNotes) { this.reviewNotes = reviewNotes; }

    public UUID getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(UUID reviewedBy) { this.reviewedBy = reviewedBy; }

    public String getProviderRef() { return providerRef; }
    public void setProviderRef(String providerRef) { this.providerRef = providerRef; }

    public BigDecimal getRiskScore() { return riskScore; }
    public void setRiskScore(BigDecimal riskScore) { this.riskScore = riskScore; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    //endregion
}