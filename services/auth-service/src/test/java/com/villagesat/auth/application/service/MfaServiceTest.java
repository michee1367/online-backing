package com.villagesat.auth.application.service;

import com.villagesat.auth.adapter.out.crypto.SecretEncryptionService;
import com.villagesat.auth.adapter.out.crypto.TotpGenerator;
import com.villagesat.auth.config.AuthProperties;
import com.villagesat.auth.domain.model.AuthSession;
import com.villagesat.auth.domain.model.AuthTokens;
import com.villagesat.auth.domain.model.MfaSetup;
import com.villagesat.auth.domain.port.in.MfaUseCase.VerifyMfaCommand;
import com.villagesat.auth.domain.port.out.IdentityProviderPort;
import com.villagesat.auth.domain.port.out.MfaSecretRepository;
import com.villagesat.auth.domain.port.out.MfaSecretRepository.MfaRecord;
import com.villagesat.auth.domain.port.out.SessionRepository;
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
@DisplayName("MfaService — tests unitaires")
class MfaServiceTest {

    @Mock
    private MfaSecretRepository mfaSecretRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private IdentityProviderPort identityProvider;
    @Mock
    private SecretEncryptionService encryptionService;
    @Mock
    private TotpGenerator totpGenerator;

    private MfaService mfaService;

    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String SECRET = "JBSWY3DPEHPK3PXP";

    @BeforeEach
    void setUp() {
        AuthProperties props = new AuthProperties("VillageSat", "0123456789abcdef0123456789abcdef", 7, 3, 5, 15);
        mfaService = new MfaService(
                mfaSecretRepository, sessionRepository, identityProvider,
                encryptionService, totpGenerator, props);
    }

    @Test
    @DisplayName("initiateSetup — génère un secret et des backup codes")
    void initiateSetup_success() {
        when(totpGenerator.generateSecret()).thenReturn(SECRET);
        when(encryptionService.encrypt(SECRET.getBytes())).thenReturn(new byte[]{1, 2, 3});
        when(totpGenerator.buildQrUri(eq("VillageSat"), eq("user@test.com"), eq(SECRET)))
                .thenReturn("otpauth://totp/VillageSat:user@test.com?secret=" + SECRET);

        MfaSetup result = mfaService.initiateSetup(USER_ID, "user@test.com");

        assertThat(result.secret()).isEqualTo(SECRET);
        assertThat(result.qrCodeUri()).contains("otpauth://totp/");
        assertThat(result.backupCodes()).hasSize(8);
        verify(mfaSecretRepository).saveEncryptedSecret(eq(USER_ID), any(byte[].class), anyList(), eq(false));
    }

    @Test
    @DisplayName("confirmSetup — succès avec code TOTP valide")
    void confirmSetup_validCode() {
        byte[] encrypted = new byte[]{1, 2, 3};
        MfaRecord record = new MfaRecord(USER_ID, encrypted, List.of(), false);

        when(mfaSecretRepository.findByUserId(USER_ID)).thenReturn(Optional.of(record));
        when(encryptionService.decrypt(encrypted)).thenReturn(SECRET.getBytes());
        when(totpGenerator.verify(SECRET, "123456")).thenReturn(true);

        mfaService.confirmSetup(USER_ID, "123456");

        verify(mfaSecretRepository).enable(USER_ID);
    }

    @Test
    @DisplayName("confirmSetup — échec avec code TOTP invalide")
    void confirmSetup_invalidCode() {
        byte[] encrypted = new byte[]{1, 2, 3};
        MfaRecord record = new MfaRecord(USER_ID, encrypted, List.of(), false);

        when(mfaSecretRepository.findByUserId(USER_ID)).thenReturn(Optional.of(record));
        when(encryptionService.decrypt(encrypted)).thenReturn(SECRET.getBytes());
        when(totpGenerator.verify(SECRET, "000000")).thenReturn(false);

        assertThatThrownBy(() -> mfaService.confirmSetup(USER_ID, "000000"))
                .isInstanceOf(MfaService.MfaException.class)
                .hasFieldOrPropertyWithValue("code", "MFA_INVALID_CODE");
        verify(mfaSecretRepository, never()).enable(any());
    }

