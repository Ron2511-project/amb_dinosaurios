package com.froneus.dinosaur.domain.port.out;

import com.froneus.dinosaur.domain.model.DinosaurEvent;
import java.util.List;

/**
 * Puerto de salida para el Outbox de eventos.
 *
 * Patrón Outbox:
 *   1. store()    — guarda el evento en Redis (lista outbox:events)
 *   2. pollBatch()— el publisher lee N eventos pendientes
 *   3. ack()      — confirma que el evento fue publicado en RabbitMQ
 *
 * Si RabbitMQ está caído, los eventos permanecen en Redis
 * y se reintentan en el próximo ciclo del publisher.
 */
public interface DinosaurEventOutboxPort {
    void store(DinosaurEvent event);
    List<DinosaurEvent> pollBatch(int maxSize);
    void ack(DinosaurEvent event);
}
