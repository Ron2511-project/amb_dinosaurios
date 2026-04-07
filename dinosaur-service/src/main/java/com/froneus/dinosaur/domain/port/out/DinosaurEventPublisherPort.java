package com.froneus.dinosaur.domain.port.out;

import com.froneus.dinosaur.domain.model.DinosaurEvent;

/**
 * Puerto de salida para publicar eventos en RabbitMQ.
 * La implementación vive en la capa de infraestructura.
 */
public interface DinosaurEventPublisherPort {
    void publish(DinosaurEvent event);
}
