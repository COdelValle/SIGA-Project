-- Clases del prototipo (2026): completa los cursos 4°B y 5°/6° A-B-C usados
-- por los mocks de docente y apoderado. Idempotente (INSERT IGNORE + unique).

INSERT IGNORE INTO clases (id, nivel, letra, anio_academico, id_docente_jefe, active) VALUES
    (7, 'CUARTO_BASICO', 'B', 2026, 14, TRUE),
    (8, 'QUINTO_BASICO', 'A', 2026, 2, TRUE),
    (9, 'QUINTO_BASICO', 'B', 2026, 2, TRUE),
    (10, 'QUINTO_BASICO', 'C', 2026, 2, TRUE),
    (11, 'SEXTO_BASICO', 'A', 2026, 2, TRUE),
    (12, 'SEXTO_BASICO', 'B', 2026, 2, TRUE),
    (13, 'SEXTO_BASICO', 'C', 2026, 2, TRUE);
