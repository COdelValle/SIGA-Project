#!/bin/bash
# Se ejecuta una sola vez, en el primer arranque de MariaDB con el directorio de
# datos vacio. Solo crea bases y permisos; el esquema lo administra Flyway dentro
# de cada microservicio. Las variables DB_USER / DB_PASS / MARIADB_ROOT_PASSWORD
# vienen del .env del compose.
set -e

mysql -uroot -p"$MARIADB_ROOT_PASSWORD" <<-EOSQL
  CREATE DATABASE IF NOT EXISTS siga_usuarios_db    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  CREATE DATABASE IF NOT EXISTS siga_estudiantes_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  CREATE DATABASE IF NOT EXISTS siga_asignaturas_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  CREATE DATABASE IF NOT EXISTS siga_notas_db       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

  CREATE USER IF NOT EXISTS '$DB_USER'@'%' IDENTIFIED BY '$DB_PASS';

  GRANT ALL PRIVILEGES ON siga_usuarios_db.*    TO '$DB_USER'@'%';
  GRANT ALL PRIVILEGES ON siga_estudiantes_db.* TO '$DB_USER'@'%';
  GRANT ALL PRIVILEGES ON siga_asignaturas_db.* TO '$DB_USER'@'%';
  GRANT ALL PRIVILEGES ON siga_notas_db.*       TO '$DB_USER'@'%';
  FLUSH PRIVILEGES;
EOSQL
