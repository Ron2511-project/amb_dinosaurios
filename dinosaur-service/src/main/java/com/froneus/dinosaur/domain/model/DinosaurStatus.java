package com.froneus.dinosaur.domain.model;

/**
 * Estados válidos del dinosaurio.
 * Alineado con el ENUM dinosaur_status de PostgreSQL:
 *   CREATE TYPE dinosaur_status AS ENUM ('ALIVE', 'ENDANGERED', 'EXTINCT');
 */
public enum DinosaurStatus {
    ALIVE,
    ENDANGERED,
    EXTINCT
}
