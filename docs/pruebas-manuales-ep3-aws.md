# EP3: guía manual de Kafka, AWS y evidencia

Guía separada para comprobar los requisitos de código y funcionamiento de la EP3. Está pensada para repetirla en local y en AWS, guardar evidencia verificable y explicar por qué se configuró así Kafka junto a RabbitMQ.

> **Estado AWS actualizado al 16 de noviembre de 2026:** PR #126 corrigió el arranque de `ms-reportes`, PR #127 amplió el timeout SSH y PR #128 cambió el healthcheck RabbitMQ a TCP. El CD `43869955825` terminó `success` con SHA `dc98259`; los 26 contenedores están saludables/en ejecución. Los dos topics de negocio tienen 3 particiones, RF=3, ISR 3/3 y retención correcta; ambos grupos de reportes están conectados. Las pruebas autorizadas de caída de cada broker pasaron y la prueba de dos brokers rechazó escrituras como se esperaba; el clúster quedó restaurado y se eliminó el topic temporal. El smoke sintético de asistencia pasó y el de rendimiento detectó `actualizado_en` nulo; la corrección está en curso. Ver [`evidencias/E3-PRES-20261116-01/README.md`](evidencias/E3-PRES-20261116-01/README.md). No declarar terminado el flujo end-to-end hasta desplegar esa corrección y repetir el smoke.

> Terraform creó los ECR `siga-kafka-admin` y `siga-reportes` y amplió EBS de 10 a 20 GB sin destrucciones. Los secrets AWS se renovaron y SSM volvió a estar accesible. El CD está resuelto; resta desplegar la corrección de rendimiento descubierta por el smoke sintético.

## 1. Justificación tecnológica para la evaluación

- **Kafka para analítica y RabbitMQ para colas de trabajo/notificaciones.** Kafka ofrece topics durables y particionados, offsets y relectura para recalcular proyecciones. RabbitMQ conserva la distribución de tareas y notificaciones ya implementada, con confirmaciones y DLQ. Kafka complementa ese flujo; no lo sustituye.
- **Dos topics de negocio.** `siga.academico.asistencias` y `siga.academico.rendimiento` separan dominios y consumidores; `ms-reportes` mantiene proyecciones y alertas.
- **Clave `idEstudiante`.** Mantiene afinidad y orden por estudiante dentro de cada topic. Los estudiantes se distribuyen entre las tres particiones. El orden no es global entre particiones ni entre topics.
- **Tres particiones.** Permiten hasta tres consumidores activos en paralelo por grupo, sujeto a que haya suficientes réplicas de `ms-reportes`.
- **Tres réplicas y `min.insync.replicas=2`.** Una pérdida de broker puede dejar dos réplicas sincronizadas. Si quedan dos brokers fuera de servicio, una escritura con `acks=all` debe rechazarse para proteger la durabilidad.
- **Retención de siete días y 128 MiB por partición.** Da una ventana acotada para reprocesar eventos sin usar Kafka como almacenamiento permanente y limita la presión sobre el EBS compartido.
- **Límite de AWS Academy.** Los tres brokers y ZooKeeper comparten una EC2. Las pruebas pueden demostrar tolerancia a la pérdida de procesos broker; no demuestran alta disponibilidad frente a una falla del host o de la zona.
- **Consumer groups y deduplicación.** Los grupos de reportes separan asistencia y rendimiento; `idEvento` y el registro de eventos procesados permiten ignorar redeliveries. El lag indica si el consumo alcanza a la publicación.

## 2. Evidencia y reglas de registro

