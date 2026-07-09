package com.villagesat.compliance.adapter.out.persistence.mapper;

import com.villagesat.compliance.adapter.out.persistence.entity.KycSubmissionEntity;
import com.villagesat.compliance.adapter.out.persistence.entity.ScreeningEntity;
import com.villagesat.compliance.domain.model.KycSubmission;
import com.villagesat.compliance.domain.model.Screening;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

public final class ComplianceMapper {

    private ComplianceMapper() {}

    public static KycSubmission toDomain(KycSubmissionEntity entity) {
        if (entity == null) {
            return null;
        }

        return new KycSubmission(
                entity.getId(),
                entity.getUserId(),
                entity.getTargetLevel(),
                KycSubmission.DocumentType.valueOf(entity.getDocumentType()),
                entity.getDocumentFrontKey(),
                entity.getDocumentBackKey(),
                entity.getSelfieKey(),
                KycSubmission.KycStatus.valueOf(entity.getStatus().name()),
                entity.getReviewNotes(),
                entity.getReviewedBy(),
                entity.getProviderRef(),
                entity.getRiskScore(),
                toInstant(entity.getSubmittedAt()),
                toInstant(entity.getReviewedAt())
        );
    }

    public static KycSubmissionEntity toEntity(KycSubmission domain) {
        if (domain == null) {
            return null;
        }

        KycSubmissionEntity entity = new KycSubmissionEntity();
        entity.setId(domain.id());
        entity.setUserId(domain.userId());
        entity.setTargetLevel((short) domain.targetLevel());
        entity.setDocumentType(domain.documentType().name());
        entity.setDocumentFrontKey(domain.documentFrontKey());
        entity.setDocumentBackKey(domain.documentBackKey());
        entity.setSelfieKey(domain.selfieKey());
        entity.setStatus(KycSubmissionEntity.StatusEntity.valueOf(domain.status().name()));
        entity.setReviewNotes(domain.reviewNotes());
        entity.setReviewedBy(domain.reviewedBy());
        entity.setProviderRef(domain.providerRef());
        entity.setRiskScore(domain.riskScore());
        entity.setSubmittedAt(toLocalDateTime(domain.submittedAt()));
        entity.setReviewedAt(toLocalDateTime(domain.reviewedAt()));
        
        // Le champ chiffré n'est pas exposé dans le modèle de domaine d'affichage
        // Il sera populé séparément lors de l'enregistrement initial ou mis à jour via l'entité
        entity.setDocumentNumberEnc(null); 

        return entity;
    }// Version surchargée acceptant le jeton chiffré destiné à la colonne BYTEA
    public static KycSubmissionEntity toEntity(KycSubmission domain, byte[] encryptedDocumentNumber) {
        if (domain == null) {
            return null;
        }

        KycSubmissionEntity entity = new KycSubmissionEntity();
        entity.setId(domain.id());
        entity.setUserId(domain.userId());
        entity.setTargetLevel((short) domain.targetLevel());
        entity.setDocumentType(domain.documentType().name());
        entity.setDocumentFrontKey(domain.documentFrontKey());
        entity.setDocumentBackKey(domain.documentBackKey());
        entity.setSelfieKey(domain.selfieKey());
        entity.setStatus(KycSubmissionEntity.StatusEntity.valueOf(domain.status().name()));
        entity.setReviewNotes(domain.reviewNotes());
        entity.setReviewedBy(domain.reviewedBy());
        entity.setProviderRef(domain.providerRef());
        entity.setRiskScore(domain.riskScore());
        entity.setSubmittedAt(toLocalDateTime(domain.submittedAt()));
        entity.setReviewedAt(toLocalDateTime(domain.reviewedAt()));
        
        // Injection du binaire chiffré reçu en paramètre
        entity.setDocumentNumberEnc(encryptedDocumentNumber); 

        return entity;
    }
    // --- Mapping pour les Screenings (Ajout) ---

    public static Screening toDomain(ScreeningEntity entity) {
        if (entity == null) {
            return null;
        }

        return new Screening(
                entity.getId(),
                entity.getUserId(),
                entity.getKycSubmissionId(),
                Screening.ScreeningType.valueOf(entity.getScreeningType()),
                Screening.ScreeningResult.valueOf(entity.getResult()),
                entity.getProvider(),
                entity.getDetails() != null ? Map.copyOf(entity.getDetails()) : Map.of(),
                entity.getScreenedAt()
        );
    }

    public static ScreeningEntity toEntity(Screening domain) {
        if (domain == null) {
            return null;
        }

        ScreeningEntity entity = new ScreeningEntity();
        entity.setId(domain.id());
        entity.setUserId(domain.userId());
        entity.setKycSubmissionId(domain.kycSubmissionId());
        entity.setScreeningType(domain.screeningType().name());
        entity.setResult(domain.result().name());
        entity.setProvider(domain.provider());
        entity.setDetails(domain.details());
        entity.setScreenedAt(domain.screenedAt());

        return entity;
    }

    // --- Utilitaires de conversion temporelle UTC ---
    
    private static Instant toInstant(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.toInstant(ZoneOffset.UTC);
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}