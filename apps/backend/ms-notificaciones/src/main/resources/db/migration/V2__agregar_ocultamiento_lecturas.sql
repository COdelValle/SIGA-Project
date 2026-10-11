-- "Limpiar mis leídas": el usuario oculta sus notificaciones ya leídas sin
-- borrar la fila compartida (varios destinatarios pueden ver el mismo aviso).
ALTER TABLE notificacion_lecturas
    ADD COLUMN IF NOT EXISTS ocultada_en DATETIME(6) NULL;

-- La purga global usa fecha_hora; el ocultamiento es por usuario.
CREATE INDEX IF NOT EXISTS idx_notificacion_fecha ON notificaciones (fecha_hora);
