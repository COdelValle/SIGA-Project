-- Cuentas base de SIGA en el tenant platformsiga (f260a804-82ac-4b57-bb01-9ed6626d71ff).
-- El "id" es el object id (oid) del usuario en Microsoft Entra ID.
-- INSERT IGNORE la hace idempotente: si la cuenta ya existe no se duplica ni se
-- sobreescribe. Sobrevive a recrear la infraestructura (base limpia -> seed).
INSERT IGNORE INTO usuarios (id, email, rol, state) VALUES
    ('3fb9467c-761f-4bff-bf74-621562e07810', 'admin@platformsiga.onmicrosoft.com', 'ADMIN', 'ACTIVO'),
    ('ed6ba585-5a6f-4d5c-8564-d7b522b0b47f', 'alejandrosilva.docente@platformsiga.onmicrosoft.com', 'DOCENTE', 'ACTIVO'),
    ('2f63f650-276d-4e7c-98ac-f9045d1f9779', 'camila.soto@platformsiga.onmicrosoft.com', 'ESTUDIANTE', 'ACTIVO'),
    ('90dca6f5-0a90-4207-88fa-0e12b38ed24f', 'claudia.hernandez@platformsiga.onmicrosoft.com', 'APODERADO', 'ACTIVO');
