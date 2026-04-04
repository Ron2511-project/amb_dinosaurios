-- =============================================================================
-- SCRIPT 02: WRITE MODEL — dinosaurs_write
-- Orden de ejecución: 2do
-- Descripción: Tabla principal de escritura (modelo CQRS Command Side).
--              Contiene la fuente de verdad del dominio.
-- Dependencias: 01_postgres_setup.sql (extensión pgcrypto + ENUM)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 3. TABLA: dinosaurs_write (Command Model)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dinosaurs_write (
    -- Identificador único generado con pgcrypto (UUID v4)
    id               UUID            PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Datos de dominio — obligatorios
    name             TEXT            NOT NULL,
    species          TEXT            NOT NULL,
    discovery_date   TIMESTAMPTZ     NOT NULL,

    -- Fecha de extinción opcional (puede ser nula si el dinosaurio sigue "vivo")
    extinction_date  TIMESTAMPTZ,

    -- Estado del ciclo de vida usando ENUM tipado
    status           dinosaur_status NOT NULL,

    -- Trazabilidad temporal
    created_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- Soft delete: cuando deleted_at IS NOT NULL, el registro está "eliminado"
    -- Se usa en lugar de DELETE físico para preservar historial y auditoría
    deleted_at       TIMESTAMPTZ     DEFAULT NULL,

    -- Constraint de negocio: discoveryDate < extinctionDate
    -- Respeta la validación definida en el challenge (pág. 2)
    CONSTRAINT chk_dates_order CHECK (
        extinction_date IS NULL OR discovery_date < extinction_date
    )
);

COMMENT ON TABLE  dinosaurs_write                IS 'Write model (Command Side CQRS). Fuente de verdad del dominio. No usar directamente para lecturas en producción.';
COMMENT ON COLUMN dinosaurs_write.id             IS 'UUID v4 generado por pgcrypto.';
COMMENT ON COLUMN dinosaurs_write.deleted_at     IS 'Soft delete. Si no es NULL, el registro está lógicamente eliminado.';
COMMENT ON COLUMN dinosaurs_write.status         IS 'Estado del dinosaurio según ciclo de vida definido en el challenge.';

-- -----------------------------------------------------------------------------
-- 4. ÍNDICES — dinosaurs_write
-- -----------------------------------------------------------------------------

-- Búsqueda por nombre (soporte para LIKE y =)
CREATE INDEX IF NOT EXISTS idx_dw_name
    ON dinosaurs_write (name);

-- Filtro rápido por estado (queries frecuentes del scheduler y API)
CREATE INDEX IF NOT EXISTS idx_dw_status
    ON dinosaurs_write (status);

-- Paginación eficiente descendente por fecha de creación
CREATE INDEX IF NOT EXISTS idx_dw_created_at_desc
    ON dinosaurs_write (created_at DESC);

-- Índice parcial para registros activos (excluye soft-deleted)
-- Optimiza casi todos los GET de la API que filtran deleted_at IS NULL
CREATE INDEX IF NOT EXISTS idx_dw_active_records
    ON dinosaurs_write (id)
    WHERE deleted_at IS NULL;

-- ÍNDICE ÚNICO PARCIAL — garantiza unicidad de nombre solo entre registros activos
-- Permite que un nombre "eliminado" vuelva a usarse (deleted_at IS NOT NULL queda excluido)
CREATE UNIQUE INDEX IF NOT EXISTS uq_dw_name_active
    ON dinosaurs_write (name)
    WHERE deleted_at IS NULL;

-- Índice compuesto: filtro por status sobre registros activos (scheduler + API list)
CREATE INDEX IF NOT EXISTS idx_dw_status_active
    ON dinosaurs_write (status)
    WHERE deleted_at IS NULL;
