-- Tabla de usuarios gestionados por SIGA.
-- El "id" corresponde al object id (oid) de Microsoft Entra ID (GUID de 36 caracteres).
CREATE TABLE IF NOT EXISTS usuarios (
    id    VARCHAR(36)  NOT NULL,
    email VARCHAR(255) NOT NULL,
    rol   VARCHAR(50)  NOT NULL,
    state VARCHAR(50)  NOT NULL,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_usuario_email ON usuarios (email);

-- IMPORTANTE (bootstrap): el primer administrador debe insertarse manualmente
-- porque sin un usuario ADMIN no es posible crear más usuarios vía API.
-- Reemplazar <OID> por el object id de la cuenta en Entra ID:
--
-- INSERT INTO usuarios (id, email, rol, state)
-- VALUES ('<OID>', 'admin@tu-dominio', 'ADMIN', 'ACTIVO');
--
-- Además, asignar manualmente el app role ADMIN a esa cuenta en Entra ID
-- (Enterprise applications > App roles) o llamar a POST /api/v1/usuarios/{oid}/sync-roles,
-- de modo que el token incluya el claim "roles": ["ADMIN"].
