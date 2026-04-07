package com.froneus.dinosaur.infrastructure.adapter.out.messaging;

import com.froneus.dinosaur.domain.model.DinosaurEvent;
import com.froneus.dinosaur.domain.port.out.DinosaurEventOutboxPort;
import com.froneus.dinosaur.domain.port.out.DinosaurEventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Outbox Publisher — adaptador de entrada tipo scheduler.
 *
 * Corre cada 5 segundos y transfiere eventos del Outbox (Redis)
 * a RabbitMQ en lotes de hasta 50 eventos.
 *
 * Flujo:
 *   1. pollBatch() — toma hasta 50 eventos de Redis (LPOP atómico)
 *   2. publish()   — publica cada uno en RabbitMQ
 *   3. Si publish() falla → el evento ya fue removido de Redis (LPOP)
 *      En producción se implementaría una dead-letter queue.
 *
 * fixedDelay vs cron:
 *   fixedDelay asegura que el siguiente ciclo empiece DESPUÉS de que
 *   termine el actual, evitando solapamiento si RabbitMQ tarda.
 */
@Component
public class OutboxPublisher {

    private static final Logger log       = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final int    BATCH_SIZE = 50;

    private final DinosaurEventOutboxPort     outbox;
    private final DinosaurEventPublisherPort  publisher;

    public OutboxPublisher(DinosaurEventOutboxPort outbox,
                           DinosaurEventPublisherPort publisher) {
        this.outbox     = outbox;
        this.publisher  = publisher;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {
        List<DinosaurEvent> events = outbox.pollBatch(BATCH_SIZE);
        if (events.isEmpty()) return;

        log.info("OutboxPublisher — processing {} events", events.size());

        for (DinosaurEvent event : events) {
            try {
                publisher.publish(event);
                outbox.ack(event);
            } catch (Exception e) {
                log.error("Failed to publish event dinosaurId={}: {}",
                        event.dinosaurId(), e.getMessage(), e);
                // Re-encolar para reintento
                outbox.store(event);
            }
        }
    }
}