1. Abrir [`evidencias/README.md`](evidencias/README.md) y asignar IDs `E3-ENC-*` para pruebas de código y `E3-PRES-*` para verificaciones de ejecución/demostración.
2. Registrar cada intento en [`registro-pruebas.md`](registro-pruebas.md): fecha/hora, SHA, entorno, ID, comando o paso, esperado, obtenido, resultado y vínculo al run/artefacto.
3. Guardar capturas y salidas resumidas en `docs/evidencias/<ID>/`. Conservar los fallos; después de corregir, guardar otra ejecución con resultado `OK`.
4. No guardar `.env`, passwords, hashes Basic Auth, tokens JWT, cookies, llaves, datos personales ni mensajes académicos reales. En capturas del navegador, ocultar identificadores y no mostrar el diálogo con la contraseña.
5. La presencia del secret `KAFKA_UI_PASSWORD_HASH` no permite recuperar la contraseña en texto plano. Usar la clave original ingresada al crear el hash; no enviar el hash como contraseña. Si se perdió la clave original, rotar coordinadamente el par usuario/clave-hash y volver a desplegar.

## 3. Preflight local y automatizado

Ejecutar desde la raíz del repositorio, usando PowerShell:

```powershell
mvn -f apps/backend/pom.xml -B -ntp package `
  "-Dtest=!*ApplicationTests,!AdminControllerSecurityTest" `
  "-DfailIfNoTests=false" `
  "-Dsurefire.failIfNoSpecifiedTests=false" `
  "-Dtypescript.generator.skip=true"

mvn -f apps/backend/pom.xml -B -ntp -pl ms-kafka-admin -am test `
  "-Dtest=AdminControllerSecurityTest" `
  "-Dsurefire.failIfNoSpecifiedTests=false" `
  "-Djacoco.skip=true" "-DargLine=-Xmx512m"

docker compose --env-file .env.example config -q
$env:REGISTRY = 'registry.example.com/siga'
$env:IMAGE_TAG = 'ci'
docker compose --env-file .env.example `
  -f infra/terraform/templates/docker-compose.yml config -q

Push-Location apps/frontend
npm test -- --watch=false --coverage
npm run build -- --configuration production
Pop-Location
```

Interpretación:

- Un `BUILD SUCCESS` que haya omitido tests de Testcontainers **no** prueba migraciones con base real ni integración Kafka. Registrar esos casos como omitidos/bloqueados y ejecutarlos en CI Linux o en un Docker Testcontainers funcional.
- La configuración AWS debe validarse con valores ficticios para `REGISTRY`/`IMAGE_TAG`; nunca copiar secrets de despliegue a una consola o evidencia.
- No usar `docker compose down -v` ni borrar volúmenes para repetir estas pruebas.

## 4. Preflight de AWS — puerta obligatoria

Con acceso de lectura a GitHub/AWS y a Session Manager o SSH:

1. Anotar el SHA del commit que se pretende demostrar y el SHA del último run CD exitoso. Deben corresponder a una versión cuyo `infra/terraform/templates/docker-compose.yml` incluya `zookeeper-1..3`, `kafka-1..3`, `ms-kafka-admin`, `ms-reportes` y `kafka-ui`.
2. Comprobar por nombre que GitHub tiene los secrets `KAFKA_UI_USER` y `KAFKA_UI_PASSWORD_HASH`; no consultar ni imprimir sus valores.
3. En la EC2 ejecutar:

   ```bash
   cd /home/ubuntu/siga
   docker compose ps -a
   docker compose config --services
   docker ps
   ```

4. Si falta cualquiera de los seis servicios Kafka indicados, o los brokers no están saludables, **detener el procedimiento**. Registrar `BLOQUEADO`; no intentar una caída de broker ni usar la UI.
5. Comprobar también las instancias de ZooKeeper, `ms-kafka-admin` y `ms-reportes`, y revisar que los datos persistentes sigan en los bind mounts EBS. No imprimir `.env` ni el Caddyfile completo.
6. Confirmar que la versión desplegada genera el hostname Kafka en Caddy y que protege la ruta con Basic Auth. Verificar solo presencia/configuración, nunca copiar el hash al artefacto.

## 5. Kafka UI y metadatos de topics

Una vez superado el preflight:

