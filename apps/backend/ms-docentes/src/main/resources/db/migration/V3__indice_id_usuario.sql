-- Indice para busquedas y validaciones por idUsuario (oid de Azure).
CREATE INDEX IF NOT EXISTS idx_docente_id_usuario ON docentes (id_usuario);
