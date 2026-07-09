package com.villagesat.auth.application.service;

import com.villagesat.auth.config.AuthProperties;
import com.villagesat.auth.domain.model.AuthSession;
import com.villagesat.auth.domain.model.AuthTokens;
import com.villagesat.auth.domain.model.MfaSetup;
import com.villagesat.auth.domain.port.in.MfaUseCase;
import com.villagesat.auth.domain.port.out.IdentityProviderPort;
import com.villagesat.auth.domain.port.out.MfaSecretRepository;
import com.villagesat.auth.domain.port.out.SessionRepository;
import com.villagesat.auth.adapter.out.crypto.SecretEncryptionService;
import com.villagesat.auth.adapter.out.crypto.TotpGenerator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MfaService implements MfaUseCase {

    private static final int BACKUP_CODE_COUNT = 8;

    private final MfaSecretRepository mfaSecretRepository;
    private final SessionRepository sessionRepository;
    private final IdentityProviderPort identityProvider;
    private final SecretEncryptionService encryptionService;
    private final TotpGenerator totpGenerator;
    private final AuthProperties authProperties;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    private final SecureRandom secureRandom = new SecureRandom();

    public MfaService(MfaSecretRepository mfaSecretRepository,
                      SessionRepository sessionRepository,
                      IdentityProviderPort identityProvider,
                      SecretEncryptionService encryptionService,
                      TotpGenerator totpGenerator,
                      AuthProperties authProperties) {
        this.mfaSecretRepository = mfaSecretRepository;
        this.sessionRepository = sessionRepository;
        this.identityProvider = identityProvider;
        this.encryptionService = encryptionService;
        this.totpGenerator = totpGenerator;
        this.authProperties = authProperties;
    }

    @Override
    @Transactional
    public MfaSetup initiateSetup(UUID userId, String email) {
        String secret = totpGenerator.generateSecret();
        byte[] encrypted = encryptionService.encrypt(secret.getBytes());

        List<String> backupCodes = generateBackupCodes();
        List<String> hashed = backupCodes.stream().map(passwordEncoder::encode).toList();

        mfaSecretRepository.saveEncryptedSecret(userId, encrypted, hashed, false);

        String qrUri = totpGenerator.buildQrUri(authProperties.mfaIssuer(), email, secret);
        return new MfaSetup(secret, qrUri, backupCodes);
    }

    @Override
    @Transactional
    public void confirmSetup(UUID userId, String code) {
        MfaSecretRepository.MfaRecord record = mfaSecretRepository.findByUserId(userId)
                .orElseThrow(() -> new MfaException("MFA_NOT_INITIATED", "Configuration MFA non démarrée"));

        String secret = new String(encryptionService.decrypt(record.totpSecretEnc()));
        if (!totpGenerator.verify(secret, code)) {
            throw new MfaException("MFA_INVALID_CODE", "Code TOTP invalide");
        }
        mfaSecretRepository.enable(userId);
    }

    @Override
    @Transactional
    public AuthTokens verifyLoginMfa(VerifyMfaCommand command) {
        AuthSession session = sessionRepository.findById(command.sessionId())
                .filter(AuthSession::isActive)
                .orElseThrow(() -> new MfaException("AUTH_SESSION_INVALID", "Session invalide ou expirée"));

        if (session.mfaVerified()) {
            throw new MfaException("MFA_ALREADY_VERIFIED", "MFA déjà validé pour cette session");
        }

        if (!"TOTP".equalsIgnoreCase(command.method())) {
            throw new MfaException("MFA_METHOD_UNSUPPORTED", "Méthode MFA non supportée");
        }

        MfaSecretRepository.MfaRecord record = mfaSecretRepository.findByUserId(session.userId())
                .filter(MfaSecretRepository.MfaRecord::enabled)
                .orElseThrow(() -> new MfaException("MFA_NOT_ENABLED", "MFA non activé"));

        String secret = new String(encryptionService.decrypt(record.totpSecretEnc()));
        boolean validTotp = totpGenerator.verify(secret, command.code());
        boolean validBackup = !validTotp && verifyBackupCode(record.backupCodesHash(), command.code());

        if (!validTotp && !validBackup) {
            throw new MfaException("MFA_INVALID_CODE", "Code TOTP ou backup invalide");
        }

        identityProvider.setUserAttribute(session.userId(), "mfa_verified", "true");
        sessionRepository.save(session.withMfaVerified());

        AuthTokens tokens = identityProvider.refreshToken(command.refreshToken());

        sessionRepository.save(new AuthSession(
                session.id(),
                session.userId(),
                AuthService.hashToken(tokens.refreshToken()),
                session.deviceFingerprint(),
                session.ipAddress(),
                session.userAgent(),
                true,
                session.expiresAt(),
                null,
                session.createdAt()
        ));

        return tokens;
    }

    @Override
    public boolean isMfaEnabled(UUID userId) {
        return mfaSecretRepository.findByUserId(userId)
                .map(MfaSecretRepository.MfaRecord::enabled)
                .orElse(false);
    }

    private boolean verifyBackupCode(List<String> hashes, String code) {
        return hashes != null && hashes.stream().anyMatch(h -> passwordEncoder.matches(code, h));
    }

    private List<String> generateBackupCodes() {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < BACKUP_CODE_COUNT; i++) {
            codes.add(String.format("%08d", secureRandom.nextInt(100_000_000)));
        }
        return codes;
    }

    public static class MfaException extends RuntimeException {
        private final String code;

        public MfaException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() { return code; }
    }
}
