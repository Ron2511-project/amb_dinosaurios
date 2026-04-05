-- =============================================================================
-- SCRIPT 02: WRITE MODEL — dinosaurs_write
-- Orden de ejecución: 2do
-- Descripción: Tabla principal de escritura (modelo CQRS Command Side).
--              Contiene la fuente de verdad del dominio.
-- Dependencias: 01_postgres_setup.sql (extensión pgcrypto + ENUM)
-- =============================================================================

CREATE TABLE IF NOT EXISTS dinosaurs_write (
    -- Identificador interno UUID (usado para joins, índices y relaciones)
    id               UUID            PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Identificador legible hacia afuera: "dino-{uuid}"
    -- Mismo UUID que id, con prefijo dino- para identificación semántica
    -- Ejemplo: dino-6934b86b-324c-45d4-93f5-d5f025e8fb7b
    code             TEXT            NOT NULL GENERATED ALWAYS AS ('dino-' || id::TEXT) STORED,

    -- Datos de dominio — obligatorios
    name             TEXT            NOT NULL,
    species          TEXT            NOT NULL,
    discovery_date   TIMESTAMPTZ     NOT NULL,

    -- Fecha de extinción opcional
    extinction_date  TIMESTAMPTZ,

    -- Estado del ciclo de vida usando ENUM tipado
    status           dinosaur_status NOT NULL,

    -- Trazabilidad temporal
    created_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- Soft delete
    deleted_at       TIMESTAMPTZ     DEFAULT NULL,

    -- Constraint de negocio: discoveryDate < extinctionDate
    CONSTRAINT chk_dates_order CHECK (
        extinction_date IS NULL OR discovery_date < extinction_date
    )
);

COMMENT ON TABLE  dinosaurs_write            IS 'Write model (Command Side CQRS). Fuente de verdad del dominio.';
COMMENT ON COLUMN dinosaurs_write.id         IS 'UUID v4 interno generado por pgcrypto. Usado para joins e índices.';
COMMENT ON COLUMN dinosaurs_write.code       IS 'Identificador legible: dino-{uuid}. Expuesto en API, logs y mensajería.';
COMMENT ON COLUMN dinosaurs_write.deleted_at IS 'Soft delete. Si no es NULL el registro está lógicamente eliminado.';
COMMENT ON COLUMN dinosaurs_write.status     IS 'Estado del dinosaurio: ALIVE | EXTINCT | INACTIVE.';

-- -----------------------------------------------------------------------------
-- ÍNDICES — dinosaurs_write
-- -----------------------------------------------------------------------------

-- Búsqueda por code (identificador público expuesto en la API)
CREATE UNIQUE INDEX IF NOT EXISTS uq_dw_code
    ON dinosaurs_write (code);

-- Búsqueda por nombre
CREATE INDEX IF NOT EXISTS idx_dw_name
    ON dinosaurs_write (name);

-- Filtro rápido por estado
CREATE INDEX IF NOT EXISTS idx_dw_status
    ON dinosaurs_write (status);

-- Paginación eficiente descendente
CREATE INDEX IF NOT EXISTS idx_dw_created_at_desc
    ON dinosaurs_write (created_at DESC);

-- Índice parcial para registros activos
CREATE INDEX IF NOT EXISTS idx_dw_active_records
    ON dinosaurs_write (id)
    WHERE deleted_at IS NULL;

-- Unicidad de nombre solo entre registros activos
CREATE UNIQUE INDEX IF NOT EXISTS uq_dw_name_active
    ON dinosaurs_write (name)
    WHERE deleted_at IS NULL;

-- Filtro por status sobre registros activos
CREATE INDEX IF NOT EXISTS idx_dw_status_active
    ON dinosaurs_write (status)
    WHERE deleted_at IS NULL;
