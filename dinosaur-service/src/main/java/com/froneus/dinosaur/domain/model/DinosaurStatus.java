package com.froneus.dinosaur.domain.model;

/**
 * Estados válidos del dinosaurio.
 * Alineado con el ENUM dinosaur_status de PostgreSQL:
 *   CREATE TYPE dinosaur_status AS ENUM ('ALIVE', 'EXTINCT', 'INACTIVE');
 *
 * ENDANGERED no existe en la BD — se mapea como INACTIVE en persistencia.
 */
public enum DinosaurStatus {
    ALIVE,
    ENDANGERED,  // Dominio Java — se persiste como INACTIVE en PostgreSQL
    EXTINCT
}
