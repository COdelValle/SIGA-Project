-- Normaliza los datos de ejemplo al estandar del proyecto: nombres en Titulo con
-- particulas y RUT sin puntos. Idempotente y acotada a las filas de seed.
UPDATE docentes SET first_name='Alejandro', middle_name='Javier', first_surname='Silva', second_surname='Morales' WHERE id=1;
UPDATE docentes SET first_name='Natalia', middle_name='Paz', first_surname='Fuentes', second_surname='Cardenas' WHERE id=2;
UPDATE docentes SET first_name='Carlos', middle_name='Alberto', first_surname='Mendoza', second_surname='Fuentes' WHERE id=3;
UPDATE docentes SET first_name='Romina', middle_name='Belén', first_surname='Cárdenas', second_surname='Pizarro' WHERE id=4;
UPDATE docentes SET first_name='Augusto', middle_name='Andrés', first_surname='Figueroa', second_surname='Ríos' WHERE id=5;
UPDATE docentes SET first_name='Camila', middle_name='Antonia', first_surname='Castro', second_surname='Medina' WHERE id=6;
UPDATE docentes SET first_name='Valeria', middle_name='Paz', first_surname='Contreras', second_surname='Navarro' WHERE id=7;
UPDATE docentes SET first_name='Paula', middle_name='Andrea', first_surname='Salazar', second_surname='Muñoz' WHERE id=8;
UPDATE docentes SET first_name='Francisco', middle_name='José', first_surname='Toledo', second_surname='Olivares' WHERE id=9;
UPDATE docentes SET first_name='Gabriel', middle_name='Antonio', first_surname='Miranda', second_surname='Lagos' WHERE id=10;
UPDATE docentes SET first_name='Valentina', middle_name='Isabel', first_surname='Alarcón', second_surname='Bustos' WHERE id=11;
UPDATE docentes SET first_name='Claudia', middle_name='Marcela', first_surname='Espinoza', second_surname='Cortez' WHERE id=12;
UPDATE docentes SET first_name='Daniela', middle_name='Ignacia', first_surname='Paredes', second_surname='Rojas' WHERE id=13;
UPDATE docentes SET first_name='Carolina', middle_name='Paz', first_surname='Vega', second_surname='Fuentes' WHERE id=14;
UPDATE docentes SET first_name='Marcela', middle_name='Soledad', first_surname='Guzmán', second_surname='Rivas' WHERE id=15;
UPDATE docentes SET first_name='Rodrigo', middle_name='Andrés', first_surname='Salinas', second_surname='Torres' WHERE id=16;
UPDATE docentes SET first_name='Javiera', middle_name='Paz', first_surname='Molina', second_surname='Sepúlveda' WHERE id=17;
UPDATE docentes SET first_name='Héctor', middle_name='Manuel', first_surname='Bravo', second_surname='Cárdenas' WHERE id=18;
UPDATE docentes SET first_name='Ignacio', middle_name='Tomás', first_surname='Herrera', second_surname='Soto' WHERE id=19;
UPDATE docentes SET first_name='Sebastián', middle_name='Andrés', first_surname='Cáceres', second_surname='Núñez' WHERE id=20;
UPDATE docentes SET first_name='Constanza', middle_name='Belén', first_surname='Reyes', second_surname='Fuentes' WHERE id=21;
UPDATE docentes SET first_name='Patricia', middle_name='Elena', first_surname='Orellana', second_surname='Díaz' WHERE id=22;
UPDATE docentes SET first_name='Mónica', middle_name='Alejandra', first_surname='Leiva', second_surname='Campos' WHERE id=23;
UPDATE docentes SET first_name='Javiera', middle_name='Andrea', first_surname='Rojas', second_surname='Peña' WHERE id=24;
UPDATE docentes SET first_name='Cristóbal', middle_name='Ignacio', first_surname='Muñoz', second_surname='Vera' WHERE id=25;
UPDATE docentes SET first_name='Fernanda', middle_name='Paz', first_surname='Soto', second_surname='Lagos' WHERE id=26;

UPDATE docentes SET rut = REPLACE(rut, '.', '') WHERE rut LIKE '%.%';
