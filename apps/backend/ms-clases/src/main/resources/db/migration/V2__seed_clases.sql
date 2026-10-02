-- Datos base de clases (solo para desarrollo local). id_docente_jefe referencia
-- a los docentes del seed de ms-docentes.
INSERT IGNORE INTO clases (id, nivel, letra, anio_academico, id_docente_jefe, active) VALUES
    (1, 'SEPTIMO_BASICO', 'A', 2026, 1, TRUE),
    (2, 'SEPTIMO_BASICO', 'B', 2026, 2, TRUE),
    (3, 'SEPTIMO_BASICO', 'C', 2026, 3, TRUE),
    (4, 'OCTAVO_BASICO',  'A', 2026, 1, TRUE),
    (5, 'OCTAVO_BASICO',  'B', 2026, 2, TRUE),
    (6, 'OCTAVO_BASICO',  'C', 2026, 3, TRUE);
