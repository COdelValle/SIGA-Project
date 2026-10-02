-- Datos base de evaluaciones (solo para desarrollo local). id_asignatura
-- referencia a las asignaturas del seed de ms-asignaturas.
INSERT IGNORE INTO evaluaciones (id, nombre, tipo, ponderacion, id_asignatura, active) VALUES
    (1, 'PRUEBA 1',    'SUMATIVA',   30.0, 1, TRUE),
    (2, 'CONTROL 1',   'FORMATIVA',  20.0, 1, TRUE),
    (3, 'PRUEBA 1',    'SUMATIVA',   40.0, 2, TRUE),
    (4, 'DIAGNOSTICO', 'DIAGNOSTICO', 0.0, 3, TRUE),
    (5, 'TRABAJO 1',   'FORMATIVA',  25.0, 4, TRUE),
    (6, 'PRUEBA 1',    'SUMATIVA',   50.0, 5, TRUE);
