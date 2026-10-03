-- Esquema de ms-asistencias: registro diario de asistencia por estudiante y
-- asignatura. Idempotente para convivir con bases creadas por Hibernate.
CREATE TABLE IF NOT EXISTS asistencias (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    id_estudiante BIGINT       NOT NULL,
    id_asignatura BIGINT       NOT NULL,
    fecha         DATE         NOT NULL,
    estado        VARCHAR(20)  NOT NULL,
    justificacion VARCHAR(20)  NOT NULL,
    observacion   VARCHAR(255) NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_id_estudiante_id_asignatura_fecha UNIQUE (id_estudiante, id_asignatura, fecha)
);

CREATE INDEX IF NOT EXISTS idx_asistencia_id_estudiante ON asistencias (id_estudiante);
CREATE INDEX IF NOT EXISTS idx_asistencia_id_asignatura ON asistencias (id_asignatura);
CREATE INDEX IF NOT EXISTS idx_asistencia_fecha ON asistencias (fecha);
