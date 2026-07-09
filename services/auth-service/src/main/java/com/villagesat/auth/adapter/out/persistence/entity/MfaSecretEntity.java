package com.villagesat.auth.adapter.out.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "mfa_secrets", schema = "auth")
public class MfaSecretEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "totp_secret_enc", nullable = false)
    private byte[] totpSecretEnc;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "backup_codes_hash", columnDefinition = "text[]")
    private List<String> backupCodesHash;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public byte[] getTotpSecretEnc() { return totpSecretEnc; }
    public void setTotpSecretEnc(byte[] totpSecretEnc) { this.totpSecretEnc = totpSecretEnc; }
    public List<String> getBackupCodesHash() { return backupCodesHash; }
    public void setBackupCodesHash(List<String> backupCodesHash) { this.backupCodesHash = backupCodesHash; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant confirmedAt) { this.confirmedAt = confirmedAt; }
}
