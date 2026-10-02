-- Indice para las busquedas y el bloqueo pesimista por asignatura
-- (validacion de ponderacion acumulada en EvaluacionService).
CREATE INDEX IF NOT EXISTS idx_evaluacion_id_asignatura ON evaluaciones (id_asignatura);
