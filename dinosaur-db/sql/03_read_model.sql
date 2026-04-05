-- =============================================================================
-- SCRIPT 03: READ MODEL — dinosaurs_read
-- Orden de ejecución: 3ro
-- Descripción: Tabla desnormalizada de lectura (modelo CQRS Query Side).
-- Dependencias: 01_postgres_setup.sql, 02_write_model.sql
-- =============================================================================

CREATE TABLE IF NOT EXISTS dinosaurs_read (
    -- Mismo UUID que en dinosaurs_write
    id                  UUID        PRIMARY KEY,

    -- Identificador legible propagado desde write model
    -- Ejemplo: dino-6934b86b-324c-45d4-93f5-d5f025e8fb7b
    code                TEXT,

    name                TEXT,
    species             TEXT,

    -- Status como TEXT para flexibilidad en el Query Side
    status              TEXT,

    -- Campo derivado: true si status = 'EXTINCT'
    is_extinct          BOOLEAN,

    -- Campo pre-calculado: "nombre - especie"
    dinosaur_summary    TEXT,

    created_at          TIMESTAMPTZ,
    deleted_at          TIMESTAMPTZ
);

COMMENT ON TABLE  dinosaurs_read                    IS 'Read model (Query Side CQRS). Sincronizado via trigger desde dinosaurs_write.';
COMMENT ON COLUMN dinosaurs_read.code               IS 'Identificador legible: dino-{uuid}. Propagado desde write model.';
COMMENT ON COLUMN dinosaurs_read.is_extinct         IS 'Campo derivado. true cuando status = EXTINCT.';
COMMENT ON COLUMN dinosaurs_read.dinosaur_summary   IS 'Concatenación pre-calculada: name || '' - '' || species.';

-- -----------------------------------------------------------------------------
-- ÍNDICES — dinosaurs_read
-- -----------------------------------------------------------------------------

-- Búsqueda por code (identificador público)
CREATE UNIQUE INDEX IF NOT EXISTS uq_dr_code
    ON dinosaurs_read (code);

CREATE INDEX IF NOT EXISTS idx_dr_status
    ON dinosaurs_read (status);

CREATE INDEX IF NOT EXISTS idx_dr_is_extinct
    ON dinosaurs_read (is_extinct);

CREATE INDEX IF NOT EXISTS idx_dr_created_at_desc
    ON dinosaurs_read (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_dr_name
    ON dinosaurs_read (name);

CREATE INDEX IF NOT EXISTS idx_dr_active_records
    ON dinosaurs_read (id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_dr_active_created_desc
    ON dinosaurs_read (created_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_dr_status_active_created
    ON dinosaurs_read (status, created_at DESC)
    WHERE deleted_at IS NULL;
