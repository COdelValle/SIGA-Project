-- Solo las evaluaciones SUMATIVA ponderan. Las FORMATIVA y DIAGNOSTICO pueden
-- tener notas, pero no influyen en la nota final ni en el promedio, por lo que
-- su ponderación queda en 0. No se toca V2 (ya aplicada) para no invalidar los
-- checksums de Flyway.
UPDATE evaluaciones SET ponderacion = 0.0 WHERE tipo IN ('FORMATIVA', 'DIAGNOSTICO');
