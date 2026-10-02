-- Datos base de docentes y sus certificados (solo para desarrollo local).
INSERT IGNORE INTO docentes
    (id, id_usuario, rut, first_name, middle_name, first_surname, second_surname, fecha_contratacion, activo, area)
VALUES
    (1, 'ed6ba585-5a6f-4d5c-8564-d7b522b0b47f', '11111111-1', 'ALEJANDRO', 'JAVIER', 'SILVA',   'MORALES', '2019-03-01', TRUE, 'MATEMATICAS'),
    (2, '22222222-2222-2222-2222-222222222222', '22222222-2', 'NATALIA',   'PAZ',    'FUENTES', 'CARDENAS','2020-03-01', TRUE, 'LENGUAJE'),
    (3, '33333333-3333-3333-3333-333333333333', '33333333-3', 'CARLOS',    'ALBERTO','MENDOZA', 'FUENTES', '2021-03-01', TRUE, 'CIENCIAS');

INSERT IGNORE INTO certificados (id, nombre, institucion_realizacion, fecha_titulacion, id_docente) VALUES
    (1, 'PEDAGOGIA EN MATEMATICAS', 'UNIVERSIDAD DE CHILE',        '2018-12-15', 1),
    (2, 'PEDAGOGIA EN LENGUAJE',    'PONTIFICIA UNIVERSIDAD CATOLICA','2019-12-20', 2),
    (3, 'PEDAGOGIA EN CIENCIAS',    'UNIVERSIDAD DE CONCEPCION',   '2020-12-10', 3);