    @Test
    @DisplayName("confirmSetup — échec si MFA non initié")
    void confirmSetup_notInitiated() {
        when(mfaSecretRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mfaService.confirmSetup(USER_ID, "123456"))
                .isInstanceOf(MfaService.MfaException.class)
                .hasFieldOrPropertyWithValue("code", "MFA_NOT_INITIATED");
    }

    @Test
    @DisplayName("verifyLoginMfa — succès avec code TOTP valide")
    void verifyLoginMfa_validTotp() {
        UUID sessionId = UUID.randomUUID();
        AuthSession session = new AuthSession(
                sessionId, USER_ID, "hash", "fp", "1.2.3.4", "UA",
                false, Instant.now().plus(7, ChronoUnit.DAYS), null, Instant.now());
        byte[] encrypted = new byte[]{1, 2, 3};
        MfaRecord record = new MfaRecord(USER_ID, encrypted, List.of(), true);
        AuthTokens newTokens = new AuthTokens("access", "refresh", 3600, "Bearer");

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(mfaSecretRepository.findByUserId(USER_ID)).thenReturn(Optional.of(record));
        when(encryptionService.decrypt(encrypted)).thenReturn(SECRET.getBytes());
        when(totpGenerator.verify(SECRET, "123456")).thenReturn(true);
        when(identityProvider.refreshToken("old-refresh")).thenReturn(newTokens);
        when(sessionRepository.save(any(AuthSession.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthTokens result = mfaService.verifyLoginMfa(new VerifyMfaCommand(sessionId, "TOTP", "123456", "old-refresh"));

        assertThat(result).isEqualTo(newTokens);
        verify(identityProvider).setUserAttribute(USER_ID, "mfa_verified", "true");
    }

    @Test
    @DisplayName("verifyLoginMfa — échec avec code invalide")
    void verifyLoginMfa_invalidCode() {
        UUID sessionId = UUID.randomUUID();
        AuthSession session = new AuthSession(
                sessionId, USER_ID, "hash", "fp", "1.2.3.4", "UA",
                false, Instant.now().plus(7, ChronoUnit.DAYS), null, Instant.now());
        byte[] encrypted = new byte[]{1, 2, 3};
        MfaRecord record = new MfaRecord(USER_ID, encrypted, List.of(), true);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(mfaSecretRepository.findByUserId(USER_ID)).thenReturn(Optional.of(record));
        when(encryptionService.decrypt(encrypted)).thenReturn(SECRET.getBytes());
        when(totpGenerator.verify(SECRET, "000000")).thenReturn(false);

        assertThatThrownBy(() -> mfaService.verifyLoginMfa(
                new VerifyMfaCommand(sessionId, "TOTP", "000000", "old-refresh")))
                .isInstanceOf(MfaService.MfaException.class)
                .hasFieldOrPropertyWithValue("code", "MFA_INVALID_CODE");
    }

    @Test
    @DisplayName("verifyLoginMfa — échec si session déjà vérifiée")
    void verifyLoginMfa_alreadyVerified() {
        UUID sessionId = UUID.randomUUID();
        AuthSession session = new AuthSession(
                sessionId, USER_ID, "hash", "fp", "1.2.3.4", "UA",
                true, Instant.now().plus(7, ChronoUnit.DAYS), null, Instant.now());

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> mfaService.verifyLoginMfa(
                new VerifyMfaCommand(sessionId, "TOTP", "123456", "old-refresh")))
                .isInstanceOf(MfaService.MfaException.class)
                .hasFieldOrPropertyWithValue("code", "MFA_ALREADY_VERIFIED");
    }
}