1. Abrir `https://kafka.<IP-pública-con-guiones>.sslip.io` en una ventana privada.
2. Sin autenticar, comprobar que el acceso esté protegido. Autenticarse con el usuario y la **contraseña original** correspondiente al hash configurado; verificar que Kafka UI carga el clúster `SIGA`.
3. Capturar la página de overview sin mostrar credenciales. Debe indicar tres brokers conectados.
4. En Kafka UI inspeccionar `siga.academico.asistencias` y `siga.academico.rendimiento`: cada uno debe tener tres particiones y factor de replicación tres. Verificar líder, réplicas e ISR de cada partición.
5. Confirmar para ambos: `retention.ms=604800000`, `retention.bytes=134217728`, `cleanup.policy=delete`.

Como alternativa de inspección desde la EC2:

```bash
docker exec kafka-1 kafka-topics --bootstrap-server kafka-1:29092 \
  --describe --topic siga.academico.asistencias
docker exec kafka-1 kafka-topics --bootstrap-server kafka-1:29092 \
  --describe --topic siga.academico.rendimiento
docker exec kafka-1 kafka-configs --bootstrap-server kafka-1:29092 \
  --entity-type topics --entity-name siga.academico.asistencias --describe
docker exec kafka-1 kafka-configs --bootstrap-server kafka-1:29092 \
  --entity-type topics --entity-name siga.academico.rendimiento --describe
```

Guardar la salida sin datos personales. En Kafka UI, no dejar mensajes de producción abiertos en la captura si incluyen datos identificables.

## 6. Flujo funcional y monitorización

1. Usar una cuenta de prueba autorizada y datos sintéticos. Crear una asistencia y una nota mediante los endpoints autenticados existentes; registrar los IDs de prueba localmente, no el token.
2. En Kafka UI verificar la publicación en el topic correspondiente, clave `idEstudiante`, timestamp, `idEvento` y campos del DTO.
3. Verificar en `ms-reportes` que avanzó el offset del grupo correcto y que la proyección aparece en las rutas de reportes del BFF. Esperar a que el lag vuelva a cero.
4. Comprobar que un mismo `idEvento` no genera una segunda proyección y que una alerta de riesgo llega al flujo RabbitMQ cuando las reglas lo requieren.
5. Probar los endpoints de lectura del administrador para topics, brokers, grupos, offsets, lag y salud de particiones con el JWT de prueba autorizado. Confirmar `401/403` para llamadas sin permisos y respuesta correcta con rol/scope permitido.
6. Repetir con eventos de asistencia y rendimiento. No afirmar flujo “en tiempo real” a partir de una prueba unitaria: guardar marcas de tiempo de publicación, consumo y consulta.

Las operaciones de actualización/eliminación de asistencia y eliminación de notas deben figurar como **brecha conocida** hasta que el código publique esos cambios y la proyección los procese; no marcar consistencia completa de proyecciones para esos casos.

**Smoke AWS 2026-11-16:** un evento sintético normal de asistencia se consumió y apareció en la proyección con lag 0. El evento sintético de rendimiento fue recibido pero no persistido: la primera escritura llegó sin `actualizado_en`, `id_evento`, `correlation_id` ni `nivel_riesgo`. Ya se corrigió el orden de inicialización en `dev` y las pruebas del módulo pasan; tras promoverlo, repetir rendimiento con un `idEvento` nuevo. Detalle en [`evidencias/E3-PRES-20261116-01/README.md`](evidencias/E3-PRES-20261116-01/README.md).

## 7. Prueba controlada de disponibilidad en AWS

Realizar solo después del preflight, en ventana coordinada y con los tres brokers inicialmente saludables. Nunca detener ZooKeeper ni el host durante esta prueba.

### 7.1 Preparar un topic temporal

Crear un topic exclusivo de prueba, con nombre único, tres particiones, tres réplicas y `min.insync.replicas=2`. Limitarlo con una retención corta y `retention.bytes`; no enviar mensajes de prueba a los topics de negocio durante el escenario de dos brokers caídos.

