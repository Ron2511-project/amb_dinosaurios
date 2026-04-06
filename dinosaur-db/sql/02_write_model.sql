-- =============================================================================
-- SCRIPT 02: WRITE MODEL — dinosaurs_write
-- Orden de ejecución: 2do
-- Descripción: Tabla principal de escritura (modelo CQRS Command Side).
-- Dependencias: 01_postgres_setup.sql (extensión pgcrypto + ENUM)
-- =============================================================================

CREATE TABLE IF NOT EXISTS dinosaurs_write (
    -- ID autoincremental: 1, 2, 3, ...
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

COMMENT ON TABLE  dinosaurs_write            IS 'Write model (Command Side CQRS). Fuente de verdad del dominio.';
COMMENT ON COLUMN dinosaurs_write.id         IS 'ID autoincremental (BIGSERIAL). Generado automáticamente por PostgreSQL.';
COMMENT ON COLUMN dinosaurs_write.deleted_at IS 'Soft delete. Si no es NULL el registro está lógicamente eliminado.';
COMMENT ON COLUMN dinosaurs_write.status     IS 'Estado del dinosaurio: ALIVE | EXTINCT | INACTIVE.';

-- -----------------------------------------------------------------------------
-- ÍNDICES — dinosaurs_write
-- -----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_dw_name
    ON dinosaurs_write (name);

CREATE INDEX IF NOT EXISTS idx_dw_status
    ON dinosaurs_write (status);

CREATE INDEX IF NOT EXISTS idx_dw_created_at_desc
    ON dinosaurs_write (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_dw_active_records
    ON dinosaurs_write (id)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_dw_name_active
    ON dinosaurs_write (name)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_dw_status_active
    ON dinosaurs_write (status)
    WHERE deleted_at IS NULL;
