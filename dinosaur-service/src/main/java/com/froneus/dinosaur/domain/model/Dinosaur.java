package com.froneus.dinosaur.domain.model;

import com.froneus.dinosaur.domain.exception.InvalidDinosaurDateException;

import java.time.LocalDateTime;

/**
 * Entidad de dominio Dinosaur — inmutable, sin dependencias de framework.
 *
 * Adaptada a la BD existente con arquitectura CQRS:
 *   - Tabla:  dinosaurs_write (Command Side)
 *   - ID:     BIGSERIAL autoincremental generado por PostgreSQL (Long)
 *   - Status: ALIVE | ENDANGERED (→ INACTIVE en BD) | EXTINCT
 */
public class Dinosaur {

    private final Long id;               // BIGSERIAL — null al crear, asignado por BD
    private final String name;
    private final String species;
    private final LocalDateTime discoveryDate;
    private final LocalDateTime extinctionDate;
    private final DinosaurStatus status;

    private Dinosaur(Builder b) {
        this.id             = b.id;
        this.name           = b.name;
        this.species        = b.species;
        this.discoveryDate  = b.discoveryDate;
        this.extinctionDate = b.extinctionDate;
        this.status         = b.status;
    }

    /**
     * Factory para creación nueva.
     * Siempre status ALIVE (regla de negocio).
     * ID es null — lo genera PostgreSQL con BIGSERIAL.
     */
    public static Dinosaur create(String name, String species,
                                  LocalDateTime discoveryDate,
                                  LocalDateTime extinctionDate) {
        validateDates(discoveryDate, extinctionDate);
        return new Builder()
                .id(null)
                .name(name)
                .species(species)
                .discoveryDate(discoveryDate)
                .extinctionDate(extinctionDate)
                .status(DinosaurStatus.ALIVE)
                .build();
    }

    /**
     * Factory para reconstitución desde persistencia.
     */
    public static Dinosaur reconstitute(Long id, String name, String species,
                                        LocalDateTime discoveryDate,
                                        LocalDateTime extinctionDate,
                                        DinosaurStatus status) {
        return new Builder()
                .id(id).name(name).species(species)
                .discoveryDate(discoveryDate).extinctionDate(extinctionDate)
                .status(status).build();
    }

    private static void validateDates(LocalDateTime discovery, LocalDateTime extinction) {
        if (discovery == null || extinction == null)
            throw new InvalidDinosaurDateException("Discovery and extinction dates are required");
        if (!discovery.isBefore(extinction))
            throw new InvalidDinosaurDateException(
                    "Discovery date must be earlier than extinction date");
    }

    public Long getId()                      { return id; }
    public String getName()                  { return name; }
    public String getSpecies()               { return species; }
    public LocalDateTime getDiscoveryDate()  { return discoveryDate; }
    public LocalDateTime getExtinctionDate() { return extinctionDate; }
    public DinosaurStatus getStatus()        { return status; }

    public static class Builder {
        private Long id;
        private String name, species;
        private LocalDateTime discoveryDate, extinctionDate;
        private DinosaurStatus status;

        public Builder id(Long v)                    { id = v; return this; }
        public Builder name(String v)                { name = v; return this; }
        public Builder species(String v)             { species = v; return this; }
        public Builder discoveryDate(LocalDateTime v){ discoveryDate = v; return this; }
        public Builder extinctionDate(LocalDateTime v){ extinctionDate = v; return this; }
        public Builder status(DinosaurStatus v)      { status = v; return this; }
        public Dinosaur build()                      { return new Dinosaur(this); }
    }
}
