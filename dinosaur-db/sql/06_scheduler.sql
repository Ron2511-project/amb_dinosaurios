-- =============================================================================
-- SCRIPT 06: SCHEDULER — Actualización automática de estados
-- Orden de ejecución: 6to
-- Requiere: extensión pg_cron (incluida en postgres:16 con configuración)
--
-- Reglas del challenge (punto II):
--   - 24hs antes del extinctionDate: ALIVE → ENDANGERED (INACTIVE en BD)
--   - Al llegar extinctionDate:      ANY   → EXTINCT
--   - Corre cada 10 minutos
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Habilitar pg_cron
-- -----------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS pg_cron;

-- Dar permisos al usuario froneus para ejecutar jobs
GRANT USAGE ON SCHEMA cron TO froneus;

-- -----------------------------------------------------------------------------
-- 2. Función: actualizar estados de dinosaurios
-- Ejecuta ambas actualizaciones en orden correcto:
--   primero EXTINCT (los que ya vencieron),
--   luego  ENDANGERED (los que están próximos a vencer)
-- El trigger sync_dinosaurs_read propaga cada cambio a dinosaurs_read.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION update_dinosaur_statuses()
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    extinct_count    INT;
    endangered_count INT;
BEGIN
    -- Paso 1: ANY → EXTINCT (extinctionDate ya pasó)
    UPDATE dinosaurs_write
    SET    status     = CAST('EXTINCT' AS dinosaur_status),
           updated_at = NOW()
    WHERE  status    != CAST('EXTINCT' AS dinosaur_status)
      AND  deleted_at  IS NULL
      AND  extinction_date <= NOW();

    GET DIAGNOSTICS extinct_count = ROW_COUNT;

    -- Paso 2: ALIVE → INACTIVE/ENDANGERED (≤ 24hs para extinguirse)
    UPDATE dinosaurs_write
    SET    status     = CAST('INACTIVE' AS dinosaur_status),
           updated_at = NOW()
    WHERE  status     = CAST('ALIVE' AS dinosaur_status)
      AND  deleted_at  IS NULL
      AND  extinction_date <= NOW() + INTERVAL '24 hours'
      AND  extinction_date  > NOW();

    GET DIAGNOSTICS endangered_count = ROW_COUNT;

    IF extinct_count > 0 OR endangered_count > 0 THEN
        RAISE NOTICE '[update_dinosaur_statuses] EXTINCT: %, ENDANGERED: %',
            extinct_count, endangered_count;
    END IF;
END;
$$;

COMMENT ON FUNCTION update_dinosaur_statuses() IS
    'Actualiza estados de dinosaurios según reglas de negocio. '
    'Ejecutada por pg_cron cada 10 minutos.';

-- -----------------------------------------------------------------------------
-- 3. Registrar el job en pg_cron — cada 10 minutos
-- -----------------------------------------------------------------------------

-- Eliminar si ya existe (idempotente)
SELECT cron.unschedule(jobid)
FROM   cron.job
WHERE  jobname = 'update-dinosaur-statuses';

SELECT cron.schedule(
    'update-dinosaur-statuses',   -- nombre del job
    '*/10 * * * *',               -- cada 10 minutos
    $$SELECT update_dinosaur_statuses()$$
);

-- Verificar que quedó registrado
DO $$
BEGIN
    RAISE NOTICE 'pg_cron job registrado: update-dinosaur-statuses (cada 10 minutos)';
END $$;
