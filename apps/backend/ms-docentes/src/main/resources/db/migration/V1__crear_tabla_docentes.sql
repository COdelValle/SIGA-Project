-- Esquema de ms-docentes. Idempotente para convivir con bases creadas
-- previamente por Hibernate (ver baseline-on-migrate).
CREATE TABLE IF NOT EXISTS docentes (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    id_usuario         VARCHAR(36)  NOT NULL,
    rut                VARCHAR(255) NOT NULL,
    first_name         VARCHAR(255) NOT NULL,
    middle_name        VARCHAR(255) NULL,
    first_surname      VARCHAR(255) NOT NULL,
    second_surname     VARCHAR(255) NULL,
    fecha_contratacion DATE         NOT NULL,
    activo             BOOLEAN      NOT NULL DEFAULT TRUE,
    area               VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY idx_docente_rut (rut)
);

CREATE TABLE IF NOT EXISTS certificados (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    nombre                  VARCHAR(100) NOT NULL,
    institucion_realizacion VARCHAR(150) NOT NULL,
    fecha_titulacion        DATE         NOT NULL,
    id_docente              BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_certificado_docente FOREIGN KEY (id_docente) REFERENCES docentes (id)
);
