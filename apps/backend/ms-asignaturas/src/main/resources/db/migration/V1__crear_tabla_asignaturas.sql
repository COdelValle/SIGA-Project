-- Esquema de ms-asignaturas. Idempotente para convivir con bases creadas
-- previamente por Hibernate (ver baseline-on-migrate).
CREATE TABLE IF NOT EXISTS asignaturas (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(150) NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_asignatura_name (name)
);
