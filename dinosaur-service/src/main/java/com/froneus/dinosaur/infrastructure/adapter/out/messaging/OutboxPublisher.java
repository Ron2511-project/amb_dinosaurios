package com.froneus.dinosaur.infrastructure.adapter.out.messaging;

import com.froneus.dinosaur.domain.model.DinosaurEvent;
import com.froneus.dinosaur.domain.port.out.DinosaurEventOutboxPort;
import com.froneus.dinosaur.domain.port.out.DinosaurEventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboxPublisher {

    private static final Logger log        = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final int    BATCH_SIZE = 50;

    private final DinosaurEventOutboxPort    outbox;
    private final DinosaurEventPublisherPort publisher;

    public OutboxPublisher(DinosaurEventOutboxPort outbox,
                           DinosaurEventPublisherPort publisher) {
        this.outbox     = outbox;
        this.publisher  = publisher;
        log.info(">>> OutboxPublisher initialized");
    }

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {
        log.debug(">>> OutboxPublisher tick — checking for pending events");
        List<DinosaurEvent> events = outbox.pollBatch(BATCH_SIZE);
        if (events.isEmpty()) return;

        log.info(">>> OutboxPublisher — found {} event(s) to publish", events.size());

        for (DinosaurEvent event : events) {
            try {
                publisher.publish(event);
                outbox.ack(event);
            } catch (Exception e) {
                log.error(">>> Failed to publish to RabbitMQ, re-queuing — dinosaurId={}: {}",
                        event.dinosaurId(), e.getMessage(), e);
                outbox.store(event);
            }
        }
    }
}
