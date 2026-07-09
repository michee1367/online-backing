package com.villagesat.auth.adapter.out.persistence;

import com.villagesat.auth.adapter.out.persistence.entity.MfaSecretEntity;
import com.villagesat.auth.domain.port.out.MfaSecretRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class MfaSecretRepositoryAdapter implements MfaSecretRepository {

    private final MfaSecretJpaRepository jpaRepository;

    public MfaSecretRepositoryAdapter(MfaSecretJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void saveEncryptedSecret(UUID userId, byte[] encryptedSecret, List<String> backupCodesHashed, boolean enabled) {
        MfaSecretEntity entity = jpaRepository.findById(userId).orElse(new MfaSecretEntity());
        entity.setUserId(userId);
        entity.setTotpSecretEnc(encryptedSecret);
        entity.setBackupCodesHash(backupCodesHashed);
        entity.setEnabled(enabled);
        jpaRepository.save(entity);
    }

    @Override
    public Optional<MfaRecord> findByUserId(UUID userId) {
        return jpaRepository.findById(userId).map(e ->
                new MfaRecord(e.getUserId(), e.getTotpSecretEnc(), e.getBackupCodesHash(), e.isEnabled()));
    }

    @Override
    public void enable(UUID userId) {
        jpaRepository.findById(userId).ifPresent(e -> {
            e.setEnabled(true);
            e.setConfirmedAt(Instant.now());
            jpaRepository.save(e);
        });
    }
}
