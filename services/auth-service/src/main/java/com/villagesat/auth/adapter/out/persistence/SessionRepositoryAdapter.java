package com.villagesat.auth.adapter.out.persistence;

import com.villagesat.auth.adapter.out.persistence.entity.SessionEntity;
import com.villagesat.auth.domain.model.AuthSession;
import com.villagesat.auth.domain.port.out.SessionRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SessionRepositoryAdapter implements SessionRepository {

    private final SessionJpaRepository jpaRepository;

    public SessionRepositoryAdapter(SessionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AuthSession save(AuthSession session) {
        return toDomain(jpaRepository.save(toEntity(session)));
    }

    @Override
    public Optional<AuthSession> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<AuthSession> findByRefreshTokenHash(String hash) {
        return jpaRepository.findByRefreshTokenHash(hash).map(this::toDomain);
    }

    @Override
    public List<AuthSession> findActiveByUserId(UUID userId) {
        return jpaRepository.findActiveByUserId(userId, Instant.now()).stream()
                .map(this::toDomain).toList();
    }

    @Override
    public void revokeAllForUser(UUID userId) {
        findActiveByUserId(userId).forEach(s -> save(s.revoke()));
    }

    private SessionEntity toEntity(AuthSession session) {
        SessionEntity entity = new SessionEntity();
        entity.setId(session.id());
        entity.setUserId(session.userId());
        entity.setRefreshTokenHash(session.refreshTokenHash());
        entity.setDeviceFingerprint(session.deviceFingerprint());
        entity.setIpAddress(session.ipAddress());
        entity.setUserAgent(session.userAgent());
        entity.setMfaVerified(session.mfaVerified());
        entity.setExpiresAt(session.expiresAt());
        entity.setRevokedAt(session.revokedAt());
        entity.setCreatedAt(session.createdAt());
        return entity;
    }

    private AuthSession toDomain(SessionEntity entity) {
        return new AuthSession(
                entity.getId(),
                entity.getUserId(),
                entity.getRefreshTokenHash(),
                entity.getDeviceFingerprint(),
                entity.getIpAddress(),
                entity.getUserAgent(),
                entity.isMfaVerified(),
                entity.getExpiresAt(),
                entity.getRevokedAt(),
                entity.getCreatedAt()
        );
    }
}
