package com.froneus.dinosaur.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuración RabbitMQ + ObjectMapper compartido.
 *
 * Topología:
 *   Exchange: dinosaur.exchange  (topic)
 *       └── routing key: dinosaur.status.#
 *             └── Queue: dinosaur.notifications
 *
 * Un solo ObjectMapper @Primary es usado por:
 *   - DinosaurOutboxRedisAdapter  (serializar a JSON para Redis)
 *   - RabbitMQEventPublisher      (serializar para RabbitMQ)
 *   - DinosaurEventConsumer       (deserializar desde RabbitMQ)
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "dinosaur.exchange";
    public static final String QUEUE_NAME    = "dinosaur.notifications";
    public static final String ROUTING_KEY   = "dinosaur.status.#";

    /**
     * ObjectMapper @Primary compartido por toda la app.
     * Configurado para serializar LocalDateTime como ISO-8601 string
     * y manejar Java records correctamente.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    public TopicExchange dinosaurExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_NAME).durable(true).build();
    }

    @Bean
    public Queue dinosaurQueue() {
        return QueueBuilder.durable(QUEUE_NAME).build();
    }

    @Bean
    public Binding dinosaurBinding(Queue dinosaurQueue, TopicExchange dinosaurExchange) {
        return BindingBuilder.bind(dinosaurQueue).to(dinosaurExchange).with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
