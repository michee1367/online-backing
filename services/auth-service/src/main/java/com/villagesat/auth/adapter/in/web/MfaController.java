package com.villagesat.auth.adapter.in.web;

import com.villagesat.auth.application.service.JwtClaimsParser;
import com.villagesat.auth.domain.model.AuthTokens;
import com.villagesat.auth.domain.model.MfaSetup;
import com.villagesat.auth.domain.port.in.MfaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/mfa")
@Tag(name = "MFA", description = "Authentification multi-facteurs TOTP")
public class MfaController {

    private final MfaUseCase mfaUseCase;
    private final JwtClaimsParser jwtClaimsParser;

    public MfaController(MfaUseCase mfaUseCase, JwtClaimsParser jwtClaimsParser) {
        this.mfaUseCase = mfaUseCase;
        this.jwtClaimsParser = jwtClaimsParser;
    }

    @PostMapping("/setup")
    @Operation(summary = "Initier configuration MFA (authentifié)")
    public ResponseEntity<MfaSetupResponse> setup(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        String email = jwtClaimsParser.extractEmail(jwt.getTokenValue());
        MfaSetup setup = mfaUseCase.initiateSetup(userId, email);
        return ResponseEntity.ok(MfaSetupResponse.from(setup));
    }

    @PostMapping("/confirm")
    @Operation(summary = "Confirmer activation MFA avec code TOTP")
    public ResponseEntity<Void> confirm(@AuthenticationPrincipal Jwt jwt,
                                        @Valid @RequestBody MfaCodeRequest request) {
        mfaUseCase.confirmSetup(UUID.fromString(jwt.getSubject()), request.code());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify")
    @Operation(summary = "Vérifier MFA après login")
    public ResponseEntity<AuthController.TokenResponse> verify(@Valid @RequestBody MfaVerifyRequest request) {
        AuthTokens tokens = mfaUseCase.verifyLoginMfa(new MfaUseCase.VerifyMfaCommand(
                request.sessionId(), request.method(), request.code(), request.refreshToken()));
        return ResponseEntity.ok(AuthController.TokenResponse.from(tokens));
    }

    public record MfaCodeRequest(@NotBlank String code) {}

    public record MfaVerifyRequest(
            @NotNull UUID sessionId,
            @NotBlank String method,
            @NotBlank String code,
            @NotBlank String refreshToken
    ) {}

    public record MfaSetupResponse(String secret, String qrCodeUri, java.util.List<String> backupCodes) {
        static MfaSetupResponse from(MfaSetup s) {
            return new MfaSetupResponse(s.secret(), s.qrCodeUri(), s.backupCodes());
        }
    }
}
