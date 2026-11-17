#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

COMPOSE=(docker compose --env-file .env.example)
SERVICES=(
  zookeeper-1 zookeeper-2 zookeeper-3
  kafka-1 kafka-2 kafka-3
  mariadb-reportes
  rabbitmq1 rabbitmq2
)
APP_CONTAINER="ms-reportes-ci"
RESULTS_DIR="tools/performance/allure-results"
HEALTH_TIMEOUT_SECONDS=300
APP_TIMEOUT_SECONDS=240

log() {
  printf '%s\n' "$*"
}

dump_logs() {
  echo "--- logs ms-reportes ---"
  docker logs "$APP_CONTAINER" 2>&1 | tail -n 200 || true
  echo "--- logs stack ---"
  "${COMPOSE[@]}" logs --tail=80 2>&1 || true
}

cleanup() {
  docker rm -f "$APP_CONTAINER" >/dev/null 2>&1 || true
  "${COMPOSE[@]}" down -v >/dev/null 2>&1 || true
}

on_error() {
  echo "La carga Kafka en CI fallo; se muestran los logs antes de limpiar."
  dump_logs
}

trap on_error ERR
trap cleanup EXIT

wait_healthy() {
  local container="$1"
  local deadline=$((SECONDS + HEALTH_TIMEOUT_SECONDS))
  while ((SECONDS < deadline)); do
    local status
    status="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container" 2>/dev/null || true)"
    if [[ "$status" == "healthy" ]]; then
      return 0
    fi
    if [[ "$status" == "exited" || "$status" == "dead" ]]; then
      echo "El contenedor $container quedo en estado $status."
      return 1
    fi
    sleep 5
  done
  echo "Timeout esperando healthy en $container."
  return 1
}

wait_app() {
  local deadline=$((SECONDS + APP_TIMEOUT_SECONDS))
  while ((SECONDS < deadline)); do
    if curl -fsS http://localhost:8093/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
      return 0
    fi
    if [[ "$(docker inspect -f '{{.State.Status}}' "$APP_CONTAINER" 2>/dev/null || true)" != "running" ]]; then
      echo "El contenedor $APP_CONTAINER no esta en ejecucion."
      return 1
    fi
    sleep 5
  done
  echo "Timeout esperando ms-reportes en estado UP."
  return 1
}

log "Levantando stack minimo de Kafka, MariaDB y RabbitMQ"
"${COMPOSE[@]}" up -d "${SERVICES[@]}"

for service in "${SERVICES[@]}"; do
  log "Esperando healthy: $service"
  wait_healthy "$service"
done

for topic in siga.academico.asistencias siga.academico.rendimiento; do
  log "Asegurando topic $topic"
  docker exec kafka-1 kafka-topics --bootstrap-server kafka-1:29092 \
    --create --if-not-exists --topic "$topic" \
    --partitions 3 --replication-factor 3 \
    --config min.insync.replicas=2 \
    --config retention.ms=604800000 \
    --config retention.bytes=134217728 \
    --config cleanup.policy=delete
done

JAR_PATH="$(find apps/backend/ms-reportes/target -maxdepth 1 -name 'ms-reportes-*.jar' -print -quit)"
if [[ -z "$JAR_PATH" ]]; then
  echo "No se encontro el jar de ms-reportes."
  exit 1
fi
JAR_PATH="$(cd "$(dirname "$JAR_PATH")" && pwd)/$(basename "$JAR_PATH")"
log "Jar de ms-reportes: $JAR_PATH"

log "Iniciando ms-reportes"
docker run -d --name "$APP_CONTAINER" --network siga-network \
  -p 8093:8093 \
  -e TZ=America/Santiago \
  -e SPRING_JPA_HIBERNATE_DDL_AUTO=validate \
  -e SPRING_DATASOURCE_URL=jdbc:mariadb://mariadb-reportes:3306/siga_reportes_db \
  -e SPRING_DATASOURCE_USERNAME=siga_user \
  -e SPRING_DATASOURCE_PASSWORD=change_me \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka-1:29092,kafka-2:29092,kafka-3:29092 \
  -e SPRING_RABBITMQ_ADDRESSES=rabbitmq1:5672,rabbitmq2:5672 \
  -e SPRING_RABBITMQ_USERNAME=siga_rabbit \
  -e SPRING_RABBITMQ_PASSWORD=change_me_rabbit \
  -e AZURE_TENANT_ID=00000000-0000-0000-0000-000000000000 \
  -e AZURE_CLIENT_ID=00000000-0000-0000-0000-000000000000 \
  -e AZURE_APP_ID_URI=api://00000000-0000-0000-0000-000000000000 \
  -v "$JAR_PATH:/app/app.jar:ro" \
  eclipse-temurin:21-jre-alpine \
  java -jar /app/app.jar >/dev/null

wait_app
log "ms-reportes esta UP"

rm -rf "$RESULTS_DIR"
mkdir -p "$RESULTS_DIR"

log "Ejecutando prueba de throughput de broker"
node tools/ci/allure-command.mjs \
  --name "kafka broker throughput" \
  --layer performance \
  --epic Kafka \
  --feature "Kafka - Alto flujo" \
  --story "20k mensajes de 512 B" \
  --tag kafka \
  --owner qa \
  --dir "$RESULTS_DIR" \
  -- node tools/performance/kafka-load.mjs broker

log "Ejecutando prueba de throughput de consumidor"
node tools/ci/allure-command.mjs \
  --name "kafka consumer throughput" \
  --layer performance \
  --epic Kafka \
  --feature "Kafka - Alto flujo" \
  --story "300 eventos hasta lag 0" \
  --tag kafka \
  --owner qa \
  --dir "$RESULTS_DIR" \
  -- node tools/performance/kafka-load.mjs consumer

log "Carga Kafka en CI completada."
