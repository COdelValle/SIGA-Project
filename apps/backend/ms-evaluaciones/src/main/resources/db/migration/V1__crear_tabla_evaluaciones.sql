-- Esquema de ms-evaluaciones. Idempotente para convivir con bases creadas
-- previamente por Hibernate (ver baseline-on-migrate).
CREATE TABLE IF NOT EXISTS evaluaciones (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    nombre        VARCHAR(50)  NOT NULL,
    tipo          VARCHAR(255) NOT NULL,
    ponderacion   DOUBLE       NOT NULL,
    id_asignatura BIGINT       NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id)
);
