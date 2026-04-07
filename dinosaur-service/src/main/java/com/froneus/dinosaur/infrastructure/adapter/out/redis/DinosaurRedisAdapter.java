package com.froneus.dinosaur.infrastructure.adapter.out.redis;

import com.froneus.dinosaur.domain.port.out.DinosaurIdempotencyPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Adaptador Redis para idempotencia.
 * Key: "idempotency:idem-{uuid}" → dinosaurId (Long)
 * TTL: 24 horas
 */
@Component
public class DinosaurRedisAdapter implements DinosaurIdempotencyPort {

    private static final String   KEY_PREFIX = "idempotency:";
    private static final Duration TTL        = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    public DinosaurRedisAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(buildKey(key)));
    }

    @Override
    public void store(String key, Long dinosaurId) {
        redisTemplate.opsForValue().set(buildKey(key), String.valueOf(dinosaurId), TTL);
    }

    @Override
    public Optional<Long> getDinosaurId(String key) {
        String val = redisTemplate.opsForValue().get(buildKey(key));
        return Optional.ofNullable(val).map(Long::parseLong);
    }

    private String buildKey(String key) {
        return KEY_PREFIX + key;
    }
}
