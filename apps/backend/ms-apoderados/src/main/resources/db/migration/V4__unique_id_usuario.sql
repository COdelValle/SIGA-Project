-- El idUsuario (oid de Azure) debe ser unico por entidad.
-- Se reemplaza el indice no unico por uno unico (si hay duplicados previos, la
-- migracion falla: limpiar antes con: SELECT id_usuario, COUNT(*) FROM apoderados GROUP BY id_usuario HAVING COUNT(*) > 1).
DROP INDEX IF EXISTS idx_apoderado_id_usuario ON apoderados;
CREATE UNIQUE INDEX IF NOT EXISTS uk_apoderado_id_usuario ON apoderados (id_usuario);
