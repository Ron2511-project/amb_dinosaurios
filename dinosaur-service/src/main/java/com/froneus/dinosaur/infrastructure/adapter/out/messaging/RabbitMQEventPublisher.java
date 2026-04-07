package com.froneus.dinosaur.infrastructure.adapter.out.messaging;

import com.froneus.dinosaur.domain.model.DinosaurEvent;
import com.froneus.dinosaur.domain.port.out.DinosaurEventPublisherPort;
import com.froneus.dinosaur.infrastructure.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida: publica eventos en RabbitMQ.
 *
 * Exchange: dinosaur.exchange (topic)
 * Routing key: dinosaur.status.{eventType}
 *   dinosaur.status.CREATED
 *   dinosaur.status.STATUS_CHANGED
 *   dinosaur.status.DELETED
 *   dinosaur.status.SCHEDULER_UPDATE
 *
 * El consumer puede suscribirse a patrones:
 *   dinosaur.status.*       → todos los eventos
 *   dinosaur.status.CREATED → solo creaciones
 */
@Component
public class RabbitMQEventPublisher implements DinosaurEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitMQEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(DinosaurEvent event) {
        String routingKey = "dinosaur.status." + event.eventType().name();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                routingKey,
                event
        );

        log.info("Event published — exchange={} routingKey={} dinosaurId={} status={}",
                RabbitMQConfig.EXCHANGE_NAME, routingKey,
                event.dinosaurId(), event.newStatus());
    }
}
