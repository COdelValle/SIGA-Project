-- Esquema de ms-apoderados. Idempotente para convivir con bases creadas
-- previamente por Hibernate (ver baseline-on-migrate).
CREATE TABLE IF NOT EXISTS apoderados (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    id_usuario     VARCHAR(36)  NOT NULL,
    rut            VARCHAR(255) NOT NULL,
    first_name     VARCHAR(255) NOT NULL,
    middle_name    VARCHAR(255) NULL,
    first_surname  VARCHAR(255) NOT NULL,
    second_surname VARCHAR(255) NULL,
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    UNIQUE KEY idx_apoderado_rut (rut)
);

CREATE TABLE IF NOT EXISTS apoderado_telefonos (
    apoderado_id BIGINT       NOT NULL,
    telefono     VARCHAR(255) NOT NULL,
    CONSTRAINT fk_telefono_apoderado FOREIGN KEY (apoderado_id) REFERENCES apoderados (id)
);

CREATE TABLE IF NOT EXISTS apoderado_estudiantes (
    apoderado_id  BIGINT       NOT NULL,
    estudiante_id BIGINT       NOT NULL,
    parentesco    VARCHAR(255) NOT NULL,
    CONSTRAINT fk_apoderado_estudiante FOREIGN KEY (apoderado_id) REFERENCES apoderados (id)
);
