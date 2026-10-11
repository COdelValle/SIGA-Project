-- Curso (clase) al que pertenece el estudiante. Referencia a ms-clases (sin FK
-- cross-database). Nullable para no romper registros previos al seed.
ALTER TABLE estudiantes ADD COLUMN IF NOT EXISTS id_clase BIGINT NULL;

CREATE INDEX IF NOT EXISTS idx_estudiante_id_clase ON estudiantes (id_clase);
