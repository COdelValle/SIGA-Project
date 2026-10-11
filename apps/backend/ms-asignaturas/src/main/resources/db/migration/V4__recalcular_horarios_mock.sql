-- Reparto docente por grupos de niveles (7-8 / 5-6 / 4B) y horario
-- semanal sin choques. Generado por tools/generar-horarios.js.

-- 1. Docentes del grupo 5-6 (clases 8-13) pasan a los docentes del nivel.
UPDATE cursos_asignaturas SET id_docente = 14 WHERE id_clase BETWEEN 8 AND 13 AND id_asignatura = 1;
UPDATE cursos_asignaturas SET id_docente = 15 WHERE id_clase BETWEEN 8 AND 13 AND id_asignatura = 3;
UPDATE cursos_asignaturas SET id_docente = 13 WHERE id_clase BETWEEN 8 AND 13 AND id_asignatura = 9;
UPDATE cursos_asignaturas SET id_docente = 18 WHERE id_clase BETWEEN 8 AND 13 AND id_asignatura = 13;
UPDATE cursos_asignaturas SET id_docente = 17 WHERE id_clase BETWEEN 8 AND 13 AND id_asignatura = 10;
UPDATE cursos_asignaturas SET id_docente = 19 WHERE id_clase BETWEEN 8 AND 13 AND id_asignatura = 11;
UPDATE cursos_asignaturas SET id_docente = 21 WHERE id_clase BETWEEN 8 AND 13 AND id_asignatura = 12;
UPDATE cursos_asignaturas SET id_docente = 20 WHERE id_clase BETWEEN 8 AND 13 AND id_asignatura = 14;
UPDATE cursos_asignaturas SET id_docente = 22 WHERE id_clase BETWEEN 8 AND 13 AND id_asignatura = 15;

-- 4B: Lenguaje y Matematica a los docentes nuevos 24/25.
UPDATE cursos_asignaturas SET id_docente = 24 WHERE id_clase = 7 AND id_asignatura = 1;
UPDATE cursos_asignaturas SET id_docente = 25 WHERE id_clase = 7 AND id_asignatura = 3;
UPDATE cursos_asignaturas SET caracter = 'OPTATIVA' WHERE id_clase = 7 AND id_asignatura = 10;

-- 2. Artes/Musica como electiva en 7-8 (misma franja, alumno elige).
UPDATE cursos_asignaturas SET caracter = 'ELECTIVA' WHERE id_clase BETWEEN 1 AND 6 AND id_asignatura IN (11, 12);

-- 3. Educacion Financiera sale de 5-6.
UPDATE cursos_asignaturas SET active = FALSE WHERE id_clase BETWEEN 8 AND 13 AND id_asignatura = 17;
UPDATE inscripciones SET estado = 'CANCELADO' WHERE curso_asignatura_id IN (SELECT id FROM cursos_asignaturas WHERE id_clase BETWEEN 8 AND 13 AND id_asignatura = 17);

-- 4. Altas: Lengua de Senas y Religion en 5-6 (ids 144-155).
INSERT INTO cursos_asignaturas (id, id_asignatura, id_clase, id_docente, semestre, caracter, cupo_maximo, active) VALUES
    (144, 18, 8, 26, 'SEMESTRE_1', 'OPTATIVA', NULL, TRUE),
    (145, 18, 9, 26, 'SEMESTRE_1', 'OPTATIVA', NULL, TRUE),
    (146, 18, 10, 26, 'SEMESTRE_1', 'OPTATIVA', NULL, TRUE),
    (147, 18, 11, 26, 'SEMESTRE_1', 'OPTATIVA', NULL, TRUE),
    (148, 18, 12, 26, 'SEMESTRE_1', 'OPTATIVA', NULL, TRUE),
    (149, 18, 13, 26, 'SEMESTRE_1', 'OPTATIVA', NULL, TRUE),
    (150, 16, 8, 23, 'SEMESTRE_1', 'OPTATIVA', NULL, TRUE),
    (151, 16, 9, 23, 'SEMESTRE_1', 'OPTATIVA', NULL, TRUE),
    (152, 16, 10, 23, 'SEMESTRE_1', 'OPTATIVA', NULL, TRUE),
    (153, 16, 11, 23, 'SEMESTRE_1', 'OPTATIVA', NULL, TRUE),
    (154, 16, 12, 23, 'SEMESTRE_1', 'OPTATIVA', NULL, TRUE),
    (155, 16, 13, 23, 'SEMESTRE_1', 'OPTATIVA', NULL, TRUE);

-- 5. Inscripciones de la electiva Artes/Musica (7-8).
INSERT INTO inscripciones (id_alumno, curso_asignatura_id, estado, fecha_inscripcion) VALUES
    (23, 93, 'ACTIVO', NOW(6)),
    (24, 95, 'ACTIVO', NOW(6)),
    (25, 93, 'ACTIVO', NOW(6)),
    (26, 104, 'ACTIVO', NOW(6)),
    (27, 106, 'ACTIVO', NOW(6)),
    (28, 104, 'ACTIVO', NOW(6)),
    (29, 115, 'ACTIVO', NOW(6)),
    (30, 117, 'ACTIVO', NOW(6)),
    (31, 115, 'ACTIVO', NOW(6)),
    (1, 7, 'ACTIVO', NOW(6)),
    (32, 9, 'ACTIVO', NOW(6)),
    (33, 7, 'ACTIVO', NOW(6)),
    (34, 126, 'ACTIVO', NOW(6)),
    (35, 128, 'ACTIVO', NOW(6)),
    (36, 126, 'ACTIVO', NOW(6)),
    (37, 137, 'ACTIVO', NOW(6)),
    (38, 139, 'ACTIVO', NOW(6)),
    (39, 137, 'ACTIVO', NOW(6));

