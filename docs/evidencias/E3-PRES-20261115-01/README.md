# E3-PRES-20261115-01 — Preflight AWS bloqueado

- **Fecha:** 2026-11-15 local / 2026-11-16 UTC.
- **Entorno:** AWS `us-east-1`, instancia etiquetada `SIGA-app`; consulta por Systems Manager Run Command.
- **Rama/SHA local:** `dev` / `ed64504cd82408de2658d427966aedf357b7df90`.
- **CD consultado:** [run 39373705927](https://github.com/COdelValle/SIGA-Project/actions/runs/39373705927), `success`, SHA `efdd7f370652747acd1d5b45e86e8a7a90e9347b`.
- **CD posterior a la actualización de política:** [run 43792075530](https://github.com/COdelValle/SIGA-Project/actions/runs/43792075530), `success`, SHA `051d7af354c127a817a137596ba542158723a149`.
- **Promoción EP3:** PR [#121](https://github.com/COdelValle/SIGA-Project/pull/121), merge `6db01be0a160e0ad018d0a9a29f5c244aa81e1ed`.
- **CD de promoción:** [run 43799769525](https://github.com/COdelValle/SIGA-Project/actions/runs/43799769525), `failure` durante `docker compose up`: dependencia RabbitMQ unhealthy. Se crearon parcialmente brokers Kafka; la lectura SSM posterior mostró `kafka-1` unhealthy, brokers 2/3 healthy y algunos servicios `Created`/`starting`.
- **CD reintento:** [run 43811522748](https://github.com/COdelValle/SIGA-Project/actions/runs/43811522748), la verificación backend/frontend e informe Allure pasaron; el deploy falló antes de la EC2 en `ecr:GetAuthorizationToken` por denegación explícita `voc-cancel-cred`.
- **CD siguiente:** [run 43851631259](https://github.com/COdelValle/SIGA-Project/actions/runs/43851631259), SHA `473c07ab9dc88eebfb1cb5e836f4f35eea4b51e8`; las verificaciones y Allure pasaron, pero Compose abortó a las 11:07:51 UTC con `dependency failed to start: container kafka-1 is unhealthy`.
- **Secret names:** GitHub lista `KAFKA_UI_USER` y `KAFKA_UI_PASSWORD_HASH`; no se consultaron sus valores.

## Preflight antes de promover Kafka

| Comprobación | Resultado |
|---|---|
| EC2 y SSM | EC2 activa; SSM `PingStatus=Online` |
| Compose activo tras el CD más reciente | `docker compose config --services` muestra 17 servicios y no contiene Kafka |
| Servicios Kafka | No existen contenedores/imágenes de `zookeeper-*`, `kafka-*`, `kafka-ui`, `ms-kafka-admin` o `ms-reportes` |
| Topics/metadata | No verificables: `kafka-1` no existe en el host |
| Caddy/Kafka UI | El Caddyfile no contiene ruta Kafka ni bloque Basic Auth |
| SPA por API Gateway | HTTP `200` |
| BFF health | HTTP `200` |
| JWT ausente | `/api/me` responde `401` |
| JWT malformado | `/api/me` responde `401 invalid_token` |
| JWT real válido / Entra | Pendiente de iniciar sesión con usuario de prueba autorizado |
| RabbitMQ | Dos nodos aparecen en `Running Nodes`, sin alarmas ni particiones de red |
| Exchanges RabbitMQ | `intercambio-notificaciones`/`user.topic.exchange` son `topic`; `siga.dlx.direct` es `direct` |
| Failover de 1/2 brokers | `BLOQUEADO`: no hay brokers sobre los que realizar la prueba |

Este preflight inicial correspondió a la versión `051d7af`, antes de promover el código Kafka. El PR #121 inició una actualización de la EC2; su estado posterior quedó parcialmente desplegado y se registra abajo.

## Estado después de la promoción EP3

| Comprobación | Resultado de la última lectura SSM disponible |
|---|---|
| ZooKeeper | 3/3 containers healthy |
| Brokers | `kafka-1` unhealthy; `kafka-2` y `kafka-3` healthy |
| `ms-kafka-admin` | Healthy |
| `kafka-ui` | Container running; la ruta pública Caddy no quedó confirmada |
| `ms-reportes` / `ms-notas` | En `starting` en la lectura posterior al CD fallido |
| BFF / frontend | `Created` en esa misma lectura |
| RabbitMQ | Dos nodos healthy y en clúster en la lectura posterior al CD fallido |
| Failover | No ejecutado: Kafka-1 ya estaba unhealthy y el stack no estaba totalmente listo |

Terraform creó los dos repositorios ECR (`siga-kafka-admin`, `siga-reportes`) y aplicó EBS 10→20 GB/user-data in-place, sin destrucciones. El CD `43811522748` pasó verificaciones de código/Compose y el reporte Allure, pero falló antes de conectar a EC2 con `ecr:GetAuthorizationToken` denegado por `voc-cancel-cred`; no alteró el estado remoto posterior al CD `43799769525`.

La comprobación posterior confirmó que `sts:GetCallerIdentity` responde, pero `ecr:GetAuthorizationToken` y `ssm:DescribeInstanceInformation` reciben una denegación explícita de `voc-cancel-cred`. Se necesita una sesión AWS Academy vigente para volver a desplegar y leer el estado actual.

## Diagnóstico más reciente — 2026-11-16

- SSM confirmó después del CD que `kafka-1`, `kafka-2` y `kafka-3` están `healthy`; sus APIs responden. Los dos topics tienen 3 particiones, RF=3, `min.insync.replicas=2`, ISR completo por partición y retención de 7 días/128 MiB.
- RabbitMQ informa `rabbit@rabbitmq1` y `rabbit@rabbitmq2` activos, sin alarmas ni particiones de red. `ms-kafka-admin` responde `{"status":"UP"}`.
- El despliegue sigue parcial: `ms-notas`, `ms-asistencias`, `bff-web` y `frontend` aparecen `Created`; `ms-reportes` reinicia. Sus logs identificaron `UnsatisfiedDependencyException`/`No default constructor found` en `ReporteProcesamientoService` (un constructor generado por Lombok coexistía con una sobrecarga de cinco argumentos, sin constructor marcado para Spring). La corrección explícita con `@Autowired` y una prueba de wiring ya se añadieron en `dev`; falta promoverla.
- Las últimas entradas de healthcheck de Kafka incluyen timeouts (`ExitCode=-1`) aunque los brokers ya recuperaron `healthy`. Se amplió el timeout del probe de 10 a 30 segundos en el Compose de `dev` para tolerar la carga de arranque.
- No se han detenido brokers ni ejecutado failover. Mantenerlo bloqueado hasta desplegar la corrección, recuperar todos los servicios y repetir el preflight.

## Trazas SSM

- `8a312692-9732-4e78-9fc8-692367b98eab`: inventario de contenedores/imágenes; confirmó 17 contenedores activos y ausencia de imágenes Kafka.
- `b137fb33-0921-4705-b07c-0c89fcb21423`: verificación de Caddy y RabbitMQ; Kafka Basic Auth/route ausentes y RabbitMQ en clúster.
- `9d9b0084-50d8-49fc-b854-8b57c32d93b8`: consulta de topics falló con `No such container: kafka-1`; no modificó servicios.
- `b010d5c6-4f2c-465d-ac24-9175e88daf33`: SPA/API Gateway/BFF y respuestas 200/401.
- `1865ab33-f59c-41d2-af1d-136b0df6be38`: exchanges y consumidores RabbitMQ.
- `72f9e469-e88b-4a29-9a72-bdd22d52e0e6`: preflight posterior al CD más reciente; servicios healthy y Kafka ausente.
- `de6333df-6f01-4cee-ab7a-1a53ca04c8ef`: consulta SSM exitosa posterior al CD `43799769525`; Kafka broker 1 unhealthy, Kafka 2/3 healthy, admin healthy, reportes en starting y RabbitMQ recuperado.
- `a195fbf8-8958-42f5-a5a5-01a62905d7a0`: APIs Kafka, topics, configs/retención, ISR y health de `ms-kafka-admin`; los tres brokers healthy.
- `bb622815-3536-473e-858d-fce7aa9e570b` y `0d30a34a-d308-4f78-8907-a5f6edd77622`: estado RabbitMQ y diagnóstico de arranque de `ms-reportes`.
- `0c88968a-e652-4354-a2ac-6bfd9c0f9eb0`: inventario actual; confirma servicios dependientes `Created` y `ms-reportes` en reinicio.

No se realizó ninguna parada/reinicio de broker ni se cambió Compose en la EC2. La corrección de código/healthcheck está pendiente de promover; después se debe registrar un resultado nuevo `OK` y conservar este preflight bloqueado.
