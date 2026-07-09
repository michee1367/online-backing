package com.villagesat.auth.adapter.out.redis;

import com.villagesat.auth.domain.port.out.TokenBlacklistPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisTokenBlacklistAdapter implements TokenBlacklistPort {

    private static final String PREFIX = "auth:token:blacklist:";

    private final StringRedisTemplate redis;

    public RedisTokenBlacklistAdapter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void blacklist(String jti, long ttlSeconds) {
        if (jti != null && ttlSeconds > 0) {
            redis.opsForValue().set(PREFIX + jti, "1", Duration.ofSeconds(ttlSeconds));
        }
    }

    @Override
    public boolean isBlacklisted(String jti) {
        return jti != null && Boolean.TRUE.equals(redis.hasKey(PREFIX + jti));
    }
}
