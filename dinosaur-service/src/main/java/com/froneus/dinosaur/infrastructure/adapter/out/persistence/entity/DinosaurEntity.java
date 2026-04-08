package com.froneus.dinosaur.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Entidad JPA mapeada a dinosaurs_write.
 * El campo status se maneja como String simple — el cast al ENUM PostgreSQL
 * se hace mediante native query en DinosaurJpaRepository.
 */
@Entity
@Table(name = "dinosaurs_write")
public class DinosaurEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String species;

    @Column(name = "discovery_date", nullable = false)
    private OffsetDateTime discoveryDate;

    @Column(name = "extinction_date")
    private OffsetDateTime extinctionDate;

    // String simple — el cast ::dinosaur_status lo hace la native query
    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public Long getId()                              { return id; }
    public void setId(Long id)                       { this.id = id; }
    public String getName()                          { return name; }
    public void setName(String name)                 { this.name = name; }
    public String getSpecies()                       { return species; }
    public void setSpecies(String species)           { this.species = species; }
    public OffsetDateTime getDiscoveryDate()         { return discoveryDate; }
    public void setDiscoveryDate(OffsetDateTime d)   { this.discoveryDate = d; }
    public OffsetDateTime getExtinctionDate()        { return extinctionDate; }
    public void setExtinctionDate(OffsetDateTime d)  { this.extinctionDate = d; }
    public String getStatus()                        { return status; }
    public void setStatus(String s)                  { this.status = s; }
    public OffsetDateTime getCreatedAt()             { return createdAt; }
    public void setCreatedAt(OffsetDateTime d)       { this.createdAt = d; }
    public OffsetDateTime getUpdatedAt()             { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime d)       { this.updatedAt = d; }
    public OffsetDateTime getDeletedAt()             { return deletedAt; }
    public void setDeletedAt(OffsetDateTime d)       { this.deletedAt = d; }
}
