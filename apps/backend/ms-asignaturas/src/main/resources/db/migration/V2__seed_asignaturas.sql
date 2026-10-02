-- Datos base de asignaturas. INSERT IGNORE hace la migracion idempotente:
-- si la asignatura ya existe (name es UNIQUE) no se duplica ni se sobreescribe.
-- Basicas (id_clase) y electivas (cupo_maximo) comparten la tabla asignaturas.
INSERT IGNORE INTO asignaturas
    (tipo_asignatura, name, description, semestre, area, id_docente, active, id_clase, cupo_maximo)
VALUES
    ('BASICA',   'MATEMATICA',       'matematica',                 'SEMESTRE_1', 'MATEMATICAS',       1, TRUE, 1,    NULL),
    ('BASICA',   'LENGUAJE',         'lenguaje y comunicacion',    'SEMESTRE_1', 'LENGUAJE',          2, TRUE, 2,    NULL),
    ('BASICA',   'CIENCIAS',         'ciencias naturales',         'SEMESTRE_1', 'CIENCIAS',          3, TRUE, 3,    NULL),
    ('BASICA',   'HISTORIA',         'historia y ciencias sociales','SEMESTRE_1','HISTORIA',          1, TRUE, 4,    NULL),
    ('ELECTIVA', 'INGLES',           'idioma ingles',              'SEMESTRE_2', 'IDIOMAS',           2, TRUE, NULL, 30),
    ('ELECTIVA', 'EDUCACION FISICA', 'educacion fisica',           'SEMESTRE_2', 'EDUCACION_FISICA',  3, TRUE, NULL, 25);

-- Horarios de ejemplo para las asignaturas basicas (ids 1 y 2)
INSERT IGNORE INTO horarios (dia, horario_entrada, horario_salida, ubicacion, asignatura_id) VALUES
    ('LUNES',     '08:00:00', '09:30:00', 'SALA 101', 1),
    ('MIERCOLES', '09:50:00', '11:20:00', 'SALA 101', 1),
    ('MARTES',    '08:00:00', '09:30:00', 'SALA 102', 2),
    ('JUEVES',    '12:15:00', '13:45:00', 'SALA 102', 2);

-- Inscripciones de ejemplo en electivas (ids 5 y 6)
INSERT IGNORE INTO inscripciones (id_alumno, asignatura_id, estado, fecha_inscripcion) VALUES
    (1, 5, 'ACTIVO',      NOW(6)),
    (2, 5, 'PRE_INSCRITO', NOW(6)),
    (1, 6, 'ACTIVO',      NOW(6));
