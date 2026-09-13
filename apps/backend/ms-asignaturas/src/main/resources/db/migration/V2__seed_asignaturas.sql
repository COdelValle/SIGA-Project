-- Datos base de asignaturas. INSERT IGNORE hace la migracion idempotente:
-- si la asignatura ya existe (name es UNIQUE) no se duplica ni se sobreescribe.
INSERT IGNORE INTO asignaturas (name, description, active) VALUES
    ('MATEMATICA', 'matematica', TRUE),
    ('LENGUAJE',   'lenguaje y comunicacion', TRUE),
    ('CIENCIAS',   'ciencias naturales', TRUE),
    ('HISTORIA',   'historia y ciencias sociales', TRUE),
    ('INGLES',     'idioma ingles', TRUE),
    ('EDUCACION FISICA', 'educacion fisica', TRUE);
