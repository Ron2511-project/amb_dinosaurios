-- Script de inicialización para tests de integración (Testcontainers)
-- Replica el schema CQRS de producción

CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'dinosaur_status') THEN
        CREATE TYPE dinosaur_status AS ENUM ('ALIVE', 'EXTINCT', 'INACTIVE');
    END IF;
END $$;

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
    ON dinosaurs_write (name) WHERE deleted_at IS NULL;

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

-- Función de sincronización CQRS write → read
CREATE OR REPLACE FUNCTION sync_dinosaurs_read()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO dinosaurs_read (id, name, species, status, is_extinct, dinosaur_summary, created_at, deleted_at)
    VALUES (
        NEW.id, NEW.name, NEW.species, NEW.status::TEXT,
        (NEW.status = 'EXTINCT'),
        NEW.name || ' - ' || NEW.species,
        NEW.created_at, NEW.deleted_at
    )
    ON CONFLICT (id) DO UPDATE SET
        name             = EXCLUDED.name,
        species          = EXCLUDED.species,
        status           = EXCLUDED.status,
        is_extinct       = EXCLUDED.is_extinct,
        dinosaur_summary = EXCLUDED.dinosaur_summary,
        created_at       = EXCLUDED.created_at,
        deleted_at       = EXCLUDED.deleted_at;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_dinosaurs_sync ON dinosaurs_write;
CREATE TRIGGER trg_dinosaurs_sync
    AFTER INSERT OR UPDATE ON dinosaurs_write
    FOR EACH ROW EXECUTE FUNCTION sync_dinosaurs_read();

-- Función de auditoría updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF ROW(NEW.*) IS DISTINCT FROM ROW(OLD.*) THEN
        NEW.updated_at = NOW();
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_dinosaurs_write_updated_at ON dinosaurs_write;
CREATE TRIGGER trg_dinosaurs_write_updated_at
    BEFORE UPDATE ON dinosaurs_write
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
