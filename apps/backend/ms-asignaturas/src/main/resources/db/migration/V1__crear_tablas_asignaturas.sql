-- Esquema de ms-asignaturas: catálogo de asignaturas, malla curricular,
-- dictaciones por curso, horarios e inscripciones. Base limpia del modelo
-- "catálogo + oferta": la asignatura es general y la dictación la instancia
-- en un curso con docente, semestre, horarios y cupos.
CREATE TABLE IF NOT EXISTS asignaturas (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    nombre         VARCHAR(80)  NOT NULL,
    nombre_corto   VARCHAR(40)  NULL,
    descripcion    VARCHAR(150) NOT NULL,
    area           VARCHAR(255) NOT NULL,
    calificable    BOOLEAN      NOT NULL DEFAULT TRUE,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_asignatura_nombre (nombre)
);

CREATE TABLE IF NOT EXISTS malla_curricular (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    nivel           VARCHAR(255) NOT NULL,
    id_asignatura   BIGINT       NOT NULL,
    caracter        VARCHAR(20)  NOT NULL,
    horas_semanales INT          NULL,
    plan            VARCHAR(30)  NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_malla_nivel_asignatura_plan (nivel, id_asignatura, plan),
    CONSTRAINT fk_malla_asignatura FOREIGN KEY (id_asignatura) REFERENCES asignaturas (id)
);

CREATE TABLE IF NOT EXISTS cursos_asignaturas (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    id_asignatura BIGINT       NOT NULL,
    id_clase      BIGINT       NOT NULL,
    id_docente    BIGINT       NOT NULL,
    semestre      VARCHAR(255) NOT NULL,
    caracter      VARCHAR(20)  NOT NULL,
    cupo_maximo   INT          NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_curso_asignatura (id_asignatura, id_clase, semestre),
    CONSTRAINT fk_curso_asignatura FOREIGN KEY (id_asignatura) REFERENCES asignaturas (id)
);
CREATE INDEX IF NOT EXISTS idx_curso_asignatura_id_clase ON cursos_asignaturas (id_clase);
CREATE INDEX IF NOT EXISTS idx_curso_asignatura_id_docente ON cursos_asignaturas (id_docente);

CREATE TABLE IF NOT EXISTS horarios (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    dia                 VARCHAR(255) NOT NULL,
    horario_entrada     TIME(6)     NOT NULL,
    horario_salida      TIME(6)     NOT NULL,
    ubicacion           VARCHAR(50) NOT NULL,
    curso_asignatura_id BIGINT      NOT NULL,
    active              BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT fk_horario_curso_asignatura FOREIGN KEY (curso_asignatura_id) REFERENCES cursos_asignaturas (id)
);
CREATE INDEX IF NOT EXISTS idx_horario_ubicacion_dia ON horarios (ubicacion, dia);
CREATE INDEX IF NOT EXISTS idx_horario_active ON horarios (active);

CREATE TABLE IF NOT EXISTS inscripciones (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    id_alumno           BIGINT      NOT NULL,
    curso_asignatura_id BIGINT      NOT NULL,
    estado              VARCHAR(20) NOT NULL,
    fecha_inscripcion   DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_inscripcion_alumno_curso UNIQUE (id_alumno, curso_asignatura_id),
    CONSTRAINT fk_inscripcion_curso_asignatura FOREIGN KEY (curso_asignatura_id) REFERENCES cursos_asignaturas (id)
);
