package com.villagesat.auth.application.service;

import com.villagesat.auth.config.AuthProperties;
import com.villagesat.auth.domain.model.AuthSession;
import com.villagesat.auth.domain.model.AuthTokens;
import com.villagesat.auth.domain.model.LoginResult;
import com.villagesat.auth.domain.model.RegisteredUser;
import com.villagesat.auth.domain.port.in.AuthUseCase.*;
import com.villagesat.auth.domain.port.out.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — tests unitaires")
class AuthServiceTest {

    @Mock
    private IdentityProviderPort identityProvider;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private MfaSecretRepository mfaSecretRepository;
    @Mock
    private LoginAttemptPort loginAttemptPort;
    @Mock
    private TokenBlacklistPort tokenBlacklist;
    @Mock
    private UserRegistrationEventPublisher eventPublisher;

    private AuthService authService;
    private JwtClaimsParser jwtClaimsParser;

    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String FAKE_ACCESS_TOKEN = buildFakeJwt(USER_ID, "jti-123", Instant.now().plusSeconds(3600));
    private static final String FAKE_REFRESH_TOKEN = "refresh-token-value";

    @BeforeEach
    void setUp() {
        jwtClaimsParser = new JwtClaimsParser(new com.fasterxml.jackson.databind.ObjectMapper());
        AuthProperties props = new AuthProperties("VillageSat", "0123456789abcdef0123456789abcdef", 7, 3, 5, 15);
        authService = new AuthService(
                identityProvider, sessionRepository, mfaSecretRepository,
                loginAttemptPort, tokenBlacklist, eventPublisher, props, jwtClaimsParser);
    }

