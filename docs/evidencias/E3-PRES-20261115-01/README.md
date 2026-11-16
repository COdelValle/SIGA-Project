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
- **CD tras corregir el arranque:** [run 43858404711](https://github.com/COdelValle/SIGA-Project/actions/runs/43858404711), merge SHA `296a1f7ba951caefd7c3ad661a48f0e642e6d528`; verificaciones, Allure y los 15 builds/push ECR pasaron, pero la acción SSH expiró a las 12:01:42 UTC con `Run Command Timeout` al límite de 15 minutos.
- **CD con timeout ampliado:** [run 43864706041](https://github.com/COdelValle/SIGA-Project/actions/runs/43864706041), merge SHA `80cc2798e14403b22146e3fdb6681a80ead84cdc`; el arranque llegó más lejos, pero Compose abortó a las 12:35:22 UTC con `dependency failed to start: container rabbitmq1 is unhealthy`.
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

- PR #126 corrigió el constructor de `ms-reportes` y amplió a 30 s los healthchecks Kafka; PR #127 amplió el `command_timeout` SSH a 45 min. El CD `43864706041` se detuvo en Compose porque `rabbitmq1` quedó `unhealthy`.
- SSM posterior confirmó brokers Kafka y `ms-reportes` saludables. Los dos RabbitMQ aparecieron `unhealthy`, pero ambos aceptaban TCP en `127.0.0.1:5672` y un `rabbitmq-diagnostics ping` directo posterior respondió; los cinco últimos probes Docker habían expirado (`ExitCode=-1`). La instancia tenía 2 vCPU y carga media aproximada de 24 durante el arranque.
- En el momento de la detención `ms-asistencias`, `ms-evaluaciones` y `ms-notificaciones` estaban en proceso de inicio; `ms-notas`, `bff-web` y `frontend` seguían `Created`. No hay aún un CD exitoso de stack completo.
- El Compose de `dev` cambia el healthcheck RabbitMQ por una comprobación TCP de 5672, con margen de arranque y timeout ampliados; queda pendiente promoverlo y volver a desplegar.
- No se han detenido brokers ni ejecutado failover. Mantenerlo bloqueado hasta que el CD termine correctamente, todos los contenedores estén saludables y se repita el preflight.

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
- `50589113-3442-458f-a6f4-47a62073eb85`: inventario durante el CD `43858404711`; Kafka brokers y ZooKeeper saludables mientras se iniciaban dependientes.
- `d9a220f9-d32a-47bb-b7a3-e477ea0ae1cd` y `c0d0b377-88cd-4af1-b01c-ca531f2ff90f`: inventario tras el timeout; `ms-reportes`/admin y brokers saludables, algunos servicios aún `Created`/`starting`.
- `6d7afbf7-31a1-4a4b-a670-b6cae3068839`: logs RabbitMQ y probes Docker `ExitCode=-1` tras el CD `43864706041`.
- `9c819fe6-bd76-4783-95f6-0becc4d0ea4b`: la prueba de `rabbitmq-diagnostics ping` con límite de 25 s falló durante carga alta.
- `dd285b4f-2edf-4fa7-af64-a1d572566089`: RabbitMQ aceptó TCP en ambos nodos y el ping directo respondió después; host con 2 CPU y load average cercano a 24.

Este README conserva el preflight inicial y los fallos intermedios. El despliegue y los failovers se completaron después; consultar [`../E3-PRES-20261116-01/README.md`](../E3-PRES-20261116-01/README.md) para la evidencia más reciente.
