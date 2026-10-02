-- Indices para busquedas por idUsuario y unicidad del vinculo apoderado-estudiante.
CREATE INDEX IF NOT EXISTS idx_apoderado_id_usuario ON apoderados (id_usuario);
CREATE UNIQUE INDEX IF NOT EXISTS uk_apoderado_estudiante ON apoderado_estudiantes (apoderado_id, estudiante_id);
