-- Esquema de ms-asignaturas: asignaturas (herencia SINGLE_TABLE) + horarios + inscripciones.
-- Este esquema reemplaza al anterior (solo name/description/active): al actualizar hay
-- que recrear la base de ms-asignaturas para que Flyway aplique esta version limpia.
CREATE TABLE IF NOT EXISTS asignaturas (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    tipo_asignatura VARCHAR(31)  NOT NULL,
    name            VARCHAR(50)  NOT NULL,
    description     VARCHAR(150) NOT NULL,
    semestre        VARCHAR(255) NOT NULL,
    area            VARCHAR(255) NOT NULL,
    id_docente      BIGINT       NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    id_clase        BIGINT       NULL,
    cupo_maximo     INT          NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_asignatura_name (name)
);

CREATE TABLE IF NOT EXISTS horarios (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    dia             VARCHAR(255) NOT NULL,
    horario_entrada TIME(6)      NOT NULL,
    horario_salida  TIME(6)      NOT NULL,
    ubicacion       VARCHAR(50)  NOT NULL,
    asignatura_id   BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_horario_asignatura FOREIGN KEY (asignatura_id) REFERENCES asignaturas (id)
);

CREATE TABLE IF NOT EXISTS inscripciones (
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    id_alumno         BIGINT      NOT NULL,
    asignatura_id     BIGINT      NOT NULL,
    estado            VARCHAR(20) NOT NULL,
    fecha_inscripcion DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_inscripcion_alumno_asignatura UNIQUE (id_alumno, asignatura_id),
    CONSTRAINT fk_inscripcion_asignatura FOREIGN KEY (asignatura_id) REFERENCES asignaturas (id)
);
