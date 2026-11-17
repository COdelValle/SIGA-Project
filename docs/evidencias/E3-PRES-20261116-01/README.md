# E3-PRES-20261116-01 — AWS Kafka deploy y failover

- **Fecha:** 2026-11-16 UTC.
- **Entorno:** AWS `us-east-1`, instancia `SIGA-app`, consultas por SSM.
- **Promoción:** PR #128, merge `dc98259ab2fab428647c133475f5126c724d36b8`; PR #129 (fix de proyección), merge `df32827ab5101121a5969731762124ac67be7c1c`.
- **CD:** [run 43869955825](https://github.com/COdelValle/SIGA-Project/actions/runs/43869955825), `success` (SHA `dc98259`); [run 43881252562](https://github.com/COdelValle/SIGA-Project/actions/runs/43881252562), `success` (SHA `df32827`).
- **Credenciales/datos:** no se imprimieron secretos. Los eventos funcionales de humo usaron IDs sintéticos grandes; no contienen datos personales.
- **SSM post-CD:** `d0f4b7c2-6be7-4aa3-8055-52082f87c520` (contenedores/topics/health), `d348f00d-bdf8-485a-a79f-8ccd4b8d2431` (cluster RabbitMQ) y `64e3041c-9d5b-4014-a6da-6aec956223f1` (estado final, colas, grupos, topics y actuator health).

## Stack y metadatos

- El CD verificó el health de los servicios, colas RabbitMQ, cluster, topics Kafka, CORS, API Gateway, conteos de datos y Allure. La lectura SSM final (`64e3041c-9d5b-4014-a6da-6aec956223f1`) mostró los 26 contenedores esperados activos; aplicaciones, tres brokers, tres ZooKeeper, RabbitMQ 1/2, Caddy y MariaDB estaban `healthy`. Kafka UI estaba `Up`.
- Los tres brokers respondieron al API probe (`KAFKA1_API_OK`, `KAFKA2_API_OK`, `KAFKA3_API_OK`). Los topics `siga.academico.asistencias` y `siga.academico.rendimiento` tienen 3 particiones, RF=3, `min.insync.replicas=2`, ISR 3/3, `retention.ms=604800000`, `retention.bytes=134217728` y `cleanup.policy=delete`.
- Los consumer groups `siga-reportes-asistencias` y `siga-reportes-rendimiento` están conectados a sus topics; estaban sin lag y sin mensajes al comenzar el smoke test.
- `ms-kafka-admin` y `ms-reportes` respondieron `{"status":"UP"}`.
- RabbitMQ (`d348f00d-bdf8-485a-a79f-8ccd4b8d2431`) informó `rabbit@rabbitmq1` y `rabbit@rabbitmq2` en `Running Nodes`, sin alarmas ni particiones. Tras los failovers, se volvió a confirmar lo mismo (`64e3041c-9d5b-4014-a6da-6aec956223f1`); las colas esperadas/listeners permanecieron presentes.
- Kafka UI sin autenticar respondió HTTP `401` en `https://kafka.<IP-con-guiones>.sslip.io/`, confirmando que Caddy exige Basic Auth. No se probó el login interactivo porque en GitHub solo está guardado el hash, no se consultó ni expuso la contraseña original.

## Failover de un broker

Se creó el topic temporal `siga.e3.failover-20261116-01` con 3 particiones, RF=3, `min.insync.replicas=2`, retención de 1 hora y límite de 1 MiB (SSM `9fe67295-8873-4b21-80e5-32d03624b55c`). La publicación inicial con `acks=all` confirmó 1 registro.

| Broker detenido | Estado observado | Publicación con `acks=all` | Recuperación |
|---|---|---|---|
| `kafka-1` | Los líderes se redistribuyeron; las 3 particiones quedaron con ISR de 2, brokers 2/3 accesibles. SSM `5ce5bf8b-b434-424b-a68a-547ca8184847` | 1 registro confirmado; SSM `4cf4290b-ded5-49aa-9dc8-7a53731f0fc6` | `kafka-1` volvió a `healthy`, ISR 3/3; SSM `8b1fe8ec-87f3-40d6-a6f0-5d587a473d8e` |
| `kafka-2` | Las 3 particiones quedaron con ISR de 2, brokers 1/3 accesibles. SSM `6fee9c93-af49-4b5f-a33c-78d9fae470af` | 1 registro confirmado; SSM `4a61a292-98aa-4ca6-af20-032604806118` | `kafka-2` volvió a `healthy`, ISR 3/3; SSM `5696d549-d096-4051-a6a7-6fa27bd52830` |
| `kafka-3` | Las 3 particiones quedaron con ISR de 2, brokers 1/2 accesibles. SSM `bfac1200-1675-4da7-9cc7-b3a753285f16` | 1 registro confirmado; SSM `06690b70-69cf-4d42-bb53-249acd811f7a` | `kafka-3` volvió a `healthy`, ISR 3/3; SSM `754fa9bb-f6aa-4b4d-8368-39dfc41c5e89` |

Cada broker se detuvo y restauró individualmente; ZooKeeper no se modificó.

## Failover de dos brokers

1. Con el topic recuperado e ISR 3/3, se detuvieron `kafka-1` (`2266b3ce-418d-4091-bbf7-0bc323433e14`) y después `kafka-2` (`999165d6-c81f-4169-a025-7335c5f2b761`). `kafka-3` fue el único broker disponible; cada partición mostró ISR=1 y líder 3.
2. El productor observó repetidamente `NOT_ENOUGH_REPLICAS` y reportó `0 records sent` (SSM `4215bbb6-50b0-42eb-9190-d6636b9b3ea3`). `kafka-producer-perf-test` devuelve código 0 aunque no envíe ningún registro; el chequeo exterior que solo miraba el código de salida terminó con `ResponseCode=2`, por lo que la aceptación se basa en el error Kafka y en los offsets, no en el exit code de esa herramienta.
3. Se restauró primero `kafka-2` (SSM `36fb7d0b-5eb8-4bbe-b490-0d4b4d7cea10`), se esperó health e ISR=2; después `kafka-1` (SSM `c4f61b4c-b1a2-49ab-9e10-bd23719b838d`), esperando health e ISR=3/3.
4. Los offsets finales del topic temporal fueron `partition 0: 1`, `partition 1: 0`, `partition 2: 3` (SSM `fb01348e-e037-46b3-8e46-8610f8aab8d2`): total 4, correspondientes a la publicación inicial más las tres publicaciones de los escenarios de un broker. No se confirmó un registro en el escenario de dos brokers.
5. El topic temporal se eliminó y `kafka-topics --list` mostró solo los dos topics de negocio (SSM `9a25f346-c41c-4649-8094-a51e08ae5e51`). No se borraron topics de negocio ni volúmenes.

## Smoke test de proyecciones

- En `siga.academico.asistencias` se publicó un evento sintético normal (`idEvento=e3-smoke-a-20261116-01`, clave `idEstudiante=900000000000000001`). El grupo avanzó al offset 1, lag 0; la fila apareció en `proyecciones_asistencia` con estado `PRESENTE` y riesgo `NORMAL` (SSM `37778ac3-cd9a-479b-9f40-6f53caeb6ad6`).
- En `siga.academico.rendimiento` se publicó un evento sintético (`idEvento=e3-smoke-r-20261116-01`). El listener lo recibió, pero la primera escritura de la proyección falló con `Column 'actualizado_en' cannot be null`; tras reintentos, el error handler lo rechazó. No apareció fila en `proyecciones_rendimiento` ni en `eventos_reportes_procesados` para ese ID.
- La publicación de ambos eventos fue SSM `9e702bb5-3141-4482-a80c-4995a6ed3294`; la consulta de offsets/proyecciones fue `37778ac3-cd9a-479b-9f40-6f53caeb6ad6` y los logs del servicio `ms-reportes` fueron `275a7bc6-e28a-4c65-ae69-a9d17ea159f5`.
- **Defecto encontrado después del deploy:** `ReporteProcesamientoService` guardaba la proyección antes de inicializar campos NOT NULL (`actualizado_en`, `id_evento`, `correlation_id`, `nivel_riesgo`). Se corrigió el orden de inicialización, la prueba captura el estado en cada invocación y el fix se desplegó en el CD `43881252562`.
- **Reintento verificado (SHA `df32827`):** evento `e3-smoke-r-20261116-02` (SSM `84750ee1-602a-4110-89a5-c4ca05473c66`) persistió la proyección `900000000000000001 / 900000000000000052 / 900000000000000042` con `puntaje=6.5`, `promedio=6.5`, `nivel_riesgo=NORMAL`, y quedó registrado en `eventos_reportes_procesados` (topic rendimiento, partición 0, offset 1). El grupo `siga-reportes-rendimiento` quedó en offset 2 con lag 0 (SSM `db6f6862-71a1-4d88-a714-beef230cb926`).
- **Nota de arranque:** `ms-reportes` tardó ~438 s en iniciar tras el redeploy y apareció `unhealthy` durante el arranque; el productor recibió `REQUEST_TIMED_OUT` con reintento y el mensaje se entregó igualmente. Al finalizar, los 26 contenedores quedaron `healthy` (SSM `f2f07f4a-a2e0-424d-8fc0-e6893cb8ca94`).

## Resultado

- **Failover 1 broker:** `PASS` para los tres brokers, recuperación ISR 3/3.
- **Failover 2 brokers:** `PASS`: `acks=all` fue rechazado por `min.insync.replicas=2`; no hubo incremento de offsets durante ese intento.
- **Recuperación:** `PASS`: tres brokers, ISR 3/3, topics de negocio, grupos, RabbitMQ y todos los servicios saludables; el topic temporal se eliminó.
- **Proyección de asistencia sintética:** `PASS`.
- **Proyección de rendimiento sintética:** `FALLA` en SHA `dc98259` por campos NOT NULL sin inicializar; `PASS` en SHA `df32827` con `e3-smoke-r-20261116-02`, proyección persistida y lag 0.
- **Estado final del stack (SHA `df32827`):** 26 contenedores `healthy`, topics de negocio con ISR 3/3, ambos nodos RabbitMQ en `Running Nodes`, `ms-kafka-admin` y `ms-reportes` en `UP`.

## Invocaciones SSM de failover

- `5ce5bf8b-b434-424b-a68a-547ca8184847`, `4cf4290b-ded5-49aa-9dc8-7a53731f0fc6`, `8b1fe8ec-87f3-40d6-a6f0-5d587a473d8e`: kafka-1 fuera, escritura confirmada, restauración ISR 3/3.
- `6fee9c93-af49-4b5f-a33c-78d9fae470af`, `4a61a292-98aa-4ca6-af20-032604806118`, `5696d549-d096-4051-a6a7-6fa27bd52830`: kafka-2 fuera, escritura confirmada, restauración ISR 3/3.
- `bfac1200-1675-4da7-9cc7-b3a753285f16`, `06690b70-69cf-4d42-bb53-249acd811f7a`, `754fa9bb-f6aa-4b4d-8368-39dfc41c5e89`: kafka-3 fuera, escritura confirmada, restauración ISR 3/3.
- `2266b3ce-418d-4091-bbf7-0bc323433e14`, `999165d6-c81f-4169-a025-7335c5f2b761`, `4215bbb6-50b0-42eb-9190-d6636b9b3ea3`: escenario con dos brokers detenidos; ISR=1, rechazo `NOT_ENOUGH_REPLICAS`, cero registros enviados. El último comando exterior devolvió `ResponseCode=2` porque comparó el exit code de la herramienta, que fue 0 pese a la salida `0 records sent`.
- `36fb7d0b-5eb8-4bbe-b490-0d4b4d7cea10`, `c4f61b4c-b1a2-49ab-9e10-bd23719b838d`: recuperación de kafka-2 y kafka-1, ambos healthy e ISR 3/3.
- `fb01348e-e037-46b3-8e46-8610f8aab8d2`: offsets finales confirman que no se persistió el intento de dos brokers.
- `9a25f346-c41c-4649-8094-a51e08ae5e51`: eliminación del topic temporal; permanecen los dos topics de negocio.
- `84750ee1-602a-4110-89a5-c4ca05473c66`: publicación del evento de rendimiento corregido y consulta de proyección/evento procesado.
- `f2f07f4a-a2e0-424d-8fc0-e6893cb8ca94`: inventario final; los 26 contenedores `healthy`, incluido `ms-reportes`.
- `db6f6862-71a1-4d88-a714-beef230cb926`: offsets/lag de ambos grupos, ISR 3/3, nodos RabbitMQ y health de administradores.

No se afectaron los topics de negocio durante el failover de dos brokers y no se detuvo ZooKeeper ni la EC2.
