-- Indices para las busquedas por estudiante y evaluacion.
CREATE INDEX IF NOT EXISTS idx_nota_id_estudiante ON notas (id_estudiante);
CREATE INDEX IF NOT EXISTS idx_nota_id_evaluacion ON notas (id_evaluacion);
