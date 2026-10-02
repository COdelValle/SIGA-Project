-- Las notas ahora referencian evaluaciones (ms-evaluaciones) en lugar de asignaturas.
-- Idempotente: renombra la columna solo si todavia existe id_asignatura, para
-- convivir con bases que ya aplicaron el esquema anterior o el nuevo.
SET @existe := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notas'
      AND COLUMN_NAME = 'id_asignatura'
);
SET @ddl := IF(@existe > 0,
    'ALTER TABLE notas CHANGE COLUMN id_asignatura id_evaluacion BIGINT NOT NULL',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
