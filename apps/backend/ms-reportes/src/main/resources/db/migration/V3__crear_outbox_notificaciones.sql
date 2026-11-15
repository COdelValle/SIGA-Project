CREATE TABLE IF NOT EXISTS notification_event_outbox (
    id BIGINT NOT NULL AUTO_INCREMENT,
    id_evento VARCHAR(100) NOT NULL,
    intercambio VARCHAR(160) NOT NULL,
    clave VARCHAR(160) NOT NULL,
    tipo_payload VARCHAR(120) NOT NULL,
    payload LONGTEXT NOT NULL,
    estado VARCHAR(20) NOT NULL,
    intentos INT NOT NULL DEFAULT 0,
    proximo_intento DATETIME(6) NOT NULL,
    creado_en DATETIME(6) NOT NULL,
    enviado_en DATETIME(6) NULL,
    ultimo_error VARCHAR(1000) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_notification_event_outbox_evento UNIQUE (id_evento)
);

CREATE INDEX IF NOT EXISTS idx_notification_outbox_pendientes
    ON notification_event_outbox (estado, proximo_intento);
