-- =============================================================================
-- SCRIPT 01: EXTENSIONES Y TIPOS BASE
-- Orden de ejecución: 1ro
-- Descripción: Habilita extensiones necesarias y define tipos ENUM globales
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. EXTENSIONES
-- -----------------------------------------------------------------------------
-- Habilitar pgcrypto para gen_random_uuid() (generación de UUIDs v4 seguros)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- -----------------------------------------------------------------------------
-- 2. ENUM: dinosaur_status
-- -----------------------------------------------------------------------------
-- Se usa DO $$ ... $$ para garantizar idempotencia en re-ejecuciones
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_type WHERE typname = 'dinosaur_status'
    ) THEN
        CREATE TYPE dinosaur_status AS ENUM (
            'ALIVE',
            'EXTINCT',
            'INACTIVE'
        );
        RAISE NOTICE 'ENUM dinosaur_status creado correctamente.';
    ELSE
        RAISE NOTICE 'ENUM dinosaur_status ya existe, se omite creación.';
    END IF;
END
$$;
