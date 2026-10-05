CREATE TABLE IF NOT EXISTS user_registration_processes (
    process_id      VARCHAR(36)   NOT NULL,
    event_id        VARCHAR(36)   NOT NULL,
    correlation_id  VARCHAR(100)  NOT NULL,
    email           VARCHAR(255)  NOT NULL,
    full_name       VARCHAR(255)  NOT NULL,
    requested_role  VARCHAR(50)   NOT NULL,
    user_id         VARCHAR(36)   NULL,
    state           VARCHAR(30)   NOT NULL,
    azure_state     VARCHAR(30)   NOT NULL,
    domain_state    VARCHAR(30)   NOT NULL,
    error_message   VARCHAR(1000) NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (process_id),
    UNIQUE KEY uk_user_registration_process_event (event_id),
    KEY idx_user_registration_process_email (email)
);

CREATE TABLE IF NOT EXISTS user_registration_attempts (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    process_id  VARCHAR(36)   NOT NULL,
    step_type   VARCHAR(30)   NOT NULL,
    success     BOOLEAN       NOT NULL,
    message     VARCHAR(1000) NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_registration_attempt_process (process_id),
    CONSTRAINT fk_user_registration_attempt_process
        FOREIGN KEY (process_id) REFERENCES user_registration_processes(process_id)
);

CREATE TABLE IF NOT EXISTS processed_registration_events (
    event_id    VARCHAR(120) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (event_id)
);
