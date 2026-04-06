package com.froneus.dinosaur.domain.port.out;

import java.util.Optional;

/**
 * Puerto de salida para idempotencia via Redis.
 *
 * Convención de clave (alineada con redis_idempotency_reference.md):
 *   Header HTTP:  Idempotency-Key: idem-{uuid}
 *   Key en Redis: idempotency:idem-{uuid}
 *   Valor:        dinosaurId (Long como String)
 *
 * TTL: 24 horas
 */
public interface DinosaurIdempotencyPort {
    boolean exists(String idempotencyKey);
    void store(String idempotencyKey, Long dinosaurId);
    Optional<Long> getDinosaurId(String idempotencyKey);
}
