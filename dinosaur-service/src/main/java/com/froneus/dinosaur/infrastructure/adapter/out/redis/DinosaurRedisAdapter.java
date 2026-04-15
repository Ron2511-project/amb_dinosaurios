package com.froneus.dinosaur.infrastructure.adapter.out.redis;

import com.froneus.dinosaur.domain.port.out.DinosaurIdempotencyPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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
 * Si Redis no está disponible el Circuit Breaker actúa:
 *   - exists()        → fallback retorna false (permite continuar sin idempotencia)
 *   - store()         → fallback loguea y continúa (no bloquea la operación)
 *   - getDinosaurId() → fallback retorna Optional.empty() (no hay replay)
 */
@Component
public class DinosaurRedisAdapter implements DinosaurIdempotencyPort {

    private static final Logger   log        = LoggerFactory.getLogger(DinosaurRedisAdapter.class);
    private static final String   KEY_PREFIX = "idempotency:";
    private static final Duration TTL        = Duration.ofHours(24);
    private static final String   CB         = "dinosaurService";

    private final StringRedisTemplate redisTemplate;

    public DinosaurRedisAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    @CircuitBreaker(name = CB, fallbackMethod = "existsFallback")
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(buildKey(key)));
    }

    @Override
    @CircuitBreaker(name = CB, fallbackMethod = "storeFallback")
    public void store(String key, Long dinosaurId) {
        String redisKey = buildKey(key);
        redisTemplate.opsForValue().set(redisKey, String.valueOf(dinosaurId), TTL);
        log.info("Idempotency stored — key={} dinosaurId={} ttl=24h", redisKey, dinosaurId);
    }

    @Override
    @CircuitBreaker(name = CB, fallbackMethod = "getDinosaurIdFallback")
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

    // ── Fallbacks ─────────────────────────────────────────────────────────────

    public boolean existsFallback(String key, Throwable t) {
        log.warn("CB open — Redis unavailable, exists() fallback key={} error={}", key, t.getMessage());
        return false;
    }

    public void storeFallback(String key, Long dinosaurId, Throwable t) {
        log.warn("CB open — Redis unavailable, store() fallback key={} dinosaurId={} error={}",
                key, dinosaurId, t.getMessage());
    }

    public Optional<Long> getDinosaurIdFallback(String key, Throwable t) {
        log.warn("CB open — Redis unavailable, getDinosaurId() fallback key={} error={}", key, t.getMessage());
        return Optional.empty();
    }
}