```bash
export TOPIC=siga.e3.failover-$(date +%Y%m%d%H%M%S)
docker exec kafka-1 kafka-topics --bootstrap-server kafka-1:29092 \
  --create --topic "$TOPIC" --partitions 3 \
  --replication-factor 3 --config min.insync.replicas=2 \
  --config retention.ms=3600000 --config retention.bytes=1048576
docker exec kafka-1 kafka-topics --bootstrap-server kafka-1:29092 \
  --describe --topic "$TOPIC"
docker exec kafka-1 kafka-producer-perf-test --topic "$TOPIC" \
  --num-records 1 --record-size 16 --throughput -1 \
  --producer-props bootstrap.servers=kafka-1:29092 acks=all \
    request.timeout.ms=5000 delivery.timeout.ms=10000
```

Guardar offsets iniciales, líderes e ISR. Verificar primero una publicación de prueba exitosa con `acks=all`:

```bash
docker exec kafka-1 kafka-run-class kafka.tools.GetOffsetShell \
  --broker-list kafka-1:29092 --topic "$TOPIC" --time -1
```

### 7.2 Pérdida de un broker, uno por vez

1. Confirmar que los tres brokers están saludables y guardar líderes/ISR/offsets iniciales.
2. Detener un único broker; no detener el siguiente hasta restaurar el anterior. Consultar el topic desde uno de los brokers restantes. Tras la elección de líderes, verificar que cada partición mantenga ISR de dos réplicas. Ejecutar el productor contra el broker restante; debe confirmar un mensaje:

   ```bash
   docker stop kafka-1
   docker exec kafka-2 kafka-topics --bootstrap-server kafka-2:29092 \
     --describe --topic "$TOPIC"
   docker exec kafka-2 kafka-producer-perf-test --topic "$TOPIC" \
     --num-records 1 --record-size 16 --throughput -1 \
     --producer-props bootstrap.servers=kafka-2:29092 acks=all \
       request.timeout.ms=5000 delivery.timeout.ms=10000
   docker start kafka-1
   docker inspect -f '{{.State.Health.Status}}' kafka-1
   ```
3. Esperar `healthy` y que las tres réplicas vuelvan al ISR. Confirmar que consumidores, offsets y lag se estabilizan.
4. Repetir con los otros brokers, uno por vez, restaurando el clúster entre intentos.

### 7.3 Pérdida de dos brokers

Este escenario reduce deliberadamente la disponibilidad de Kafka:

1. Con el clúster completamente recuperado y el topic temporal sano, detener dos brokers en secuencia. Mantener el tercero activo y ZooKeeper sin cambios.
2. Verificar que Kafka informe solo un broker disponible e ISR de una réplica. Con `min.insync.replicas=2` y `acks=all`, la publicación al topic temporal debe fallar por réplicas insuficientes; no se espera una escritura exitosa:

   ```bash
   docker stop kafka-1
   docker stop kafka-2
   docker exec kafka-3 kafka-topics --bootstrap-server kafka-3:29092 \
     --describe --topic "$TOPIC"
   PRODUCER_OUTPUT=$(docker exec kafka-3 kafka-producer-perf-test --topic "$TOPIC" \
     --num-records 1 --record-size 16 --throughput -1 \
     --producer-props bootstrap.servers=kafka-3:29092 acks=all \
       request.timeout.ms=5000 delivery.timeout.ms=10000 2>&1 || true)
   printf '%s\n' "$PRODUCER_OUTPUT"
   echo "$PRODUCER_OUTPUT" | grep -q 'NOT_ENOUGH_REPLICAS'
   echo "$PRODUCER_OUTPUT" | grep -q '^0 records sent'
   ```

   `kafka-producer-perf-test` puede salir con código 0 aunque no entregue ningún registro. La prueba solo pasa si la salida contiene `NOT_ENOUGH_REPLICAS` y `0 records sent`; compara los offsets con la captura inicial para confirmar que no hubo escritura:

   ```bash
   docker exec kafka-1 kafka-run-class kafka.tools.GetOffsetShell \
     --broker-list kafka-1:29092 --topic "$TOPIC" --time -1
   ```
