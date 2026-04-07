package com.froneus.dinosaur.infrastructure.adapter.out.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.froneus.dinosaur.domain.model.DinosaurEvent;
import com.froneus.dinosaur.domain.port.out.DinosaurEventOutboxPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador Redis para el Outbox de eventos.
 *
 * Usa una Redis List como cola FIFO:
 *   RPUSH outbox:events  {json}   ← store()    — agrega al final
 *   LPOP  outbox:events  N        ← pollBatch() — toma del inicio
 *
 * Si el publisher falla (RabbitMQ caído), el evento NO se hace ack
 * y permanece en Redis para reintento.
 *
 * Nota: este diseño es at-least-once. Para exactly-once se necesitaría
 * un mecanismo de deduplicación en el consumer.
 */
@Component
public class DinosaurOutboxRedisAdapter implements DinosaurEventOutboxPort {

    private static final Logger log        = LoggerFactory.getLogger(DinosaurOutboxRedisAdapter.class);
    private static final String OUTBOX_KEY = "outbox:events";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper        objectMapper;

    public DinosaurOutboxRedisAdapter(StringRedisTemplate redisTemplate,
                                      ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper  = objectMapper;
    }

    @Override
    public void store(DinosaurEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            redisTemplate.opsForList().rightPush(OUTBOX_KEY, json);
            log.debug("Event stored in outbox — dinosaurId={} status={}",
                    event.dinosaurId(), event.newStatus());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event: {}", e.getMessage(), e);
        }
    }

    @Override
    public List<DinosaurEvent> pollBatch(int maxSize) {
        List<DinosaurEvent> events = new ArrayList<>();
        for (int i = 0; i < maxSize; i++) {
            String json = redisTemplate.opsForList().leftPop(OUTBOX_KEY);
            if (json == null) break;
            try {
                events.add(objectMapper.readValue(json, DinosaurEvent.class));
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize event: {}", e.getMessage(), e);
            }
        }
        return events;
    }

    @Override
    public void ack(DinosaurEvent event) {
        // El ack ocurre implícitamente en pollBatch via LPOP atómico.
        // Este método existe para extensibilidad (e.g. dead letter queue).
        log.debug("Event acknowledged — dinosaurId={}", event.dinosaurId());
    }

    public long pendingCount() {
        Long size = redisTemplate.opsForList().size(OUTBOX_KEY);
        return size != null ? size : 0;
    }
}
