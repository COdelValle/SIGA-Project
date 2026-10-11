-- Primer estudiante de SIGA, enlazado al usuario de Entra ID por su oid.
-- INSERT IGNORE + rut UNIQUE => idempotente (no duplica ni sobreescribe).
INSERT IGNORE INTO estudiantes
    (id_usuario, rut, first_name, middle_name, first_surname, second_surname, birth_date, state)
VALUES
    ('2f63f650-276d-4e7c-98ac-f9045d1f9779',
     '24112345-6', 'CAMILA', 'ANTONIETA', 'SOTO', 'HERNÁNDEZ', '2012-05-15', 'REGISTRADO');
