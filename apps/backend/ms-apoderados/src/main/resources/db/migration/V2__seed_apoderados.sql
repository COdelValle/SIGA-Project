-- Datos base de apoderados, telefonos y estudiantes asociados (solo para desarrollo local).
INSERT IGNORE INTO apoderados
    (id, id_usuario, rut, first_name, middle_name, first_surname, second_surname, activo)
VALUES
    (1, '90dca6f5-0a90-4207-88fa-0e12b38ed24f', '44444444-4', 'CLAUDIA', 'ANDREA', 'HERNANDEZ', 'MORALES', TRUE),
    (2, '55555555-5555-5555-5555-555555555555', '55555555-5', 'ROBERTO', 'ANDRES', 'SOTO',      'PEREZ',   TRUE);

INSERT IGNORE INTO apoderado_telefonos (apoderado_id, telefono) VALUES
    (1, '+56911111111'),
    (1, '+56922222222'),
    (2, '+56933333333');

INSERT IGNORE INTO apoderado_estudiantes (apoderado_id, estudiante_id, parentesco) VALUES
    (1, 1, 'MADRE_PADRE'),
    (1, 2, 'MADRE_PADRE'),
    (2, 1, 'TUTOR_LEGAL');
