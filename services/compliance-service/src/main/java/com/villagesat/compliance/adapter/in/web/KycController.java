package com.villagesat.compliance.adapter.in.web;

import com.villagesat.common.security.SecurityUtils;
import com.villagesat.compliance.domain.model.KycLimits;
import com.villagesat.compliance.domain.model.KycStatusSummary;
import com.villagesat.compliance.domain.model.KycSubmission;
import com.villagesat.compliance.domain.port.in.KycUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/kyc")
@Tag(name = "KYC", description = "Vérification d'identité")
@PreAuthorize("hasRole('CUSTOMER') or hasRole('MERCHANT')")
public class KycController {

    private final KycUseCase kycUseCase;

    public KycController(KycUseCase kycUseCase) {
        this.kycUseCase = kycUseCase;
    }

    @PostMapping("/submit")
    @Operation(summary = "Soumettre une demande KYC")
    public ResponseEntity<KycSubmitResponse> submit(@Valid @RequestBody KycSubmitRequest request) {
        KycSubmission submission = kycUseCase.submit(new KycUseCase.SubmitKycCommand(
                SecurityUtils.getCurrentUserId(),
                request.level(),
                request.documentType(),
                request.documentNumber(),
                request.documentFrontUrl(),
                request.documentBackUrl(),
                request.selfieUrl()));

        return ResponseEntity.status(
                submission.status() == KycSubmission.KycStatus.APPROVED ? HttpStatus.OK : HttpStatus.ACCEPTED
        ).body(KycSubmitResponse.from(submission));
    }

    @GetMapping("/status")
    @Operation(summary = "Statut KYC de l'utilisateur connecté")
    public KycStatusResponse getStatus() {
        return KycStatusResponse.from(kycUseCase.getStatus(SecurityUtils.getCurrentUserId()));
    }

    public record KycSubmitRequest(
            @Min(1) @Max(3) int level,
            @NotBlank String documentType,
            @NotBlank String documentNumber,
            @NotBlank String documentFrontUrl,
            String documentBackUrl,
            @NotBlank String selfieUrl
    ) {}

    public record KycSubmitResponse(
            String submissionId,
            String status,
            int targetLevel,
            BigDecimal riskScore,
            String message
    ) {
        static KycSubmitResponse from(KycSubmission s) {
            String message = switch (s.status()) {
                case APPROVED -> "KYC approuvé automatiquement";
                case IN_REVIEW -> "KYC en cours de revue par notre équipe conformité";
                case REJECTED -> "KYC rejeté";
                default -> "KYC soumis, traitement en cours";
            };
            return new KycSubmitResponse(
                    s.id().toString(), s.status().name(), s.targetLevel(), s.riskScore(), message);
        }
    }

    public record KycStatusResponse(
            int level,
            String status,
            Instant approvedAt,
            LimitsResponse limits,
            String reviewNotes
    ) {
        static KycStatusResponse from(KycStatusSummary s) {
            return new KycStatusResponse(
                    s.level(), s.status(), s.approvedAt(),
                    LimitsResponse.from(s.limits()), s.reviewNotes());
        }
    }

    public record LimitsResponse(
            BigDecimal dailyTransfer,
            BigDecimal monthlyTransfer,
            boolean internationalEnabled,
            boolean cardsEnabled
    ) {
        static LimitsResponse from(KycLimits l) {
            return new LimitsResponse(
                    l.dailyTransferLimit(), l.monthlyTransferLimit(),
                    l.internationalEnabled(), l.cardsEnabled());
        }
    }
}
