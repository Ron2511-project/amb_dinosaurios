-- =============================================================================
-- SCRIPT 03: READ MODEL — dinosaurs_read
-- Orden de ejecución: 3ro
-- Descripción: Tabla desnormalizada de lectura (modelo CQRS Query Side).
--              Optimizada para consultas GET con paginación y filtros.
--              NUNCA se escribe directamente desde la aplicación;
--              sólo es actualizada por el trigger de sincronización.
-- Dependencias: 01_postgres_setup.sql, 02_write_model.sql
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 5. TABLA: dinosaurs_read (Query Model)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dinosaurs_read (
    -- Mismo UUID que en dinosaurs_write (FK lógica; sin FK física para desacoplar)
    id                  UUID        PRIMARY KEY,

    -- Proyección plana de los campos relevantes para la API de lectura
    name                TEXT,
    species             TEXT,

    -- Status como TEXT (no ENUM) para flexibilidad en el Query Side:
    -- permite agregar valores sin migrar el read model
    status              TEXT,

    -- Campo derivado: true si status = 'EXTINCT'
    -- Calculado en la función de sincronización; evita lógica en queries
    is_extinct          BOOLEAN,

    -- Campo pre-calculado para búsqueda/display: "nombre - especie"
    -- Ejemplo: "Tyrannosaurus Rex - Theropod"
    dinosaur_summary    TEXT,

    -- Trazabilidad temporal (copiada del write model)
    created_at          TIMESTAMPTZ,

    -- Soft delete propagado desde write model
    deleted_at          TIMESTAMPTZ
);

COMMENT ON TABLE  dinosaurs_read                    IS 'Read model (Query Side CQRS). Desnormalizado y optimizado para lecturas. Sincronizado via trigger desde dinosaurs_write.';
COMMENT ON COLUMN dinosaurs_read.is_extinct         IS 'Campo derivado. true cuando status = EXTINCT. Calculado en sync_dinosaurs_read().';
COMMENT ON COLUMN dinosaurs_read.dinosaur_summary   IS 'Concatenación pre-calculada: name || '' - '' || species. Útil para listados y búsquedas de texto.';
COMMENT ON COLUMN dinosaurs_read.status             IS 'TEXT (no ENUM) para desacoplar el read model de cambios en el tipo del write model.';

-- -----------------------------------------------------------------------------
-- 6. ÍNDICES — dinosaurs_read
-- -----------------------------------------------------------------------------

-- Filtro por status (el más común en los GET /dinosaur?status=...)
CREATE INDEX IF NOT EXISTS idx_dr_status
    ON dinosaurs_read (status);

-- Filtro por is_extinct (campo derivado frecuente en reportes)
CREATE INDEX IF NOT EXISTS idx_dr_is_extinct
    ON dinosaurs_read (is_extinct);

-- Paginación descendente por fecha de creación (GET /dinosaur?page=N)
CREATE INDEX IF NOT EXISTS idx_dr_created_at_desc
    ON dinosaurs_read (created_at DESC);

-- Búsqueda por name
CREATE INDEX IF NOT EXISTS idx_dr_name
    ON dinosaurs_read (name);

-- Índice parcial: registros activos (deleted_at IS NULL)
-- Cubre la mayoría de los GET de la API pública
CREATE INDEX IF NOT EXISTS idx_dr_active_records
    ON dinosaurs_read (id)
    WHERE deleted_at IS NULL;

-- Índice compuesto para el caso más frecuente: activos + paginados
CREATE INDEX IF NOT EXISTS idx_dr_active_created_desc
    ON dinosaurs_read (created_at DESC)
    WHERE deleted_at IS NULL;

-- Índice compuesto: activos filtrados por status + paginación
CREATE INDEX IF NOT EXISTS idx_dr_status_active_created
    ON dinosaurs_read (status, created_at DESC)
    WHERE deleted_at IS NULL;
