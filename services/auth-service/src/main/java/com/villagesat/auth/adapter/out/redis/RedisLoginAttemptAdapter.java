package com.villagesat.auth.adapter.out.redis;

import com.villagesat.auth.config.AuthProperties;
import com.villagesat.auth.domain.port.out.LoginAttemptPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

@Component
public class RedisLoginAttemptAdapter implements LoginAttemptPort {

    private static final String ATTEMPTS_PREFIX = "auth:login:attempts:";
    private static final String LOCK_PREFIX = "auth:login:lock:";

    private final StringRedisTemplate redis;
    private final AuthProperties authProperties;

    public RedisLoginAttemptAdapter(StringRedisTemplate redis, AuthProperties authProperties) {
        this.redis = redis;
        this.authProperties = authProperties;
    }

    @Override
    public void recordSuccess(String email) {
        String key = hashEmail(email);
        redis.delete(ATTEMPTS_PREFIX + key);
        redis.delete(LOCK_PREFIX + key);
    }

    @Override
    public void recordFailure(String email, String reason) {
        String key = hashEmail(email);
        String attemptsKey = ATTEMPTS_PREFIX + key;
        Long attempts = redis.opsForValue().increment(attemptsKey);
        redis.expire(attemptsKey, Duration.ofMinutes(authProperties.loginLockMinutes()));

        if (attempts != null && attempts >= authProperties.loginMaxAttempts()) {
            redis.opsForValue().set(LOCK_PREFIX + key, reason,
                    Duration.ofMinutes(authProperties.loginLockMinutes()));
        }
    }

    @Override
    public boolean isLocked(String email) {
        return Boolean.TRUE.equals(redis.hasKey(LOCK_PREFIX + hashEmail(email)));
    }

    @Override
    public long remainingLockSeconds(String email) {
        Long ttl = redis.getExpire(LOCK_PREFIX + hashEmail(email));
        return ttl != null && ttl > 0 ? ttl : 0;
    }

    private String hashEmail(String email) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(email.toLowerCase().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