-- 6. Horario semanal regenerado.
DELETE FROM horarios;
INSERT INTO horarios (curso_asignatura_id, dia, horario_entrada, horario_salida, ubicacion, active) VALUES
    (92, 'LUNES', '08:00:00', '08:45:00', 'Sala 7° Básico A', TRUE),
    (92, 'LUNES', '08:45:00', '09:30:00', 'Sala 7° Básico A', TRUE),
    (91, 'LUNES', '09:50:00', '10:35:00', 'Sala 7° Básico A', TRUE),
    (91, 'LUNES', '10:35:00', '11:20:00', 'Sala 7° Básico A', TRUE),
    (93, 'LUNES', '12:15:00', '13:00:00', 'Sala 7° Básico A', TRUE),
    (93, 'LUNES', '13:00:00', '13:45:00', 'Sala 7° Básico A', TRUE),
    (95, 'LUNES', '12:15:00', '13:00:00', 'Sala 7° Básico A', TRUE),
    (95, 'LUNES', '13:00:00', '13:45:00', 'Sala 7° Básico A', TRUE),
    (98, 'LUNES', '13:55:00', '14:40:00', 'Sala 7° Básico A', TRUE),
    (98, 'LUNES', '14:40:00', '15:25:00', 'Sala 7° Básico A', TRUE),
    (89, 'MARTES', '08:00:00', '08:45:00', 'Sala 7° Básico A', TRUE),
    (89, 'MARTES', '08:45:00', '09:30:00', 'Sala 7° Básico A', TRUE),
    (97, 'MARTES', '09:50:00', '10:35:00', 'Sala 7° Básico A', TRUE),
    (97, 'MARTES', '10:35:00', '11:20:00', 'Sala 7° Básico A', TRUE),
    (90, 'MARTES', '12:15:00', '13:00:00', 'Sala 7° Básico A', TRUE),
    (90, 'MARTES', '13:00:00', '13:45:00', 'Sala 7° Básico A', TRUE),
    (98, 'MARTES', '13:55:00', '14:40:00', 'Sala 7° Básico A', TRUE),
    (98, 'MARTES', '14:40:00', '15:25:00', 'Sala 7° Básico A', TRUE),
    (94, 'MIERCOLES', '08:00:00', '08:45:00', 'Sala 7° Básico A', TRUE),
    (94, 'MIERCOLES', '08:45:00', '09:30:00', 'Sala 7° Básico A', TRUE),
    (91, 'MIERCOLES', '09:50:00', '10:35:00', 'Sala 7° Básico A', TRUE),
    (91, 'MIERCOLES', '10:35:00', '11:20:00', 'Sala 7° Básico A', TRUE),
    (99, 'MIERCOLES', '12:15:00', '13:00:00', 'Cancha Techada 1', TRUE),
    (99, 'MIERCOLES', '13:00:00', '13:45:00', 'Cancha Techada 1', TRUE),
    (92, 'MIERCOLES', '13:55:00', '14:40:00', 'Sala 7° Básico A', TRUE),
    (92, 'MIERCOLES', '14:40:00', '15:25:00', 'Sala 7° Básico A', TRUE),
    (90, 'JUEVES', '08:00:00', '08:45:00', 'Sala 7° Básico A', TRUE),
    (90, 'JUEVES', '08:45:00', '09:30:00', 'Sala 7° Básico A', TRUE),
    (99, 'JUEVES', '09:50:00', '10:35:00', 'Cancha Techada 1', TRUE),
    (99, 'JUEVES', '10:35:00', '11:20:00', 'Cancha Techada 1', TRUE),
    (89, 'JUEVES', '12:15:00', '13:00:00', 'Sala 7° Básico A', TRUE),
    (89, 'JUEVES', '13:00:00', '13:45:00', 'Sala 7° Básico A', TRUE),
    (91, 'JUEVES', '13:55:00', '14:40:00', 'Sala 7° Básico A', TRUE),
    (91, 'JUEVES', '14:40:00', '15:25:00', 'Sala 7° Básico A', TRUE),
    (96, 'VIERNES', '08:00:00', '08:45:00', 'Sala 7° Básico A', TRUE),
    (96, 'VIERNES', '08:45:00', '09:30:00', 'Sala 7° Básico A', TRUE),
    (89, 'VIERNES', '09:50:00', '10:35:00', 'Sala 7° Básico A', TRUE),
    (89, 'VIERNES', '10:35:00', '11:20:00', 'Sala 7° Básico A', TRUE),
    (92, 'VIERNES', '12:15:00', '13:00:00', 'Sala 7° Básico A', TRUE),
    (92, 'VIERNES', '13:00:00', '13:45:00', 'Sala 7° Básico A', TRUE),
    (90, 'VIERNES', '13:55:00', '14:40:00', 'Sala 7° Básico A', TRUE),
    (90, 'VIERNES', '14:40:00', '15:25:00', 'Sala 7° Básico A', TRUE),
    (101, 'LUNES', '08:00:00', '08:45:00', 'Sala 7° Básico B', TRUE),
    (101, 'LUNES', '08:45:00', '09:30:00', 'Sala 7° Básico B', TRUE),
    (100, 'LUNES', '09:50:00', '10:35:00', 'Sala 7° Básico B', TRUE),
    (100, 'LUNES', '10:35:00', '11:20:00', 'Sala 7° Básico B', TRUE),
    (105, 'LUNES', '12:15:00', '13:00:00', 'Sala 7° Básico B', TRUE),
    (105, 'LUNES', '13:00:00', '13:45:00', 'Sala 7° Básico B', TRUE),
    (103, 'LUNES', '13:55:00', '14:40:00', 'Sala 7° Básico B', TRUE),
    (103, 'LUNES', '14:40:00', '15:25:00', 'Sala 7° Básico B', TRUE),
    (107, 'MARTES', '08:00:00', '08:45:00', 'Sala 7° Básico B', TRUE),
    (107, 'MARTES', '08:45:00', '09:30:00', 'Sala 7° Básico B', TRUE),
    (101, 'MARTES', '09:50:00', '10:35:00', 'Sala 7° Básico B', TRUE),
    (101, 'MARTES', '10:35:00', '11:20:00', 'Sala 7° Básico B', TRUE),
    (110, 'MARTES', '12:15:00', '13:00:00', 'Cancha Techada 1', TRUE),
    (110, 'MARTES', '13:00:00', '13:45:00', 'Cancha Techada 1', TRUE),
    (103, 'MARTES', '13:55:00', '14:40:00', 'Sala 7° Básico B', TRUE),
    (103, 'MARTES', '14:40:00', '15:25:00', 'Sala 7° Básico B', TRUE),
    (102, 'MIERCOLES', '08:00:00', '08:45:00', 'Sala 7° Básico B', TRUE),
    (102, 'MIERCOLES', '08:45:00', '09:30:00', 'Sala 7° Básico B', TRUE),
    (100, 'MIERCOLES', '09:50:00', '10:35:00', 'Sala 7° Básico B', TRUE),
    (100, 'MIERCOLES', '10:35:00', '11:20:00', 'Sala 7° Básico B', TRUE),
    (109, 'MIERCOLES', '12:15:00', '13:00:00', 'Sala 7° Básico B', TRUE),
    (109, 'MIERCOLES', '13:00:00', '13:45:00', 'Sala 7° Básico B', TRUE),
    (110, 'MIERCOLES', '13:55:00', '14:40:00', 'Cancha Techada 1', TRUE),
    (110, 'MIERCOLES', '14:40:00', '15:25:00', 'Cancha Techada 1', TRUE),
    (100, 'JUEVES', '08:00:00', '08:45:00', 'Sala 7° Básico B', TRUE),
    (100, 'JUEVES', '08:45:00', '09:30:00', 'Sala 7° Básico B', TRUE),
    (104, 'JUEVES', '09:50:00', '10:35:00', 'Sala 7° Básico B', TRUE),
    (104, 'JUEVES', '10:35:00', '11:20:00', 'Sala 7° Básico B', TRUE),
    (106, 'JUEVES', '09:50:00', '10:35:00', 'Sala 7° Básico B', TRUE),
    (106, 'JUEVES', '10:35:00', '11:20:00', 'Sala 7° Básico B', TRUE),
    (102, 'JUEVES', '12:15:00', '13:00:00', 'Sala 7° Básico B', TRUE),
    (102, 'JUEVES', '13:00:00', '13:45:00', 'Sala 7° Básico B', TRUE),
    (108, 'JUEVES', '13:55:00', '14:40:00', 'Sala 7° Básico B', TRUE),
    (108, 'JUEVES', '14:40:00', '15:25:00', 'Sala 7° Básico B', TRUE),
    (109, 'VIERNES', '08:00:00', '08:45:00', 'Sala 7° Básico B', TRUE),
    (109, 'VIERNES', '08:45:00', '09:30:00', 'Sala 7° Básico B', TRUE),
    (101, 'VIERNES', '09:50:00', '10:35:00', 'Sala 7° Básico B', TRUE),
    (101, 'VIERNES', '10:35:00', '11:20:00', 'Sala 7° Básico B', TRUE),
    (102, 'VIERNES', '12:15:00', '13:00:00', 'Sala 7° Básico B', TRUE),
    (102, 'VIERNES', '13:00:00', '13:45:00', 'Sala 7° Básico B', TRUE),
    (103, 'VIERNES', '13:55:00', '14:40:00', 'Sala 7° Básico B', TRUE),
    (103, 'VIERNES', '14:40:00', '15:25:00', 'Sala 7° Básico B', TRUE),
    (121, 'LUNES', '08:00:00', '08:45:00', 'Cancha Techada 1', TRUE),
    (121, 'LUNES', '08:45:00', '09:30:00', 'Cancha Techada 1', TRUE),
    (120, 'LUNES', '09:50:00', '10:35:00', 'Sala 7° Básico C', TRUE),
    (120, 'LUNES', '10:35:00', '11:20:00', 'Sala 7° Básico C', TRUE),
    (114, 'LUNES', '12:15:00', '13:00:00', 'Sala 7° Básico C', TRUE),
    (114, 'LUNES', '13:00:00', '13:45:00', 'Sala 7° Básico C', TRUE),
    (112, 'LUNES', '13:55:00', '14:40:00', 'Sala 7° Básico C', TRUE),
    (112, 'LUNES', '14:40:00', '15:25:00', 'Sala 7° Básico C', TRUE),
    (120, 'MARTES', '08:00:00', '08:45:00', 'Sala 7° Básico C', TRUE),
    (120, 'MARTES', '08:45:00', '09:30:00', 'Sala 7° Básico C', TRUE),
    (115, 'MARTES', '09:50:00', '10:35:00', 'Sala 7° Básico C', TRUE),
    (115, 'MARTES', '10:35:00', '11:20:00', 'Sala 7° Básico C', TRUE),
    (117, 'MARTES', '09:50:00', '10:35:00', 'Sala 7° Básico C', TRUE),
    (117, 'MARTES', '10:35:00', '11:20:00', 'Sala 7° Básico C', TRUE),
    (113, 'MARTES', '12:15:00', '13:00:00', 'Sala 7° Básico C', TRUE),
    (113, 'MARTES', '13:00:00', '13:45:00', 'Sala 7° Básico C', TRUE),
    (112, 'MARTES', '13:55:00', '14:40:00', 'Sala 7° Básico C', TRUE),
    (112, 'MARTES', '14:40:00', '15:25:00', 'Sala 7° Básico C', TRUE),
    (119, 'MIERCOLES', '08:00:00', '08:45:00', 'Sala 7° Básico C', TRUE),
    (119, 'MIERCOLES', '08:45:00', '09:30:00', 'Sala 7° Básico C', TRUE),
    (112, 'MIERCOLES', '09:50:00', '10:35:00', 'Sala 7° Básico C', TRUE),
    (112, 'MIERCOLES', '10:35:00', '11:20:00', 'Sala 7° Básico C', TRUE),
    (118, 'MIERCOLES', '12:15:00', '13:00:00', 'Sala 7° Básico C', TRUE),
    (118, 'MIERCOLES', '13:00:00', '13:45:00', 'Sala 7° Básico C', TRUE),
    (111, 'MIERCOLES', '13:55:00', '14:40:00', 'Sala 7° Básico C', TRUE),
    (111, 'MIERCOLES', '14:40:00', '15:25:00', 'Sala 7° Básico C', TRUE),
    (113, 'JUEVES', '08:00:00', '08:45:00', 'Sala 7° Básico C', TRUE),
    (113, 'JUEVES', '08:45:00', '09:30:00', 'Sala 7° Básico C', TRUE),
    (111, 'JUEVES', '09:50:00', '10:35:00', 'Sala 7° Básico C', TRUE),
    (111, 'JUEVES', '10:35:00', '11:20:00', 'Sala 7° Básico C', TRUE),
    (121, 'JUEVES', '12:15:00', '13:00:00', 'Cancha Techada 1', TRUE),
    (121, 'JUEVES', '13:00:00', '13:45:00', 'Cancha Techada 1', TRUE),
    (114, 'JUEVES', '13:55:00', '14:40:00', 'Sala 7° Básico C', TRUE),
    (114, 'JUEVES', '14:40:00', '15:25:00', 'Sala 7° Básico C', TRUE),
    (114, 'VIERNES', '08:00:00', '08:45:00', 'Sala 7° Básico C', TRUE),
    (114, 'VIERNES', '08:45:00', '09:30:00', 'Sala 7° Básico C', TRUE),
    (113, 'VIERNES', '09:50:00', '10:35:00', 'Sala 7° Básico C', TRUE),
    (113, 'VIERNES', '10:35:00', '11:20:00', 'Sala 7° Básico C', TRUE),
    (116, 'VIERNES', '12:15:00', '13:00:00', 'Sala 7° Básico C', TRUE),
    (116, 'VIERNES', '13:00:00', '13:45:00', 'Sala 7° Básico C', TRUE),
    (111, 'VIERNES', '13:55:00', '14:40:00', 'Sala 7° Básico C', TRUE),
    (111, 'VIERNES', '14:40:00', '15:25:00', 'Sala 7° Básico C', TRUE),
    (3, 'LUNES', '08:00:00', '08:45:00', 'Sala 8° Básico A', TRUE),
    (3, 'LUNES', '08:45:00', '09:30:00', 'Sala 8° Básico A', TRUE),
    (10, 'LUNES', '09:50:00', '10:35:00', 'Sala 8° Básico A', TRUE),
    (10, 'LUNES', '10:35:00', '11:20:00', 'Sala 8° Básico A', TRUE),
    (1, 'LUNES', '12:15:00', '13:00:00', 'Sala 8° Básico A', TRUE),
    (1, 'LUNES', '13:00:00', '13:45:00', 'Sala 8° Básico A', TRUE),
    (7, 'LUNES', '13:55:00', '14:40:00', 'Sala 8° Básico A', TRUE),
    (7, 'LUNES', '14:40:00', '15:25:00', 'Sala 8° Básico A', TRUE),
    (9, 'LUNES', '13:55:00', '14:40:00', 'Sala 8° Básico A', TRUE),
    (9, 'LUNES', '14:40:00', '15:25:00', 'Sala 8° Básico A', TRUE),
    (3, 'MARTES', '08:00:00', '08:45:00', 'Sala 8° Básico A', TRUE),
    (3, 'MARTES', '08:45:00', '09:30:00', 'Sala 8° Básico A', TRUE),
    (1, 'MARTES', '09:50:00', '10:35:00', 'Sala 8° Básico A', TRUE),
    (1, 'MARTES', '10:35:00', '11:20:00', 'Sala 8° Básico A', TRUE),
    (4, 'MARTES', '12:15:00', '13:00:00', 'Sala 8° Básico A', TRUE),
    (4, 'MARTES', '13:00:00', '13:45:00', 'Sala 8° Básico A', TRUE),
    (11, 'MARTES', '13:55:00', '14:40:00', 'Sala 8° Básico A', TRUE),
    (11, 'MARTES', '14:40:00', '15:25:00', 'Sala 8° Básico A', TRUE),
    (4, 'MIERCOLES', '08:00:00', '08:45:00', 'Sala 8° Básico A', TRUE),
    (4, 'MIERCOLES', '08:45:00', '09:30:00', 'Sala 8° Básico A', TRUE),
    (6, 'MIERCOLES', '09:50:00', '10:35:00', 'Cancha Techada 1', TRUE),
    (6, 'MIERCOLES', '10:35:00', '11:20:00', 'Cancha Techada 1', TRUE),
    (3, 'MIERCOLES', '12:15:00', '13:00:00', 'Sala 8° Básico A', TRUE),
    (3, 'MIERCOLES', '13:00:00', '13:45:00', 'Sala 8° Básico A', TRUE),
    (2, 'MIERCOLES', '13:55:00', '14:40:00', 'Sala 8° Básico A', TRUE),
    (2, 'MIERCOLES', '14:40:00', '15:25:00', 'Sala 8° Básico A', TRUE),
    (6, 'JUEVES', '08:00:00', '08:45:00', 'Cancha Techada 1', TRUE),
    (6, 'JUEVES', '08:45:00', '09:30:00', 'Cancha Techada 1', TRUE),
    (2, 'JUEVES', '09:50:00', '10:35:00', 'Sala 8° Básico A', TRUE),
    (2, 'JUEVES', '10:35:00', '11:20:00', 'Sala 8° Básico A', TRUE),
    (4, 'JUEVES', '12:15:00', '13:00:00', 'Sala 8° Básico A', TRUE),
    (4, 'JUEVES', '13:00:00', '13:45:00', 'Sala 8° Básico A', TRUE),
    (5, 'JUEVES', '13:55:00', '14:40:00', 'Sala 8° Básico A', TRUE),
    (5, 'JUEVES', '14:40:00', '15:25:00', 'Sala 8° Básico A', TRUE),
    (2, 'VIERNES', '08:00:00', '08:45:00', 'Sala 8° Básico A', TRUE),
    (2, 'VIERNES', '08:45:00', '09:30:00', 'Sala 8° Básico A', TRUE),
    (5, 'VIERNES', '09:50:00', '10:35:00', 'Sala 8° Básico A', TRUE),
    (5, 'VIERNES', '10:35:00', '11:20:00', 'Sala 8° Básico A', TRUE),
    (1, 'VIERNES', '12:15:00', '13:00:00', 'Sala 8° Básico A', TRUE),
    (1, 'VIERNES', '13:00:00', '13:45:00', 'Sala 8° Básico A', TRUE),
    (8, 'VIERNES', '13:55:00', '14:40:00', 'Sala 8° Básico A', TRUE),
    (8, 'VIERNES', '14:40:00', '15:25:00', 'Sala 8° Básico A', TRUE),
    (131, 'LUNES', '08:00:00', '08:45:00', 'Sala 8° Básico B', TRUE),
    (131, 'LUNES', '08:45:00', '09:30:00', 'Sala 8° Básico B', TRUE),
    (123, 'LUNES', '09:50:00', '10:35:00', 'Sala 8° Básico B', TRUE),
    (123, 'LUNES', '10:35:00', '11:20:00', 'Sala 8° Básico B', TRUE),
    (124, 'LUNES', '12:15:00', '13:00:00', 'Sala 8° Básico B', TRUE),
    (124, 'LUNES', '13:00:00', '13:45:00', 'Sala 8° Básico B', TRUE),
    (132, 'LUNES', '13:55:00', '14:40:00', 'Cancha Techada 1', TRUE),
    (132, 'LUNES', '14:40:00', '15:25:00', 'Cancha Techada 1', TRUE),
    (127, 'MARTES', '08:00:00', '08:45:00', 'Sala 8° Básico B', TRUE),
    (127, 'MARTES', '08:45:00', '09:30:00', 'Sala 8° Básico B', TRUE),
    (125, 'MARTES', '09:50:00', '10:35:00', 'Sala 8° Básico B', TRUE),
    (125, 'MARTES', '10:35:00', '11:20:00', 'Sala 8° Básico B', TRUE),
    (122, 'MARTES', '12:15:00', '13:00:00', 'Sala 8° Básico B', TRUE),
    (122, 'MARTES', '13:00:00', '13:45:00', 'Sala 8° Básico B', TRUE),
    (126, 'MARTES', '13:55:00', '14:40:00', 'Sala 8° Básico B', TRUE),
    (126, 'MARTES', '14:40:00', '15:25:00', 'Sala 8° Básico B', TRUE),
    (128, 'MARTES', '13:55:00', '14:40:00', 'Sala 8° Básico B', TRUE),
    (128, 'MARTES', '14:40:00', '15:25:00', 'Sala 8° Básico B', TRUE),
    (132, 'MIERCOLES', '08:00:00', '08:45:00', 'Cancha Techada 1', TRUE),
    (132, 'MIERCOLES', '08:45:00', '09:30:00', 'Cancha Techada 1', TRUE),
    (125, 'MIERCOLES', '09:50:00', '10:35:00', 'Sala 8° Básico B', TRUE),
    (125, 'MIERCOLES', '10:35:00', '11:20:00', 'Sala 8° Básico B', TRUE),
    (122, 'MIERCOLES', '12:15:00', '13:00:00', 'Sala 8° Básico B', TRUE),
    (122, 'MIERCOLES', '13:00:00', '13:45:00', 'Sala 8° Básico B', TRUE),
    (130, 'MIERCOLES', '13:55:00', '14:40:00', 'Sala 8° Básico B', TRUE),
    (130, 'MIERCOLES', '14:40:00', '15:25:00', 'Sala 8° Básico B', TRUE),
    (125, 'JUEVES', '08:00:00', '08:45:00', 'Sala 8° Básico B', TRUE),
    (125, 'JUEVES', '08:45:00', '09:30:00', 'Sala 8° Básico B', TRUE),
    (124, 'JUEVES', '09:50:00', '10:35:00', 'Sala 8° Básico B', TRUE),
    (124, 'JUEVES', '10:35:00', '11:20:00', 'Sala 8° Básico B', TRUE),
    (123, 'JUEVES', '12:15:00', '13:00:00', 'Sala 8° Básico B', TRUE),
    (123, 'JUEVES', '13:00:00', '13:45:00', 'Sala 8° Básico B', TRUE),
    (122, 'JUEVES', '13:55:00', '14:40:00', 'Sala 8° Básico B', TRUE),
    (122, 'JUEVES', '14:40:00', '15:25:00', 'Sala 8° Básico B', TRUE),
    (124, 'VIERNES', '08:00:00', '08:45:00', 'Sala 8° Básico B', TRUE),
    (124, 'VIERNES', '08:45:00', '09:30:00', 'Sala 8° Básico B', TRUE),
    (129, 'VIERNES', '09:50:00', '10:35:00', 'Sala 8° Básico B', TRUE),
    (129, 'VIERNES', '10:35:00', '11:20:00', 'Sala 8° Básico B', TRUE),
    (123, 'VIERNES', '12:15:00', '13:00:00', 'Sala 8° Básico B', TRUE),
    (123, 'VIERNES', '13:00:00', '13:45:00', 'Sala 8° Básico B', TRUE),
    (131, 'VIERNES', '13:55:00', '14:40:00', 'Sala 8° Básico B', TRUE),
    (131, 'VIERNES', '14:40:00', '15:25:00', 'Sala 8° Básico B', TRUE),
    (133, 'LUNES', '08:00:00', '08:45:00', 'Sala 8° Básico C', TRUE),
    (133, 'LUNES', '08:45:00', '09:30:00', 'Sala 8° Básico C', TRUE),
    (136, 'LUNES', '09:50:00', '10:35:00', 'Sala 8° Básico C', TRUE),
    (136, 'LUNES', '10:35:00', '11:20:00', 'Sala 8° Básico C', TRUE),
    (134, 'LUNES', '12:15:00', '13:00:00', 'Sala 8° Básico C', TRUE),
    (134, 'LUNES', '13:00:00', '13:45:00', 'Sala 8° Básico C', TRUE),
    (135, 'LUNES', '13:55:00', '14:40:00', 'Sala 8° Básico C', TRUE),
    (135, 'LUNES', '14:40:00', '15:25:00', 'Sala 8° Básico C', TRUE),
    (134, 'MARTES', '08:00:00', '08:45:00', 'Sala 8° Básico C', TRUE),
    (134, 'MARTES', '08:45:00', '09:30:00', 'Sala 8° Básico C', TRUE),
    (135, 'MARTES', '09:50:00', '10:35:00', 'Sala 8° Básico C', TRUE),
    (135, 'MARTES', '10:35:00', '11:20:00', 'Sala 8° Básico C', TRUE),
    (141, 'MARTES', '12:15:00', '13:00:00', 'Sala 8° Básico C', TRUE),
    (141, 'MARTES', '13:00:00', '13:45:00', 'Sala 8° Básico C', TRUE),
    (143, 'MARTES', '13:55:00', '14:40:00', 'Cancha Techada 1', TRUE),
    (143, 'MARTES', '14:40:00', '15:25:00', 'Cancha Techada 1', TRUE),
    (133, 'MIERCOLES', '08:00:00', '08:45:00', 'Sala 8° Básico C', TRUE),
    (133, 'MIERCOLES', '08:45:00', '09:30:00', 'Sala 8° Básico C', TRUE),
    (140, 'MIERCOLES', '09:50:00', '10:35:00', 'Sala 8° Básico C', TRUE),
    (140, 'MIERCOLES', '10:35:00', '11:20:00', 'Sala 8° Básico C', TRUE),
    (134, 'MIERCOLES', '12:15:00', '13:00:00', 'Sala 8° Básico C', TRUE),
    (134, 'MIERCOLES', '13:00:00', '13:45:00', 'Sala 8° Básico C', TRUE),
    (142, 'MIERCOLES', '13:55:00', '14:40:00', 'Sala 8° Básico C', TRUE),
    (142, 'MIERCOLES', '14:40:00', '15:25:00', 'Sala 8° Básico C', TRUE),
    (142, 'JUEVES', '08:00:00', '08:45:00', 'Sala 8° Básico C', TRUE),
    (142, 'JUEVES', '08:45:00', '09:30:00', 'Sala 8° Básico C', TRUE),
    (136, 'JUEVES', '09:50:00', '10:35:00', 'Sala 8° Básico C', TRUE),
    (136, 'JUEVES', '10:35:00', '11:20:00', 'Sala 8° Básico C', TRUE),
    (138, 'JUEVES', '12:15:00', '13:00:00', 'Sala 8° Básico C', TRUE),
    (138, 'JUEVES', '13:00:00', '13:45:00', 'Sala 8° Básico C', TRUE),
    (137, 'JUEVES', '13:55:00', '14:40:00', 'Sala 8° Básico C', TRUE),
    (137, 'JUEVES', '14:40:00', '15:25:00', 'Sala 8° Básico C', TRUE),
    (139, 'JUEVES', '13:55:00', '14:40:00', 'Sala 8° Básico C', TRUE),
    (139, 'JUEVES', '14:40:00', '15:25:00', 'Sala 8° Básico C', TRUE),
    (133, 'VIERNES', '08:00:00', '08:45:00', 'Sala 8° Básico C', TRUE),
    (133, 'VIERNES', '08:45:00', '09:30:00', 'Sala 8° Básico C', TRUE),
    (136, 'VIERNES', '09:50:00', '10:35:00', 'Sala 8° Básico C', TRUE),
    (136, 'VIERNES', '10:35:00', '11:20:00', 'Sala 8° Básico C', TRUE),
    (143, 'VIERNES', '12:15:00', '13:00:00', 'Cancha Techada 1', TRUE),
    (143, 'VIERNES', '13:00:00', '13:45:00', 'Cancha Techada 1', TRUE),
    (135, 'VIERNES', '13:55:00', '14:40:00', 'Sala 8° Básico C', TRUE),
    (135, 'VIERNES', '14:40:00', '15:25:00', 'Sala 8° Básico C', TRUE),
    (13, 'LUNES', '08:00:00', '08:45:00', 'Sala 4° Básico B', TRUE),
    (13, 'LUNES', '08:45:00', '09:30:00', 'Sala 4° Básico B', TRUE),
    (15, 'LUNES', '09:50:00', '10:35:00', 'Sala 4° Básico B', TRUE),
    (15, 'LUNES', '10:35:00', '11:20:00', 'Sala 4° Básico B', TRUE),
    (12, 'LUNES', '12:15:00', '13:00:00', 'Sala 4° Básico B', TRUE),
    (12, 'LUNES', '13:00:00', '13:45:00', 'Sala 4° Básico B', TRUE),
    (14, 'LUNES', '13:55:00', '14:40:00', 'Sala 4° Básico B', TRUE),
    (14, 'LUNES', '14:40:00', '15:25:00', 'Sala 4° Básico B', TRUE),
    (22, 'MARTES', '08:00:00', '08:45:00', 'Patio Cubierto 2', TRUE),
    (22, 'MARTES', '08:45:00', '09:30:00', 'Patio Cubierto 2', TRUE),
    (21, 'MARTES', '09:50:00', '10:35:00', 'Sala 4° Básico B', TRUE),
    (21, 'MARTES', '10:35:00', '11:20:00', 'Sala 4° Básico B', TRUE),
    (14, 'MARTES', '12:15:00', '13:00:00', 'Sala 4° Básico B', TRUE),
    (14, 'MARTES', '13:00:00', '13:45:00', 'Sala 4° Básico B', TRUE),
    (12, 'MARTES', '13:55:00', '14:40:00', 'Sala 4° Básico B', TRUE),
    (12, 'MARTES', '14:40:00', '15:25:00', 'Sala 4° Básico B', TRUE),
    (15, 'MIERCOLES', '08:00:00', '08:45:00', 'Sala 4° Básico B', TRUE),
    (15, 'MIERCOLES', '08:45:00', '09:30:00', 'Sala 4° Básico B', TRUE),
    (19, 'MIERCOLES', '09:50:00', '10:35:00', 'Sala 4° Básico B', TRUE),
    (19, 'MIERCOLES', '10:35:00', '11:20:00', 'Sala 4° Básico B', TRUE),
    (14, 'MIERCOLES', '12:15:00', '13:00:00', 'Sala 4° Básico B', TRUE),
    (14, 'MIERCOLES', '13:00:00', '13:45:00', 'Sala 4° Básico B', TRUE),
    (22, 'MIERCOLES', '13:55:00', '14:40:00', 'Patio Cubierto 2', TRUE),
    (22, 'MIERCOLES', '14:40:00', '15:25:00', 'Patio Cubierto 2', TRUE),
    (15, 'JUEVES', '08:00:00', '08:45:00', 'Sala 4° Básico B', TRUE),
    (15, 'JUEVES', '08:45:00', '09:30:00', 'Sala 4° Básico B', TRUE),
    (13, 'JUEVES', '09:50:00', '10:35:00', 'Sala 4° Básico B', TRUE),
    (13, 'JUEVES', '10:35:00', '11:20:00', 'Sala 4° Básico B', TRUE),
    (17, 'JUEVES', '12:15:00', '13:00:00', 'Sala 4° Básico B', TRUE),
    (17, 'JUEVES', '13:00:00', '13:45:00', 'Sala 4° Básico B', TRUE),
    (20, 'JUEVES', '13:55:00', '14:40:00', 'Sala 4° Básico B', TRUE),
    (20, 'JUEVES', '14:40:00', '15:25:00', 'Sala 4° Básico B', TRUE),
    (13, 'VIERNES', '08:00:00', '08:45:00', 'Sala 4° Básico B', TRUE),
    (13, 'VIERNES', '08:45:00', '09:30:00', 'Sala 4° Básico B', TRUE),
    (16, 'VIERNES', '09:50:00', '10:35:00', 'Sala 4° Básico B', TRUE),
    (16, 'VIERNES', '10:35:00', '11:20:00', 'Sala 4° Básico B', TRUE),
    (12, 'VIERNES', '12:15:00', '13:00:00', 'Sala 4° Básico B', TRUE),
    (12, 'VIERNES', '13:00:00', '13:45:00', 'Sala 4° Básico B', TRUE),
    (18, 'VIERNES', '13:55:00', '14:40:00', 'Sala 4° Básico B', TRUE),
    (18, 'VIERNES', '14:40:00', '15:25:00', 'Sala 4° Básico B', TRUE),
    (26, 'LUNES', '08:00:00', '08:45:00', 'Sala 5° Básico A', TRUE),
    (26, 'LUNES', '08:45:00', '09:30:00', 'Sala 5° Básico A', TRUE),
    (30, 'LUNES', '09:50:00', '10:35:00', 'Sala 5° Básico A', TRUE),
    (30, 'LUNES', '10:35:00', '11:20:00', 'Sala 5° Básico A', TRUE),
    (33, 'LUNES', '12:15:00', '13:00:00', 'Cancha Techada 1', TRUE),
    (33, 'LUNES', '13:00:00', '13:45:00', 'Cancha Techada 1', TRUE),
    (29, 'LUNES', '13:55:00', '14:40:00', 'Sala 5° Básico A', TRUE),
    (29, 'LUNES', '14:40:00', '15:25:00', 'Sala 5° Básico A', TRUE),
    (144, 'MARTES', '08:00:00', '08:45:00', 'Sala 5° Básico A', TRUE),
    (144, 'MARTES', '08:45:00', '09:30:00', 'Sala 5° Básico A', TRUE),
    (26, 'MARTES', '09:50:00', '10:35:00', 'Sala 5° Básico A', TRUE),
    (26, 'MARTES', '10:35:00', '11:20:00', 'Sala 5° Básico A', TRUE),
    (25, 'MARTES', '12:15:00', '13:00:00', 'Sala 5° Básico A', TRUE),
    (25, 'MARTES', '13:00:00', '13:45:00', 'Sala 5° Básico A', TRUE),
    (27, 'MARTES', '13:55:00', '14:40:00', 'Sala 5° Básico A', TRUE),
    (27, 'MARTES', '14:40:00', '15:25:00', 'Sala 5° Básico A', TRUE),
    (24, 'MIERCOLES', '08:00:00', '08:45:00', 'Sala 5° Básico A', TRUE),
    (24, 'MIERCOLES', '08:45:00', '09:30:00', 'Sala 5° Básico A', TRUE),
    (32, 'MIERCOLES', '09:50:00', '10:35:00', 'Sala 5° Básico A', TRUE),
    (32, 'MIERCOLES', '10:35:00', '11:20:00', 'Sala 5° Básico A', TRUE),
    (23, 'MIERCOLES', '12:15:00', '13:00:00', 'Sala 5° Básico A', TRUE),
    (23, 'MIERCOLES', '13:00:00', '13:45:00', 'Sala 5° Básico A', TRUE),
    (150, 'MIERCOLES', '13:55:00', '14:40:00', 'Sala 5° Básico A', TRUE),
    (150, 'MIERCOLES', '14:40:00', '15:25:00', 'Sala 5° Básico A', TRUE),
    (24, 'JUEVES', '08:00:00', '08:45:00', 'Sala 5° Básico A', TRUE),
    (24, 'JUEVES', '08:45:00', '09:30:00', 'Sala 5° Básico A', TRUE),
    (33, 'JUEVES', '09:50:00', '10:35:00', 'Cancha Techada 1', TRUE),
    (33, 'JUEVES', '10:35:00', '11:20:00', 'Cancha Techada 1', TRUE),
    (23, 'JUEVES', '12:15:00', '13:00:00', 'Sala 5° Básico A', TRUE),
    (23, 'JUEVES', '13:00:00', '13:45:00', 'Sala 5° Básico A', TRUE),
    (25, 'JUEVES', '13:55:00', '14:40:00', 'Sala 5° Básico A', TRUE),
    (25, 'JUEVES', '14:40:00', '15:25:00', 'Sala 5° Básico A', TRUE),
    (28, 'VIERNES', '08:00:00', '08:45:00', 'Sala 5° Básico A', TRUE),
    (28, 'VIERNES', '08:45:00', '09:30:00', 'Sala 5° Básico A', TRUE),
    (24, 'VIERNES', '09:50:00', '10:35:00', 'Sala 5° Básico A', TRUE),
    (24, 'VIERNES', '10:35:00', '11:20:00', 'Sala 5° Básico A', TRUE),
    (23, 'VIERNES', '12:15:00', '13:00:00', 'Sala 5° Básico A', TRUE),
    (23, 'VIERNES', '13:00:00', '13:45:00', 'Sala 5° Básico A', TRUE),
    (32, 'VIERNES', '13:55:00', '14:40:00', 'Sala 5° Básico A', TRUE),
    (32, 'VIERNES', '14:40:00', '15:25:00', 'Sala 5° Básico A', TRUE),
    (34, 'LUNES', '08:00:00', '08:45:00', 'Sala 5° Básico B', TRUE),
    (34, 'LUNES', '08:45:00', '09:30:00', 'Sala 5° Básico B', TRUE),
    (35, 'LUNES', '09:50:00', '10:35:00', 'Sala 5° Básico B', TRUE),
    (35, 'LUNES', '10:35:00', '11:20:00', 'Sala 5° Básico B', TRUE),
    (39, 'LUNES', '12:15:00', '13:00:00', 'Sala 5° Básico B', TRUE),
    (39, 'LUNES', '13:00:00', '13:45:00', 'Sala 5° Básico B', TRUE),
    (44, 'LUNES', '13:55:00', '14:40:00', 'Cancha Techada 1', TRUE),
    (44, 'LUNES', '14:40:00', '15:25:00', 'Cancha Techada 1', TRUE),
    (38, 'MARTES', '08:00:00', '08:45:00', 'Sala 5° Básico B', TRUE),
    (38, 'MARTES', '08:45:00', '09:30:00', 'Sala 5° Básico B', TRUE),
    (145, 'MARTES', '09:50:00', '10:35:00', 'Sala 5° Básico B', TRUE),
    (145, 'MARTES', '10:35:00', '11:20:00', 'Sala 5° Básico B', TRUE),
    (43, 'MARTES', '12:15:00', '13:00:00', 'Sala 5° Básico B', TRUE),
    (43, 'MARTES', '13:00:00', '13:45:00', 'Sala 5° Básico B', TRUE),
    (34, 'MARTES', '13:55:00', '14:40:00', 'Sala 5° Básico B', TRUE),
    (34, 'MARTES', '14:40:00', '15:25:00', 'Sala 5° Básico B', TRUE),
    (36, 'MIERCOLES', '08:00:00', '08:45:00', 'Sala 5° Básico B', TRUE),
    (36, 'MIERCOLES', '08:45:00', '09:30:00', 'Sala 5° Básico B', TRUE),
    (44, 'MIERCOLES', '09:50:00', '10:35:00', 'Cancha Techada 1', TRUE),
    (44, 'MIERCOLES', '10:35:00', '11:20:00', 'Cancha Techada 1', TRUE),
    (43, 'MIERCOLES', '12:15:00', '13:00:00', 'Sala 5° Básico B', TRUE),
    (43, 'MIERCOLES', '13:00:00', '13:45:00', 'Sala 5° Básico B', TRUE),
    (34, 'MIERCOLES', '13:55:00', '14:40:00', 'Sala 5° Básico B', TRUE),
    (34, 'MIERCOLES', '14:40:00', '15:25:00', 'Sala 5° Básico B', TRUE),
    (36, 'JUEVES', '08:00:00', '08:45:00', 'Sala 5° Básico B', TRUE),
    (36, 'JUEVES', '08:45:00', '09:30:00', 'Sala 5° Básico B', TRUE),
    (35, 'JUEVES', '09:50:00', '10:35:00', 'Sala 5° Básico B', TRUE),
    (35, 'JUEVES', '10:35:00', '11:20:00', 'Sala 5° Básico B', TRUE),
    (37, 'JUEVES', '12:15:00', '13:00:00', 'Sala 5° Básico B', TRUE),
    (37, 'JUEVES', '13:00:00', '13:45:00', 'Sala 5° Básico B', TRUE),
    (41, 'JUEVES', '13:55:00', '14:40:00', 'Sala 5° Básico B', TRUE),
    (41, 'JUEVES', '14:40:00', '15:25:00', 'Sala 5° Básico B', TRUE),
    (151, 'VIERNES', '08:00:00', '08:45:00', 'Sala 5° Básico B', TRUE),
    (151, 'VIERNES', '08:45:00', '09:30:00', 'Sala 5° Básico B', TRUE),
    (40, 'VIERNES', '09:50:00', '10:35:00', 'Sala 5° Básico B', TRUE),
    (40, 'VIERNES', '10:35:00', '11:20:00', 'Sala 5° Básico B', TRUE),
    (37, 'VIERNES', '12:15:00', '13:00:00', 'Sala 5° Básico B', TRUE),
    (37, 'VIERNES', '13:00:00', '13:45:00', 'Sala 5° Básico B', TRUE),
    (35, 'VIERNES', '13:55:00', '14:40:00', 'Sala 5° Básico B', TRUE),
    (35, 'VIERNES', '14:40:00', '15:25:00', 'Sala 5° Básico B', TRUE),
    (54, 'LUNES', '08:00:00', '08:45:00', 'Sala 5° Básico C', TRUE),
    (54, 'LUNES', '08:45:00', '09:30:00', 'Sala 5° Básico C', TRUE),
    (152, 'LUNES', '09:50:00', '10:35:00', 'Sala 5° Básico C', TRUE),
    (152, 'LUNES', '10:35:00', '11:20:00', 'Sala 5° Básico C', TRUE),
    (45, 'LUNES', '12:15:00', '13:00:00', 'Sala 5° Básico C', TRUE),
    (45, 'LUNES', '13:00:00', '13:45:00', 'Sala 5° Básico C', TRUE),
    (46, 'LUNES', '13:55:00', '14:40:00', 'Sala 5° Básico C', TRUE),
    (46, 'LUNES', '14:40:00', '15:25:00', 'Sala 5° Básico C', TRUE),
    (46, 'MARTES', '08:00:00', '08:45:00', 'Sala 5° Básico C', TRUE),
    (46, 'MARTES', '08:45:00', '09:30:00', 'Sala 5° Básico C', TRUE),
    (55, 'MARTES', '09:50:00', '10:35:00', 'Cancha Techada 1', TRUE),
    (55, 'MARTES', '10:35:00', '11:20:00', 'Cancha Techada 1', TRUE),
    (52, 'MARTES', '12:15:00', '13:00:00', 'Sala 5° Básico C', TRUE),
    (52, 'MARTES', '13:00:00', '13:45:00', 'Sala 5° Básico C', TRUE),
    (48, 'MARTES', '13:55:00', '14:40:00', 'Sala 5° Básico C', TRUE),
    (48, 'MARTES', '14:40:00', '15:25:00', 'Sala 5° Básico C', TRUE),
    (51, 'MIERCOLES', '08:00:00', '08:45:00', 'Sala 5° Básico C', TRUE),
    (51, 'MIERCOLES', '08:45:00', '09:30:00', 'Sala 5° Básico C', TRUE),
    (48, 'MIERCOLES', '09:50:00', '10:35:00', 'Sala 5° Básico C', TRUE),
    (48, 'MIERCOLES', '10:35:00', '11:20:00', 'Sala 5° Básico C', TRUE),
    (47, 'MIERCOLES', '12:15:00', '13:00:00', 'Sala 5° Básico C', TRUE),
    (47, 'MIERCOLES', '13:00:00', '13:45:00', 'Sala 5° Básico C', TRUE),
    (46, 'MIERCOLES', '13:55:00', '14:40:00', 'Sala 5° Básico C', TRUE),
    (46, 'MIERCOLES', '14:40:00', '15:25:00', 'Sala 5° Básico C', TRUE),
    (45, 'JUEVES', '08:00:00', '08:45:00', 'Sala 5° Básico C', TRUE),
    (45, 'JUEVES', '08:45:00', '09:30:00', 'Sala 5° Básico C', TRUE),
    (50, 'JUEVES', '09:50:00', '10:35:00', 'Sala 5° Básico C', TRUE),
    (50, 'JUEVES', '10:35:00', '11:20:00', 'Sala 5° Básico C', TRUE),
    (49, 'JUEVES', '12:15:00', '13:00:00', 'Sala 5° Básico C', TRUE),
    (49, 'JUEVES', '13:00:00', '13:45:00', 'Sala 5° Básico C', TRUE),
    (146, 'JUEVES', '13:55:00', '14:40:00', 'Sala 5° Básico C', TRUE),
    (146, 'JUEVES', '14:40:00', '15:25:00', 'Sala 5° Básico C', TRUE),
    (45, 'VIERNES', '08:00:00', '08:45:00', 'Sala 5° Básico C', TRUE),
    (45, 'VIERNES', '08:45:00', '09:30:00', 'Sala 5° Básico C', TRUE),
    (55, 'VIERNES', '09:50:00', '10:35:00', 'Cancha Techada 1', TRUE),
    (55, 'VIERNES', '10:35:00', '11:20:00', 'Cancha Techada 1', TRUE),
    (54, 'VIERNES', '12:15:00', '13:00:00', 'Sala 5° Básico C', TRUE),
    (54, 'VIERNES', '13:00:00', '13:45:00', 'Sala 5° Básico C', TRUE),
    (47, 'VIERNES', '13:55:00', '14:40:00', 'Sala 5° Básico C', TRUE),
    (47, 'VIERNES', '14:40:00', '15:25:00', 'Sala 5° Básico C', TRUE),
    (57, 'LUNES', '08:00:00', '08:45:00', 'Sala 6° Básico A', TRUE),
    (57, 'LUNES', '08:45:00', '09:30:00', 'Sala 6° Básico A', TRUE),
    (65, 'LUNES', '09:50:00', '10:35:00', 'Sala 6° Básico A', TRUE),
    (65, 'LUNES', '10:35:00', '11:20:00', 'Sala 6° Básico A', TRUE),
    (153, 'LUNES', '12:15:00', '13:00:00', 'Sala 6° Básico A', TRUE),
    (153, 'LUNES', '13:00:00', '13:45:00', 'Sala 6° Básico A', TRUE),
    (56, 'LUNES', '13:55:00', '14:40:00', 'Sala 6° Básico A', TRUE),
    (56, 'LUNES', '14:40:00', '15:25:00', 'Sala 6° Básico A', TRUE),
    (56, 'MARTES', '08:00:00', '08:45:00', 'Sala 6° Básico A', TRUE),
    (56, 'MARTES', '08:45:00', '09:30:00', 'Sala 6° Básico A', TRUE),
    (62, 'MARTES', '09:50:00', '10:35:00', 'Sala 6° Básico A', TRUE),
    (62, 'MARTES', '10:35:00', '11:20:00', 'Sala 6° Básico A', TRUE),
    (57, 'MARTES', '12:15:00', '13:00:00', 'Sala 6° Básico A', TRUE),
    (57, 'MARTES', '13:00:00', '13:45:00', 'Sala 6° Básico A', TRUE),
    (66, 'MARTES', '13:55:00', '14:40:00', 'Cancha Techada 1', TRUE),
    (66, 'MARTES', '14:40:00', '15:25:00', 'Cancha Techada 1', TRUE),
    (61, 'MIERCOLES', '08:00:00', '08:45:00', 'Sala 6° Básico A', TRUE),
    (61, 'MIERCOLES', '08:45:00', '09:30:00', 'Sala 6° Básico A', TRUE),
    (58, 'MIERCOLES', '09:50:00', '10:35:00', 'Sala 6° Básico A', TRUE),
    (58, 'MIERCOLES', '10:35:00', '11:20:00', 'Sala 6° Básico A', TRUE),
    (57, 'MIERCOLES', '12:15:00', '13:00:00', 'Sala 6° Básico A', TRUE),
    (57, 'MIERCOLES', '13:00:00', '13:45:00', 'Sala 6° Básico A', TRUE),
    (147, 'MIERCOLES', '13:55:00', '14:40:00', 'Sala 6° Básico A', TRUE),
    (147, 'MIERCOLES', '14:40:00', '15:25:00', 'Sala 6° Básico A', TRUE),
    (60, 'JUEVES', '08:00:00', '08:45:00', 'Sala 6° Básico A', TRUE),
    (60, 'JUEVES', '08:45:00', '09:30:00', 'Sala 6° Básico A', TRUE),
    (59, 'JUEVES', '09:50:00', '10:35:00', 'Sala 6° Básico A', TRUE),
    (59, 'JUEVES', '10:35:00', '11:20:00', 'Sala 6° Básico A', TRUE),
    (65, 'JUEVES', '12:15:00', '13:00:00', 'Sala 6° Básico A', TRUE),
    (65, 'JUEVES', '13:00:00', '13:45:00', 'Sala 6° Básico A', TRUE),
    (56, 'JUEVES', '13:55:00', '14:40:00', 'Sala 6° Básico A', TRUE),
    (56, 'JUEVES', '14:40:00', '15:25:00', 'Sala 6° Básico A', TRUE),
    (59, 'VIERNES', '08:00:00', '08:45:00', 'Sala 6° Básico A', TRUE),
    (59, 'VIERNES', '08:45:00', '09:30:00', 'Sala 6° Básico A', TRUE),
    (63, 'VIERNES', '09:50:00', '10:35:00', 'Sala 6° Básico A', TRUE),
    (63, 'VIERNES', '10:35:00', '11:20:00', 'Sala 6° Básico A', TRUE),
    (58, 'VIERNES', '12:15:00', '13:00:00', 'Sala 6° Básico A', TRUE),
    (58, 'VIERNES', '13:00:00', '13:45:00', 'Sala 6° Básico A', TRUE),
    (66, 'VIERNES', '13:55:00', '14:40:00', 'Cancha Techada 1', TRUE),
    (66, 'VIERNES', '14:40:00', '15:25:00', 'Cancha Techada 1', TRUE),
    (69, 'LUNES', '08:00:00', '08:45:00', 'Sala 6° Básico B', TRUE),
    (69, 'LUNES', '08:45:00', '09:30:00', 'Sala 6° Básico B', TRUE),
    (67, 'LUNES', '09:50:00', '10:35:00', 'Sala 6° Básico B', TRUE),
    (67, 'LUNES', '10:35:00', '11:20:00', 'Sala 6° Básico B', TRUE),
    (68, 'LUNES', '12:15:00', '13:00:00', 'Sala 6° Básico B', TRUE),
    (68, 'LUNES', '13:00:00', '13:45:00', 'Sala 6° Básico B', TRUE),
    (70, 'LUNES', '13:55:00', '14:40:00', 'Sala 6° Básico B', TRUE),
    (70, 'LUNES', '14:40:00', '15:25:00', 'Sala 6° Básico B', TRUE),
    (76, 'MARTES', '08:00:00', '08:45:00', 'Sala 6° Básico B', TRUE),
    (76, 'MARTES', '08:45:00', '09:30:00', 'Sala 6° Básico B', TRUE),
    (68, 'MARTES', '09:50:00', '10:35:00', 'Sala 6° Básico B', TRUE),
    (68, 'MARTES', '10:35:00', '11:20:00', 'Sala 6° Básico B', TRUE),
    (77, 'MARTES', '12:15:00', '13:00:00', 'Cancha Techada 1', TRUE),
    (77, 'MARTES', '13:00:00', '13:45:00', 'Cancha Techada 1', TRUE),
    (69, 'MARTES', '13:55:00', '14:40:00', 'Sala 6° Básico B', TRUE),
    (69, 'MARTES', '14:40:00', '15:25:00', 'Sala 6° Básico B', TRUE),
    (71, 'MIERCOLES', '08:00:00', '08:45:00', 'Sala 6° Básico B', TRUE),
    (71, 'MIERCOLES', '08:45:00', '09:30:00', 'Sala 6° Básico B', TRUE),
    (67, 'MIERCOLES', '09:50:00', '10:35:00', 'Sala 6° Básico B', TRUE),
    (67, 'MIERCOLES', '10:35:00', '11:20:00', 'Sala 6° Básico B', TRUE),
    (154, 'MIERCOLES', '12:15:00', '13:00:00', 'Sala 6° Básico B', TRUE),
    (154, 'MIERCOLES', '13:00:00', '13:45:00', 'Sala 6° Básico B', TRUE),
    (76, 'MIERCOLES', '13:55:00', '14:40:00', 'Sala 6° Básico B', TRUE),
    (76, 'MIERCOLES', '14:40:00', '15:25:00', 'Sala 6° Básico B', TRUE),
    (74, 'JUEVES', '08:00:00', '08:45:00', 'Sala 6° Básico B', TRUE),
    (74, 'JUEVES', '08:45:00', '09:30:00', 'Sala 6° Básico B', TRUE),
    (67, 'JUEVES', '09:50:00', '10:35:00', 'Sala 6° Básico B', TRUE),
    (67, 'JUEVES', '10:35:00', '11:20:00', 'Sala 6° Básico B', TRUE),
    (73, 'JUEVES', '12:15:00', '13:00:00', 'Sala 6° Básico B', TRUE),
    (73, 'JUEVES', '13:00:00', '13:45:00', 'Sala 6° Básico B', TRUE),
    (77, 'JUEVES', '13:55:00', '14:40:00', 'Cancha Techada 1', TRUE),
    (77, 'JUEVES', '14:40:00', '15:25:00', 'Cancha Techada 1', TRUE),
    (68, 'VIERNES', '08:00:00', '08:45:00', 'Sala 6° Básico B', TRUE),
    (68, 'VIERNES', '08:45:00', '09:30:00', 'Sala 6° Básico B', TRUE),
    (70, 'VIERNES', '09:50:00', '10:35:00', 'Sala 6° Básico B', TRUE),
    (70, 'VIERNES', '10:35:00', '11:20:00', 'Sala 6° Básico B', TRUE),
    (148, 'VIERNES', '12:15:00', '13:00:00', 'Sala 6° Básico B', TRUE),
    (148, 'VIERNES', '13:00:00', '13:45:00', 'Sala 6° Básico B', TRUE),
    (72, 'VIERNES', '13:55:00', '14:40:00', 'Sala 6° Básico B', TRUE),
    (72, 'VIERNES', '14:40:00', '15:25:00', 'Sala 6° Básico B', TRUE),
    (88, 'LUNES', '08:00:00', '08:45:00', 'Cancha Techada 1', TRUE),
    (88, 'LUNES', '08:45:00', '09:30:00', 'Cancha Techada 1', TRUE),
    (80, 'LUNES', '09:50:00', '10:35:00', 'Sala 6° Básico C', TRUE),
    (80, 'LUNES', '10:35:00', '11:20:00', 'Sala 6° Básico C', TRUE),
    (81, 'LUNES', '12:15:00', '13:00:00', 'Sala 6° Básico C', TRUE),
    (81, 'LUNES', '13:00:00', '13:45:00', 'Sala 6° Básico C', TRUE),
    (87, 'LUNES', '13:55:00', '14:40:00', 'Sala 6° Básico C', TRUE),
    (87, 'LUNES', '14:40:00', '15:25:00', 'Sala 6° Básico C', TRUE),
    (81, 'MARTES', '08:00:00', '08:45:00', 'Sala 6° Básico C', TRUE),
    (81, 'MARTES', '08:45:00', '09:30:00', 'Sala 6° Básico C', TRUE),
    (155, 'MARTES', '09:50:00', '10:35:00', 'Sala 6° Básico C', TRUE),
    (155, 'MARTES', '10:35:00', '11:20:00', 'Sala 6° Básico C', TRUE),
    (78, 'MARTES', '12:15:00', '13:00:00', 'Sala 6° Básico C', TRUE),
    (78, 'MARTES', '13:00:00', '13:45:00', 'Sala 6° Básico C', TRUE),
    (87, 'MARTES', '13:55:00', '14:40:00', 'Sala 6° Básico C', TRUE),
    (87, 'MARTES', '14:40:00', '15:25:00', 'Sala 6° Básico C', TRUE),
    (78, 'MIERCOLES', '08:00:00', '08:45:00', 'Sala 6° Básico C', TRUE),
    (78, 'MIERCOLES', '08:45:00', '09:30:00', 'Sala 6° Básico C', TRUE),
    (79, 'MIERCOLES', '09:50:00', '10:35:00', 'Sala 6° Básico C', TRUE),
    (79, 'MIERCOLES', '10:35:00', '11:20:00', 'Sala 6° Básico C', TRUE),
    (85, 'MIERCOLES', '12:15:00', '13:00:00', 'Sala 6° Básico C', TRUE),
    (85, 'MIERCOLES', '13:00:00', '13:45:00', 'Sala 6° Básico C', TRUE),
    (83, 'MIERCOLES', '13:55:00', '14:40:00', 'Sala 6° Básico C', TRUE),
    (83, 'MIERCOLES', '14:40:00', '15:25:00', 'Sala 6° Básico C', TRUE),
    (84, 'JUEVES', '08:00:00', '08:45:00', 'Sala 6° Básico C', TRUE),
    (84, 'JUEVES', '08:45:00', '09:30:00', 'Sala 6° Básico C', TRUE),
    (149, 'JUEVES', '09:50:00', '10:35:00', 'Sala 6° Básico C', TRUE),
    (149, 'JUEVES', '10:35:00', '11:20:00', 'Sala 6° Básico C', TRUE),
    (88, 'JUEVES', '12:15:00', '13:00:00', 'Cancha Techada 1', TRUE),
    (88, 'JUEVES', '13:00:00', '13:45:00', 'Cancha Techada 1', TRUE),
    (79, 'JUEVES', '13:55:00', '14:40:00', 'Sala 6° Básico C', TRUE),
    (79, 'JUEVES', '14:40:00', '15:25:00', 'Sala 6° Básico C', TRUE),
    (80, 'VIERNES', '08:00:00', '08:45:00', 'Sala 6° Básico C', TRUE),
    (80, 'VIERNES', '08:45:00', '09:30:00', 'Sala 6° Básico C', TRUE),
    (78, 'VIERNES', '09:50:00', '10:35:00', 'Sala 6° Básico C', TRUE),
    (78, 'VIERNES', '10:35:00', '11:20:00', 'Sala 6° Básico C', TRUE),
    (79, 'VIERNES', '12:15:00', '13:00:00', 'Sala 6° Básico C', TRUE),
    (79, 'VIERNES', '13:00:00', '13:45:00', 'Sala 6° Básico C', TRUE),
    (82, 'VIERNES', '13:55:00', '14:40:00', 'Sala 6° Básico C', TRUE),
    (82, 'VIERNES', '14:40:00', '15:25:00', 'Sala 6° Básico C', TRUE);

