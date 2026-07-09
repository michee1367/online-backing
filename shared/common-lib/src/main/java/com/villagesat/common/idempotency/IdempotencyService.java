package com.villagesat.common.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Gestion idempotence via Redis — garantit qu'une opération financière
 * n'est exécutée qu'une seule fois pour un Idempotency-Key donné.
 */
@Component
public class IdempotencyService {

    private static final Duration TTL = Duration.ofHours(24);
    private static final String PREFIX = "idempotency:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public IdempotencyService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public <T> Optional<T> getCachedResponse(UUID key, Class<T> responseType) {
        String cached = redis.opsForValue().get(PREFIX + key);
        if (cached == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(cached, responseType));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void storeResponse(UUID key, Object response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redis.opsForValue().set(PREFIX + key, json, TTL);
        } catch (Exception e) {
            throw new IdempotencyStoreException("Failed to store idempotency response", e);
        }
    }

    public boolean tryAcquireLock(UUID key) {
        Boolean acquired = redis.opsForValue()
                .setIfAbsent(PREFIX + key + ":lock", "1", Duration.ofMinutes(5));
        return Boolean.TRUE.equals(acquired);
    }

    public static class IdempotencyStoreException extends RuntimeException {
        public IdempotencyStoreException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
