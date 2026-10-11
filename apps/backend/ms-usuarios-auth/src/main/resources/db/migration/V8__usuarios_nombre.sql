-- Nombre completo del usuario (derivado del rol en el registro asincrono) para
-- mostrarlo en la gestion de usuarios del admin. Nulo en cuentas legacy.
ALTER TABLE usuarios
    ADD COLUMN full_name VARCHAR(255) NULL AFTER email;
