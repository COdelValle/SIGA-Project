ALTER TABLE alertas_riesgo
    ADD COLUMN porcentaje_inasistencia DOUBLE NULL,
    ADD COLUMN promedio DOUBLE NULL,
    ADD COLUMN evaluaciones_insuficientes BIGINT NULL;
