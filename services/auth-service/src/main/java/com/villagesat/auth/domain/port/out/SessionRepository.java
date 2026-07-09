package com.villagesat.auth.domain.port.out;

import com.villagesat.auth.domain.model.AuthSession;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository {

    AuthSession save(AuthSession session);

    Optional<AuthSession> findById(UUID id);

    Optional<AuthSession> findByRefreshTokenHash(String hash);

    List<AuthSession> findActiveByUserId(UUID userId);

    void revokeAllForUser(UUID userId);
}
