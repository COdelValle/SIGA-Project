-- Esquema de ms-notas. Idempotente para convivir con bases creadas previamente
-- por Hibernate (ver baseline-on-migrate).
CREATE TABLE IF NOT EXISTS notas (
    id            BIGINT  NOT NULL AUTO_INCREMENT,
    id_estudiante BIGINT  NOT NULL,
    id_asignatura BIGINT  NOT NULL,
    score         DOUBLE  NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id)
);
