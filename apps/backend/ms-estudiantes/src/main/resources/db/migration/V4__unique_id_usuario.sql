-- El idUsuario (oid de Azure) debe ser unico por entidad.
-- Se reemplaza el indice no unico por uno unico (si hay duplicados previos, la
-- migracion falla: limpiar antes con: SELECT id_usuario, COUNT(*) FROM estudiantes GROUP BY id_usuario HAVING COUNT(*) > 1).
DROP INDEX IF EXISTS idx_estudiante_id_usuario ON estudiantes;
CREATE UNIQUE INDEX IF NOT EXISTS uk_estudiante_id_usuario ON estudiantes (id_usuario);
