-- Docentes de apoyo para el reparto por niveles: Lenguaje y Matematica de
-- 4° Básico B (ids 24-25) y Lengua de Señas de 5°-6° (id 26).
-- INSERT IGNORE => idempotente.

INSERT IGNORE INTO docentes (id, id_usuario, rut, first_name, middle_name, first_surname, second_surname, fecha_contratacion, activo, area) VALUES
    (24, '11111124-1111-4111-8111-111111111124', '23000021-2', 'JAVIERA', 'ANDREA', 'ROJAS', 'PEÑA', '2020-03-02', TRUE, 'LENGUAJE'),
    (25, '11111125-1111-4111-8111-111111111125', '23000022-0', 'CRISTÓBAL', 'IGNACIO', 'MUÑOZ', 'VERA', '2020-03-02', TRUE, 'MATEMATICAS'),
    (26, '11111126-1111-4111-8111-111111111126', '23000023-9', 'FERNANDA', 'PAZ', 'SOTO', 'LAGOS', '2021-03-01', TRUE, 'LENGUAJE');

INSERT IGNORE INTO certificados (id, nombre, institucion_realizacion, fecha_titulacion, id_docente) VALUES
    (24, 'PEDAGOGIA EN LENGUAJE Y COMUNICACION', 'UNIVERSIDAD DE CHILE', '2019-12-15', 24),
    (25, 'PEDAGOGIA EN MATEMATICAS', 'UNIVERSIDAD DE CONCEPCION', '2019-12-15', 25),
    (26, 'PEDAGOGIA EN EDUCACION GENERAL BASICA', 'UNIVERSIDAD AUSTRAL DE CHILE', '2020-12-15', 26);
