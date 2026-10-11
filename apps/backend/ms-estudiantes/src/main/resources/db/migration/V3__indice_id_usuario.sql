-- Indice para busquedas y validaciones por idUsuario (oid de Azure).
CREATE INDEX IF NOT EXISTS idx_estudiante_id_usuario ON estudiantes (id_usuario);
