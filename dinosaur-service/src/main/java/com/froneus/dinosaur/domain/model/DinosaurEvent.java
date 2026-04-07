package com.froneus.dinosaur.domain.model;

import java.time.LocalDateTime;

/**
 * Evento de dominio que se publica cuando un dinosaurio cambia de estado.
 *
 * Formato del mensaje (challenge punto III):
 * {
 *   "dinosaurId": 1,
 *   "newStatus":  "ENDANGERED",
 *   "timestamp":  "2023-10-01T09:00:00"
 * }
 *
 * Inmutable — se crea una vez y no se modifica.
 */
public record DinosaurEvent(
        Long          dinosaurId,
        DinosaurStatus newStatus,
        LocalDateTime  timestamp,
        EventType      eventType
) {
    /**
     * Tipo de operación que originó el evento.
     * Útil para el consumer para distinguir el origen.
     */
    public enum EventType {
        CREATED,          // POST /v1/dinosaur
        STATUS_CHANGED,   // PUT  /v1/dinosaur/{id} con cambio de status
        DELETED,          // DELETE /v1/dinosaur/{id}
        SCHEDULER_UPDATE  // Actualización automática por scheduler
    }

    public static DinosaurEvent of(Long id, DinosaurStatus status, EventType type) {
        return new DinosaurEvent(id, status, LocalDateTime.now(), type);
    }
}
