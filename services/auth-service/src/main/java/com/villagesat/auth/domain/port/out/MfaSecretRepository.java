package com.villagesat.auth.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MfaSecretRepository {

    void saveEncryptedSecret(UUID userId, byte[] encryptedSecret, List<String> backupCodesHashed, boolean enabled);

    Optional<MfaRecord> findByUserId(UUID userId);

    void enable(UUID userId);

    record MfaRecord(UUID userId, byte[] totpSecretEnc, List<String> backupCodesHash, boolean enabled) {}
}
