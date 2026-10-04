-- Esquema de ms-notas: notas por estudiante y evaluación.
CREATE TABLE IF NOT EXISTS notas (
    id            BIGINT  NOT NULL AUTO_INCREMENT,
    id_estudiante BIGINT  NOT NULL,
    id_evaluacion BIGINT  NOT NULL,
    score         DOUBLE  NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_nota_estudiante_evaluacion UNIQUE (id_estudiante, id_evaluacion)
);
CREATE INDEX IF NOT EXISTS idx_nota_id_estudiante ON notas (id_estudiante);
CREATE INDEX IF NOT EXISTS idx_nota_id_evaluacion ON notas (id_evaluacion);
