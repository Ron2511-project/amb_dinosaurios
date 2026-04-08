package com.froneus.dinosaur.domain.model;

import java.time.LocalDateTime;

/**
 * Modelo de lectura del Query Side CQRS — tabla dinosaurs_read.
 * Contiene campos pre-calculados: isExtinct, dinosaurSummary.
 * Solo se usa para GET — nunca para escritura.
 */
public class DinosaurReadModel {

    private final Long          id;
    private final String        name;
    private final String        species;
    private final DinosaurStatus status;
    private final boolean       isExtinct;
    private final String        dinosaurSummary;
    private final LocalDateTime createdAt;

    private DinosaurReadModel(Builder b) {
        this.id              = b.id;
        this.name            = b.name;
        this.species         = b.species;
        this.status          = b.status;
        this.isExtinct       = b.isExtinct;
        this.dinosaurSummary = b.dinosaurSummary;
        this.createdAt       = b.createdAt;
    }

    public Long          getId()             { return id; }
    public String        getName()           { return name; }
    public String        getSpecies()        { return species; }
    public DinosaurStatus getStatus()        { return status; }
    public boolean       isExtinct()         { return isExtinct; }
    public String        getDinosaurSummary(){ return dinosaurSummary; }
    public LocalDateTime getCreatedAt()      { return createdAt; }

    public static class Builder {
        private Long id;
        private String name, species, dinosaurSummary;
        private DinosaurStatus status;
        private boolean isExtinct;
        private LocalDateTime createdAt;

        public Builder id(Long v)                    { id = v; return this; }
        public Builder name(String v)                { name = v; return this; }
        public Builder species(String v)             { species = v; return this; }
        public Builder status(DinosaurStatus v)      { status = v; return this; }
        public Builder isExtinct(boolean v)          { isExtinct = v; return this; }
        public Builder dinosaurSummary(String v)     { dinosaurSummary = v; return this; }
        public Builder createdAt(LocalDateTime v)    { createdAt = v; return this; }
        public DinosaurReadModel build()             { return new DinosaurReadModel(this); }
    }
}
