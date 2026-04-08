package com.froneus.dinosaur.infrastructure.adapter.out.redis;

import com.froneus.dinosaur.domain.port.out.DinosaurIdempotencyPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Adaptador Redis para idempotencia.
 *
 * Estructura en Redis:
 *   Key:   idempotency:idem-{uuid}
 *   Value: dinosaurId como String (ej: "21")
 *   TTL:   24 horas
 *
 * Ejemplo:
 *   Key:   idempotency:idem-a1b2c3d4-0001-0001-0001-ef1234567890
 *   Value: "21"
 *   TTL:   86400 segundos
 *
 * Verificar desde terminal:
 *   redis-cli GET "idempotency:idem-a1b2c3d4-0001-0001-0001-ef1234567890"
 *   → "21"
 *
 *   redis-cli TTL "idempotency:idem-a1b2c3d4-0001-0001-0001-ef1234567890"
 *   → 86352 (segundos restantes)
 */
@Component
public class DinosaurRedisAdapter implements DinosaurIdempotencyPort {

    private static final Logger   log        = LoggerFactory.getLogger(DinosaurRedisAdapter.class);
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
        String redisKey = buildKey(key);
        redisTemplate.opsForValue().set(redisKey, String.valueOf(dinosaurId), TTL);
        log.info("Idempotency stored — key={} dinosaurId={} ttl=24h", redisKey, dinosaurId);
    }

    @Override
    public Optional<Long> getDinosaurId(String key) {
        String val = redisTemplate.opsForValue().get(buildKey(key));
        if (val != null) {
            log.info("Idempotency hit — key={} dinosaurId={}", buildKey(key), val);
        }
        return Optional.ofNullable(val).map(Long::parseLong);
    }

    private String buildKey(String key) {
        return KEY_PREFIX + key;
    }
}
