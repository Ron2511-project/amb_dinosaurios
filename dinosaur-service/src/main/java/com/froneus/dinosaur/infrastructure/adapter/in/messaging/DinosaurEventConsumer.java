package com.froneus.dinosaur.infrastructure.adapter.in.messaging;

import com.froneus.dinosaur.domain.model.DinosaurEvent;
import com.froneus.dinosaur.infrastructure.adapter.out.persistence.entity.DinosaurMessageEntity;
import com.froneus.dinosaur.infrastructure.adapter.out.persistence.repository.DinosaurMessageRepository;
import com.froneus.dinosaur.infrastructure.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Consumer RabbitMQ — procesa eventos de dinosaurios.
 *
 * Al recibir cada mensaje:
 *   1. Loguea el evento
 *   2. Persiste el registro en dinosaur_messages (PostgreSQL)
 *
 * Consultar historial de mensajes:
 *   SELECT * FROM dinosaur_messages ORDER BY received_at DESC;
 *
 * Ver en pgAdmin:
 *   Tables → dinosaur_messages
 */
@Component
public class DinosaurEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(DinosaurEventConsumer.class);

    private final DinosaurMessageRepository messageRepository;

    public DinosaurEventConsumer(DinosaurMessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void onDinosaurEvent(DinosaurEvent event) {
        log.info("╔══════════════════════════════════════════════");
        log.info("║ EVENT RECEIVED FROM RABBITMQ");
        log.info("║ dinosaurId : {}", event.dinosaurId());
        log.info("║ newStatus  : {}", event.newStatus());
        log.info("║ eventType  : {}", event.eventType());
        log.info("║ timestamp  : {}", event.timestamp());
        log.info("╚══════════════════════════════════════════════");

        // Persistir en tabla dinosaur_messages para visibilidad y auditoría
        try {
            DinosaurMessageEntity message = new DinosaurMessageEntity();
            message.setDinosaurId(event.dinosaurId());
            message.setNewStatus(event.newStatus().name());
            message.setEventType(event.eventType().name());
            message.setEventTimestamp(event.timestamp().atOffset(ZoneOffset.UTC));
            message.setReceivedAt(OffsetDateTime.now(ZoneOffset.UTC));
            message.setRoutingKey("dinosaur.status." + event.eventType().name());
            message.setProcessed(true);

            DinosaurMessageEntity saved = messageRepository.save(message);
            log.info("Message persisted in dinosaur_messages — id={} dinosaurId={} eventType={}",
                    saved.getId(), event.dinosaurId(), event.eventType());

        } catch (Exception e) {
            log.error("Failed to persist message for dinosaurId={}: {}",
                    event.dinosaurId(), e.getMessage(), e);
        }
    }
}
