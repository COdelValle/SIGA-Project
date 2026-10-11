#!/bin/bash
# Importa los 10 dumps locales a la MariaDB del stack AWS (misma instancia,
# 10 bases). Reemplaza el contenido de cada base: DROP + CREATE utf8mb4 +
# import + GRANT, y al final verifica migraciones Flyway por base.
#
# Uso (en la EC2, con los dumps copiados por scp):
#   bash importar-bd-aws.sh [dumps_dir] [contenedor_mariadb] [--force]
#
# Prerrequisito: detener los servicios de aplicacion (docker compose stop) y
# dejar solo mariadb/rabbitmq corriendo. RabbitMQ conviene purgarlo antes para
# no reprocesar mensajes viejos.
set -euo pipefail

DUMPS_DIR="${1:-/home/ubuntu/siga/dumps}"
CONTENEDOR="${2:-mariadb}"
FORCE="${3:-}"
ENV_FILE="${ENV_FILE:-/home/ubuntu/siga/.env}"

if [ -f "$ENV_FILE" ]; then
  set -a
  # shellcheck disable=SC1090
  . "$ENV_FILE"
  set +a
fi
DB_USER="${DB_USER:-siga_user}"

BASES=(
  siga_usuarios_db
  siga_estudiantes_db
  siga_asignaturas_db
  siga_notas_db
  siga_docentes_db
  siga_apoderados_db
  siga_clases_db
  siga_evaluaciones_db
  siga_asistencias_db
  siga_notificaciones_db
)

# Preflight: la aplicacion debe estar detenida
if [ "$FORCE" != "--force" ]; then
  corriendo=$(docker ps --format '{{.Names}}' | grep -E '^(bff-web|ms-|frontend)' || true)
  if [ -n "$corriendo" ]; then
    echo "Hay contenedores de aplicacion corriendo:" >&2
    echo "$corriendo" >&2
    echo "Detenlos antes de importar (docker compose stop) o usa --force." >&2
    exit 1
  fi
fi

for base in "${BASES[@]}"; do
  archivo="$DUMPS_DIR/$base.sql"
  if [ ! -f "$archivo" ]; then
    echo "Falta el dump $archivo" >&2
    exit 1
  fi

  echo "== Importando $base =="
  docker exec "$CONTENEDOR" sh -c "mariadb -uroot -p\"\$MARIADB_ROOT_PASSWORD\" -e \"DROP DATABASE IF EXISTS $base; CREATE DATABASE $base CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\""
  docker exec -i "$CONTENEDOR" sh -c "mariadb -uroot -p\"\$MARIADB_ROOT_PASSWORD\" $base" < "$archivo"
  docker exec "$CONTENEDOR" sh -c "mariadb -uroot -p\"\$MARIADB_ROOT_PASSWORD\" -e \"GRANT ALL PRIVILEGES ON $base.* TO '$DB_USER'@'%'; FLUSH PRIVILEGES;\""
done

echo "== Verificacion (migraciones Flyway aplicadas por base) =="
for base in "${BASES[@]}"; do
  resumen=$(docker exec "$CONTENEDOR" sh -c "mariadb -uroot -p\"\$MARIADB_ROOT_PASSWORD\" -N -e \"SELECT CONCAT(COUNT(*), ' migraciones OK, max V', COALESCE(MAX(version), '-')) FROM $base.flyway_schema_history WHERE success = 1;\"")
  echo "$base: $resumen"
done

echo "Import completado desde $DUMPS_DIR"
