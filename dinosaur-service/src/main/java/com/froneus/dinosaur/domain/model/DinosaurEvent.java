package com.froneus.dinosaur.domain.model;

import java.time.LocalDateTime;

/**
 * Evento de dominio publicado cuando un dinosaurio cambia de estado.
 *
 * Formato del mensaje (challenge punto III):
 * {
 *   "dinosaurId": 1,
 *   "newStatus":  "ENDANGERED",
 *   "timestamp":  "2023-10-01T09:00:00",
 *   "eventType":  "STATUS_CHANGED"
 * }
 *
 * Se emite cuando:
 *   POST   → CREATED        (estado inicial ALIVE)
 *   PUT    → STATUS_CHANGED (solo si el status cambió)
 *   DELETE → DELETED
 *   Scheduler → SCHEDULER_UPDATE (ALIVE→ENDANGERED o ANY→EXTINCT)
 */
public record DinosaurEvent(
        Long           dinosaurId,
        DinosaurStatus newStatus,
        LocalDateTime  timestamp,
        EventType      eventType
) {
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
