-- =============================================================================
-- SCRIPT 07: TABLA DE MENSAJES RECIBIDOS
-- Orden de ejecución: 7mo
-- Descripción: Registra todos los eventos procesados por el consumer RabbitMQ.
--              Provee visibilidad completa del historial de mensajería.
-- =============================================================================

CREATE TABLE IF NOT EXISTS dinosaur_messages (
    id            BIGSERIAL    PRIMARY KEY,
    dinosaur_id   BIGINT       NOT NULL,
    new_status    TEXT         NOT NULL,
    event_type    TEXT         NOT NULL,
    event_timestamp TIMESTAMPTZ NOT NULL,
    received_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    routing_key   TEXT,
    processed     BOOLEAN      NOT NULL DEFAULT TRUE
);

COMMENT ON TABLE  dinosaur_messages               IS 'Historial de eventos procesados por el consumer RabbitMQ.';
COMMENT ON COLUMN dinosaur_messages.dinosaur_id   IS 'ID del dinosaurio que generó el evento.';
COMMENT ON COLUMN dinosaur_messages.new_status    IS 'Nuevo estado: ALIVE, ENDANGERED, EXTINCT.';
COMMENT ON COLUMN dinosaur_messages.event_type    IS 'Tipo de evento: CREATED, STATUS_CHANGED, DELETED, SCHEDULER_UPDATE.';
COMMENT ON COLUMN dinosaur_messages.routing_key   IS 'Routing key de RabbitMQ: dinosaur.status.CREATED, etc.';
COMMENT ON COLUMN dinosaur_messages.received_at   IS 'Timestamp de cuando el consumer procesó el mensaje.';

CREATE INDEX IF NOT EXISTS idx_dm_dinosaur_id  ON dinosaur_messages (dinosaur_id);
CREATE INDEX IF NOT EXISTS idx_dm_event_type   ON dinosaur_messages (event_type);
CREATE INDEX IF NOT EXISTS idx_dm_received_at  ON dinosaur_messages (received_at DESC);
