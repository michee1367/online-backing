package com.villagesat.auth.adapter.out.persistence;

import com.villagesat.auth.adapter.out.persistence.entity.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionJpaRepository extends JpaRepository<SessionEntity, UUID> {

    Optional<SessionEntity> findByRefreshTokenHash(String refreshTokenHash);

    @Query("SELECT s FROM SessionEntity s WHERE s.userId = :userId AND s.revokedAt IS NULL AND s.expiresAt > :now")
    List<SessionEntity> findActiveByUserId(@Param("userId") UUID userId, @Param("now") Instant now);
}
