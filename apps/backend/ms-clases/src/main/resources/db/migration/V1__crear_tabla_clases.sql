-- Esquema de ms-clases. Idempotente para convivir con bases creadas
-- previamente por Hibernate (ver baseline-on-migrate).
CREATE TABLE IF NOT EXISTS clases (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    nivel           VARCHAR(255) NOT NULL,
    letra           VARCHAR(1)   NOT NULL,
    anio_academico  INT          NOT NULL,
    id_docente_jefe BIGINT       NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_nivel_letra_anio UNIQUE (nivel, letra, anio_academico)
);