    @Test
    @DisplayName("register — succès avec termes acceptés")
    void register_success() {
        RegisterCommand command = new RegisterCommand(
                "user@test.com", "+33612345678", "P@ssword1234!", "Jean", "Dupont", "FR", true);
        when(identityProvider.registerUser(command)).thenReturn(USER_ID);

        RegisteredUser result = authService.register(command);

        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.email()).isEqualTo("user@test.com");
        assertThat(result.status()).isEqualTo("PENDING_VERIFICATION");
        verify(identityProvider).assignRealmRole(USER_ID, "CUSTOMER");
        verify(identityProvider).setUserAttribute(USER_ID, "mfa_verified", "false");
        verify(eventPublisher).publishUserRegistered(result, command);
    }

    @Test
    @DisplayName("register — échec si termes non acceptés")
    void register_termsNotAccepted() {
        RegisterCommand command = new RegisterCommand(
                "user@test.com", "+33612345678", "P@ssword1234!", "Jean", "Dupont", "FR", false);

        assertThatThrownBy(() -> authService.register(command))
                .isInstanceOf(AuthService.AuthException.class)
                .hasFieldOrPropertyWithValue("code", "TERMS_NOT_ACCEPTED");
    }

    @Test
    @DisplayName("login — succès sans MFA")
    void login_success_noMfa() {
        LoginCommand command = new LoginCommand("user@test.com", "P@ssword1234!", "fp-123", "192.168.1.1", "Mozilla");
        AuthTokens tokens = new AuthTokens(FAKE_ACCESS_TOKEN, FAKE_REFRESH_TOKEN, 3600, "Bearer");

        when(loginAttemptPort.isLocked("user@test.com")).thenReturn(false);
        when(identityProvider.authenticate("user@test.com", "P@ssword1234!")).thenReturn(tokens);
        when(mfaSecretRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(sessionRepository.findActiveByUserId(USER_ID)).thenReturn(List.of());
        when(sessionRepository.save(any(AuthSession.class))).thenAnswer(inv -> inv.getArgument(0));

        LoginResult result = authService.login(command);

        assertThat(result.tokens()).isEqualTo(tokens);
        assertThat(result.mfaRequired()).isFalse();
        assertThat(result.mfaMethods()).isEmpty();
        verify(loginAttemptPort).recordSuccess("user@test.com");
        verify(sessionRepository).save(any(AuthSession.class));
    }

    @Test
    @DisplayName("login — compte verrouillé (bruteforce)")
    void login_accountLocked() {
        LoginCommand command = new LoginCommand("locked@test.com", "pass", "fp", "1.2.3.4", "UA");

        when(loginAttemptPort.isLocked("locked@test.com")).thenReturn(true);
        when(loginAttemptPort.remainingLockSeconds("locked@test.com")).thenReturn(120L);

        assertThatThrownBy(() -> authService.login(command))
                .isInstanceOf(AuthService.AuthException.class)
                .hasFieldOrPropertyWithValue("code", "AUTH_ACCOUNT_LOCKED")
                .hasMessageContaining("120");
    }

    @Test
    @DisplayName("login — échec d'authentification enregistre un failure")
    void login_invalidCredentials_recordsFailure() {
        LoginCommand command = new LoginCommand("user@test.com", "bad", "fp", "1.2.3.4", "UA");

        when(loginAttemptPort.isLocked("user@test.com")).thenReturn(false);
        when(identityProvider.authenticate("user@test.com", "bad"))
                .thenThrow(new RuntimeException("invalid"));

        assertThatThrownBy(() -> authService.login(command))
                .isInstanceOf(AuthService.AuthException.class)
                .hasFieldOrPropertyWithValue("code", "AUTH_INVALID_CREDENTIALS");
        verify(loginAttemptPort).recordFailure("user@test.com", "INVALID_CREDENTIALS");
    }

    @Test
    @DisplayName("refresh — succès avec session active")
    void refresh_success() {
        String refreshToken = "old-refresh-token";
        String hash = AuthService.hashToken(refreshToken);
        AuthSession session = new AuthSession(
                UUID.randomUUID(), USER_ID, hash, "fp", "1.2.3.4", "UA",
                true, Instant.now().plus(7, ChronoUnit.DAYS), null, Instant.now());
        AuthTokens newTokens = new AuthTokens("new-access", "new-refresh", 3600, "Bearer");

        when(sessionRepository.findByRefreshTokenHash(hash)).thenReturn(Optional.of(session));
        when(identityProvider.refreshToken(refreshToken)).thenReturn(newTokens);
        when(sessionRepository.save(any(AuthSession.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthTokens result = authService.refresh(new RefreshCommand(refreshToken, "1.2.3.4"));

        assertThat(result).isEqualTo(newTokens);
        verify(sessionRepository).save(argThat(s -> s.refreshTokenHash().equals(AuthService.hashToken("new-refresh"))));
    }

    @Test
    @DisplayName("refresh — échec si session inexistante")
    void refresh_invalidToken() {
        when(sessionRepository.findByRefreshTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshCommand("bad-token", "1.2.3.4")))
                .isInstanceOf(AuthService.AuthException.class)
                .hasFieldOrPropertyWithValue("code", "AUTH_INVALID_REFRESH_TOKEN");
    }

    @Test
    @DisplayName("logout — blacklist le JTI et révoque la session")
    void logout_success() {
        String hash = AuthService.hashToken(FAKE_REFRESH_TOKEN);
        AuthSession session = new AuthSession(
                UUID.randomUUID(), USER_ID, hash, "fp", "1.2.3.4", "UA",
                true, Instant.now().plus(7, ChronoUnit.DAYS), null, Instant.now());

        when(sessionRepository.findByRefreshTokenHash(hash)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(AuthSession.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.logout(new LogoutCommand(FAKE_ACCESS_TOKEN, FAKE_REFRESH_TOKEN, USER_ID));

        verify(tokenBlacklist).blacklist(eq("jti-123"), anyLong());
        verify(identityProvider).revokeRefreshToken(FAKE_REFRESH_TOKEN);
        verify(sessionRepository).save(argThat(s -> s.revokedAt() != null));
        verify(identityProvider).setUserAttribute(USER_ID, "mfa_verified", "false");
    }

    /**
     * Construit un JWT fictif (non signé) pour les tests unitaires.
     */
    private static String buildFakeJwt(UUID sub, String jti, Instant exp) {
        String header = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes());
        String payload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"sub\":\"" + sub + "\",\"jti\":\"" + jti
                        + "\",\"exp\":" + exp.getEpochSecond()
                        + ",\"email\":\"user@test.com\"}").getBytes());
        return header + "." + payload + ".signature";
    }
}
