-- Outbox transaccional del registro asincrono: el evento se persiste junto al
-- cambio de estado y un publicador programado lo envia a RabbitMQ con
-- confirmacion del broker y reintentos con backoff exponencial.
CREATE TABLE IF NOT EXISTS user_registration_outbox (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    event_id        VARCHAR(36)  NOT NULL,
    process_id      VARCHAR(36)  NULL,
    exchange        VARCHAR(120) NOT NULL,
    routing_key     VARCHAR(120) NOT NULL,
    correlation_id  VARCHAR(100) NULL,
    payload         LONGTEXT     NOT NULL,
    state           VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    attempts        INT          NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP    NULL DEFAULT CURRENT_TIMESTAMP,
    last_error      VARCHAR(1000) NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at         TIMESTAMP    NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_registration_outbox_event (event_id),
    KEY idx_registration_outbox_pendientes (state, next_attempt_at)
);
