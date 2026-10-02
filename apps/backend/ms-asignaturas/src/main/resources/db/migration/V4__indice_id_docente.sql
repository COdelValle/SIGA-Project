-- Indice para busquedas de asignaturas por docente.
CREATE INDEX IF NOT EXISTS idx_asignatura_id_docente ON asignaturas (id_docente);
