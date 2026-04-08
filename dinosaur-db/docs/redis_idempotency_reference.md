-- =============================================================================
-- SCRIPT 05: IDEMPOTENCIA EN REDIS
-- Orden de ejecución: independiente de los scripts SQL de PostgreSQL
-- =============================================================================

-- =============================================================================
-- CONVENCIÓN DE IDENTIFICADORES
-- =============================================================================

/*
  Dinosaurio (PostgreSQL):
    id:   6934b86b-324c-45d4-93f5-d5f025e8fb7b      ← UUID interno
    code: dino-6934b86b-324c-45d4-93f5-d5f025e8fb7b  ← expuesto en API

  Idempotency key (Redis):
    header HTTP:  idem-6934b86b-324c-45d4-93f5-d5f025e8fb7b
    key en Redis: idempotency:idem-6934b86b-324c-45d4-93f5-d5f025e8fb7b

  El UUID base es el mismo para ambos — el prefijo (dino- / idem-)
  identifica de qué entidad se trata sin ambigüedad.
*/


-- =============================================================================
-- SECCIÓN 1 — ESTRUCTURA DE LA CLAVE
-- =============================================================================

/*
  Patrón:  idempotency:idem-{uuid}
  Ejemplo: idempotency:idem-6934b86b-324c-45d4-93f5-d5f025e8fb7b

  Valor almacenado (JSON):
  {
    "statusCode":   201,
    "responseBody": { "code": "dino-6934b86b-...", "name": "...", ... }
  }

  TTL: 86400 segundos (24 horas)
*/


-- =============================================================================
-- SECCIÓN 2 — COMANDOS REDIS
-- =============================================================================

/*
  ── GUARDAR (solo si NO existe) ───────────────────────────────────────────────
  SET idempotency:idem-{uuid} "{\"statusCode\":201,\"responseBody\":{...}}" NX EX 86400

  ── VERIFICAR si existe ───────────────────────────────────────────────────────
  EXISTS idempotency:idem-{uuid}

  ── RECUPERAR la respuesta cacheada ──────────────────────────────────────────
  GET idempotency:idem-{uuid}

  ── ELIMINAR manualmente ─────────────────────────────────────────────────────
  DEL idempotency:idem-{uuid}

  ── LISTAR todas las keys activas (solo desarrollo) ──────────────────────────
  SCAN 0 MATCH idempotency:idem-* COUNT 100
*/


-- =============================================================================
-- SECCIÓN 3 — FLUJO EN LA APLICACIÓN (pseudocódigo)
-- =============================================================================

/*
  POST /dinosaur
  Header: Idempotency-Key: idem-6934b86b-324c-45d4-93f5-d5f025e8fb7b

  1. key = "idempotency:" + request.header("Idempotency-Key")
  2. cached = Redis.GET(key)

  3. IF cached != null THEN
       -- Request duplicada → devolver respuesta cacheada
       return HttpResponse(cached.statusCode, cached.responseBody)

  4. ELSE
       -- Request nueva → procesar
       result = dinosaurService.create(request.body)
       -- result.code = "dino-6934b86b-..."  (mismo UUID base que el idem-)

       payload = JSON { statusCode: 201, responseBody: result }
       Redis.SET(key, payload, NX, EX, 86400)

       return HttpResponse(201, result)
*/


-- =============================================================================
-- SECCIÓN 4 — IMPLEMENTACIÓN JAVA (Spring Boot)
-- =============================================================================

/*
  @Service
  public class IdempotencyService {

      private final StringRedisTemplate redisTemplate;
      private static final Duration TTL    = Duration.ofHours(24);
      private static final String   PREFIX = "idempotency:";

      // Validar formato del header
      public void validateKey(String key) {
          if (!key.startsWith("idem-")) {
              throw new IllegalArgumentException(
                  "Idempotency-Key debe tener formato idem-{uuid}"
              );
          }
      }

      public Optional<String> get(String key) {
          return Optional.ofNullable(
              redisTemplate.opsForValue().get(PREFIX + key)
          );
      }

      public boolean setIfAbsent(String key, String responseJson) {
          return Boolean.TRUE.equals(
              redisTemplate.opsForValue().setIfAbsent(PREFIX + key, responseJson, TTL)
          );
      }
  }
*/
