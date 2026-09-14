-- Cuentas base de SIGA (mismas que en el entorno local).
-- El "id" es el object id (oid) del usuario en Microsoft Entra ID.
-- INSERT IGNORE la hace idempotente: si la cuenta ya existe no se duplica ni se
-- sobreescribe. Sobrevive a recrear la infraestructura (base limpia -> seed).
INSERT IGNORE INTO usuarios (id, email, rol, state) VALUES
    ('0406ec8d-8fd5-4b55-a813-806bc0172be2', 'apoderado@genesisfloress.onmicrosoft.com', 'APODERADO', 'ACTIVO'),
    ('5782eb6b-a771-4e5a-b467-950eb3e46ba9', 'admin@genesisfloress.onmicrosoft.com', 'ADMIN', 'ACTIVO'),
    ('d7a8a3c4-3108-4233-a88e-3e4c08f62b86', 'docente@genesisfloress.onmicrosoft.com', 'DOCENTE', 'ACTIVO'),
    ('fa17a8dc-ba05-4ec9-a6ff-015115810141', 'estudiante@genesisfloress.onmicrosoft.com', 'ESTUDIANTE', 'ACTIVO');
