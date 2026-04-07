package com.froneus.dinosaur.infrastructure.adapter.in.messaging;

import com.froneus.dinosaur.domain.model.DinosaurEvent;
import com.froneus.dinosaur.infrastructure.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada: consumer RabbitMQ.
 *
 * Escucha la cola dinosaur.notifications y procesa los eventos
 * de cambio de estado de dinosaurios.
 *
 * En un sistema real este consumer estaría en un microservicio separado
 * (notificaciones, auditoría, etc.). Aquí lo incluimos en el mismo servicio
 * para demostrar el flujo completo end-to-end.
 *
 * El mensaje recibido tiene el formato del challenge (punto III):
 * {
 *   "dinosaurId": 1,
 *   "newStatus":  "ENDANGERED",
 *   "timestamp":  "2023-10-01T09:00:00"
 * }
 */
@Component
public class DinosaurEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(DinosaurEventConsumer.class);

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void onDinosaurEvent(DinosaurEvent event) {
        log.info("Event received — dinosaurId={} newStatus={} eventType={} timestamp={}",
                event.dinosaurId(),
                event.newStatus(),
                event.eventType(),
                event.timestamp());

        // Aquí iría la lógica de procesamiento:
        //   - Enviar notificación por email
        //   - Actualizar sistema de auditoría
        //   - Notificar a otros microservicios
        //   - etc.
    }
}
