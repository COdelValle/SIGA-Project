CREATE TABLE IF NOT EXISTS notificaciones (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    id_evento       VARCHAR(36)  NOT NULL,
    tipo            VARCHAR(20)  NOT NULL,
    accion          VARCHAR(24)  NOT NULL,
    tipo_destino    VARCHAR(32)  NOT NULL,
    id_destino      BIGINT       NOT NULL,
    titulo          VARCHAR(160) NOT NULL,
    resumen         VARCHAR(500) NOT NULL,
    fecha_hora      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_notificaciones_id_evento UNIQUE (id_evento),
    INDEX idx_notificacion_destino_fecha (tipo_destino, id_destino, fecha_hora, id)
);

CREATE TABLE IF NOT EXISTS notificacion_lecturas (
    id_notificacion BIGINT      NOT NULL,
    id_usuario      VARCHAR(36) NOT NULL,
    leida_en        DATETIME(6) NOT NULL,
    PRIMARY KEY (id_notificacion, id_usuario),
    INDEX idx_notificacion_lectura_usuario (id_usuario, leida_en),
    CONSTRAINT fk_notificacion_lectura_notificacion
        FOREIGN KEY (id_notificacion) REFERENCES notificaciones (id)
        ON DELETE CASCADE
);
