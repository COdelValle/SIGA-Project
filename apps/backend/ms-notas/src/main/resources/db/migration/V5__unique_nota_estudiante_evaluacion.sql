-- Unicidad (estudiante, evaluacion) para blindar el auto-guardado del portal
-- docente ante reintentos/carreras. Antes de crear el indice se sanean los
-- duplicados historicos: se conserva la fila activa de menor id (o la de menor
-- id si no hay activa) y se eliminan las demas.
DELETE FROM notas
WHERE id IN (
    SELECT id FROM (
        SELECT id,
               ROW_NUMBER() OVER (
                   PARTITION BY id_estudiante, id_evaluacion
                   ORDER BY active DESC, id ASC
               ) AS rn
        FROM notas
    ) duplicados
    WHERE rn > 1
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_nota_estudiante_evaluacion
    ON notas (id_estudiante, id_evaluacion);
