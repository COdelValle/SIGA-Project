# E3-PRES-20261115-01 — Preflight AWS bloqueado

- **Fecha:** 2026-11-15 local / 2026-11-16 UTC.
- **Entorno:** AWS `us-east-1`, instancia etiquetada `SIGA-app`; consulta por Systems Manager Run Command.
- **Rama/SHA local:** `dev` / `ed64504cd82408de2658d427966aedf357b7df90`.
- **CD consultado:** [run 39373705927](https://github.com/COdelValle/SIGA-Project/actions/runs/39373705927), `success`, SHA `efdd7f370652747acd1d5b45e86e8a7a90e9347b`.
- **CD posterior a la actualización de política:** [run 43792075530](https://github.com/COdelValle/SIGA-Project/actions/runs/43792075530), `success`, SHA `051d7af354c127a817a137596ba542158723a149`.
- **Secret names:** GitHub lista `KAFKA_UI_USER` y `KAFKA_UI_PASSWORD_HASH`; no se consultaron sus valores.

## Resultado de la inspección

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

El CD más reciente terminó correctamente y dejó 17 servicios saludables, pero `deploy` todavía apunta a una versión sin los componentes Kafka. Secrets configurados en GitHub no cambian una EC2 hasta que una versión que los consuma se despliega.

## Trazas SSM

- `8a312692-9732-4e78-9fc8-692367b98eab`: inventario de contenedores/imágenes; confirmó 17 contenedores activos y ausencia de imágenes Kafka.
- `b137fb33-0921-4705-b07c-0c89fcb21423`: verificación de Caddy y RabbitMQ; Kafka Basic Auth/route ausentes y RabbitMQ en clúster.
- `9d9b0084-50d8-49fc-b854-8b57c32d93b8`: consulta de topics falló con `No such container: kafka-1`; no modificó servicios.
- `b010d5c6-4f2c-465d-ac24-9175e88daf33`: SPA/API Gateway/BFF y respuestas 200/401.
- `1865ab33-f59c-41d2-af1d-136b0df6be38`: exchanges y consumidores RabbitMQ.
- `72f9e469-e88b-4a29-9a72-bdd22d52e0e6`: preflight posterior al CD más reciente; servicios healthy y Kafka ausente.

No se realizó ninguna parada/reinicio de broker ni se intentó cambiar el Compose remoto. Para completar esta evidencia se requiere desplegar primero la versión que contiene los servicios Kafka; después registrar un resultado nuevo `OK` y conservar este preflight bloqueado.
