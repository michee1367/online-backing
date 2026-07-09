package com.villagesat.auth.application.service;

import com.villagesat.auth.config.AuthProperties;
import com.villagesat.auth.domain.model.*;
import com.villagesat.auth.domain.port.in.AuthUseCase;
import com.villagesat.auth.domain.port.out.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService implements AuthUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final IdentityProviderPort identityProvider;
    private final SessionRepository sessionRepository;
    private final MfaSecretRepository mfaSecretRepository;
    private final LoginAttemptPort loginAttemptPort;
    private final TokenBlacklistPort tokenBlacklist;
    private final UserRegistrationEventPublisher eventPublisher;
    private final AuthProperties authProperties;
    private final JwtClaimsParser jwtClaimsParser;

    public AuthService(IdentityProviderPort identityProvider,
                       SessionRepository sessionRepository,
                       MfaSecretRepository mfaSecretRepository,
                       LoginAttemptPort loginAttemptPort,
                       TokenBlacklistPort tokenBlacklist,
                       UserRegistrationEventPublisher eventPublisher,
                       AuthProperties authProperties,
                       JwtClaimsParser jwtClaimsParser) {
        this.identityProvider = identityProvider;
        this.sessionRepository = sessionRepository;
        this.mfaSecretRepository = mfaSecretRepository;
        this.loginAttemptPort = loginAttemptPort;
        this.tokenBlacklist = tokenBlacklist;
        this.eventPublisher = eventPublisher;
        this.authProperties = authProperties;
        this.jwtClaimsParser = jwtClaimsParser;
    }

    @Override
    @Transactional
    public RegisteredUser register(RegisterCommand command) {
        if (!command.acceptedTerms()) {
            throw new AuthException("TERMS_NOT_ACCEPTED", "Vous devez accepter les conditions d'utilisation");
        }

        UUID userId = identityProvider.registerUser(command);
        identityProvider.assignRealmRole(userId, "CUSTOMER");
        identityProvider.setUserAttribute(userId, "mfa_verified", "false");

        RegisteredUser user = new RegisteredUser(
                userId, command.email(), 0, "PENDING_VERIFICATION", true);
        eventPublisher.publishUserRegistered(user, command);
        return user;
    }

    @Override
    @Transactional
    public LoginResult login(LoginCommand command) {
        if (loginAttemptPort.isLocked(command.email())) {
            long seconds = loginAttemptPort.remainingLockSeconds(command.email());
            throw new AuthException("AUTH_ACCOUNT_LOCKED",
                    "Compte temporairement verrouillé. Réessayez dans " + seconds + " secondes");
        }

        try {
            AuthTokens tokens = identityProvider.authenticate(command.email(), command.password());
            UUID userId = jwtClaimsParser.extractUserId(tokens.accessToken());

            boolean mfaEnabled = mfaSecretRepository.findByUserId(userId)
                    .map(MfaSecretRepository.MfaRecord::enabled)
                    .orElse(false);

            String sessionRefreshToken = tokens.refreshToken() != null
                    ? tokens.refreshToken()
                    : tokens.accessToken();
            AuthSession session = persistSession(userId, sessionRefreshToken, command, mfaEnabled);

            loginAttemptPort.recordSuccess(command.email());

            if (mfaEnabled) {
                identityProvider.setUserAttribute(userId, "mfa_verified", "false");
            }

            return new LoginResult(
                    tokens,
                    mfaEnabled,
                    mfaEnabled ? List.of("TOTP") : List.of(),
                    session.id()
            );
        } catch (AuthException e) {
            loginAttemptPort.recordFailure(command.email(), e.getCode());
            throw e;
        } catch (Exception e) {
            log.warn("Login failed for {}: {}", command.email(), e.toString());
            loginAttemptPort.recordFailure(command.email(), "INVALID_CREDENTIALS");
            throw new AuthException("AUTH_INVALID_CREDENTIALS", "Email ou mot de passe incorrect");
        }
    }

    @Override
    @Transactional
    public AuthTokens refresh(RefreshCommand command) {
        String hash = hashToken(command.refreshToken());
        AuthSession session = sessionRepository.findByRefreshTokenHash(hash)
                .filter(AuthSession::isActive)
                .orElseThrow(() -> new AuthException("AUTH_INVALID_REFRESH_TOKEN", "Refresh token invalide ou expiré"));

        AuthTokens tokens = identityProvider.refreshToken(command.refreshToken());
        sessionRepository.save(new AuthSession(
                session.id(), session.userId(), hashToken(tokens.refreshToken()),
                session.deviceFingerprint(), session.ipAddress(), session.userAgent(),
                session.mfaVerified(), session.expiresAt(), null, session.createdAt()));
        return tokens;
    }

    @Override
    @Transactional
    public void logout(LogoutCommand command) {
        if (command.accessToken() != null) {
            jwtClaimsParser.extractJti(command.accessToken()).ifPresent(jti ->
                    tokenBlacklist.blacklist(jti, jwtClaimsParser.remainingTtlSeconds(command.accessToken())));
        }
        if (command.refreshToken() != null) {
            identityProvider.revokeRefreshToken(command.refreshToken());
            sessionRepository.findByRefreshTokenHash(hashToken(command.refreshToken()))
                    .ifPresent(s -> sessionRepository.save(s.revoke()));
        }
        if (command.userId() != null) {
            identityProvider.setUserAttribute(command.userId(), "mfa_verified", "false");
        }
    }

    private AuthSession persistSession(UUID userId, String refreshToken, LoginCommand command, boolean mfaEnabled) {
        enforceSessionLimit(userId);

        Instant expiresAt = Instant.now().plus(authProperties.sessionRefreshDays(), ChronoUnit.DAYS);
        AuthSession session = new AuthSession(
                UUID.randomUUID(),
                userId,
                hashToken(refreshToken),
                command.deviceFingerprint(),
                command.ipAddress(),
                command.userAgent(),
                !mfaEnabled,
                expiresAt,
                null,
                Instant.now()
        );
        return sessionRepository.save(session);
    }

    private void enforceSessionLimit(UUID userId) {
        List<AuthSession> active = sessionRepository.findActiveByUserId(userId);
        if (active.size() >= authProperties.maxSessionsPerUser()) {
            active.stream()
                    .sorted((a, b) -> a.createdAt().compareTo(b.createdAt()))
                    .limit(active.size() - authProperties.maxSessionsPerUser() + 1L)
                    .forEach(s -> sessionRepository.save(s.revoke()));
        }
    }

    static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static class AuthException extends RuntimeException {
        private final String code;

        public AuthException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() { return code; }
    }
}
