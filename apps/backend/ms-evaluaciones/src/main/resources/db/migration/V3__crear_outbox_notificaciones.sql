CREATE TABLE IF NOT EXISTS notification_event_outbox (
    id_evento       VARCHAR(36)   NOT NULL,
    intercambio     VARCHAR(160)  NOT NULL,
    clave           VARCHAR(160)  NOT NULL,
    tipo_payload    VARCHAR(40)   NOT NULL,
    payload         LONGTEXT      NOT NULL,
    estado          VARCHAR(16)   NOT NULL,
    intentos        INT           NOT NULL DEFAULT 0,
    proximo_intento DATETIME(6)   NOT NULL,
    creado_en       DATETIME(6)   NOT NULL,
    enviado_en      DATETIME(6)   NULL,
    ultimo_error    VARCHAR(1000) NULL,
    PRIMARY KEY (id_evento),
    INDEX idx_notification_outbox_pendiente (estado, proximo_intento, creado_en)
);
