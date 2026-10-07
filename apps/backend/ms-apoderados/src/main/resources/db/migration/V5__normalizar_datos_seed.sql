-- Normaliza los datos de ejemplo al estandar del proyecto: nombres en Titulo con
-- particulas y RUT sin puntos. Idempotente y acotada a las filas de seed.
UPDATE apoderados SET first_name='Claudia', middle_name='Andrea', first_surname='Hernandez', second_surname='Morales' WHERE id=1;
UPDATE apoderados SET first_name='Roberto', middle_name='Andres', first_surname='Soto', second_surname='Perez' WHERE id=2;

UPDATE apoderados SET rut = REPLACE(rut, '.', '') WHERE rut LIKE '%.%';
