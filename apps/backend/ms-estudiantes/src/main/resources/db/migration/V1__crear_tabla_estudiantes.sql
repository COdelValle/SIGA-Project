-- Esquema de ms-estudiantes. Idempotente para poder convivir con bases
-- creadas previamente por Hibernate (ver baseline-on-migrate).
CREATE TABLE IF NOT EXISTS estudiantes (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    id_usuario     VARCHAR(36)  NOT NULL,
    rut            VARCHAR(255) NOT NULL,
    first_name     VARCHAR(255) NOT NULL,
    middle_name    VARCHAR(255) NULL,
    first_surname  VARCHAR(255) NOT NULL,
    second_surname VARCHAR(255) NULL,
    birth_date     DATE         NOT NULL,
    state          VARCHAR(50)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY idx_estudiante_rut (rut)
);

CREATE TABLE IF NOT EXISTS estudiante_allergies (
    estudiante_id BIGINT       NOT NULL,
    allergies     VARCHAR(255) NULL,
    CONSTRAINT fk_estudiante_allergies
        FOREIGN KEY (estudiante_id) REFERENCES estudiantes (id) ON DELETE CASCADE
);