3. Registrar el error esperado, offsets antes/después y estado del grupo. Confirmar que el mensaje fallido no se confirme como publicado.
4. Arrancar primero el segundo broker; esperar dos réplicas en ISR. Después arrancar el primero; esperar tres réplicas en ISR:

    ```bash
    docker start kafka-2
    docker inspect -f '{{.State.Health.Status}}' kafka-2
    docker start kafka-1
    docker inspect -f '{{.State.Health.Status}}' kafka-1
    ```
5. Verificar salud de los tres brokers, líderes, ISR, consumer groups y lag. Hacer una última publicación exitosa al topic temporal.
6. Eliminar el topic temporal y verificar que desapareció. No borrar ni recrear topics de negocio ni volúmenes:

   ```bash
   docker exec kafka-1 kafka-topics --bootstrap-server kafka-1:29092 \
     --delete --topic "$TOPIC"
   docker exec kafka-1 kafka-topics --bootstrap-server kafka-1:29092 \
     --list
   ```

Si un paso no recupera el estado esperado, detener la prueba, mantener los demás brokers activos y registrar `FALLA`; no seguir provocando fallas.

## 8. Regresión RabbitMQ y demostración transversal

Kafka no debe alterar RabbitMQ. Antes y después de las pruebas de broker:

- Revisar `docker exec rabbitmq1 rabbitmqctl cluster_status`: ambos nodos deben aparecer en `Running Nodes`, sin partición de red ni alarmas.
- Comprobar las tres colas académicas con consumidores, sus DLQ y los exchanges `direct`/`topic` requeridos. Ejecutar un mensaje sintético y confirmar ACK; en un entorno de pruebas, comprobar la ruta de DLQ.
- Verificar el frontend por API Gateway: login OIDC con usuario de prueba, JWT válido autorizado y peticiones sin token/token inválido rechazadas. No guardar tokens ni capturas de datos personales.
- Confirmar que la ruta de reportes del BFF devuelve las proyecciones generadas por los eventos.

El caso de prueba no sustituye la explicación de que la EC2 es un único dominio de falla.

## 9. Matriz de aceptación y cierre

| Evidencia | Criterio que cubre | Resultado requerido |
|---|---|---|
| Build backend/frontend y pruebas CI | Compilación y pruebas básicas del encargo | Verde; declarar por separado tests omitidos |
| Compose local y AWS validado | Kafka/ZooKeeper/brokers y Kafka UI | Servicios definidos, configuración parseable y healthchecks verdes |
| Kafka UI y metadatos | Tres brokers, dos topics, 3 particiones/3 réplicas | Metadatos exactos y autenticación Basic Auth probada |
| Publicación/consumo de ambos DTO | Productores, consumidores, claves y flujo | Eventos visibles, procesados y consultables en reportes |
| Consumer groups, offsets y lag | Administración y operación | Grupos correctos, offsets avanzan y lag se recupera |
| Errores y duplicados | Validación, reintentos, logs e idempotencia | Resultado observado coincide con política documentada |
| Uno y dos brokers fuera | Tolerancia a fallas, ISR y min ISR | Un broker: escritura de prueba; dos: rechazo esperado; recuperación completa |
| RabbitMQ antes/después | Kafka complementa sin romper colas previas | Cluster, colas, exchanges, ACK/DLQ sanos |
| API Gateway, JWT y login | Integración cloud de presentación | Inválidos rechazados y flujo autenticado válido demostrado |

Un indicador solo pasa a `100% demostrado` cuando su requisito de código está presente, las pruebas automáticas aplicables pasan y hay evidencia vigente del comportamiento requerido en el entorno. La ausencia de Kafka en AWS mantiene bloqueados los indicadores que requieren mostrar o probar Kafka UI y brokers.
