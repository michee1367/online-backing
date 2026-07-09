package com.villagesat.fraud.adapter.in.web;

import com.villagesat.fraud.domain.model.FraudScoreRequest;
import com.villagesat.fraud.domain.model.FraudScoreResult;
import com.villagesat.fraud.domain.port.in.FraudScoringUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/fraud")
@Tag(name = "Fraud Scoring", description = "Scoring de fraude en temps réel (appel interne)")
public class FraudScoringController {

    private final FraudScoringUseCase scoringUseCase;

    public FraudScoringController(FraudScoringUseCase scoringUseCase) {
        this.scoringUseCase = scoringUseCase;
    }

    @PostMapping("/score")
    @Operation(summary = "Calculer le score de fraude d'une transaction")
    public ResponseEntity<FraudScoreResponse> score(@Valid @RequestBody FraudScoreRequestDto request) {
        FraudScoreRequest domainRequest = new FraudScoreRequest(
                request.userId(), request.walletId(),
                request.amount(), request.currency(),
                request.ipAddress(), request.deviceId(),
                Instant.now()
        );

        FraudScoreResult result = scoringUseCase.score(domainRequest);

        return ResponseEntity.ok(new FraudScoreResponse(
                result.score(), result.action().name(), result.reasons(), result.rulesFired()));
    }

    public record FraudScoreRequestDto(
            @NotNull UUID userId,
            UUID walletId,
            @NotNull BigDecimal amount,
            @NotBlank String currency,
            String ipAddress,
            String deviceId
    ) {}

    public record FraudScoreResponse(
            int score,
            String action,
            List<String> reasons,
            List<String> rulesFired
    ) {}
}
