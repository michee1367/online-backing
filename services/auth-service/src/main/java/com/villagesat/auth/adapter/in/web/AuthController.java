package com.villagesat.auth.adapter.in.web;

import com.villagesat.auth.application.service.AuthService;
import com.villagesat.auth.domain.model.AuthTokens;
import com.villagesat.auth.domain.model.LoginResult;
import com.villagesat.auth.domain.model.RegisteredUser;
import com.villagesat.auth.domain.port.in.AuthUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Inscription, login, refresh, logout")
public class AuthController {

    private final AuthUseCase authUseCase;

    public AuthController(AuthUseCase authUseCase) {
        this.authUseCase = authUseCase;
    }

    @PostMapping("/register")
    @Operation(summary = "Créer un compte")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisteredUser user = authUseCase.register(new AuthUseCase.RegisterCommand(
                request.email(), request.phone(), request.password(),
                request.firstName(), request.lastName(), request.countryCode(), request.acceptedTerms()));
        return ResponseEntity.status(HttpStatus.CREATED).body(RegisterResponse.from(user));
    }

    @PostMapping("/login")
    @Operation(summary = "Connexion")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                             HttpServletRequest httpRequest) {
        LoginResult result = authUseCase.login(new AuthUseCase.LoginCommand(
                request.email(),
                request.password(),
                request.deviceFingerprint(),
                clientIp(httpRequest),
                httpRequest.getHeader("User-Agent")));
        return ResponseEntity.ok(LoginResponse.from(result));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rafraîchir le token")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request,
                                               HttpServletRequest httpRequest) {
        AuthTokens tokens = authUseCase.refresh(new AuthUseCase.RefreshCommand(
                request.refreshToken(), clientIp(httpRequest)));
        return ResponseEntity.ok(TokenResponse.from(tokens));
    }

    @PostMapping("/logout")
    @Operation(summary = "Déconnexion")
    public ResponseEntity<Void> logout(@RequestBody(required = false) LogoutRequest request,
                                       @AuthenticationPrincipal Jwt jwt) {
        UUID userId = jwt != null ? UUID.fromString(jwt.getSubject()) : null;
        String accessToken = request != null ? request.accessToken() : null;
        String refreshToken = request != null ? request.refreshToken() : null;
        authUseCase.logout(new AuthUseCase.LogoutCommand(accessToken, refreshToken, userId));
        return ResponseEntity.noContent().build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String remote = request.getRemoteAddr();
        if ("0:0:0:0:0:0:0:1".equals(remote) || "::1".equals(remote)) {
            return "127.0.0.1";
        }
        return remote;
    }

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Pattern(regexp = "^\\+[1-9]\\d{6,14}$") String phone,
            @NotBlank @Size(min = 12) @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$") String password,
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @NotBlank @Size(min = 2, max = 2) String countryCode,
            @AssertTrue(message = "acceptedTerms must be true") boolean acceptedTerms
    ) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password,
            String deviceFingerprint
    ) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record LogoutRequest(String accessToken, String refreshToken) {}

    public record RegisterResponse(
            UUID userId, String email, int kycLevel, String status, boolean verificationSent
    ) {
        static RegisterResponse from(RegisteredUser u) {
            return new RegisterResponse(u.userId(), u.email(), u.kycLevel(), u.status(), u.verificationSent());
        }
    }

    public record LoginResponse(
            String accessToken, String refreshToken, long expiresIn, String tokenType,
            boolean mfaRequired, java.util.List<String> mfaMethods, UUID sessionId
    ) {
        static LoginResponse from(LoginResult r) {
            return new LoginResponse(
                    r.tokens().accessToken(), r.tokens().refreshToken(),
                    r.tokens().expiresIn(), r.tokens().tokenType(),
                    r.mfaRequired(), r.mfaMethods(), r.sessionId());
        }
    }

    public record TokenResponse(String accessToken, String refreshToken, long expiresIn, String tokenType) {
        static TokenResponse from(AuthTokens t) {
            return new TokenResponse(t.accessToken(), t.refreshToken(), t.expiresIn(), t.tokenType());
        }
    }
}
