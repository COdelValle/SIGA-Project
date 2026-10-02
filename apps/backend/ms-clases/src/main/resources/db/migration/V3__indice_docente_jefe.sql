-- Indice para busquedas por docente jefe.
CREATE INDEX IF NOT EXISTS idx_clase_docente_jefe ON clases (id_docente_jefe);