-- 7. Malla curricular: horas semanales por nivel (horas de 45 min).
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'PRIMERO_BASICO' AND id_asignatura = 1 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'PRIMERO_BASICO' AND id_asignatura = 3 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'PRIMERO_BASICO' AND id_asignatura = 4 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'PRIMERO_BASICO' AND id_asignatura = 9 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 4 WHERE nivel = 'PRIMERO_BASICO' AND id_asignatura = 13 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'PRIMERO_BASICO' AND id_asignatura = 10 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'PRIMERO_BASICO' AND id_asignatura = 11 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'PRIMERO_BASICO' AND id_asignatura = 12 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'PRIMERO_BASICO' AND id_asignatura = 15 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'PRIMERO_BASICO' AND id_asignatura = 14 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'PRIMERO_BASICO' AND id_asignatura = 16 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'SEGUNDO_BASICO' AND id_asignatura = 1 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'SEGUNDO_BASICO' AND id_asignatura = 3 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'SEGUNDO_BASICO' AND id_asignatura = 4 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'SEGUNDO_BASICO' AND id_asignatura = 9 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 4 WHERE nivel = 'SEGUNDO_BASICO' AND id_asignatura = 13 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'SEGUNDO_BASICO' AND id_asignatura = 10 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'SEGUNDO_BASICO' AND id_asignatura = 11 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'SEGUNDO_BASICO' AND id_asignatura = 12 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'SEGUNDO_BASICO' AND id_asignatura = 15 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'SEGUNDO_BASICO' AND id_asignatura = 14 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'SEGUNDO_BASICO' AND id_asignatura = 16 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'TERCERO_BASICO' AND id_asignatura = 1 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'TERCERO_BASICO' AND id_asignatura = 3 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'TERCERO_BASICO' AND id_asignatura = 4 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'TERCERO_BASICO' AND id_asignatura = 9 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 4 WHERE nivel = 'TERCERO_BASICO' AND id_asignatura = 13 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'TERCERO_BASICO' AND id_asignatura = 10 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'TERCERO_BASICO' AND id_asignatura = 11 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'TERCERO_BASICO' AND id_asignatura = 12 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'TERCERO_BASICO' AND id_asignatura = 15 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'TERCERO_BASICO' AND id_asignatura = 14 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'TERCERO_BASICO' AND id_asignatura = 16 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'CUARTO_BASICO' AND id_asignatura = 1 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'CUARTO_BASICO' AND id_asignatura = 3 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'CUARTO_BASICO' AND id_asignatura = 4 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'CUARTO_BASICO' AND id_asignatura = 9 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 4 WHERE nivel = 'CUARTO_BASICO' AND id_asignatura = 13 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'CUARTO_BASICO' AND id_asignatura = 10 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'CUARTO_BASICO' AND id_asignatura = 11 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'CUARTO_BASICO' AND id_asignatura = 12 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'CUARTO_BASICO' AND id_asignatura = 15 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'CUARTO_BASICO' AND id_asignatura = 14 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'CUARTO_BASICO' AND id_asignatura = 16 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'QUINTO_BASICO' AND id_asignatura = 1 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'QUINTO_BASICO' AND id_asignatura = 3 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 4 WHERE nivel = 'QUINTO_BASICO' AND id_asignatura = 4 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 4 WHERE nivel = 'QUINTO_BASICO' AND id_asignatura = 9 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 4 WHERE nivel = 'QUINTO_BASICO' AND id_asignatura = 13 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 4 WHERE nivel = 'QUINTO_BASICO' AND id_asignatura = 10 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'QUINTO_BASICO' AND id_asignatura = 11 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'QUINTO_BASICO' AND id_asignatura = 12 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'QUINTO_BASICO' AND id_asignatura = 15 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'QUINTO_BASICO' AND id_asignatura = 14 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'QUINTO_BASICO' AND id_asignatura = 18 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'QUINTO_BASICO' AND id_asignatura = 16 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'SEXTO_BASICO' AND id_asignatura = 1 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'SEXTO_BASICO' AND id_asignatura = 3 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 4 WHERE nivel = 'SEXTO_BASICO' AND id_asignatura = 4 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 4 WHERE nivel = 'SEXTO_BASICO' AND id_asignatura = 9 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 4 WHERE nivel = 'SEXTO_BASICO' AND id_asignatura = 13 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 4 WHERE nivel = 'SEXTO_BASICO' AND id_asignatura = 10 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'SEXTO_BASICO' AND id_asignatura = 11 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'SEXTO_BASICO' AND id_asignatura = 12 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'SEXTO_BASICO' AND id_asignatura = 15 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'SEXTO_BASICO' AND id_asignatura = 14 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'SEXTO_BASICO' AND id_asignatura = 18 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'SEXTO_BASICO' AND id_asignatura = 16 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'SEPTIMO_BASICO' AND id_asignatura = 2 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'SEPTIMO_BASICO' AND id_asignatura = 3 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'SEPTIMO_BASICO' AND id_asignatura = 4 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'SEPTIMO_BASICO' AND id_asignatura = 9 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 4 WHERE nivel = 'SEPTIMO_BASICO' AND id_asignatura = 13 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 4 WHERE nivel = 'SEPTIMO_BASICO' AND id_asignatura = 10 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'SEPTIMO_BASICO' AND id_asignatura = 11 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'SEPTIMO_BASICO' AND id_asignatura = 12 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'SEPTIMO_BASICO' AND id_asignatura = 15 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'SEPTIMO_BASICO' AND id_asignatura = 14 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'SEPTIMO_BASICO' AND id_asignatura = 17 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'OCTAVO_BASICO' AND id_asignatura = 2 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'OCTAVO_BASICO' AND id_asignatura = 3 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'OCTAVO_BASICO' AND id_asignatura = 4 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'OCTAVO_BASICO' AND id_asignatura = 9 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 4 WHERE nivel = 'OCTAVO_BASICO' AND id_asignatura = 13 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 4 WHERE nivel = 'OCTAVO_BASICO' AND id_asignatura = 10 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'OCTAVO_BASICO' AND id_asignatura = 11 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'OCTAVO_BASICO' AND id_asignatura = 12 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'OCTAVO_BASICO' AND id_asignatura = 15 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'OCTAVO_BASICO' AND id_asignatura = 14 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'OCTAVO_BASICO' AND id_asignatura = 17 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'PRIMERO_MEDIO' AND id_asignatura = 2 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'PRIMERO_MEDIO' AND id_asignatura = 3 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'PRIMERO_MEDIO' AND id_asignatura = 5 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'PRIMERO_MEDIO' AND id_asignatura = 7 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'PRIMERO_MEDIO' AND id_asignatura = 6 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'PRIMERO_MEDIO' AND id_asignatura = 9 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 4 WHERE nivel = 'PRIMERO_MEDIO' AND id_asignatura = 13 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 4 WHERE nivel = 'PRIMERO_MEDIO' AND id_asignatura = 10 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'PRIMERO_MEDIO' AND id_asignatura = 11 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'PRIMERO_MEDIO' AND id_asignatura = 12 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'PRIMERO_MEDIO' AND id_asignatura = 15 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'PRIMERO_MEDIO' AND id_asignatura = 14 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'PRIMERO_MEDIO' AND id_asignatura = 20 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'SEGUNDO_MEDIO' AND id_asignatura = 2 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'SEGUNDO_MEDIO' AND id_asignatura = 3 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'SEGUNDO_MEDIO' AND id_asignatura = 5 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'SEGUNDO_MEDIO' AND id_asignatura = 7 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'SEGUNDO_MEDIO' AND id_asignatura = 6 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 6 WHERE nivel = 'SEGUNDO_MEDIO' AND id_asignatura = 9 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 4 WHERE nivel = 'SEGUNDO_MEDIO' AND id_asignatura = 13 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 4 WHERE nivel = 'SEGUNDO_MEDIO' AND id_asignatura = 10 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'SEGUNDO_MEDIO' AND id_asignatura = 11 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'SEGUNDO_MEDIO' AND id_asignatura = 12 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'SEGUNDO_MEDIO' AND id_asignatura = 15 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'SEGUNDO_MEDIO' AND id_asignatura = 14 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = 2 WHERE nivel = 'SEGUNDO_MEDIO' AND id_asignatura = 20 AND plan = 'COMUN';
UPDATE malla_curricular SET caracter = 'ELECTIVA' WHERE nivel = 'SEPTIMO_BASICO' AND id_asignatura IN (11, 12) AND plan = 'COMUN';
UPDATE malla_curricular SET caracter = 'ELECTIVA' WHERE nivel = 'OCTAVO_BASICO' AND id_asignatura IN (11, 12) AND plan = 'COMUN';
UPDATE malla_curricular SET caracter = 'ELECTIVA' WHERE nivel = 'PRIMERO_MEDIO' AND id_asignatura IN (11, 12) AND plan = 'COMUN';
UPDATE malla_curricular SET caracter = 'ELECTIVA' WHERE nivel = 'SEGUNDO_MEDIO' AND id_asignatura IN (11, 12) AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = NULL, active = FALSE WHERE nivel = 'QUINTO_BASICO' AND id_asignatura = 17 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = NULL, active = FALSE WHERE nivel = 'SEXTO_BASICO' AND id_asignatura = 17 AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = NULL, active = FALSE WHERE nivel = 'SEPTIMO_BASICO' AND id_asignatura IN (16, 18, 19) AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = NULL, active = FALSE WHERE nivel = 'OCTAVO_BASICO' AND id_asignatura IN (16, 18, 19) AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = NULL, active = FALSE WHERE nivel = 'PRIMERO_MEDIO' AND id_asignatura IN (16, 21, 22) AND plan = 'COMUN';
UPDATE malla_curricular SET horas_semanales = NULL, active = FALSE WHERE nivel = 'SEGUNDO_MEDIO' AND id_asignatura IN (16, 21, 22) AND plan = 'COMUN';
INSERT INTO malla_curricular (nivel, id_asignatura, caracter, horas_semanales, plan, active) VALUES ('QUINTO_BASICO', 18, 'OPTATIVA', 2, 'COMUN', TRUE);
INSERT INTO malla_curricular (nivel, id_asignatura, caracter, horas_semanales, plan, active) VALUES ('SEXTO_BASICO', 18, 'OPTATIVA', 2, 'COMUN', TRUE);
