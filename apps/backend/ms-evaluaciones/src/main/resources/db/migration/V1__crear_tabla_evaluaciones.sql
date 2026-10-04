-- Esquema de ms-evaluaciones: evaluaciones por dictación (curso + asignatura).
CREATE TABLE IF NOT EXISTS evaluaciones (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    nombre               VARCHAR(50)  NOT NULL,
    tipo                 VARCHAR(255) NOT NULL,
    ponderacion          DOUBLE       NOT NULL,
    id_curso_asignatura  BIGINT       NOT NULL,
    active               BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_evaluacion_id_curso_asignatura ON evaluaciones (id_curso_asignatura);
