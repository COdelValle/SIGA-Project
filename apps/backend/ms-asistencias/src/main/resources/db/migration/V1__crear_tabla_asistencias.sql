-- Esquema de ms-asistencias: registro diario de asistencia por estudiante y
-- asignatura. Idempotente para convivir con bases creadas por Hibernate.
CREATE TABLE IF NOT EXISTS asistencias (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    id_estudiante BIGINT       NOT NULL,
    id_curso_asignatura BIGINT       NOT NULL,
    fecha         DATE         NOT NULL,
    estado        VARCHAR(20)  NOT NULL,
    justificacion VARCHAR(20)  NOT NULL,
    observacion   VARCHAR(255) NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_asistencia_estudiante_curso_fecha UNIQUE (id_estudiante, id_curso_asignatura, fecha)
);

CREATE INDEX IF NOT EXISTS idx_asistencia_id_estudiante ON asistencias (id_estudiante);
CREATE INDEX IF NOT EXISTS idx_asistencia_id_curso_asignatura ON asistencias (id_curso_asignatura);
CREATE INDEX IF NOT EXISTS idx_asistencia_fecha ON asistencias (fecha);
