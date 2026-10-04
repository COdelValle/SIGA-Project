-- Completa la franja 2 (09:50-11:20) de Lunes y Martes para el 8° Básico A
-- (curso_asignatura 2 = Lengua y Literatura; curso_asignatura 1 = Matemática),
-- que era la unica clase con ese hueco en el seed. Idempotente: no inserta el
-- bloque si ya existe.
INSERT INTO horarios (curso_asignatura_id, dia, horario_entrada, horario_salida, ubicacion, active)
SELECT 2, 'LUNES', '09:50:00', '10:35:00', 'Sala 8° Básico A', TRUE FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM horarios
    WHERE curso_asignatura_id = 2 AND dia = 'LUNES' AND horario_entrada = '09:50:00'
);

INSERT INTO horarios (curso_asignatura_id, dia, horario_entrada, horario_salida, ubicacion, active)
SELECT 2, 'LUNES', '10:35:00', '11:20:00', 'Sala 8° Básico A', TRUE FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM horarios
    WHERE curso_asignatura_id = 2 AND dia = 'LUNES' AND horario_entrada = '10:35:00'
);

INSERT INTO horarios (curso_asignatura_id, dia, horario_entrada, horario_salida, ubicacion, active)
SELECT 1, 'MARTES', '09:50:00', '10:35:00', 'Sala 8° Básico A', TRUE FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM horarios
    WHERE curso_asignatura_id = 1 AND dia = 'MARTES' AND horario_entrada = '09:50:00'
);

INSERT INTO horarios (curso_asignatura_id, dia, horario_entrada, horario_salida, ubicacion, active)
SELECT 1, 'MARTES', '10:35:00', '11:20:00', 'Sala 8° Básico A', TRUE FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM horarios
    WHERE curso_asignatura_id = 1 AND dia = 'MARTES' AND horario_entrada = '10:35:00'
);
