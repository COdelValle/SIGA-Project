CREATE TABLE IF NOT EXISTS eventos_reportes_procesados (
    id BIGINT NOT NULL AUTO_INCREMENT,
    id_evento VARCHAR(36) NOT NULL,
    topic VARCHAR(160) NOT NULL,
    particion INT NULL,
    offset_kafka BIGINT NULL,
    correlation_id VARCHAR(100) NOT NULL,
    timestamp_evento DATETIME(6) NOT NULL,
    procesado_en DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_evento_reportes_id_evento UNIQUE (id_evento)
);

CREATE INDEX IF NOT EXISTS idx_evento_reportes_topic_offset
    ON eventos_reportes_procesados (topic, particion, offset_kafka);

CREATE TABLE IF NOT EXISTS proyecciones_asistencia (
    id BIGINT NOT NULL AUTO_INCREMENT,
    id_estudiante BIGINT NOT NULL,
    id_curso_asignatura BIGINT NOT NULL,
    id_asistencia BIGINT NOT NULL,
    fecha DATE NOT NULL,
    estado VARCHAR(20) NOT NULL,
    porcentaje_inasistencia DOUBLE NOT NULL,
    total_registros BIGINT NOT NULL,
    total_inasistencias BIGINT NOT NULL,
    nivel_riesgo VARCHAR(20) NOT NULL,
    id_evento VARCHAR(36) NOT NULL,
    correlation_id VARCHAR(100) NOT NULL,
    actualizado_en DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_proyeccion_asistencia_estudiante_curso
        UNIQUE (id_estudiante, id_curso_asignatura)
);

CREATE TABLE IF NOT EXISTS proyecciones_rendimiento (
    id BIGINT NOT NULL AUTO_INCREMENT,
    id_estudiante BIGINT NOT NULL,
    id_evaluacion BIGINT NOT NULL,
    id_curso_asignatura BIGINT NULL,
    id_nota BIGINT NOT NULL,
    puntaje DOUBLE NULL,
    promedio DOUBLE NULL,
    evaluaciones_insuficientes BIGINT NOT NULL,
    nivel_riesgo VARCHAR(20) NOT NULL,
    id_evento VARCHAR(36) NOT NULL,
    correlation_id VARCHAR(100) NOT NULL,
    actualizado_en DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_proyeccion_rendimiento_estudiante_evaluacion
        UNIQUE (id_estudiante, id_evaluacion)
);
