-- Indice para la validacion de solapamiento de horarios por ubicacion y dia.
CREATE INDEX IF NOT EXISTS idx_horario_ubicacion_dia ON horarios (ubicacion, dia);
