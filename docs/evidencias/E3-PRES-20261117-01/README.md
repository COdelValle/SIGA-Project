# E3-PRES-20261117-01 — Evidencia Kafka local (EP3)

Fecha: 2026-11-17.
Entorno: Windows + Docker Desktop, stack Compose local (35 contenedores; `kafka-ui` y
`ms-kafka-admin` sin healthcheck). Kafka 3 brokers, ZooKeeper 3 nodos, RabbitMQ 2 nodos,
MariaDB de reportes `siga_reportes_db`.

## 1. Topics y grupos de partida

- `siga.academico.asistencias` y `siga.academico.rendimiento`: 3 particiones, RF 3,
  `min.insync.replicas=2`, `retention.ms=604800000`, `retention.bytes=134217728`,
  `cleanup.policy=delete`.
- Grupos `siga-reportes-asistencias` y `siga-reportes-rendimiento` con lag 0 antes y
  después de las pruebas.
- Solo topics de negocio + `__consumer_offsets` al cierre.

## 2. Errores y duplicados (runtime, sin token)

### A1. Payload inválido (sin `idEstudiante`)

- Evento `local-bad-a-20261117-01` publicado en `siga.academico.asistencias`.
- El listener recibió el registro 3 veces (intento original + 2 reintentos).
- Log del `DefaultErrorHandler`:
  `Mensaje Kafka inválido o no procesable topic=siga.academico.asistencias partición=0 offset=2`.
- Offset del grupo avanzó de 2 a 3 (lag 0) y no se creó fila en
  `proyecciones_asistencia` ni en `eventos_reportes_procesados`.

### A2. Duplicado del mismo evento

- Evento `local-dup-a-20261117-01` publicado dos veces.
- Log: `Evento de asistencia duplicado ignorado idEvento=local-dup-a-20261117-01`.
- Resultado: exactamente 1 fila en `proyecciones_asistencia`, 1 en
  `eventos_reportes_procesados` y 1 alerta (`INASISTENCIA`, nivel `CRITICO`, 45% de
  inasistencia).

### A3. Política documentada

La política efectiva es **2 reintentos** con `FixedBackOff(1000 ms, 2)` + log de error y
descarte del registro. **No hay DLQ en Kafka**; RabbitMQ sí conserva su DLQ.

## 3. Carga 3a (alto flujo, sin token)

### B1. Broker — 20 000 mensajes de 512 B

- Topic temporal `siga.e3.local-load-20261117012719` (3/3, min ISR 2,
  `retention.ms=3600000`, `retention.bytes=1048576`).
- `kafka-producer-perf-test`: **20 000 records sent**, **14 577,259 rec/s**,
  0 errores, 542,69 ms de latencia promedio.
- Offsets por partición: 6820 + 6386 + 6794 = **20 000**.
- Topic temporal eliminado al finalizar (C).

### B2. Consumidor — 300 eventos de rendimiento

- 300 eventos únicos, `idEstudiante` 930000000000000000+i, `idCursoAsignatura=null`,
  `puntaje=6.5`, `idEvento=local-load-r-<ts>-<i>`.
- Lag 0 observado en **5,173 s** (máximo observado 114), por debajo del límite de 120 s.
- 300 filas en `proyecciones_rendimiento`, 300 en `eventos_reportes_procesados`,
  0 alertas (puntaje 6.5 ⇒ nivel `NORMAL`).

### B3. Recursos (muestras `docker stats --no-stream`)

| Servicio | Antes (CPU / RAM) | Durante (CPU / RAM) | Después (CPU / RAM) |
|---|---|---|---|
| kafka-1 | 1,38% / 395,4 MiB | 1,59% / 401,4 MiB | 1,60% / 400,7 MiB |
| kafka-2 | 1,48% / 388,2 MiB | 3,24% / 388,4 MiB | 1,11% / 388,0 MiB |
| kafka-3 | 2,81% / 360,9 MiB | 1,13% / 361,6 MiB | 2,31% / 360,7 MiB |
| ms-reportes | 0,99% / 374,3 MiB | 4,77% / 418,0 MiB | 0,64% / 416,2 MiB |
| mariadb-reportes | 0,07% / 48,6 MiB | 0,07% / 50,5 MiB | 0,11% / 48,8 MiB |

