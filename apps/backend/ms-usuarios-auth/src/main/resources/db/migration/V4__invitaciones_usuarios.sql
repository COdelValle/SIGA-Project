-- Invitaciones de usuario por correo (UPN de Entra ID).
-- El object id (oid) ya no se ingresa manualmente: la invitación guarda solo
-- correo + rol y, en el primer inicio de sesión del invitado, el servicio
-- vincula su oid (claim del JWT) creando la fila en usuarios.
CREATE TABLE IF NOT EXISTS invitaciones_usuarios (
    email      VARCHAR(255) NOT NULL,
    rol        VARCHAR(50)  NOT NULL,
    state      VARCHAR(50)  NOT NULL DEFAULT 'INVITADO',
    invited_by VARCHAR(36)  NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    bound_at   TIMESTAMP    NULL,
    PRIMARY KEY (email)
);

-- Bootstrap del primer administrador sin copiar oid: se invita su correo y al
-- primer login se crea la fila en usuarios con el oid del token. Reemplazar el
-- correo por el del administrador real del colegio. Idempotente y no pisa una
-- cuenta ya registrada.
INSERT INTO invitaciones_usuarios (email, rol, state)
SELECT 'admin@platformsiga.onmicrosoft.com', 'ADMIN', 'INVITADO'
WHERE NOT EXISTS (
        SELECT 1 FROM usuarios WHERE email = 'admin@platformsiga.onmicrosoft.com'
    )
    AND NOT EXISTS (
        SELECT 1 FROM invitaciones_usuarios WHERE email = 'admin@platformsiga.onmicrosoft.com'
    );
