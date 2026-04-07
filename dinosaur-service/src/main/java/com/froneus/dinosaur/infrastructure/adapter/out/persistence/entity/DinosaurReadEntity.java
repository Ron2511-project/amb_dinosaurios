package com.froneus.dinosaur.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Entidad JPA mapeada a dinosaurs_read (Query Side CQRS).
 * Solo lectura — nunca se hace INSERT/UPDATE desde Java.
 * El trigger sync_dinosaurs_read la mantiene actualizada automáticamente.
 */
@Entity
@Table(name = "dinosaurs_read")
public class DinosaurReadEntity {

    @Id
    private Long id;

    @Column
    private String name;

    @Column
    private String species;

    @Column
    private String status;

    @Column(name = "is_extinct")
    private Boolean isExtinct;

    @Column(name = "dinosaur_summary")
    private String dinosaurSummary;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public Long          getId()              { return id; }
    public String        getName()            { return name; }
    public String        getSpecies()         { return species; }
    public String        getStatus()          { return status; }
    public Boolean       getIsExtinct()       { return isExtinct; }
    public String        getDinosaurSummary() { return dinosaurSummary; }
    public OffsetDateTime getCreatedAt()      { return createdAt; }
    public OffsetDateTime getDeletedAt()      { return deletedAt; }
}
