package com.froneus.dinosaur.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de RabbitMQ.
 *
 * Topología:
 *   Exchange: dinosaur.exchange (topic)
 *       │
 *       ├── routing key: dinosaur.status.*
 *       │
 *       └── Queue: dinosaur.notifications
 *
 * Topic exchange permite filtrar por tipo de evento:
 *   dinosaur.status.*              → todos los eventos
 *   dinosaur.status.CREATED        → solo creaciones
 *   dinosaur.status.STATUS_CHANGED → solo cambios de estado
 *   dinosaur.status.SCHEDULER_*    → solo del scheduler
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "dinosaur.exchange";
    public static final String QUEUE_NAME    = "dinosaur.notifications";
    public static final String ROUTING_KEY   = "dinosaur.status.#";

    @Bean
    public TopicExchange dinosaurExchange() {
        return ExchangeBuilder
                .topicExchange(EXCHANGE_NAME)
                .durable(true)
                .build();
    }

    @Bean
    public Queue dinosaurQueue() {
        return QueueBuilder
                .durable(QUEUE_NAME)
                .build();
    }

    @Bean
    public Binding dinosaurBinding(Queue dinosaurQueue, TopicExchange dinosaurExchange) {
        return BindingBuilder
                .bind(dinosaurQueue)
                .to(dinosaurExchange)
                .with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
