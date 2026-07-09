package com.villagesat.compliance.adapter.in.web;

import com.villagesat.common.security.SecurityUtils;
import com.villagesat.compliance.domain.model.KycSubmission;
import com.villagesat.compliance.domain.port.in.KycReviewUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/compliance/kyc")
@Tag(name = "KYC Review", description = "Revue manuelle KYC (officiers conformité)")
@PreAuthorize("hasRole('COMPLIANCE_OFFICER') or hasRole('ADMIN')")
public class KycReviewController {

    private final KycReviewUseCase reviewUseCase;

    public KycReviewController(KycReviewUseCase reviewUseCase) {
        this.reviewUseCase = reviewUseCase;
    }

    @GetMapping("/pending")
    @Operation(summary = "Liste des KYC en attente de revue")
    public List<KycReviewItem> listPending() {
        return reviewUseCase.listPendingReview().stream().map(KycReviewItem::from).toList();
    }

    @PostMapping("/{submissionId}/approve")
    @Operation(summary = "Approuver une soumission KYC")
    public KycReviewItem approve(@PathVariable UUID submissionId,
                                 @RequestBody(required = false) ReviewNotesRequest request) {
        String notes = request != null ? request.notes() : "Approuvé manuellement";
        return KycReviewItem.from(reviewUseCase.approve(submissionId, SecurityUtils.getCurrentUserId(), notes));
    }

    @PostMapping("/{submissionId}/reject")
    @Operation(summary = "Rejeter une soumission KYC")
    public KycReviewItem reject(@PathVariable UUID submissionId,
                                @RequestBody @NotBlank ReviewNotesRequest request) {
        return KycReviewItem.from(reviewUseCase.reject(submissionId, SecurityUtils.getCurrentUserId(), request.notes()));
    }

    public record ReviewNotesRequest(String notes) {}

    public record KycReviewItem(
            UUID submissionId,
            UUID userId,
            int targetLevel,
            String status,
            BigDecimal riskScore,
            String documentType
    ) {
        static KycReviewItem from(KycSubmission s) {
            return new KycReviewItem(s.id(), s.userId(), s.targetLevel(),
                    s.status().name(), s.riskScore(), s.documentType().name());
        }
    }
}
