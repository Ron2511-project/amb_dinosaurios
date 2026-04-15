package com.froneus.dinosaur.infrastructure.adapter.out.messaging;

import com.froneus.dinosaur.domain.model.DinosaurEvent;
import com.froneus.dinosaur.domain.port.out.DinosaurEventPublisherPort;
import com.froneus.dinosaur.infrastructure.config.RabbitMQConfig;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida: publica eventos en RabbitMQ.
 *
 * Exchange: dinosaur.exchange (topic)
 * Routing key: dinosaur.status.{eventType}
 *
 * Ejemplos de routing keys generadas:
 *   dinosaur.status.CREATED          → POST exitoso
 *   dinosaur.status.STATUS_CHANGED   → PUT con cambio de status
 *   dinosaur.status.DELETED          → DELETE
 *   dinosaur.status.SCHEDULER_UPDATE → scheduler automático
 *
 * El JSON publicado tiene el formato del challenge (punto III):
 * {
 *   "dinosaurId": 21,
 *   "newStatus":  "ALIVE",
 *   "timestamp":  "2026-04-07T10:30:00",
 *   "eventType":  "CREATED"
 * }
 *
 * Si RabbitMQ no está disponible el Circuit Breaker actúa:
 *   - publish() → fallback loguea el evento perdido (no bloquea la operación)
 */
@Component
public class RabbitMQEventPublisher implements DinosaurEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQEventPublisher.class);
    private static final String CB  = "dinosaurService";

    private final RabbitTemplate rabbitTemplate;

    public RabbitMQEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    @CircuitBreaker(name = CB, fallbackMethod = "publishFallback")
    public void publish(DinosaurEvent event) {
        String routingKey = "dinosaur.status." + event.eventType().name();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                routingKey,
                event
        );

        log.info("Event published to RabbitMQ — exchange={} routingKey={} dinosaurId={} newStatus={}",
                RabbitMQConfig.EXCHANGE_NAME, routingKey,
                event.dinosaurId(), event.newStatus());
    }

    // ── Fallback ──────────────────────────────────────────────────────────────

    public void publishFallback(DinosaurEvent event, Throwable t) {
        log.error("CB open — RabbitMQ unavailable, event lost — dinosaurId={} status={} type={} error={}",
                event.dinosaurId(), event.newStatus(), event.eventType(), t.getMessage());
    }
}
