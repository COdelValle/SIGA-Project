-- Metadatos del proceso de registro asincrono: administrador iniciador,
-- correo de contacto para la futura notificacion y credencial temporal
-- cifrada (AES-GCM) que se entrega una unica vez.
ALTER TABLE user_registration_processes
    ADD COLUMN created_by VARCHAR(36) NULL AFTER requested_role,
    ADD COLUMN contact_email VARCHAR(255) NULL AFTER created_by,
    ADD COLUMN role_data LONGTEXT NULL AFTER user_id,
    ADD COLUMN credential_ciphertext VARCHAR(512) NULL,
    ADD COLUMN credential_iv VARCHAR(64) NULL,
    ADD COLUMN credential_expires_at TIMESTAMP NULL,
    ADD COLUMN credential_retrieved_at TIMESTAMP NULL,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