- Sin reinicios ni contenedores `unhealthy` durante la ventana.
- **Desviación:** la RAM del host ya estaba en 87,8% antes de la carga y se mantuvo en
  86,4% durante/después. El umbral < 80% no se cumple por consumo base del host, no por
  el flujo Kafka (contenedores bajo 5,3% de RAM cada uno).

### B4. Límite de alcance

El objetivo del cliente (20 VUs durante 10 min y p95 de API) **no queda cubierto por la
carga 3a**: requiere `Bearer token` y se ejecuta con los scripts de API, no con este
flujo directo a Kafka.

## 4. Limpieza

En `siga_reportes_db` con `LIKE 'local-%'` se eliminaron, en orden:

1. `alertas_riesgo` por `correlation_id`: 4 filas.
2. `proyecciones_asistencia` por `id_evento`: 2 filas.
3. `proyecciones_rendimiento` por `id_evento`: 302 filas.
4. `eventos_reportes_procesados` por `id_evento`: 305 filas.

Se borraron todos los topics `siga.e3.local-load-*`. Verificación final: 0 filas
`local-%` en las cuatro tablas, lag 0 en ambos grupos y solo los dos topics de negocio
(+ `__consumer_offsets`).

## 5. Checks Allure

Nombres exactos de los checks generados por los scripts:

- `kafka error policy` — layer `infra`, epic `Kafka`, feature `Kafka - Infraestructura`,
  story `Log y descarte sin DLQ`, tag `kafka`.
- `kafka topic provisioning` — layer `infra`, epic `Kafka`, feature
  `Kafka - Infraestructura`, story `3 particiones, RF 3, min ISR 2, retencion 7d/128MiB`,
  tag `kafka`.
- `kafka broker throughput` — layer `performance`, epic `Kafka`, feature
  `Kafka - Alto flujo`, story `20k mensajes de 512 B`, tag `kafka`.
- `kafka consumer throughput` — layer `performance`, epic `Kafka`, feature
  `Kafka - Alto flujo`, story `300 eventos hasta lag 0`, tag `kafka`.

Métricas del script `tools/performance/kafka-load.mjs` en la corrida D8:

- broker: `records_sent=20000`, `records_per_sec=13633.265`,
  `offsets_totales=20000`, `errores=0`.
- consumer: `segundos_hasta_lag_0=5.173`, `lag_maximo_observado=114`,
  `filas_proyecciones_rendimiento=300`, `filas_eventos_procesados=300`,
  `alertas_generadas=0`, limpieza de las 300 filas propia.

El reporte de CI en GitHub Pages incluye el epic `Kafka` con los grupos
`Kafka - Infraestructura` (infra), `Kafka - Administracion` (security),
`Kafka - Consumo y proyecciones` (unit), `Kafka - Alertas de riesgo` (unit),
`Kafka - Migraciones` (integration) y `Kafka - Alto flujo` (performance). El job
`performance` corre solo en push a `dev`, `main` o `deploy`: levanta Kafka, MariaDB y
RabbitMQ, ejecuta `ms-reportes` desde el jar generado por el job `backend`, corre
`kafka broker throughput` y `kafka consumer throughput`, y publica el artifact
`performance-allure-<sha>` que `allure-report` fusiona y publica en Pages.

En el run [44105174173](https://github.com/COdelValle/SIGA-Project/actions/runs/44105174173)
el job `performance` terminó en verde y Pages quedó con **1368 tests (1357 passed)**,
incluyendo `Kafka - Alto flujo` con los dos checks en `passed`:

- broker: `records_sent=20000`, `records_per_sec=5733.945`, `offsets_totales=20000`,
  `errores=0`.
- consumer: `segundos_hasta_lag_0=5.396`, `lag_maximo_observado=49`,
  `filas_proyecciones_rendimiento=300`, `filas_eventos_procesados=300`,
  `alertas_generadas=0`, limpieza de las 300 filas propia.

El reporte local `allure-report-local` sigue disponible para ejecuciones manuales.

## 6. Incidencia operativa

Durante la verificación inicial, Docker Desktop dejó de responder a la API
(`docker ps` y `docker logs` bloqueados o con error 500). Se reinició Docker Desktop
**sin** `docker compose down -v`; los contenedores se reiniciaron automáticamente y se
revalidaron topics, grupos, lag y salud antes de continuar con A→E.
