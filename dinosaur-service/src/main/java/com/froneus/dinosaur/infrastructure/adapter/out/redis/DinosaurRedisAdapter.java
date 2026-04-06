package com.froneus.dinosaur.infrastructure.adapter.out.redis;

import com.froneus.dinosaur.domain.port.out.DinosaurIdempotencyPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Adaptador de salida: idempotencia en Redis.
 *
 * Convención alineada con redis_idempotency_reference.md:
 *   Header:    Idempotency-Key: idem-{uuid}
 *   Key Redis: idempotency:idem-{uuid}
 *   Valor:     dinosaurId (Long como String)
 *   TTL:       86400s (24 horas)
 *
 * Flujo:
 *   1. Controller recibe header Idempotency-Key
 *   2. Consulta Redis → si existe devuelve respuesta cacheada
 *   3. Si no existe → crea en Postgres → guarda en Redis
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
    public boolean exists(String idempotencyKey) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(buildKey(idempotencyKey)));
    }

    @Override
    public void store(String idempotencyKey, Long dinosaurId) {
        redisTemplate.opsForValue()
                .set(buildKey(idempotencyKey), String.valueOf(dinosaurId), TTL);
    }

    @Override
    public Optional<Long> getDinosaurId(String idempotencyKey) {
        String value = redisTemplate.opsForValue().get(buildKey(idempotencyKey));
        return Optional.ofNullable(value).map(Long::parseLong);
    }

    private String buildKey(String key) {
        return KEY_PREFIX + key;
    }
}
