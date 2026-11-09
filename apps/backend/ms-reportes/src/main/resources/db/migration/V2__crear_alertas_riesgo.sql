CREATE TABLE IF NOT EXISTS alertas_riesgo (
    id BIGINT NOT NULL AUTO_INCREMENT,
    clave_idempotencia VARCHAR(180) NOT NULL,
    tipo_riesgo VARCHAR(32) NOT NULL,
    id_estudiante BIGINT NOT NULL,
    id_curso_asignatura BIGINT NULL,
    nivel_anterior VARCHAR(20) NULL,
    nivel_nuevo VARCHAR(20) NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    id_evento VARCHAR(36) NOT NULL,
    correlation_id VARCHAR(100) NOT NULL,
    creada_en DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_alerta_riesgo_idempotencia UNIQUE (clave_idempotencia)
);
