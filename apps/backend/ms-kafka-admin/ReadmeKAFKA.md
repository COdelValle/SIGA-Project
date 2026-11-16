# Provisionamiento de topics Kafka – SIGA Académico

## 1. Propósito

El módulo `ms-kafka-admin` provisiona y administra los topics de eventos académicos de asistencia y rendimiento. `ms-asistencias` y `ms-notas` publican eventos tipados; `ms-reportes` los consume para mantener proyecciones y calcular alertas.

Kafka complementa RabbitMQ: Kafka conserva un log particionado que permite consumir y volver a leer eventos para analítica; RabbitMQ conserva los flujos existentes de colas, notificaciones, confirmaciones y DLQ. Esta extensión no reemplaza esos flujos RabbitMQ.

## 2. Configuración vigente

Los dos topics de negocio usan los valores configurados en `TopicProvisioningConfig.java` y en la plantilla AWS `infra/terraform/templates/docker-compose.yml`:

| Propiedad | Valor vigente |
|---|---|
| Particiones por topic | 3 |
| Factor de replicación | 3 |
| `cleanup.policy` | `delete` |
| `retention.ms` | `604800000` (7 días) |
| `retention.bytes` | `134217728` bytes (128 MiB por partición) |
| Mínimo ISR del clúster (`KAFKA_MIN_INSYNC_REPLICAS`) | 2 |

La retención temporal y el límite por bytes son límites de borrado independientes; en la práctica, los segmentos pueden expirar al alcanzar cualquiera de las condiciones aplicables. El límite por bytes ayuda a acotar el uso del EBS, que comparte una sola instancia AWS con el resto del stack.

## 3. Topics, claves y orden

### `siga.academico.asistencias`

- Productor: `ms-asistencias`.
- Consumidor: `ms-reportes`, grupo `siga-reportes-asistencias`.
- Clave: `idEstudiante` como texto.
- DTO: `EventoAsistenciaKafka`.

### `siga.academico.rendimiento`

- Productor: `ms-notas`.
- Consumidor: `ms-reportes`, grupo `siga-reportes-rendimiento`.
- Clave: `idEstudiante` como texto.
- DTO: `EventoRendimientoKafka`.

La clave actual se mantiene deliberadamente en `idEstudiante`: todos los eventos de un estudiante dentro de un mismo topic quedan asociados a una clave y conservan su orden en esa partición. El hash de la clave distribuye distintos estudiantes entre las tres particiones y permite paralelismo. Kafka no garantiza orden global entre particiones ni entre los dos topics.

Cada topic tiene un consumer group propio para que las proyecciones de asistencia y rendimiento escalen de forma independiente. Con tres particiones, hasta tres consumidores de cada grupo pueden procesar particiones en paralelo; consumidores adicionales quedan sin asignación mientras no aumente el número de particiones.

## 4. Justificación de particiones y réplicas

- **Tres particiones** distribuyen los eventos y ofrecen hasta tres unidades de paralelismo por grupo. La clave por estudiante mantiene afinidad y orden por estudiante dentro de cada topic.
- **Factor de replicación tres** coloca una réplica de cada partición en cada uno de los tres brokers configurados.
- **Mínimo de ISR dos** exige dos réplicas sincronizadas para confirmar escrituras con `acks=all`. La pérdida aislada de un broker puede conservar dos réplicas sincronizadas; con dos brokers fuera de servicio, la escritura debe rechazarse para proteger la durabilidad.
- **Límite de disponibilidad:** los tres brokers y los tres ZooKeeper viven en una sola EC2. La topología permite probar tolerancia a fallas de proceso/broker, pero no tolerancia a la pérdida del host, EBS o zona de disponibilidad.

## 5. Retención e historial

La política `delete` conserva eventos hasta que alcanzan la retención temporal o el límite por tamaño. Esto permite reprocesar un historial reciente de analítica sin convertir Kafka en el almacenamiento permanente de los datos académicos: las proyecciones se persisten en `siga_reportes_db`.

Los valores vigentes son siete días y 128 MiB por partición. La decisión fue mantenerlos alineados con la plantilla AWS y el tamaño disponible del EBS; este documento sustituye la propuesta antigua de 365 días.

## 6. Errores, idempotencia y observabilidad

- Los DTO incluyen `idEvento` y `correlationId`; `ms-reportes` valida payloads y registra los eventos procesados para ignorar duplicados.
- El consumidor usa commit manual por registro y reintenta fallas de procesamiento dos veces antes de registrar el error final. La política actual no declara un Kafka DLT; no confundirla con las DLQ de RabbitMQ.
- Los productores registran el resultado asíncrono de envío. La política actual no garantiza reintento durable del productor; las verificaciones deben comprobar y registrar el comportamiento real, no asumir entrega eventual.
- Los consumer groups, offsets, lag, líderes, réplicas e ISR pueden inspeccionarse mediante Kafka UI, CLI y los endpoints protegidos de `ms-kafka-admin`.

## 7. Provisionamiento

`TopicProvisioningConfig.java` declara los dos beans `NewTopic`; Kafka los crea mediante `KafkaAdmin` al iniciar `ms-kafka-admin`. La creación automática del broker está deshabilitada, por lo que el despliegue debe comprobar explícitamente la presencia y metadatos de ambos topics.

Kafka UI se conecta a los tres brokers. En AWS, Caddy debe publicar la interfaz en un hostname separado con Basic Auth configurada mediante los secrets `KAFKA_UI_USER` y `KAFKA_UI_PASSWORD_HASH`. La contraseña en texto plano no se recupera del hash: se ingresa solo en el navegador y nunca se registra en capturas o logs.

## 8. Guía de verificación

Los pasos reproducibles de pruebas locales, AWS, Kafka UI, métricas, recuperación de brokers y evidencia están en [`../../../docs/pruebas-manuales-ep3-aws.md`](../../../docs/pruebas-manuales-ep3-aws.md).
