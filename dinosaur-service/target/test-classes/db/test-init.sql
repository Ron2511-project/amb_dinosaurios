-- Script de inicialización para tests de integración con Testcontainers
-- Crea el ENUM y las tablas CQRS igual que en producción

CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'dinosaur_status') THEN
        CREATE TYPE dinosaur_status AS ENUM ('ALIVE', 'EXTINCT', 'INACTIVE');
    END IF;
END
$$;

CREATE TABLE IF NOT EXISTS dinosaurs_write (
    id               BIGSERIAL       PRIMARY KEY,
    name             TEXT            NOT NULL,
    species          TEXT            NOT NULL,
    discovery_date   TIMESTAMPTZ     NOT NULL,
    extinction_date  TIMESTAMPTZ,
    status           dinosaur_status NOT NULL,
    created_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMPTZ     DEFAULT NULL,
    CONSTRAINT chk_dates_order CHECK (
        extinction_date IS NULL OR discovery_date < extinction_date
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_dw_name_active
    ON dinosaurs_write (name)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS dinosaurs_read (
    id               BIGINT      PRIMARY KEY,
    name             TEXT,
    species          TEXT,
    status           TEXT,
    is_extinct       BOOLEAN,
    dinosaur_summary TEXT,
    created_at       TIMESTAMPTZ,
    deleted_at       TIMESTAMPTZ
);
