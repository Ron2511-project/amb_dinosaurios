package com.froneus.dinosaur.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Entidad JPA mapeada a dinosaur_messages.
 * Registra cada evento procesado por el consumer RabbitMQ.
 *
 * Consultar desde pgAdmin o psql:
 *   SELECT * FROM dinosaur_messages ORDER BY received_at DESC;
 */
@Entity
@Table(name = "dinosaur_messages")
public class DinosaurMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dinosaur_id", nullable = false)
    private Long dinosaurId;

    @Column(name = "new_status", nullable = false)
    private String newStatus;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "event_timestamp", nullable = false)
    private OffsetDateTime eventTimestamp;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    @Column(name = "routing_key")
    private String routingKey;

    @Column(name = "processed", nullable = false)
    private Boolean processed = true;

    public Long getId()                        { return id; }
    public Long getDinosaurId()                { return dinosaurId; }
    public void setDinosaurId(Long v)          { dinosaurId = v; }
    public String getNewStatus()               { return newStatus; }
    public void setNewStatus(String v)         { newStatus = v; }
    public String getEventType()               { return eventType; }
    public void setEventType(String v)         { eventType = v; }
    public OffsetDateTime getEventTimestamp()  { return eventTimestamp; }
    public void setEventTimestamp(OffsetDateTime v) { eventTimestamp = v; }
    public OffsetDateTime getReceivedAt()      { return receivedAt; }
    public void setReceivedAt(OffsetDateTime v){ receivedAt = v; }
    public String getRoutingKey()              { return routingKey; }
    public void setRoutingKey(String v)        { routingKey = v; }
    public Boolean getProcessed()              { return processed; }
    public void setProcessed(Boolean v)        { processed = v; }
}
