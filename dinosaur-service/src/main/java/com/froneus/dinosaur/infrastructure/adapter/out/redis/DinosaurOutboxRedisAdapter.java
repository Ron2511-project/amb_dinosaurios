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
        log.info(">>> DinosaurOutboxRedisAdapter initialized");
    }

    @Override
    public void store(DinosaurEvent event) {
        log.info(">>> DinosaurOutboxRedisAdapter.store() called — dinosaurId={} status={} type={}",
                event.dinosaurId(), event.newStatus(), event.eventType());
        try {
            String json = objectMapper.writeValueAsString(event);
            log.info(">>> Serialized event JSON: {}", json);

            Long listSize = redisTemplate.opsForList().rightPush(OUTBOX_KEY, json);
            log.info(">>> RPUSH to outbox:events — new list size={}", listSize);

        } catch (JsonProcessingException e) {
            log.error(">>> FAILED to serialize event: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error(">>> FAILED to push to Redis: {}", e.getMessage(), e);
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
                log.error(">>> Failed to deserialize outbox event: {}", e.getMessage(), e);
            }
        }
        if (!events.isEmpty()) {
            log.info(">>> Outbox polled {} events", events.size());
        }
        return events;
    }

    @Override
    public void ack(DinosaurEvent event) {
        log.info(">>> Event acked — dinosaurId={} type={}", event.dinosaurId(), event.eventType());
    }
}
