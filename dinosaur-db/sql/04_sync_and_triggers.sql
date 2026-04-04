-- =============================================================================
-- SCRIPT 04: SINCRONIZACIÓN CQRS + AUDITORÍA
-- Orden de ejecución: 4to
-- Descripción: Funciones y triggers que mantienen automáticamente el read
--              model sincronizado con el write model, y auditan updated_at.
-- Dependencias: 02_write_model.sql, 03_read_model.sql
-- =============================================================================

-- =============================================================================
-- PARTE A — AUDITORÍA: actualización automática de updated_at
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Función: update_updated_at_column
-- Propósito: Actualiza automáticamente el campo updated_at en cada UPDATE.
--            Reutilizable en cualquier tabla que tenga updated_at.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    -- Sólo actualiza si la fila cambió realmente (evita escrituras innecesarias)
    IF ROW(NEW.*) IS DISTINCT FROM ROW(OLD.*) THEN
        NEW.updated_at = NOW();
    END IF;
    RETURN NEW;
END;
$$;

COMMENT ON FUNCTION update_updated_at_column() IS
    'Trigger function de auditoría. Actualiza updated_at automáticamente en cada UPDATE '
    'siempre que al menos un campo haya cambiado. Reutilizable en múltiples tablas.';

-- -----------------------------------------------------------------------------
-- Trigger: trg_dinosaurs_write_updated_at
-- Se ejecuta BEFORE UPDATE para que NEW.updated_at llegue ya actualizado
-- a la transacción (y al trigger de sync posterior)
-- -----------------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_dinosaurs_write_updated_at ON dinosaurs_write;

CREATE TRIGGER trg_dinosaurs_write_updated_at
    BEFORE UPDATE ON dinosaurs_write
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TRIGGER trg_dinosaurs_write_updated_at ON dinosaurs_write IS
    'Mantiene updated_at al día en cada modificación del write model. '
    'Se ejecuta BEFORE UPDATE para garantizar que el valor correcto llegue '
    'al trigger de sincronización CQRS que corre AFTER UPDATE.';


-- =============================================================================
-- PARTE B — SINCRONIZACIÓN CQRS: write → read
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Función: sync_dinosaurs_read
-- Propósito: Proyecta un registro de dinosaurs_write hacia dinosaurs_read.
--            Calcula campos derivados y propaga soft delete.
--            Se invoca desde el trigger AFTER INSERT OR UPDATE.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION sync_dinosaurs_read()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    -- Usar NEW en INSERT/UPDATE; en DELETE usaríamos OLD (no aplica aquí
    -- porque el delete es lógico vía updated deleted_at, no un DELETE físico)
    INSERT INTO dinosaurs_read (
        id,
        name,
        species,
        status,
        is_extinct,
        dinosaur_summary,
        created_at,
        deleted_at
    )
    VALUES (
        NEW.id,
        NEW.name,
        NEW.species,

        -- Proyectar status como TEXT (desacoplado del ENUM del write model)
        NEW.status::TEXT,

        -- Campo derivado: extinct cuando status = 'EXTINCT'
        (NEW.status = 'EXTINCT'),

        -- Campo pre-calculado: "name - species"
        NEW.name || ' - ' || NEW.species,

        NEW.created_at,

        -- Propagar soft delete desde el write model
        NEW.deleted_at
    )
    ON CONFLICT (id) DO UPDATE
        SET
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

COMMENT ON FUNCTION sync_dinosaurs_read() IS
    'Sincroniza write → read model (CQRS). '
    'Calcula is_extinct y dinosaur_summary como campos derivados. '
    'Usa UPSERT (ON CONFLICT DO UPDATE) para garantizar idempotencia. '
    'Invocada automáticamente por trg_dinosaurs_sync AFTER INSERT OR UPDATE.';

-- -----------------------------------------------------------------------------
-- Trigger: trg_dinosaurs_sync
-- Se ejecuta AFTER INSERT OR UPDATE para que el write model ya esté
-- persistido antes de proyectar hacia el read model.
-- FOR EACH ROW garantiza sincronización fila por fila.
-- -----------------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_dinosaurs_sync ON dinosaurs_write;

CREATE TRIGGER trg_dinosaurs_sync
    AFTER INSERT OR UPDATE ON dinosaurs_write
    FOR EACH ROW
    EXECUTE FUNCTION sync_dinosaurs_read();

COMMENT ON TRIGGER trg_dinosaurs_sync ON dinosaurs_write IS
    'Dispara la sincronización CQRS write→read después de cada INSERT o UPDATE. '
    'AFTER garantiza consistencia: el dato ya fue confirmado en el write model '
    'antes de proyectarse al read model.';
