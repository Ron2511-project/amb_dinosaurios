-- =============================================================================
-- SCRIPT 04: SINCRONIZACIÓN CQRS + AUDITORÍA
-- Orden de ejecución: 4to
-- Dependencias: 02_write_model.sql, 03_read_model.sql
-- =============================================================================

-- =============================================================================
-- PARTE A — AUDITORÍA: actualización automática de updated_at
-- =============================================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF ROW(NEW.*) IS DISTINCT FROM ROW(OLD.*) THEN
        NEW.updated_at = NOW();
    END IF;
    RETURN NEW;
END;
$$;

COMMENT ON FUNCTION update_updated_at_column() IS
    'Actualiza updated_at automáticamente en cada UPDATE si la fila cambió.';

DROP TRIGGER IF EXISTS trg_dinosaurs_write_updated_at ON dinosaurs_write;

CREATE TRIGGER trg_dinosaurs_write_updated_at
    BEFORE UPDATE ON dinosaurs_write
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =============================================================================
-- PARTE B — SINCRONIZACIÓN CQRS: write → read
-- =============================================================================

CREATE OR REPLACE FUNCTION sync_dinosaurs_read()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO dinosaurs_read (
        id,
        code,
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
        NEW.code,
        NEW.name,
        NEW.species,
        NEW.status::TEXT,
        (NEW.status = 'EXTINCT'),
        NEW.name || ' - ' || NEW.species,
        NEW.created_at,
        NEW.deleted_at
    )
    ON CONFLICT (id) DO UPDATE
        SET
            code             = EXCLUDED.code,
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
    'Sincroniza write → read model (CQRS). Propaga code, calcula is_extinct y dinosaur_summary.';

DROP TRIGGER IF EXISTS trg_dinosaurs_sync ON dinosaurs_write;

CREATE TRIGGER trg_dinosaurs_sync
    AFTER INSERT OR UPDATE ON dinosaurs_write
    FOR EACH ROW
    EXECUTE FUNCTION sync_dinosaurs_read();
