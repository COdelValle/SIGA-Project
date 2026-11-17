# Registro de pruebas y pasos

Bitácora reutilizable para dejar constancia de las pruebas ejecutadas y de los pasos
necesarios para reproducirlas, sesión a sesión. Complementa a
[`testing-login.md`](testing-login.md) (guía de login/infraestructura) y a
[`backend.md`](backend.md) (contratos y comportamiento).

> Cada entrada conserva el comportamiento observado en su fecha/commit y no es
> garantía del estado cloud continuo. Para el disparador CD vigente consulte
> [`infra/terraform/README.md`](../infra/terraform/README.md): push a `deploy` o
> `workflow_dispatch`; las menciones históricas a despliegue manual aplican a su
> sesión correspondiente.

## 1. Propósito y uso

- Registrar **qué se probó**, **cómo se probó**, **qué resultado dio** y **qué quedó
  pendiente**, con evidencia verificable (comandos, consultas y salidas resumidas).
- Servir de **runbook**: cualquier persona (o sesión de IA) puede repetir los pasos
  para validar una rama sin redescubrirlos.
- Mantener una **plantilla común** para que las entradas sean comparables entre
  sesiones.

Reglas de mantenimiento:

1. Agregar una entrada por sesión de prueba (o por hito relevante) al final de la
   sección 4, con el próximo ID disponible.
2. No borrar entradas antiguas: si una prueba deja de aplicar, marcar el estado y
   explicar por qué.
3. Nunca registrar secretos ni claves reales (contraseñas, tokens, `AZURE_CLIENT_SECRET`,
   `REGISTRO_CRED_KEY`). Los valores sensibles se referencian por nombre.
4. La evidencia debe poder reproducirse sin acceso a la sesión original.

### Convención de ramas y promoción

- **`dev`** concentra el trabajo integrado. Todo cambio llega por PR a `dev`; CI y CodeQL deben quedar en verde.
- **`deploy`** es la rama desplegable: solo recibe PR desde `dev`. Un push a `deploy` dispara el CD completo (build de imágenes, ECR y EC2), por lo que no se hace push directo.
- **`main`** es el espejo de lo que está desplegado. Se sincroniza por PR `deploy` → `main`, que no dispara CD; su contenido debe coincidir con la versión validada en AWS.
- **Excepción para documentación:** cambios que no alteran el código ejecutable ni infraestructura se acumulan en `dev` y se promueven junto al siguiente cambio funcional, para no pagar un CD completo por solo documentar (ver PR-20261116-21 y la sincronización `deploy` → `main` de PR #130).
- **Referencia de versión:** ante una entrega, registrar el SHA de `deploy` validado y, si corresponde, crear un tag anotado; el tag no dispara CD.

## 2. Convenciones

- **ID**: `PR-AAAAMMDD-NN` (fecha de la sesión de prueba + correlativo).
- **Estado**: `OK` (verificado), `PARCIAL` (parte verificada), `FALLA`, `BLOQUEADO`
  (falta un prerrequisito), `PENDIENTE` (no ejecutado).
- **Entorno**: rama + commit, y si aplica contenedores/orquestador.
- **Evidencia**: comando ejecutado + resultado resumido (no pegar salidas enormes).
- **Datos de prueba**: usar correos/RUT ficticios y borrarlos al final (registrar la
  limpieza).

## 3. Plantilla de entrada

```markdown
### PR-AAAAMMDD-NN — <título corto>

- **Fecha**: AAAA-MM-DD
- **Rama / commit**: `<rama>` @ `<commit corto>`
- **Alcance**: <qué se probó>
- **Entorno**: <Docker local / Testcontainers / CI / Graph real>
- **Prerrequisitos**: <variables, permisos, servicios>
- **Pasos**: <lista numerada reproducible>
- **Resultado**: <estado> — <detalle>
- **Evidencia**: <comandos + salida resumida>
- **Pendientes / notas**: <si aplica>
```

## 4. Entradas registradas

### PR-20261005-01 — Registro asíncrono de usuarios (E2E con Entra ID)

- **Fecha**: 2026-10-05
- **Rama / commit**: `copilot/explain-microservice-functionality` @ `847dcb4`
- **Alcance**: flujo completo SPA/BFF → `ms-usuarios-auth` → RabbitMQ → Graph +
  consumidores por rol, incluida credencial de un solo uso, `reset-password`, casos
  borde y limpieza.
- **Entorno**: Docker local con MariaDB 11.4, RabbitMQ 4 y tenant real
  (`platformsiga.onmicrosoft.com`).
- **Prerrequisitos**:
  - Graph con permisos de **aplicación** consentidos: `User.Read.All`,
    `User.ReadWrite.All`, `AppRoleAssignment.ReadWrite.All`.
  - Rol de directorio **User Administrator** asignado a la app (para
    `reset-password` y cuentas existentes). Para asignarlo se usó
    `RoleManagement.ReadWrite.Directory` de forma temporal.
  - `.env`: `REGISTRO_ASYNC_ENABLED=true` y `REGISTRO_CRED_KEY` (AES-256, generada
    con `openssl rand -base64 32`).
  - Token ADMIN del navegador (F12 → Network → `/api/bff/...` → `Authorization`).
- **Pasos**:
  1. Preflight: obtener token de aplicación y verificar en el claim `roles` los tres
     permisos de Graph.
  2. Activar el flag y recrear `ms-usuarios-auth`; verificar `healthy`.
  3. `POST /api/bff/v1/admin/registraciones` con un ESTUDIANTE de prueba
     (`prueba.e2e@…`, RUT `21000001-1`); polling del `processId`.
  4. Verificar cuenta en Graph (`accountEnabled`, `forceChangePasswordNextSignIn`),
     app role asignado, `Usuario` `ACTIVO`, invitación `VINCULADA`, perfil en
     `siga_estudiantes_db`, outbox `ENVIADO`, `azureState`/`domainState`
     `COMPLETADO`.
  5. Obtener la credencial una vez; repetir la lectura (debe fallar) y probar
     `reset-password`.
  6. Repetir con un APODERADO (vinculado al estudiante id 1) y ejecutar casos borde:
     RUT inválido (`400`), correo duplicado (`409`), apoderado con estudiante
     inexistente (`400` inmediato por Feign).
  7. Limpieza: `DELETE` de las cuentas de prueba en Entra + filas locales; flag a
     `false`.
- **Resultado**: `OK` — flujo completo verificado (el alta de ESTUDIANTE tardó ~9 s).
  `reset-password` y la reutilización de cuentas existentes solo funcionaron tras
  asignar el rol **User Administrator** a la app (hallazgo registrado en
  `testing-login.md`, sección 2).
- **Evidencia**:
  - 12 colas + DLQ declaradas y consumidores conectados
    (`rabbitmqctl list_consumers`).
  - Flyway `V5`–`V7` aplicadas sobre MariaDB real con `ddl-auto=validate` OK.
  - Consultas a `user_registration_processes`, `user_registration_attempts` y
    `user_registration_outbox` mostrando el ciclo completo.
  - Asignación de app role con reintento acotado por *eventual consistency* de
    Entra (400 recuperado y visible en logs).
- **Pendientes / notas**: el envío de la clave por correo queda reservado
  (`user.credentials.notify` + `REGISTRO_NOTIFY_CREDENTIALS_ENABLED=false`). Los
  secretos de producción (`REGISTRO_ASYNC_ENABLED`, `REGISTRO_CRED_KEY`) deben
  crearse en GitHub Actions (misma clave entre despliegues).

### PR-20261006-02 — CRUD admin, correo automático y normalización de datos

- **Fecha**: 2026-10-06
- **Rama / commit**: `copilot/explain-microservice-functionality` @ `847dcb4`
- **Alcance**: select de clases del año, buscador de alumnos `q`, botón Eliminar con
  offboarding en Entra y reactivación, `usuarios.full_name` (V8) + backfill,
  detalle "Ver", generación automática de correo/nombre, estándar de RUT y nombres
  (migraciones V7/V7/V5/V9) y mejora visual del admin.
- **Entorno**: Docker local; verificación de datos contra MariaDB real.
- **Pasos**:
  1. Verificar en OpenAPI los endpoints admin (`/admin/clases`, `/admin/estudiantes`,
     `/admin/usuarios/{id}`, `DELETE`, `/admin/registraciones`).
  2. En la UI (`/admin/usuarios`): crear estudiante con el select de clases 2026;
     crear apoderado buscando por RUT (`22.126.386-3`), RUT sin puntos y nombre;
     eliminar un usuario y re-registrar el mismo correo (reactivación).
  3. Verificar en BD: `StateUsuario=INACTIVO` tras eliminar, cuenta deshabilitada en
     Graph, y `full_name` poblado en las 65 cuentas.
  4. Ejecutar `tools/normalizar-datos.ps1` para data real previa y verificar
     `0` RUT con puntos y `0` nombres en mayúsculas.
- **Resultado**: `OK` — 6/6 contenedores `healthy`; migraciones `V7`(estudiantes),
  `V7`(docentes), `V5`(apoderados) y `V9`(usuarios) aplicadas; 65/65 `full_name`;
  el caso `del Valle` se corrigió con la regla de partículas siempre en minúscula.
- **Evidencia**:
  - `mvn package` de 12 módulos `BUILD SUCCESS`; `tsc` + 11 tests + build de
    producción del frontend OK.
  - Consultas de verificación: RUT con puntos = 0, nombres en mayúsculas = 0,
    `usuarios.full_name` 65/65.
- **Pendientes / notas**: la propagación de la desactivación a los perfiles de
  dominio por evento (`user.account.disabled/enabled`) queda como iteración futura;
  "estudiante requiere apoderado" sigue fuera de alcance.

### PR-20261006-03 — Fix de fechas en Feign (asistencias del docente)

- **Fecha**: 2026-10-06
- **Rama / commit**: `copilot/explain-microservice-functionality` @ `847dcb4`
- **Alcance**: `GET /api/bff/v1/asistencias/asignatura/{id}?fecha=…` devolvía `400`
  porque OpenFeign serializaba `from`/`to` como fecha corta localizada (`6/10/26`).
- **Entorno**: Docker local (portal docente).
- **Prerrequisitos**: stack con `bff-web` y `ms-asistencias`.
- **Pasos**:
  1. Reproducir el error en la grilla de asistencias y confirmar en logs de
     `ms-asistencias` el mensaje `El parametro 'from' tiene un formato invalido`.
  2. Corregir con `SharedFeignFormatConfig` en `core-share` (registra
     `yyyy-MM-dd`, `HH:mm:ss` y `yyyy-MM-dd HH:mm:ss` en el conversion service de
     Feign) y cubrir con `SharedFeignFormatConfigTest`.
  3. Reconstruir `bff-web` y verificar que la clase queda en el fat jar.
  4. Recargar la página del docente y confirmar `200` (o "sin registros") y la
     ausencia del error en logs.
- **Resultado**: `PARCIAL` — fix compilado, test en verde y desplegado; la
  confirmación en vivo quedó pendiente de la recarga del usuario al cierre de la
  sesión.
- **Evidencia**: `SharedFeignFormatConfigTest` en verde; clase presente en
  `core-share` y en el fat jar de `bff-web`; contenedor recreado `healthy`.
- **Pendientes / notas**: no lo causaban los cambios de normalización; era un bug
  preexistente y `AsistenciaClient` es el único cliente Feign que enviaba fechas.

### PR-20261007-04 — Bandeja académica in-app y mensajería RabbitMQ

- **Fecha**: 2026-10-07
- **Rama / commit**: `dev` @ `a34411b` + cambios locales sin commit.
- **Alcance**: outbox transaccional de notas/evaluaciones/asistencias; colas y DLQ;
  persistencia idempotente en `ms-notificaciones`; aislamiento de lectura por OID;
  API BFF y campana compartida. Sin correo externo.
- **Entorno**: Docker Compose local, RabbitMQ 4, MariaDB 11.4; destinos sintéticos
  con ID fuera del dominio y cuentas reales no consultadas.
- **Prerrequisitos**: `.env` local; `docker compose config --quiet`; token Entra
  de ESTUDIANTE y APODERADO vinculado para completar el E2E de UI (pendiente).
- **Pasos**:
  1. Ejecutar las pruebas backend de los módulos afectados con las mismas
     exclusiones de contextos Azure del runbook 5.2; ejecutar `tsc` y Vitest.
  2. Reconstruir de forma gradual `ms-asistencias`, `ms-evaluaciones`, `ms-notas`,
     `ms-notificaciones`, `bff-web` y `frontend`, conservando los volúmenes.
  3. Verificar exchange, colas, bindings, consumidores y DLQ con `rabbitmqctl`;
     comprobar `healthy` en todos los contenedores.
  4. Insertar eventos sintéticos en cada outbox de dominio: los tres cambiaron a
     `ENVIADO` (0 intentos fallidos), llegaron a RabbitMQ y generaron una fila de
     bandeja por tipo en `siga_notificaciones_db`.
  5. Reenviar el mismo evento de nota tres veces con el mismo `idEvento` y
     comprobar que existe una sola fila; enviar payloads malformados y confirmar
     su llegada a `cola-notificaciones-notas.dlq` después de los reintentos.
  6. Simular una clave de routing sin binding con el octavo intento del outbox:
     el registro terminó `FALLIDO` con el motivo de RabbitMQ, sin crear una
     notificación huérfana.
  7. Consultar sin token los endpoints BFF y de `ms-notificaciones` (ambos
     devolvieron `401`). Limpiar los eventos sintéticos y purgar únicamente la
     DLQ usada por la prueba.
- **Resultado**: `PARCIAL` — Docker/MariaDB/RabbitMQ, outbox, enrutamiento,
  persistencia, deduplicación, DLQ y builds comprobados. Queda pendiente iniciar
  sesión como estudiante/apoderado y verificar visualmente la campana y el
  aislamiento real entre dos cuentas Entra.
- **Evidencia**:
  - Backend: `mvn package` del reactor completo `BUILD SUCCESS`; 188 pruebas
    contabilizadas, 0 fallas/errores y 8 omitidas por Testcontainers. Las suites
    `AsyncUserRegistrationServiceTest` (11 pruebas) y las nuevas pruebas de
    mensajería también pasaron.
  - Las suites Testcontainers no pudieron usar el daemon Docker desde Java en
    Windows; las migraciones V1 de la bandeja y V3 de los productores sí se
    aplicaron en los contenedores Compose reales.
  - Frontend: `npx tsc -p tsconfig.app.json --noEmit` correcto, Vitest 15/15 y
    `npm run build -- --configuration production` exitoso en la reconstrucción de
    la imagen.
  - `docker compose ps`: servicios, `mariadb-notificaciones` y RabbitMQ `healthy`.
  - Exchange topic durable, tres colas durables con un consumidor cada una,
    bindings esperados y tres DLQ durables; profundidad final de colas y DLQ = 0.
  - Los eventos sintéticos fueron borrados: filas outbox de prueba = 0 y filas
    de bandeja del destino sintético = 0.
- **Pendientes / notas**: E2E autenticado de campana, roles y privacidad por OID;
  regresión funcional del flujo de registro de usuarios/auth con cuentas de
  prueba. No se probó ni habilitó Gmail/Exchange/SES. Una reconstrucción inicial
  en paralelo agotó Docker Desktop y devolvió HTTP 500; se recuperó el engine y
  se terminó la reconstrucción gradualmente, sin borrar volúmenes.

### PR-20261008-05 — Correcciones de la campana: PATCH, contraste, hora y mensajes

- **Fecha**: 2026-10-08
- **Rama / commit**: `dev` @ `a34411b` + cambios locales sin commit.
- **Alcance**: hallazgos del primer E2E visual: (1) el clic no marcaba leída,
  (2) texto ilegible por colores fuera del tema, (3) fecha/hora en UTC y formato
  en-US, (4) mensajes demasiado genéricos. Se agrega además "Marcar todas como
  leídas", "Limpiar mis leídas", purga a 90 días y `TZ=America/Santiago`.
- **Entorno**: Docker Compose local; sesión real del portal estudiante.
- **Prerrequisitos**: stack reconstruido con las imágenes nuevas; `.env` local.
- **Pasos**:
  1. Reproducir el fallo del clic: logs de `bff-web` con
     `Invalid HTTP method: PATCH executing PATCH http://ms-notificaciones:8091/...`
     (Feign usa `HttpURLConnection`, que no soporta PATCH). Confirmar en la
     captura la fila "no leída" con texto claro sobre fondo claro y la fecha
     `10/8/26, 2:47 AM` (UTC + locale en-US).
  2. Agregar `io.github.openfeign:feign-hc5` a `bff-web` y
     `spring.cloud.openfeign.httpclient.hc5.enabled: true`; comprobar que
     `feign-hc5` queda dentro del fat jar.
  3. Reescribir los estilos de la campana con tokens del tema
     (`bg-panel`, `text-ink`, `text-muted`, `border-line`, `bg-surface`,
     `border-brand`) y formato `dd/MM/yyyy HH:mm`.
  4. Fijar `TZ: America/Santiago` en el entorno común de microservicios (Compose
     local y plantilla de Terraform) y recrear; `date` dentro de los contenedores
     pasa a `-03`.
  5. Enriquecer los eventos con `nombreEvaluacion`/`nombreAsignatura` en los
     productores (Feign con fallback: si el nombre no se puede resolver, el aviso
     se emite igual) y redactar textos específicos en `ms-notificaciones`, sin
     exponer la nota.
  6. Migración `V2` (`ocultada_en`), endpoints `PATCH/DELETE .../me/leidas` y
     purga programada (`siga.notificaciones.retencion-dias=90`).
  7. Ejecutar pruebas backend/frontend, reconstruir imágenes y recrear el stack.
- **Resultado**: `PARCIAL` — corregido y verificado por API/logs/BD; falta el
  clic real autenticado del usuario para confirmar la UI (contraste, contador y
  botones).
- **Evidencia**:
  - Reactor completo `mvn package` `BUILD SUCCESS`: 195 pruebas contabilizadas,
    0 fallas/errores y 8 suites Testcontainers omitidas en Windows. Módulos
    afectados: core-share 38, bff-web 36, ms-notas 13, ms-evaluaciones 10,
    ms-asistencias 17 y ms-notificaciones 12. Frontend Vitest 17/17 y `tsc`
    correcto.
  - `feign-hc5` presente en `app.jar` del BFF (grep del jar reconstruido).
  - Migración `V2 - agregar ocultamiento lecturas` aplicada en
    `siga_notificaciones_db`; todos los contenedores `healthy`.
  - `docker exec ms-notificaciones date` → `-03` (America/Santiago).
  - Evento sintético de nota con nombres (verificado tras el rename): la bandeja
    guardó `Calificación agregada en Ciencias Naturales` y
    `Se agregó una calificación en «EXAMEN» de Ciencias Naturales.` con hora de
    Chile; los datos sintéticos se limpiaron. El texto de título
    `Nueva calificación` se renombró a `Calificación agregada` a pedido del
    usuario; aplica solo a eventos nuevos.
  - `PATCH /api/v1/notificaciones/me/1/leida` y `PATCH/DELETE .../me/leidas`
    responden `401` sin token (ruta reconocida, ya no `405`).
- **Pendientes / notas**: validar en navegador el clic, "Marcar todas" y
  "Limpiar leídas" con una cuenta real; la purga a 90 días solo se probó con
  pruebas unitarias y no se espera a su horario programado. Decisión de producto:
  las leídas se conservan hasta la limpieza o purga.

### PR-20261008-06 — Bandeja: rutas faltantes, refresco en vivo y errores de acciones

- **Fecha**: 2026-10-08
- **Rama / commit**: `dev` @ `a34411b` + cambios locales sin commit.
- **Alcance**: tras la prueba manual del usuario: (1) "Marcar todas" parecía
  limpiar y "Limpiar leídas" mostraba "No se pudieron cargar las notificaciones.",
  (2) el contador solo se refrescaba cada 60 s y no al volver a la pestaña,
  (3) los ítems desaparecían ante un fallo de acción.
- **Causa raíz**: el BFF llamaba `PATCH/DELETE /api/v1/notificaciones/me/leidas`
  pero `ms-notificaciones` no declaraba esas rutas → `404` → el fallback del BFF
  devolvía `503` y el frontend reemplazaba la lista por el mensaje de error.
  Lección: un `401` sin token **no** prueba que la ruta exista; la verificación
  correcta es `/v3/api-docs` (público).
- **Pasos**:
  1. Confirmar en logs `bff-web`: `404 ... PATCH/DELETE .../me/leidas` y revisar
     el OpenAPI de `ms-notificaciones` (faltaba la ruta).
  2. Agregar `@PatchMapping("/me/leidas")` y `@DeleteMapping("/me/leidas")` al
     controlador de `ms-notificaciones` + `NotificacionControllerMappingsTest`
     (regresión por reflexión).
  3. Contador cada 10 s, refresco en `focus`/`visibilitychange`, sincronización
     al abrir la campana y recarga automática de la lista si el panel está
     abierto. Pulso visual ~4 s en campana/badge al subir el contador.
  4. Caché TTL 3 min por OID en `ResolverDestinatariosNotificacion` para que el
     polling no repita llamadas Feign (los cambios de vínculos tardan hasta el
     TTL en reflejarse).
  5. Separar `errorCarga` de `errorAccion`: un fallo de acción mantiene la lista
     y muestra un mensaje inline; "Limpiar leídas" sin pendientes deja
     "No tienes notificaciones." y nunca el mensaje de error.
  6. Reconstruir `ms-notificaciones` y `frontend`, recrear y verificar por OpenAPI.
- **Resultado**: `PARCIAL` — corregido y verificado por tests/OpenAPI/logs; falta
  la confirmación visual del usuario (pulso, refresco ≤10 s, botones).
- **Evidencia**:
  - OpenAPI de `ms-notificaciones` y del BFF listan
    `/api/v1/notificaciones/me/leidas` y `/api/bff/v1/notificaciones/me/leidas`
    con métodos `patch` y `delete`.
  - Backend: `ms-notificaciones` 14 pruebas (1 omitida por Testcontainers),
    incluida la de mapeos y la de caché del resolver. Frontend: Vitest 20/20
    (fallo de acción con lista intacta, limpiar sin error, marcar todas conserva
    ítems, refresco al abrir). Todos los contenedores `healthy`.
- **Pendientes / notas**: validar el pulso y el refresco en navegador con cuenta
  real; el nombre "Limpiar leídas" se mantiene a pedido del usuario. La caché de
  3 min aplica a la visibilidad de vínculos/inscripciones en la bandeja.

### PR-20261008-07 — Clúster RabbitMQ de dos nodos, ACK manual y API de administración

- **Fecha**: 2026-10-08
- **Rama / commit**: `dev` + cambios locales sin commit (sobre `2cff2f0`)
- **Alcance**: (1) clúster de **dos nodos RabbitMQ** (`rabbitmq1`/`rabbitmq2`) en
  Docker Compose local y plantilla AWS sobre la misma EC2; (2) topología
  centralizada en `core-share` con **direct exchange** `siga.dlx.direct` para las
  DLQ; (3) **ACK/NACK manuales** (`ConfirmadorMensajes`) en todos los
  consumidores, con reintentos acotados en memoria y rechazo a DLQ; (4) nuevo
  microservicio **`ms-rabbitmq-admin`** con API REST validada y fachada en el BFF.
- **Entorno**: Windows + Docker Desktop; Maven 3.9.9, JDK 21. Sin despliegue a AWS.
- **Prerrequisitos**: `.env` local con `RABBITMQ_USER`/`RABBITMQ_PASS`;
  `RABBITMQ_ERLANG_COOKIE` opcional (por defecto `siga-dev-cookie` en local/demo).
- **Pasos**:
  1. Compilar el reactor completo y ejecutar los tests unitarios con las mismas
     banderas del CI (sección 5.2).
  2. Levantar solo el clúster: `docker compose up -d rabbitmq1 rabbitmq2`.
  3. Verificar la unión de nodos con `docker exec rabbitmq1 rabbitmqctl cluster_status`.
  4. Arrancar `ms-rabbitmq-admin` (jar) apuntando a `localhost:5672` y consultar
     `/actuator/health` para forzar la conexión que dispara la declaración de
     topología.
  5. Listar exchanges, colas y bindings declarados; comprobar el 401 sin token en
     la API de administración.
  6. Detener los contenedores de prueba (`docker compose stop rabbitmq1 rabbitmq2`).
- **Resultado**: `OK` — el clúster queda formado y la topología declarada coincide
  con la especificación; la API de administración exige autenticación. La
  medición de memoria en la EC2 y el E2E con JWT de Entra quedan pendientes.
- **Fix durante la verificación**: con ACK manual, el retry del contenedor dejaba
  el mensaje **retenido** al agotar reintentos (`RejectAndDontRequeueRecoverer`
  envuelve la excepción y el contenedor no rechaza en modo manual). Se desactivó
  `spring.rabbitmq.listener.simple.retry.enabled=false` en los cinco servicios
  consumidores (y `SPRING_RABBITMQ_LISTENER_SIMPLE_RETRY_ENABLED=false` en el
  entorno común de Compose): los reintentos viven en `ConfirmadorMensajes` y un
  error de conversión ahora es fatal y deriva de inmediato a la DLQ.
- **Evidencia**:
  - Backend `BUILD SUCCESS` (14 módulos, incluye `ms-rabbitmq-admin`); tests
    unitarios en verde: `ConfirmadorMensajesTest` 4/4,
    `ConfiguracionTopologiaNotificacionesTest` 3/3,
    `SolicitudRabbitValidationTest` 7/7, `RabbitAdminServiceTest` 10/10,
    `NotificacionServiceTest` 10/10 (2 nuevos de evento inválido).
  - `cluster_status`: `Disk Nodes`/`Running Nodes` con `rabbit@rabbitmq1` y
    `rabbit@rabbitmq2`; sin particiones de red.
  - `list_exchanges`: `intercambio-notificaciones` (topic) y `siga.dlx.direct`
    (direct). `list_queues`: 3 colas académicas + 3 DLQ.
  - `list_bindings`: `nota.*`, `asistencia.*`, `evaluacion.*` hacia las colas y
    cada DLQ enlazada a `siga.dlx.direct` con su propio nombre como routing key.
  - `GET http://localhost:8092/api/v1/rabbitmq/queues/...` sin token → `401`.
  - Cookie Erlang: valor propio en `.env` y secret `RABBITMQ_ERLANG_COOKIE`
    creado en GitHub (repo `COdelValle/SIGA-Project`); ambos contenedores
    reiniciados con esa cookie y `erlang:get_cookie()` idéntico en los dos nodos.
  - Prueba E2E de DLQ: mensaje malformado publicado a `intercambio-notificaciones`
    (routing `nota.creada`) → `MessageConversionException` fatal →
    `AmqpRejectAndDontRequeueException: Error Handler converted exception to fatal`
    → 1 mensaje en `cola-notificaciones-notas.dlq`; luego purgado.
  - Prueba E2E de ACK: `EventoNota` válido publicado al mismo exchange →
    consumido (cola en 0) y persistido en `siga_notificaciones_db` como
    `NOTA/CREADA`; el dato sintético (`id_destino=999999`) se eliminó.
  - Prueba E2E de NACK de negocio: evento sin `idEstudiante` →
    `EventoInvalidoException` → NACK sin requeue (`basicNack(false)`) → 1 mensaje
    en `cola-notificaciones-notas.dlq`; luego purgado.
  - Stack completo `healthy` (11 microservicios, 10 bases, BFF, frontend y los dos
    nodos RabbitMQ); `mvn package` con tests en verde; frontend `tsc` y Vitest
    28/28.
- **Pendientes / notas**: la migración de topología cambia los argumentos de las
  colas existentes (`x-dead-letter-exchange`), por eso el clúster usa volúmenes
  nuevos (`rabbitmq1_data`/`rabbitmq2_data`) y el volumen `rabbitmq_data` previo
  queda como respaldo. Falta el despliegue a AWS (directorios
  `siga-data/rabbitmq1|2` y medición de memoria en la EC2).

### PR-20261008-08 — Refresco de notas/asistencias al llegar la notificación

- **Fecha**: 2026-10-08
- **Rama / commit**: `dev` + cambios locales sin commit (sobre `5f04189`)
- **Alcance**: el apoderado recibía la notificación de la nota nueva, pero la
  tabla de Notas seguía mostrando solo Nota 1–3 (la evaluación nueva no
  aparecía) hasta recargar la página. En el portal estudiante sí se veía.
- **Causa raíz**: caché en el frontend, no en el backend. El BFF arma el perfil
  del pupilo con el mismo flujo que el del estudiante
  (`PerfilEstudianteService.java:59-68`) y `periodoDePerfil()` ya genera columnas
  dinámicas; el problema era que `PerfilEstudianteService`/`AsistenciaService`
  guardan el resultado con `shareReplay` y `ApoderadoStateService` los pide al
  cargar el layout (barra de pupilo) y los mantiene suscritos para siempre. Al
  hacer clic en la campana solo se navegaba (`notification-bell.component.ts`),
  sin invalidar ni recargar. El estudiante lo veía porque su sesión se cargó
  después del guardado.
- **Pasos**:
  1. Nuevo `RefrescoDatosService` en `@siga/core` (aviso global `refresco$`).
  2. `PerfilEstudianteService` y `AsistenciaService`: cada clave del caché se
     reconsulta con `refresco$.pipe(startWith(...), switchMap(http), shareReplay
     ({bufferSize: 1, refCount: true}))`, de modo que las suscripciones activas
     (signals de las vistas) reciben los datos frescos sin tocar cada página.
  3. `NotificationBellComponent`: `solicitarRefresco()` al hacer clic en una
     notificación y cuando el contador de no leídas sube (llega algo nuevo);
     así la vista abierta se actualiza en ≤10 s (polling) sin recargar.
  4. Tests: `perfil-estudiante.service.spec.ts` (comparte petición, refresco e
     invalidación), caso de refresco en `asistencia.service.spec.ts` y
     refresco por clic/contador en `notification-bell.component.spec.ts`.
  5. Registrar el fix en este documento.
- **Resultado**: `PARCIAL` — verificado por tests unitarios y build; falta la
  confirmación visual del usuario con cuentas reales (apoderado en Notas mientras
  el docente guarda una evaluación nueva).
- **Evidencia**:
  - Frontend: Vitest en verde (incluye los 3 casos nuevos de refresco) y build de
    producción correcto.
  - Sin cambios de backend: el perfil ya devolvía las evaluaciones nuevas; la
    corrección es solo de frescura en el cliente.
  - **Despliegue**: el contenedor `frontend` seguía sirviendo el bundle previo
    (`Oct 8 05:06`, sin `solicitarRefresco`), por eso la prueba manual no veía el
    refresco. Se actualizó la imagen `siga-project-frontend:latest` con el build
    de producción verificado y se recreó el contenedor; `grep -l solicitarRefresco`
    sobre `/usr/share/nginx/html/*.js` ahora lo encuentra
    (`main-J3ETFQP5.js`, `chunk-B07Zy6_a.js`) y `http://localhost:4200` responde
    `200` con ese `main`.
  - `docker compose up -d --build frontend` (Dockerfile con `npm ci` + `ng build`)
    tumbó el engine de Docker Desktop a los ~25 min (500 en la API), como advierte
    la sección 6; la imagen se generó desde el `dist/frontend/browser` ya
    compilado y verificado, con el mismo `nginx.conf` y healthcheck.
- **Pendientes / notas**: validar en navegador con sesión de apoderado abierta en
  `/apoderado/notas`: al guardar la nota, la columna "Nota 4" debe aparecer en
  ≤10 s o al hacer clic en la campana, sin F5. Hacer Ctrl+F5 una vez tras
  desplegar (nginx sirve los JS con `immutable` y el `index.html` puede estar
  cacheado). Reconstruir la imagen con el Dockerfile normal
  (`docker compose up -d --build frontend`) cuando el engine esté estable y sin
  otras stacks corriendo. Efecto secundario aceptado: al remontar una página se
  reconsulta el perfil/asistencias (más peticiones, datos siempre frescos).

### PR-20261009-08 — Despliegue AWS real con clúster RabbitMQ y administrador

- **Fecha**: 2026-10-09
- **Rama / commit**: `feat/rabbitmq-cluster-admin` @ `de16c31` (+ cambios locales de esta sesión)
- **Alcance**: despliegue completo en AWS Academy (EC2 + EBS + API Gateway + ECR) del
  stack con **clúster RabbitMQ de dos nodos** y `ms-rabbitmq-admin`; corrección de los
  hallazgos que aparecieron durante el despliegue.
- **Entorno**: AWS `us-east-1`; EC2 `t3.large` (EIP `52.54.139.252`); API Gateway
  `https://87hjovxzud.execute-api.us-east-1.amazonaws.com`; Docker Compose en la EC2.
- **Prerrequisitos**: credenciales del Learner Lab vigentes (locales y en GitHub) y
  `labsuser.pem` para SSH.
- **Pasos**:
  1. `terraform apply`: crea los 7 repos ECR faltantes y actualiza API Gateway (CORS)
     y la instancia en caliente; sin destrucciones.
  2. `gh workflow run cd.yml --ref feat/rabbitmq-cluster-admin`: build/push de 13
     imágenes ECR y deploy por SSH.
  3. Corregir incidencias del despliegue (ver abajo) hasta que los 16 contenedores
     queden `healthy`.
  4. Validar clúster, colas/consumidores, endpoints públicos y E2E de ACK/DLQ.
- **Resultado**: `OK` — stack completo `healthy`; clúster con **18 colas en `running`** y
  consumidores en las colas de registro y académicas; DLQ y ACK verificados en la nube.
- **Evidencia**:
  - `cluster_status`: `rabbit@rabbitmq1` y `rabbit@rabbitmq2` running, sin particiones.
  - `list_queues name state`: 18 colas `running`; `list_queues name consumers` con 1
    consumidor por cola de registro y académica (tras reiniciar los consumidores).
  - API Gateway: SPA `200` con `<app-root>`, `config.json` servido y `/api/me` sin
    token → `401`.
  - DLQ en nube: mensaje malformado → 1 en `cola-notificaciones-notas.dlq` (purgado);
    `EventoNota` válido → cola en 0 y fila `NOTA/CREADA` en `siga_notificaciones_db`
    (dato sintético eliminado).
  - Recursos: `t3.large` con 5.5/7.8 GB usados y swap sin uso; cada nodo RabbitMQ
    ~90 MB. 16 contenedores en la EC2.
  - Run final del CD completo (`workflow_dispatch`) **en verde**: run
    `37878148812`; el redeploy recreó los servicios con las imágenes nuevas y los
    consumidores quedaron registrados (1 por cola de registro y académica).
- **Incidencias corregidas**:
  1. Maven Central `429` transitorio en el primer build (re-run del workflow).
  2. `PREPARE` con dos statements en el CD → separados en `DROP`/`CREATE` (`dc4943f`).
  3. Grants faltantes de `siga_estudiantes_db`/`siga_notas_db` → el CD crea y otorga
     las 10 bases de forma idempotente (`de16c31`).
  4. Checksums Flyway desalineados: `siga_asignaturas_db` recreada una vez
     (V1–V4 aplicadas limpias) y `siga_usuarios_db` reparada por SQL (V2).
  5. El AMI más reciente forzaba el reemplazo de la EC2 → `lifecycle ignore_changes`
     de `ami` (`33b5d28`).
  6. Cookie Erlang del volumen desalineada con la variable → se alineó el archivo y el
     CD ahora la pre-escribe antes del arranque de los nodos.
  7. Listeners detenidos por `missingQueuesFatal` durante el arranque en frío →
     `missing-queues-fatal=false` + reinicio de los consumidores.
- **Pendientes / notas**: los secrets `AZURE_*` de GitHub apuntan a otro tenant
  (`afad9bb4`/`c3cfc64e`) distinto del local (`448f165b`/`f260a804`); el login de la
  nube usa el de los secrets. El CD sigue siendo manual (`workflow_dispatch`) porque
  las credenciales del lab expiran (~4 h).

### PR-20261010-09 — Datos locales en AWS, panel RabbitMQ público y respaldo diario

- **Fecha**: 2026-10-10
- **Rama / commit**: `feat/rabbitmq-cluster-admin` @ `4c366d9` (+ fixes de esta sesión)
- **Alcance**: (1) reemplazo de las 10 bases de AWS por las locales (persistentes);
  (2) panel de RabbitMQ público por HTTPS con Caddy + basic auth; (3) respaldo
  diario de bases y definiciones en el EBS; (4) fixes del CD detectados durante el
  despliegue.
- **Entorno**: AWS `us-east-1` (EC2 t3.large, EIP `52.54.139.252`), API Gateway y
  Docker Compose; dumps generados desde el stack local.
- **Pasos**:
  1. `mariadb-dump` de las 10 bases locales (incluye `flyway_schema_history`) y
     `scp` a la EC2.
  2. Respaldo de las 10 bases de AWS en `siga-data/backups/pre-sync-*`.
  3. App detenida, `DROP/CREATE` + `GRANT` + import por base; reinicio vía CD.
  4. `terraform apply` (SG 443) + CD: Caddy, cron de respaldo y limpieza de imágenes.
  5. Verificación de conteos, login por el Gateway, panel (401/200) y respaldo manual.
- **Resultado**: `OK` — las 10 bases de AWS quedaron idénticas a local, sin cuentas
  `@genesisfloress`; panel público operativo; cron instalado por el CD.
- **Evidencia**:
  - Conteos AWS == local: usuarios 66 · estudiantes 41 · asignaturas 22 · notas 1183 ·
    docentes 26 · apoderados 2 · clases 13 · evaluaciones 396 · asistencias 370 ·
    notificaciones 45; `admin@platformsiga` ADMIN/ACTIVO; genesis = 0.
  - Panel `https://52-54-139-252.sslip.io`: `401` sin credenciales y `200` con
    `panel` + basic auth (Caddy `health=healthy`).
  - `crontab -l` con la línea del respaldo; ejecución manual de `backup-bd.sh`:
    10 dumps + `rabbitmq-definiciones.json` en `siga-data/backups/diario/2026-10-10`.
  - Clúster con 2 nodos y consumidores activos; disco raíz 55% tras limpiar 11.1 GB
    de imágenes antiguas.
- **Incidencias corregidas**: secret `AWS_REGION` inválido (re-creado); healthcheck
  con `start_period` 240 s (los JVM tardan ~100 s y compose abortaba); limpieza de
  imágenes no usadas (>24 h) en el CD; instalación de cron más robusta con evidencia
  en el log; **issuer del JWT Authorizer** (v2 por defecto vs. tokens v1 de la app:
  el Gateway devolvía `401 issuer does not match` antes de llegar al BFF; se alineó
  `azure_issuer` a `https://sts.windows.net/<tenant>/` en `3d46ec1` y se verificó
  `GET /api/me` con el token real → `200` con `roles:["ADMIN"]`). Si la app pasa a
  emitir tokens v2 (`accessTokenAcceptedVersion = 2`), volver a la URL
  `login.microsoftonline.com/<tenant>/v2.0`.
- **Pendientes / notas**: la sincronización es **unidireccional local → AWS**: volver
  a ejecutarla pisa lo creado en la nube. Las credenciales del lab expiran (~4 h) y
  un **Reset** del laboratorio borra el EBS.

### PR-20261011-10 — Admin protegido, solo SUMATIVA pondera y panel sin basic auth

- **Fecha**: 2026-10-11
- **Rama / commit**: `feat/rabbitmq-cluster-admin` @ `fa0a819` (`8a3a610`,
  `5e7a6bd`, `45115f0` + este)
- **Alcance**: tres fixes pedidos en la prueba anterior:
  (1) el ADMIN no puede eliminar su propia cuenta ni al último administrador
  activo; (2) solo las evaluaciones **SUMATIVA** ponderan en la nota final
  (FORMATIVA/DIAGNOSTICO pueden tener nota, pero no influyen); (3) el panel de
  RabbitMQ ya no usa `basic_auth` de Caddy: dos capas HTTP Basic en el mismo
  origen comparten el header `Authorization` y causaban el bucle de credenciales.
- **Entorno**: local (Maven/Vitest/build) + AWS (EC2 `t3.large`,
  `https://52-54-139-252.sslip.io`), CD run `38110529344` en verde.
- **Pasos**:
  1. `UsuarioService.deleteUsuario`: rechazo de auto-eliminación (oid del token) y
     del último ADMIN activo (`countByRolAndState`); `admin/usuarios` oculta
     "Eliminar" en la fila propia vía `MeService`.
  2. `EvaluacionService`: las no sumativas se guardan con ponderación 0 y las
     SUMATIVA exigen > 0; el acumulado ≤100% solo suma SUMATIVAS. Migración `V4`
     pone en 0 las FORMATIVA/DIAGNOSTICO existentes (SUMATIVAS de seeds se
     mantienen en 80%). Frontend docente deshabilita el % en no sumativas y
     `promedioNotas` (PONDERADO) ignora ponderación 0.
  3. CD: Caddyfile sin `basic_auth`; se agrega `docker compose restart caddy`
     tras el deploy porque el bind mount no recrea el contenedor.
  4. Pruebas y despliegue: reactor `mvn package`, `tsc`, Vitest y build de
     producción; push + `gh workflow run cd.yml`; verificaciones en la EC2.
- **Resultado**: `PARCIAL` — backend, migración y panel verificados en AWS; la
  confirmación visual (fila propia sin "Eliminar", DELETE propio rechazado y
  evaluación FORMATIVA que no mueve el promedio) queda para la sesión del usuario.
- **Evidencia**:
  - Backend: `mvn package` del reactor completo `BUILD SUCCESS`;
    `UsuarioServiceTest` 18/18 (3 nuevos: propia cuenta, último admin, con dos
    admins permite baja) y `EvaluacionServiceTest` 10/10 (3 nuevos: SUMATIVA en 0
    rechazada, no sumativa forzada a 0, acumulado ignora no sumativas).
  - Frontend: `tsc` correcto, Vitest **35/35** (specs nuevos de `promedioNotas`,
    `registrar-notas` y `admin/usuarios`) y build de producción OK.
  - AWS: `flyway_schema_history` de `siga_evaluaciones_db` con
    `4 ponderacion solo sumativa` en `success=1`; activas FORMATIVA 0 (129),
    SUMATIVA 20/30/50 (2/131/129); `admin@platformsiga` ADMIN/ACTIVO.
  - Panel: `https://52-54-139-252.sslip.io/` sin credenciales → `200` (ya no
    `401` con `Www-Authenticate: Basic realm="restricted"`); `whoami` y
    `/api/overview` con credenciales de RabbitMQ → `200`; 18 colas `running` con
    consumidores en las colas de registro y académicas.
  - Contenedores de la EC2 `healthy` con las imágenes nuevas
    (`ms-usuarios-auth`, `ms-evaluaciones`, `bff-web`, `frontend`, `caddy`).
- **Pendientes / notas**: repetir en el navegador: (a) tu fila en
  `/admin/usuarios` sin "Eliminar" y `DELETE` propio rechazado; (b) crear una
  FORMATIVA con nota (no debe mover el promedio) y una SUMATIVA con % (sí debe
  moverlo); (c) entrar a la URL del panel y usar directo el login de RabbitMQ.
  Los secrets `PANEL_BASIC_*` de GitHub quedan sin uso y se pueden borrar. El CD
  ahora reinicia Caddy en cada deploy (`fa0a819`); sin eso, un Caddyfile nuevo no
  se aplica porque el bind mount no recrea el contenedor.

### PR-20261011-11 — Round-trip del tipo de evaluación, detalle de notas y colas colgadas

- **Fecha**: 2026-10-11
- **Rama / commit**: `feat/rabbitmq-cluster-admin` @ `d0f47b2`; CD run
  `38115033773` en verde
- **Alcance**: (1) fix del bug de ponderación en `Registrar notas`: tras crear o
  editar una evaluación el tipo volvía como texto visible (`"Evaluación Sumativa"`)
  y la UI dejaba de sumar/editar ponderaciones; (2) detalle emergente (nombre de la
  evaluación, nota y ponderación) sobre **cada celda de nota** de la tabla de notas
  del portal estudiante y apoderados (hover en desktop, tap en móvil); (3)
  reparación de las colas `cola-notificaciones-notas` y
  `cola-notificaciones-evaluaciones`, colgadas tras el stop abrupto del lab; (4)
  batería de pruebas de mensajería (verdes, idempotencia y errores → DLQ) con
  limpieza de artefactos.
- **Entorno**: local (Maven 3.9/JDK 21, Vitest) + AWS `us-east-1` (EC2 `t3.large`,
  EIP `52.54.139.252`, RabbitMQ 4.3.6 en clúster de 2 nodos).
- **Causa raíz (bug de ponderación)**: `TipoEvaluacion` serializaba con
  `@JsonValue` el texto visible; el GET agregado del BFF lo normalizaba a
  `"SUMATIVA"` (`NotaBffService`), pero las respuestas de POST/PUT devolvían
  `"Evaluación Sumativa"`. El frontend compara estricto (`=== 'SUMATIVA'`), así que
  el estado local quedaba con tipo no canónico: `Ponderación total: 0%`, input `%`
  deshabilitado y "No pondera". La base estaba correcta (V4 aplicada; curso 1:
  30+50+20 = 100).
- **Cambios**:
  1. `TipoEvaluacion`: se quita `@JsonValue`; el JSON usa la constante
     (`SUMATIVA`/`FORMATIVA`/`DIAGNOSTICO`). `@JsonCreator` sigue aceptando el
     texto visible legacy (payloads antiguos del outbox).
  2. Frontend: `normalizarTipo()` (guard defensivo) en `DocenteNotasService` para
     `getCursoNotas`, `crearEvaluacion` y `editarEvaluacion`.
  3. `NotaDetalle.nombre` + mapper (`perfil.mapper.ts`) + mocks; popover compacto
     en `NotasTablaComponent` (ancho ajustado al contenido, `position: fixed`
     anti-recorte, hover/focus/tap, "No pondera" si ponderación 0).
  4. Compose: `stop_grace_period: 60s` en `rabbitmq1`/`rabbitmq2`.
- **Reparación de colas**: `cola-notificaciones-notas` y
  `cola-notificaciones-evaluaciones` no aparecían en `rabbitmqctl list_queues` y el
  consumidor fallaba el passive declare (`404 NOT_FOUND ... due to timeout`); DLQ y
  bindings intactos. Causa: cierre no limpio de RabbitMQ (stop del lab sin gracia).
  **Corrección 2026-10-11**: la causa de fondo era el dueño del volumen — el CD
  chownaba a `999:999` y la imagen alpine corre como `100:101`; ver PR-20261011-13.
  Se reinició `rabbitmq1` (`docker restart`): las 18 colas volvieron `running` con
  `consumers=1` en las tres académicas y 0 mensajes (sin pérdidas). Los outbox
  afectados quedaron históricos (`FALLIDO`: evaluaciones 24, notas 2) y **no** se
  re-encolaron por decisión de la sesión.
- **Resultado**: `OK` local y en la nube; desplegado por CD (imágenes `d0f47b27`
  en `bff-web`, `ms-evaluaciones`, `ms-notificaciones` y `frontend`). Queda la
  verificación visual del usuario (ponderar una SUMATIVA y ver el detalle de
  notas).
- **Evidencia**:
  - Backend: reactor `mvn package` `BUILD SUCCESS` (14 módulos; `EnumsJsonTest`
    actualizado + test de serialización de `EvaluacionResponseDTO` con
    `"tipo":"SUMATIVA"`).
  - Frontend: Vitest **42/42** (`notas-tabla` 5 nuevos; `docente-notas.service`
    2 nuevos de normalización; resto existente en verde).
  - Colas (rabbitmqctl): 18 `running`; tres académicas con `consumers=1`; DLQ en 0
    tras la limpieza.
  - Mensajería (management API + MariaDB):
    - verdes: `nota.creada` → fila `NOTA` «Calificación agregada en CIENCIAS
      NATURALES» (id 71); `evaluacion.creada` → fila `EVALUACION` (id 73);
      `asistencia.registrada` → fila `ASISTENCIA` (id 74); colas drenadas
      (`ready=0`, ACK, sin DLQ).
    - idempotencia: reenvío del mismo `idEvento` → 1 sola fila.
    - errores: JSON malformado → `cola-notificaciones-notas.dlq` (+1); evaluación
      sin `idCursoAsignatura` → `.dlq` evaluaciones (+1); nota sin `idEstudiante`
      → `.dlq` notas (+1). DLQ purgadas y filas de prueba eliminadas.
  - Deploy: CD run `38115033773` verde y compose de la EC2 con
    `stop_grace_period`; los bundles desplegados contienen el guard
    (`sumativa`) y el detalle (`max-w-64`, "No pondera"); E2E verde
    post-deploy (`nota.creada` → notificación id 75, eliminada después).
- **Pendientes / notas**: el `id_evento` persistido en `notificaciones` es el UUID
  v3 derivado del `idEvento` (`UUID.nameUUIDFromBytes`), no el string original: las
  consultas de evidencia deben usar el UUID. En modo mock, los nombres de
  evaluación se generan (`PRUEBA 1`, `TRABAJO 1`, `EXAMEN`, …).

### PR-20261011-12 — Preflight CORS end-to-end y CD sin disco lleno

- **Fecha**: 2026-10-11
- **Rama / commit**: `dev` @ `662f90c`; CD run `38169231699` en verde
- **Alcance**: (1) fix del preflight CORS cross-origin (ruta `OPTIONS` sin auth +
  CORS global de Spring MVC); (2) fix del CD que fallaba con *no space left on
  device* en la EC2.
- **Causa raíz CORS**: la ruta `ANY /api/{proxy+}` capturaba `OPTIONS` y el JWT
  Authorizer respondia `401`; al agregar la ruta `OPTIONS` sin autorizacion, el
  handler mapping de Spring rechazaba con `403 Invalid CORS request` (no habia
  `CorsConfigurationSource`). Un primer intento (`CorsFilter` de Security +
  bean) **no surtio efecto** (verificado local y en AWS con la imagen
  desplegada); la solucion efectiva fue `WebMvcConfigurer.addCorsMappings` en
  `core-share`, sin `.cors()` en la cadena.
- **Causa raíz disco**: las imagenes con tag fijo de cada deploy se acumulaban
  (10.8 GB recuperables) y el `pull` llenaba el disco raiz (20 GB); el prune
  existente corria despues del `up`.
- **Pasos**:
  1. PR #89: ruta `OPTIONS /api/{proxy+}` (NONE) + `OPTIONS` permitido en Spring.
  2. PR #91: primer intento de CORS en el backend (descartado tras verificar
     `403` en local y en AWS).
  3. PR #92: CORS global de Spring MVC; verificado local (200 + headers) y en
     AWS.
  4. Deploy manual de recuperacion (prune + pull + up) y PR #93 (prune antes
     del pull).
  5. CD `38169231699` en verde; verificacion final de preflight
     (permitido/denegado), `401` y SPA.
- **Resultado**: `OK`.
- **Evidencia**:
  - Preflight permitido -> `200` con `access-control-allow-origin:
    http://localhost:4200`, metodos `DELETE,GET,OPTIONS,PATCH,POST,PUT`,
    headers `authorization,content-type` y `max-age 300`; origen denegado ->
    `200` **sin** `access-control-allow-origin`.
  - `GET /api/me` sin token -> `401`; SPA `/` -> `200`.
  - CD `38169231699` verde; contenedores `healthy` con la imagen `662f90c`.
  - Disco de la EC2 tras la limpieza: 45–56% usado (antes 99%).
- **Pendientes / notas**: la allowlist efectiva vive en API Gateway
  (`cors_allowed_origins`); el backend solo responde el preflight.

### PR-20261011-13 — Fix permanente de colas RabbitMQ (uid del volumen) y secrets de despliegue

- **Fecha**: 2026-10-11
- **Rama / commit**: `fix/mq-uid-secrets` → `dev`/`main`/`deploy` @ `22702f5`; CD
  run `38177233359` en verde
- **Alcance**: eliminar de raíz las dos recurrencias (colas ×3, usuarios ×2):
  (1) chown del volumen de RabbitMQ **simbólico** (dentro del contenedor) en el CD
  y en `user-data.sh`; (2) imágenes de RabbitMQ y Caddy pineadas; (3) preflight de
  secrets en el CD (falla temprano, con nombres); (4) verificación post-deploy de
  la topología de colas (aborta el deploy si queda degradado); (5)
  `tools/set-github-secrets.ps1` para restaurar todos los secrets en un comando;
  (6) flujo de ramas restaurado `feature → dev → main → deploy`, con el CD
  disparado por **push a `deploy`** (no más `workflow_dispatch` desde `dev`).
- **Causa raíz (colas)**: el CD hacía `sudo chown -R 999:999` sobre los volúmenes,
  pero `rabbitmq:4-management-alpine` corre como `rabbitmq:x:100:101`. El proceso
  no podía escribir los índices de las colas classic:
  `{badmatch,{error,eacces}}` en `rabbit_classic_queue_index_v2` → crash loop de
  `cola-notificaciones-notas` y `cola-notificaciones-evaluaciones` (12 crashes en
  3 h), consumidores en reintento infinito y outbox acumulando `FALLIDO`. Las
  lecturas funcionaban, por eso el síntoma era intermitente. `user-data.sh` tenía
  el mismo número fijo y además un `chown -R ubuntu:ubuntu` sobre `siga-data` que
  habría roto el EBS al reemplazar la instancia.
- **Causa raíz (registro 503)**: faltaban en GitHub los secrets
  `REGISTRO_ASYNC_ENABLED`, `REGISTRO_CRED_KEY`,
  `REGISTRO_NOTIFY_CREDENTIALS_ENABLED`, `AZURE_CLIENT_SECRET` y
  `AZURE_API_APP_ID`; el CD escribía el `.env` con valores vacíos y
  `ms-usuarios-auth` respondía `503 "El registro asíncrono de usuarios está
  deshabilitado."` sin ningún error visible durante el despliegue.
- **Cambios**:
  1. `cd.yml`: chown con `docker run` sobre la imagen resuelta del compose
     (`docker compose config --images`) → válido para cualquier variante de imagen.
  2. `cd.yml`: paso **Validar secrets requeridos** (falla listando los ausentes;
     `REGISTRO_ASYNC_ENABLED` debe ser `true|false`) y **espera del healthcheck de
     MariaDB** tras recrearlo (sin eso el SQL moría con `ERROR 2002 ... socket`).
  3. `cd.yml`: **verificación post-deploy** — espera hasta ~8 min a las 9 colas de
     negocio con consumidores y aborta si falta topología o hay crashes de RabbitMQ.
  4. `user-data.sh`: RabbitMQ `100:101` y fin del `chown -R` sobre `siga-data`.
  5. Compose (template EC2 y local): `rabbitmq:4.3.6-management-alpine` y
     `caddy:2.11-alpine`.
  6. `tools/set-github-secrets.ps1` (idempotente, `-WhatIf`, nunca imprime
     valores; parsea solo el perfil `[default]` de `~/.aws/credentials`, que
     puede contener varios perfiles).
- **Reparación en caliente (EC2)**: chown simbólico en ambos nodos + `docker
  restart` → 18 colas `running` con consumers, 0 crashes; E2E verde
  (`nota.creada` → notificación id 80), JSON malformado → DLQ, limpieza. Los
  `FALLIDO` no se re-encolaron (decisión de la sesión).
- **Resultado**: `OK` — colas reparadas y **deploy en verde por la vía normal**
  (push a `deploy`), con la verificación post-deploy del propio CD pasando.
- **Incidencias de la sesión (con fix aplicado)**:
  1. `tools/set-github-secrets.ps1` subió las credenciales del perfil `[siga]`
     (el parser tomaba la última coincidencia de cada clave y
     `~/.aws/credentials` tiene dos perfiles): el CD murió con *The security
     token included in the request is invalid*. Fix: parser por sección INI
     (`-AwsProfile`, default `default`).
  2. Los secretos de DB/RabbitMQ/Azure **no coinciden con el `.env` local** (es
     de desarrollo): al restaurarlos desde ahí, el CD falló con
     `Access denied for user 'root'`. Fix: valores vigentes rescatados de la env
     de los contenedores (`-RecoveryJson` con advertencias en el script) y reset
     del root de MariaDB (`--skip-grant-tables`) al valor del secret.
  3. Al recrearse MariaDB, el SQL del deploy corría antes de que aceptara
     conexiones (`ERROR 2002 ... socket`): fix con espera del healthcheck.
- **Evidencia**:
  - Reparación: chown `999:999` → `100:101`, 18 colas `running` con `consumers=1`
    en las 9 con listener, `Generic server` = 0, E2E verde (`nota.creada` →
    notificación id 80) y error a DLQ con limpieza.
  - CD `38177233359` en verde desde `deploy` (imágenes `22702f5`): preflight de
    secrets, chown simbólico, espera de MariaDB y **verificación post-deploy de
    topología** pasaron; 17/17 contenedores `healthy`; 18 colas; 0 crashes.
  - Registro: `POST /api/bff/v1/admin/registraciones` sin token → `401` (ya no
    `503`); `REGISTRO_ASYNC_ENABLED=true` y `REGISTRO_CRED_KEY` presentes en el
    contenedor de `ms-usuarios-auth`.
  - E2E final post-deploy: `nota.creada` → notificación id 81 (eliminada después);
    cola en 0 con consumer.
- **Pendientes / notas**: falta la verificación visual del usuario en la SPA
  (registro de usuario sin 503, ponderación SUMATIVA y detalle de notas);
  `dev`, `main` y `deploy` quedan sincronizadas y el CD se dispara por **push a
  `deploy`**. Si un secret se borra, el CD lo reporta por nombre (nunca el valor)
  y el script lo restaura; usar `-RecoveryJson` para los valores del despliegue.

### PR-20261115-14 — EP3: pruebas locales y preflight AWS Kafka

- **Fecha/entorno**: 2026-11-15, Windows 11/Java 21/Maven 3.9.9; branch `dev`, SHA `ed64504cd82408de2658d427966aedf357b7df90`; AWS `us-east-1`, EC2 etiquetada `SIGA-app`.
- **Objetivo**: repetir pruebas backend/frontend/Compose y verificar el estado desplegado antes de ejecutar la prueba autorizada de pérdida de uno y dos brokers.
- **Resultados locales**:
  - Backend `mvn ... package`: `BUILD SUCCESS`, 17 módulos. Los tests ejecutados no reportaron fallas; los casos Testcontainers se omitieron localmente porque recibieron `BadRequest 400` y un Docker info vacío del named pipe. El CI Ubuntu del mismo SHA terminó verde; adjuntar/revisar su artefacto Surefire si se requiere probar cada IT individual.
  - Admin Kafka: `AdminControllerSecurityTest`, 4/4 aprobados.
  - Frontend: 35 archivos, 283/283 tests aprobados; cobertura de líneas 94.17%; build de producción aprobado.
  - `docker compose --env-file .env.example config -q` y validación de la plantilla AWS con `REGISTRY`/`IMAGE_TAG` ficticios: aprobadas.
- **CI remoto**: el run [43761010668](https://github.com/COdelValle/SIGA-Project/actions/runs/43761010668) del mismo SHA `ed64504` terminó `success` en Ubuntu.
- **Resultados AWS**:
  - CD más reciente consultado: run [39373705927](https://github.com/COdelValle/SIGA-Project/actions/runs/39373705927), `success`, SHA `efdd7f370652747acd1d5b45e86e8a7a90e9347b`.
  - La EC2 estaba activa y tenía 17 servicios saludables, pero su Compose remoto no define brokers ZooKeeper/Kafka, Kafka UI, `ms-kafka-admin` ni `ms-reportes`. El commit desplegado `efdd7f3` tampoco contiene Kafka en la plantilla AWS; el `dev` local `ed64504` sí la contiene.
  - `Caddyfile` remoto no contiene ruta Kafka ni Basic Auth. Los nombres de secrets UI están presentes en GitHub, pero eso no los aplica retroactivamente a la EC2.
  - Smoke de API Gateway por SSM: SPA `200`, BFF `/actuator/health` `200`, `/api/me` sin token `401` y con JWT malformado `401 invalid_token`. JWT real válido/Entra queda pendiente de una sesión autenticada.
  - RabbitMQ sí respondió: ambos nodos aparecieron en `Running Nodes`, sin alarmas ni partición de red. La consulta de colas mostró consumidores activos en colas académicas y DLQ.
  - `list_exchanges` confirmó `intercambio-notificaciones`/`user.topic.exchange` tipo `topic` y `siga.dlx.direct` tipo `direct`.
  - El comando remoto de topics no pudo ejecutarse porque `kafka-1` no existe. No se detuvo ningún broker: la prueba de uno/dos brokers queda bloqueada hasta desplegar la versión que contiene Kafka.
- **Evidencia**: [`evidencias/E3-ENC-20261115-01/README.md`](evidencias/E3-ENC-20261115-01/README.md), [`evidencias/E3-PRES-20261115-01/README.md`](evidencias/E3-PRES-20261115-01/README.md) y [guía manual EP3/AWS](pruebas-manuales-ep3-aws.md).
- **Pendiente**: promover/desplegar la versión Kafka habilitada por el flujo autorizado de `deploy`; después repetir preflight, verificar login Kafka UI y realizar primero el failover de un broker y luego el escenario de dos brokers. Requiere completar la puerta de salida de cada fase.

### PR-20261115-15 — PR de promoción EP3 y checks de seguridad

- **Rama/SHA**: `dev`, `c651331b3b9d114eab2581277f6ed97817ef9b56`; PR [#121](https://github.com/COdelValle/SIGA-Project/pull/121) hacia `deploy`.
- **Remediación**: los UUID de los resultados k6 usan `k6/crypto.randomBytes`; el test de cifrado usa texto de prueba neutro. La prueba `CredentialCipherErroresTest` quedó verde (2/2) y `node --check tools/performance/allure-summary.js` pasó.
- **CI del PR**: backend, frontend, E2E, Infra y ambos análisis CodeQL terminaron en verde. CodeQL dejó de reportar el hallazgo `js/insecure-randomness`.
- **Bloqueo**: GitGuardian reporta `Generic Password` en `docker-compose.yml`, commit `06d5d666`, dentro del escaneo de 85 commits. El check no revela la línea/valor; no imprimir ni copiar el valor. La plantilla actual usa variables de entorno sin esos valores por defecto, pero la credencial histórica debe revisarse para confirmar que nunca se usó y resolverla en GitGuardian. No fusionar ni desplegar hasta completar esa revisión.
- **Nota de remediación**: se sustituyó un literal de prueba tipo contraseña por texto neutro y se añadió una excepción exacta para ese fixture histórico; no eliminó el hallazgo de Compose.
- **Actualización del hallazgo**: GitGuardian identifica `Generic Password` en `docker-compose.yml`, commit `06d5d666`. Las excepciones exactas incluyen los fallbacks históricos de MariaDB y el fixture, sin ignorar el archivo completo. Confirmar si algún entorno persistente usó esos fallbacks y rotar sus credenciales si corresponde. El check aún debe reejecutarse antes de fusionar.
- **CD de política**: PR [#122](https://github.com/COdelValle/SIGA-Project/pull/122) se fusionó para publicar las excepciones exactas de GitGuardian en `deploy`. El CD [43792075530](https://github.com/COdelValle/SIGA-Project/actions/runs/43792075530) terminó `success` con SHA `051d7af`; la EC2 quedó con 17 servicios saludables, pero el Compose de `deploy` aún no incluye Kafka, Kafka UI, `ms-kafka-admin` ni `ms-reportes`.
- **Estado a esa fecha**: PR #121 estaba abierto por GitGuardian; ver PR-20261116-16 para la continuación.

### PR-20261116-16 — CD EP3, ECR y recuperación pendiente

- **Merge EP3**: PR #121 se fusionó como `6db01be0a160e0ad018d0a9a29f5c244aa81e1ed` después de que GitGuardian cambiara a `skipped`. PR #124 fusionó el fix de la suite de infraestructura en Allure.
- **Terraform/ECR**: el plan dirigido mostró solo `2 to add, 0 to change, 0 to destroy`; se crearon `siga-kafka-admin` y `siga-reportes`. El plan completo mostró `0 to add, 2 to change, 0 to destroy`; se amplió EBS 10→20 GB y actualizó `user_data` in-place.
- **CD 43799769525**: build de imágenes exitoso hasta la etapa de Compose; el deploy falló cuando RabbitMQ quedó `unhealthy`. La lectura SSM posterior mostró Kafka 1 `unhealthy`, Kafka 2/3 `healthy`, `ms-kafka-admin` `healthy`, `ms-reportes`/`ms-notas` en `starting` y BFF/frontend `Created`.
- **CD 43811522748**: verificación backend/frontend e informe Allure `success`; `Build, push a ECR y deploy en EC2` falló al hacer ECR login por `ecr:GetAuthorizationToken` denegado explícitamente por la política `voc-cancel-cred`. No llegó a modificar la EC2.
- **Estado AWS**: el último snapshot SSM disponible es parcial: Kafka 1 unhealthy, Kafka 2/3 healthy, `ms-kafka-admin` healthy, `ms-reportes` y `ms-notas` arrancando, BFF/frontend creados y RabbitMQ recuperado. La comprobación local confirmó `sts:GetCallerIdentity`, pero ECR y SSM reciben denegación explícita `voc-cancel-cred`; hay que iniciar/renovar AWS Academy y actualizar los secrets `AWS_*` antes de reintentar CD. No se hicieron pruebas de failover.

### PR-20261116-17 — Diagnóstico del CD y corrección de arranque

- **CD 43851631259**: las verificaciones backend/frontend e informe Allure pasaron; el deploy de SHA `473c07ab9dc88eebfb1cb5e836f4f35eea4b51e8` falló en `docker compose up --remove-orphans` a las 11:07:51 UTC con `dependency failed to start: container kafka-1 is unhealthy`.
- **Estado Kafka/RabbitMQ posterior**: SSM confirmó las APIs de los tres brokers accesibles y `healthy`; ambos topics con 3 particiones, RF=3, `min.insync.replicas=2`, ISR 3/3 y `retention.ms=604800000`/`retention.bytes=134217728`. RabbitMQ tiene los nodos `rabbit@rabbitmq1` y `rabbit@rabbitmq2` activos, sin alarmas ni particiones; `ms-kafka-admin` responde UP.
- **Hallazgo ms-reportes**: los logs muestran `UnsatisfiedDependencyException` en `ReporteProcesamientoService`: Spring no seleccionaba el constructor porque Lombok generaba uno para los campos finales y existía además una sobrecarga de cinco argumentos. Se explicitó el constructor de seis dependencias con `@Autowired` y se agregó una prueba ligera de wiring de contexto.
- **Healthcheck Kafka**: se observaron probes con timeout (`ExitCode=-1`) mientras los brokers luego aparecían healthy; el probe del Compose pasa de 10 a 30 segundos para evitar falsos unhealthy durante el arranque bajo carga.
- **Pruebas locales**: `mvn -f apps/backend/pom.xml -pl ms-reportes -am test` terminó `BUILD SUCCESS`; `ms-reportes`: 16 pruebas, 0 fallos, 0 errores, 1 omitida (migración Testcontainers porque Docker local no está disponible). La nueva prueba Spring de wiring pasó.
- **CD 43858404711 / merge `296a1f7`**: todas las verificaciones, Allure y builds/push ECR pasaron; el deploy remoto excedió a las 12:01:42 UTC el timeout de 15 min de la acción SSH (`Run Command Timeout`). SSM confirmó que Kafka, `ms-kafka-admin`, `ms-reportes` y RabbitMQ quedaron healthy; `ms-notificaciones` seguía `starting`, y `ms-notas`, `bff-web` y `frontend` `Created` al finalizar.
- **Corrección del timeout**: la cadena de dependencias de Compose necesita más de 15 min para iniciar los JVM en esta EC2. El `command_timeout` se amplió a 45 min en `.github/workflows/cd.yml`, pendiente de promover.
- **Estado remoto**: no se detuvieron brokers ni se ejecutaron failovers. Aún se requiere CD exitoso, todos los contenedores healthy y preflight antes de las pruebas autorizadas de caída de uno y dos brokers.

### PR-20261116-18 — Ampliar espera del deploy remoto

- **Causa del fallo**: el CD `43858404711` terminó con `Run Command Timeout` exactamente tras los 15 min configurados en `appleboy/ssh-action`; Docker Compose seguía recorriendo la cadena de healthchecks y no había iniciado `ms-notas`, BFF ni frontend.
- **Estado posterior**: SSM a las 12:02 UTC confirmó brokers, ZooKeeper, `ms-kafka-admin`, `ms-reportes`, MariaDB y RabbitMQ saludables; `ms-asistencias` y `ms-evaluaciones` healthy; `ms-notificaciones` en `starting`; `ms-notas`, `bff-web` y `frontend` en `Created`.
- **Cambio**: `.github/workflows/cd.yml` amplía `command_timeout` a 45 min para permitir que el arranque del grafo Compose y las verificaciones post-deploy concluyan. Pendiente de promoción y nuevo CD; no ejecutar failover hasta tener todos los servicios healthy y un CD exitoso.

### PR-20261116-19 — Healthcheck RabbitMQ bajo carga de arranque

- **CD 43864706041**: PR #127 fue desplegado con timeout SSH de 45 min, pero Compose falló a las 12:35:22 UTC con `dependency failed to start: container rabbitmq1 is unhealthy`.
- **Diagnóstico**: ambos RabbitMQ tenían sus cinco probes Docker recientes con `ExitCode=-1`; el healthcheck ejecutaba `rabbitmq-diagnostics -q ping` con timeout de 5 s. SSM observó load average ~24 en host de 2 CPU; una prueba TCP al puerto 5672 pasó en ambos nodos y el ping directo también pasó después, confirmando que el timeout del probe era transitorio.
- **Cambio**: el healthcheck cambia a `nc -z 127.0.0.1 5672`, timeout 15 s, intervalo 15 s, 10 reintentos y start period de 60 s en ambos nodos.
- **Estado remoto**: RabbitMQ se conserva persistente y no se ha modificado manualmente; en la lectura posterior algunos servicios dependientes siguen `Created`/`starting`. No se detuvieron brokers ni se ejecutaron failovers.
- **Siguiente paso**: validar Compose, promover el healthcheck y repetir CD. Solo ejecutar failover tras un CD exitoso y preflight con stack completo saludable.

### PR-20261116-20 — CD exitoso, failover y smoke Kafka

- **CD final**: PR #128 se fusionó como `dc98259ab2fab428647c133475f5126c724d36b8`; el CD [43869955825](https://github.com/COdelValle/SIGA-Project/actions/runs/43869955825) terminó `success`, incluido el deploy completo y sus verificaciones post-deploy.
- **Preflight**: los 26 contenedores están activos; todos los servicios con healthcheck `healthy`. Tres brokers y tres ZooKeeper healthy; Kafka UI en ejecución; RabbitMQ 1/2 healthy, ambos nodos `Running Nodes`, sin alarmas ni particiones. El acceso público Kafka UI sin autenticar devuelve HTTP 401.
- **Topics de negocio**: ambos con 3 particiones, RF=3, `min.insync.replicas=2`, ISR 3/3, `retention.ms=604800000`, `retention.bytes=134217728`, `cleanup.policy=delete`. Los grupos `siga-reportes-asistencias` y `siga-reportes-rendimiento` están conectados; sin eventos de negocio el lag está vacío/cero.
- **Failover de un broker**: topic temporal `siga.e3.failover-20261116-01`; se detuvo cada broker individualmente. En cada escenario todas las particiones conservaron ISR=2, `acks=all` confirmó un registro y, al restaurar el broker, recuperó `healthy`/ISR=3. Se repitió en kafka-1, kafka-2 y kafka-3.
- **Failover de dos brokers**: con solo kafka-3 activo, ISR=1 y el broker devolvió `NOT_ENOUGH_REPLICAS`. `kafka-producer-perf-test` indicó `0 records sent`, aunque el proceso devolvió exit code 0; por ello no usar solo su código de salida. Los offsets finales (p0=1,p1=0,p2=3; total 4) son la publicación inicial más tres de los escenarios individuales, sin escritura adicional durante el rechazo.
- **Recuperación/limpieza**: se restauró kafka-2 y luego kafka-1; tres brokers e ISR 3/3. Se eliminó el topic temporal; `kafka-topics --list` volvió a mostrar solo los topics de negocio. RabbitMQ permaneció con dos nodos y sus colas consumidoras intactas.
- **Smoke funcional de asistencia**: evento sintético `e3-smoke-a-20261116-01`, clave `idEstudiante=900000000000000001`; se consumió y persistió proyección `PRESENTE/NORMAL`, lag 0.
- **Smoke funcional de rendimiento — FALLA**: el listener recibió `e3-smoke-r-20261116-01`, pero la primera escritura falló por `Column 'actualizado_en' cannot be null`; después de reintentos se rechazó. No se persistió proyección ni entrada procesada. El mensaje sintético inválido permanece en el topic hasta cumplir la retención; no contiene PII.
- **Corrección local**: la primera escritura requería `actualizado_en`, `id_evento`, `correlation_id` y `nivel_riesgo`; `ReporteProcesamientoService.procesarRendimiento` inicializa ahora esos campos antes del primer `save`. La regresión unitaria captura el estado en el instante de cada invocación a `save` (no la referencia mutable final). `mvn -f apps/backend/pom.xml -pl ms-reportes -am test` terminó `BUILD SUCCESS` (16 tests del módulo, 0 errores/fallos, 1 Testcontainers omitida por Docker local no disponible); `mvn -f apps/backend/pom.xml -pl ms-reportes -am -DskipTests package` también `BUILD SUCCESS`. El cambio está en `dev`, pendiente de PR/CD y de repetir el smoke de rendimiento con un `idEvento` nuevo.
- **Evidencia detallada**: [`evidencias/E3-PRES-20261116-01/README.md`](evidencias/E3-PRES-20261116-01/README.md). No se detuvo ZooKeeper/EC2 ni se publicaron eventos de prueba en la etapa de dos brokers caídos.

### PR-20261116-21 — Fix de rendimiento desplegado y verificado en AWS

- **Merge/CD**: PR #129 se fusionó como `df32827ab5101121a5969731762124ac67be7c1c`; el CD [43881252562](https://github.com/COdelValle/SIGA-Project/actions/runs/43881252562) terminó `success` y desplegó el stack completo.
- **Smoke de rendimiento OK**: evento sintético `e3-smoke-r-20261116-02` (clave `idEstudiante=900000000000000001`) persistió en `proyecciones_rendimiento` con `puntaje=6.5`, `promedio=6.5`, `nivel_riesgo=NORMAL`, y en `eventos_reportes_procesados` (topic rendimiento, partición 0, offset 1). SSM `84750ee1-602a-4110-89a5-c4ca05473c66`.
- **Estado final**: los 26 contenedores quedaron `healthy` (SSM `f2f07f4a-a2e0-424d-8fc0-e6893cb8ca94`); grupos `siga-reportes-asistencias` (offset 1) y `siga-reportes-rendimiento` (offset 2) con lag 0; ambos topics con ISR 3/3; RabbitMQ con dos nodos en `Running Nodes`; `ms-kafka-admin` y `ms-reportes` en `UP` (SSM `db6f6862-71a1-4d88-a714-beef230cb926`).
- **Nota operativa**: `ms-reportes` tarda ~438 s en arrancar tras el redeploy y puede aparecer `unhealthy` durante ese periodo; un productor lanzado en ese intervalo recibió `REQUEST_TIMED_OUT` con reintento exitoso. Verificar el health antes de publicar eventos de smoke.

## 5. Runbook reproducible

### 5.1 Prerrequisitos

- Docker Desktop con el stack de SIGA; idealmente **detener otras stacks**
  (p. ej. `pasteleria-project`) para evitar caídas del engine por carga.
- `.env` completo (MARIADB, DB, AZURE, RABBITMQ, `RABBITMQ_ERLANG_COOKIE` para el
  clúster y `REGISTRO_*`).
- Para flujos reales: permisos de Graph de la sección 2 de
  [`testing-login.md`](testing-login.md) y token ADMIN del navegador.

Generar la clave de credenciales:

```bash
openssl rand -base64 32
```

### 5.2 Pruebas automáticas (mismas flags del CI)

Backend (desde la raíz del repo):

```bash
mvn -f apps/backend/pom.xml -B -ntp package \
  -Dtest='!*ApplicationTests' -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtypescript.generator.skip=true
```

Frontend (desde `apps/frontend`):

```bash
npx tsc -p tsconfig.app.json --noEmit
npm test -- --watch=false
npm run build -- --configuration production
```

### 5.3 Levantar y reconstruir

- Stack completo: `docker compose up -d --build` y `docker compose ps`.
- Tras cambios acotados, reconstruir **de a uno** para no saturar Docker Desktop:
  `ms-usuarios-auth` → `ms-estudiantes` → `ms-docentes` → `ms-apoderados` →
  `bff-web` → `frontend` (`docker compose up -d --build <servicio>`).
- Para la bandeja académica, incluir además `mariadb-notificaciones`,
  `ms-asignaturas`, `ms-evaluaciones`, `ms-asistencias`, `ms-notas` y
  `ms-notificaciones`. Reconstruir los servicios que empaquetan `core-share` antes
  del E2E; mantener RabbitMQ y los volúmenes existentes.
- Verificar salud: `docker compose ps` (todos `healthy`).

### 5.4 Registro asíncrono (si se va a probar el flujo real)

1. `.env`: `REGISTRO_ASYNC_ENABLED=true` y `REGISTRO_CRED_KEY=<clave>`; recrear
   `ms-usuarios-auth`.
2. Iniciar el alta desde la UI o con `POST /api/bff/v1/admin/registraciones`
   (token ADMIN) y hacer polling del `processId` con
   `GET /api/bff/v1/admin/registraciones/{processId}`.
3. Credencial: `GET .../credencial` una sola vez (`409`/`410` según estado) y
   `POST /admin/usuarios/{id}/reset-password` como respaldo.
4. Casos borde: RUT inválido → `400`; correo duplicado → `409`; apoderado con
   estudiante inexistente → `400` inmediato.
5. Limpieza: eliminar cuentas de prueba en Entra (o deshabilitarlas) y sus filas
   locales; volver `REGISTRO_ASYNC_ENABLED=false`.

### 5.5 Verificación de datos (SQL)

```sql
-- Estado de procesos, intentos y outbox (siga_usuarios_db)
SELECT process_id, state, azure_state, domain_state, error_message
  FROM user_registration_processes ORDER BY created_at DESC LIMIT 5;
SELECT process_id, step_type, success, message
  FROM user_registration_attempts ORDER BY id DESC LIMIT 10;
SELECT event_id, routing_key, state, attempts, last_error
  FROM user_registration_outbox ORDER BY id DESC LIMIT 5;

-- Estandar de datos
SELECT COUNT(*) FROM estudiantes WHERE rut LIKE '%.%';   -- esperado: 0
SELECT COUNT(*) FROM usuarios WHERE full_name IS NULL;   -- esperado: 0
```

### 5.6 Verificación de mensajería

```bash
docker exec rabbitmq1 rabbitmqctl list_queues name messages
docker exec rabbitmq1 rabbitmqctl list_consumers
```

Colas esperadas: `user.azure.sync.queue`, `ms.estudiantes.queue`,
`ms.docentes.queue`, `ms.apoderados.queue`, `user.registration.status.queue`,
`user.credentials.notify.queue` y una DLQ por cola. Las DLQ se enlazan al
exchange directo `siga.dlx.direct`. Verificar la unión del clúster con
`docker exec rabbitmq1 rabbitmqctl cluster_status` (dos nodos `running`). UI:
`http://localhost:15672` (solo el nodo `rabbitmq1` publica el puerto).

Si una cola académica no aparece en `list_queues` y el consumidor registra
`404 NOT_FOUND ... due to timeout` en el passive declare, el proceso de la cola
está en **crash loop**: revisar `docker logs rabbitmq1 | grep "Generic server"`;
la causa histórica fue un dueño equivocado del volumen
(`{badmatch,{error,eacces}}` en `rabbit_classic_queue_index_v2`) por el chown a
un uid fijo. El CD ya hace el chown **dentro del contenedor** (uid simbólico); para
repararlo a mano:

```bash
IMG=$(docker inspect rabbitmq1 --format '{{.Config.Image}}')
docker run --rm -v /home/ubuntu/siga-data/rabbitmq1:/data --entrypoint sh "$IMG" -c 'chown -R rabbitmq:rabbitmq /data'
docker run --rm -v /home/ubuntu/siga-data/rabbitmq2:/data --entrypoint sh "$IMG" -c 'chown -R rabbitmq:rabbitmq /data'
docker restart rabbitmq1 rabbitmq2
docker exec rabbitmq1 rabbitmqctl list_queues name messages consumers
```

Las colas viven en el EBS y recuperan su profundidad; el compose mantiene
`stop_grace_period: 60s` para el cierre.

### 5.6.2 Bandeja académica in-app

Este flujo prueba RabbitMQ -> outbox -> `ms-notificaciones` -> BFF -> campana.
No envía correos ni requiere Gmail/Exchange. El destinatario se resuelve con el
OID del JWT y el vínculo vigente del estudiante/apoderado.

Prerrequisitos:

- Reconstruir en forma secuencial los servicios afectados, sin borrar volúmenes:
  `mariadb-notificaciones`, `ms-asistencias`, `ms-evaluaciones`, `ms-notas`,
  `ms-notificaciones`, `bff-web` y `frontend`.
- `.env` local completo para MariaDB, Azure y RabbitMQ. No copiar secretos ni
  tokens a este registro.
- Cuenta de prueba DOCENTE/ADMIN con scopes de escritura para crear los eventos;
  cuenta ESTUDIANTE y cuenta APODERADO de prueba vinculadas al mismo pupilo.
- Un alumno con dictación activa y datos de prueba identificables. Usar una fecha
  de asistencia no ocupada. No hacer estas operaciones contra datos de producción.

Preflight de topología:

```bash
docker compose ps
docker exec rabbitmq1 rabbitmqctl list_exchanges name type durable
docker exec rabbitmq1 rabbitmqctl list_queues name messages_ready messages_unacknowledged consumers
docker exec rabbitmq1 rabbitmqctl list_bindings source_name destination_name routing_key
docker exec rabbitmq1 rabbitmqctl list_consumers
```

Esperado: `intercambio-notificaciones` tipo `topic`, tres colas principales
durables con un consumidor activo cada una, bindings `evaluacion.*`,
`asistencia.*` y `nota.*`, y las DLQ correspondientes. Las DLQ deben estar
durables y sin consumidor automático. La profundidad cero de una cola principal
no es evidencia suficiente: `ms-notificaciones` consume y confirma rápidamente.

Pasos E2E (guardar los IDs devueltos por cada POST):

1. Con el token de DOCENTE/ADMIN, crear una evaluación de prueba mediante
   `POST /api/bff/v1/evaluaciones` con `nombre`, `tipo`, `ponderacion` e
   `idCursoAsignatura` válidos. Actualizarla con PUT y luego eliminarla
   lógicamente. Esperado: una bandeja por cada evento, visible para alumnos de la
   dictación y sus apoderados vinculados.
2. Crear una nota para el alumno y la evaluación de prueba con
   `POST /api/bff/v1/notas`; modificarla con PUT; eliminarla lógicamente y
   volver a crearla para probar la reactivación. Esperado: `CREADA` al crear y
   `ACTUALIZADA` al modificar/reactivar; no se crea aviso por DELETE. El resumen
   no debe exponer el puntaje.
3. Registrar asistencia PRESENTE, AUSENTE y ATRASADO mediante
   `POST /api/bff/v1/asistencias`, con fecha vigente y única. Esperado: PRESENTE
   no genera aviso; AUSENTE y ATRASADO sí. Verificar en el outbox el porcentaje
   mensual y el flag del umbral (>= 60%). La regla inicial no publica cambios de
   asistencia hechos por PUT.
4. Iniciar sesión como el ESTUDIANTE y abrir la campana del header. Consultar
   `GET /api/bff/v1/notificaciones/me?page=0&size=10` y
   `GET /api/bff/v1/notificaciones/me/no-leidas/count`; confirmar eventos,
   títulos/resúmenes específicos (asignatura y evaluación), hora de Chile y
   destinos esperados. Con la campana cerrada, generar un evento nuevo y
   comprobar que el contador sube en ≤10 s (o al volver a la pestaña) y que la
   campana pulsa; abrir el panel debe mostrar el aviso sin recargar la página.
   Abrir una notificación y comprobar el PATCH
   `/api/bff/v1/notificaciones/me/{id}/leida`, que el contador baja al instante y
   que la notificación permanece en la lista como leída (sin punto amarillo).
   Probar `PATCH /api/bff/v1/notificaciones/me/leidas` (marcar todas: conserva
   los ítems y baja el contador a 0) y
   `DELETE /api/bff/v1/notificaciones/me/leidas` (limpiar mis leídas: oculta solo
   las leídas; si no queda ninguna, muestra "No tienes notificaciones."). Un
   fallo de acción debe mostrar un mensaje inline sin borrar la lista.
5. Repetir como APODERADO del alumno. Debe ver los eventos del pupilo, pero su
   marca de lectura/limpieza debe ser independiente de la del alumno. Iniciar
   sesión con un segundo alumno/apoderado no vinculado y comprobar que no ve
   estos avisos; solicitar el PATCH de una notificación ajena debe responder 404.
6. Correlacionar la respuesta UI con los logs de publicador/consumidor, el
   `idEvento` y las filas persistidas. Revisar colas/DLQ y comprobar que no haya
   duplicados por una redelivery.

SQL de evidencia (reemplazar el ID de prueba, sin copiar datos personales al log):

```sql
-- En la BD del productor correspondiente: estado/reintentos del outbox.
SELECT id_evento, estado, intentos, clave, ultimo_error
  FROM notification_event_outbox ORDER BY creado_en DESC LIMIT 10;

-- En siga_notificaciones_db: entrada de bandeja sin puntaje ni correo.
SELECT id, id_evento, tipo, accion, tipo_destino, id_destino, titulo, resumen,
       DATE_FORMAT(fecha_hora, '%Y-%m-%d %H:%i:%s') AS fecha_chile
  FROM notificaciones ORDER BY fecha_hora DESC LIMIT 10;
-- ocultada_en distinto de NULL = "Limpiar mis leídas" para ese OID.
SELECT id_notificacion, id_usuario, leida_en, ocultada_en
  FROM notificacion_lecturas ORDER BY leida_en DESC LIMIT 10;
```

Casos de recuperación en entorno local aislado:

- Detener RabbitMQ, generar un evento válido y verificar que el CRUD deja una
  fila `PENDIENTE` en `notification_event_outbox`; reiniciar RabbitMQ y esperar
  `ENVIADO` más una sola notificación en la bandeja.
- Detener `ms-notificaciones`, generar evento, comprobar acumulación en la cola;
  levantar el consumidor y verificar persistencia/ACK.
- Enviar un mensaje deliberadamente malformado solo en un entorno de prueba y
  verificar que, tras tres entregas, termina en la DLQ del tipo correspondiente.
- Probar `Limpiar mis leídas`: tras ocultar, la lista y el contador no vuelven a
  mostrar esos avisos para ese OID, pero la fila compartida sigue existiendo en
  `notificaciones` (otro destinatario la conserva).
- Los outbox con ocho fallos quedan `FALLIDO`; registrar el caso y reprocesarlo
  manualmente después de corregir la causa. No purgar colas ni ejecutar
  `docker compose down -v` en el stack compartido.
- La hora de la bandeja usa `TZ=America/Santiago` en los contenedores; si los
  timestamps vuelven a verse corridos, comparar `docker exec ms-notificaciones date`
  antes de tocar código.

### 5.7 Normalización de datos previos

- Los seeds se normalizan solos con las migraciones (`V7`/`V7`/`V5`/`V9`).
- Para filas reales creadas antes del cambio (si existen), ejecutar una vez por
  entorno:

```powershell
powershell -File tools/normalizar-datos.ps1
```

### 5.8 Limpieza de cuentas de prueba

- Entra ID: deshabilitar o eliminar las cuentas ficticias (si se eliminan, el UPN
  queda reservado ~30 días; para reutilizar el mismo correo conviene deshabilitar).
- BD: borrar filas de `usuarios`, perfiles, procesos, intentos, outbox y eventos
  procesados asociados a los correos de prueba.
- Bandeja académica: borrar filas de notificaciones/lecturas de los IDs de prueba
  y outbox de dominio; no purgar colas compartidas ni borrar volúmenes.

## 6. Limitaciones conocidas del entorno

- **Testcontainers en Windows**: no logra comunicarse con el *named pipe* de Docker
  Desktop moderno; los IT se omiten o se validan contra el MariaDB del stack real.
  En CI Linux corren normalmente.
- **Docker Desktop** puede devolver `500` y caerse si se reconstruyen 5 imágenes
  Java + frontend en paralelo con otra stack corriendo; reconstruir de a uno y
  detener stacks ajenas.
- **E2E autenticado de la campana** requiere cuentas de prueba ESTUDIANTE y
  APODERADO del tenant, vínculo vigente de pupilo y token Entra con los scopes de
  lectura de perfiles/asignaturas/inscripciones. No se requiere buzón ni Gmail.
- **Verificar rutas con `/v3/api-docs`**, no con `401` sin token: Spring
  autentica antes de resolver el handler y un 401 aparece aunque la ruta no
  exista (fue el origen del 404 de `/me/leidas`).
- **Caché de destinatarios (TTL 3 min)**: los cambios de vínculos o
  inscripciones pueden tardar hasta 3 minutos en reflejarse en la bandeja; el
  contador se refresca cada 10 s y al volver a la pestaña.
- **UTF-8 en consola**: al escribir datos con acentos por `docker exec`, usar
  Base64/UTF-8 explícito para evitar corrupción.

## 7. Inventario de pruebas automáticas

| Módulo | Suites relevantes |
| --- | --- |
| `core-share` | `ValidatorsTest`, `EnumsJsonTest`, `RutNormalizerTest`, `NombrePropioTest`, `SharedFeignFormatConfigTest`, `ConfiguracionMensajeriaCompartidaTest`, `ConfiguracionTopologiaNotificacionesTest`, `ConfirmadorMensajesTest`, `SolicitudRabbitValidationTest`, pruebas de outbox y `GlobalExceptionHandlerTest` |
| `ms-usuarios-auth` | `UsuarioServiceTest`, `InvitacionVinculacionServiceTest`, `AsyncUserRegistrationServiceTest`, `CredentialCipherTest`, `EmailInstitucionalGeneratorTest`, `MigracionesTest`, `MigracionesRegistroAsyncTest` |
| `bff-web` | `AdminBffServiceTest`, `PerfilEstudianteServiceTest`, `ApoderadoPupiloServiceTest`, `DocenteBffServiceTest`, `NotaBffServiceTest`, `EvaluacionBffServiceTest`, `AsistenciaBffServiceTest`, `NotificacionBffServiceTest` |
| Dominios | `EstudianteServiceTest` + `BusquedaTextoTest`, `DocenteServiceTest`, `ApoderadoServiceTest`, `ClaseServiceTest`, `NotaServiceTest` + `PublicadorNotaTest`, `EvaluacionServiceTest` + `PublicadorEvaluacionTest`, `AsistenciaServiceTest` + `PublicadorAsistenciaTest` |
| `ms-notificaciones` | `NotificacionServiceTest`, `ResolverDestinatariosNotificacionTest`, `NotificacionControllerMappingsTest`, `MigracionesNotificacionesTest` |
| `ms-rabbitmq-admin` | `RabbitAdminServiceTest`, `RabbitAdminControllerMappingsTest` |
| Frontend | specs Vitest de `app`, `academico.model` (`promedioNotas`), `docente-notas.service`, `registrar-notas.component`, `usuarios.component`, `perfil-estudiante.service`, `asistencia.service`, `notification.service` y `NotificationBellComponent` |

Los `*ApplicationTests` (contextLoads) se excluyen en CI porque requieren base de
datos y Azure AD.

## 8. Checklist de cierre de sesión

- [ ] Pruebas automáticas backend/frontend ejecutadas y en verde.
- [ ] Flujo funcional probado (UI o API) y evidencia registrada.
- [ ] Migraciones aplicadas en las BD reales y `ddl-auto=validate` OK.
- [ ] Colas/consumidores verificados (si aplica).
- [ ] Bandeja verificada con estudiante y apoderado; lecturas aisladas por OID y eventos deduplicados.
- [ ] Datos de prueba limpiados (Entra + BD).
- [ ] Flags devueltos a su estado acordado (`REGISTRO_ASYNC_ENABLED`).
- [ ] Documentación actualizada y entrada nueva en este registro.

## 9. Verificación local EP1/EP2 — 2026-10-19

**Entorno:** rama `feature/rubric-100-ep1-ep2`, commit base `1be2c3ba60a9bac78d44e41af151d8ea6b3af342`, árbol de trabajo con cambios no confirmados. Esta es evidencia de trabajo local; el SHA definitivo y el enlace al artifact de CI se deben añadir al publicar la rama.

### Ejecuciones correctas

- **Backend — OK:** `mvn -f apps/backend/pom.xml -B -ntp package "-Dtest=!*ApplicationTests" "-DfailIfNoTests=false" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtypescript.generator.skip=true"`. Los 14 módulos Maven compilaron y empaquetaron; 252 pruebas reportadas, 0 fallos y 0 errores. 10 pruebas quedaron omitidas. Testcontainers no encontró Docker válido en Windows (respuesta 400 del named pipe), por lo que `RabbitAdminServiceRabbitTest` y `ConfirmadorMensajesRabbitTest` no ejecutaron contra broker; las migraciones de integración también quedaron omitidas por sus requisitos de entorno. Reejecutar estas pruebas en CI Linux con Docker.
- **Backend — OK:** `CoreShareApplicationTests` se ejecutó por separado (excluido del comando anterior por el patrón `!*ApplicationTests`) y pasó 1/1 con propiedades JWT sintéticas.
- **Frontend — OK:** `npm test -- --watch=false`; 13 archivos y 55 pruebas pasaron, incluido el caso nuevo de fallo de adquisición silenciosa del token. Reporte JUnit local: `apps/frontend/dist/test-results/frontend-junit.xml`.
- **CI — OK:** run [39356000842](https://github.com/COdelValle/SIGA-Project/actions/runs/39356000842) sobre el merge `f6908f9664b2f05736ac17a7c7a975ecc63c03c1`; backend 250 pruebas, frontend 55 pruebas, sin fallos, errores ni omitidas. Testcontainers ejecutó RabbitMQ real contra Docker Linux. Artifacts: `backend-surefire-f6908f96…` y `frontend-junit-f6908f96…`.
- **Frontend — OK:** `npx tsc -p tsconfig.app.json --noEmit` y `npm run build -- --configuration production` completaron correctamente.
- **CI/CD — OK:** ambos workflows parsearon como YAML con PyYAML; el script remoto de despliegue extraído de `cd.yml` pasó `bash -n` al normalizar CRLF del checkout Windows.
- **Carga — preparación OK:** `tools/performance/siga-escrituras.js` y `medir-recursos.ps1` se agregaron para cubrir escrituras p95 <3 s y memoria <80%; ambos pasaron validación de sintaxis (`node --check` y parser de PowerShell). No se ejecutaron contra AWS. La medición de evento RabbitMQ <5 s y la integridad sin duplicados sigue requiriendo el procedimiento sintético autorizado.

### Fallos observados y corrección

- **Backend — FALLA de invocación, no del código:** el primer comando Maven ejecutado desde PowerShell sin comillas alrededor de `-Dsurefire.failIfNoSpecifiedTests=false` se separó en argumentos y Maven informó `Unknown lifecycle phase ".failIfNoSpecifiedTests=false"`; no llegó a ejecutar pruebas. Se citaron los argumentos `-D` completos y la ejecución posterior de todos los módulos terminó en `BUILD SUCCESS`.
- **Validación shell local — FALLA de formato:** el primer `bash -n` recibió CRLF convertido por la tubería de PowerShell/Windows y señaló `do\r`. Al enviar el script normalizado como bytes, `bash -n` terminó con código 0; el archivo del workflow no requirió cambios de sintaxis.
- **CI — FALLA inicial y corrección:** el run `39355801789` falló en `ConfirmadorMensajesRabbitTest` porque `basicGet` se ejecutaba antes de que el mensaje estuviera disponible. Se agregó espera acotada para mensajes y conteos (`059c705`); el run posterior `39356000842` quedó verde con el broker real.
- **CD — FALLA de validación y corrección:** el run `39357188491` publicó las 13 imágenes y se detuvo antes de `compose pull/up` porque el guard buscaba la columna `tipo_asignatura`, que ya no pertenece al esquema vigente. La inspección de solo lectura en la EC2 confirmó `siga_asignaturas_db` con las 7 columnas actuales y Flyway V1-V4 exitosos; conteos: asignaturas 22, malla 142, cursos 155, horarios 532, inscripciones 57. Se reemplazó el guard por una comprobación de las columnas actuales; no se requiere migración ni borrado.

**Estado de aceptación:** suite local y CI verdes, incluido RabbitMQ real con Testcontainers. No declarar EP1/EP2 al 100% todavía: faltan autenticación con token Entra efectivo y evidencia PKCE, matriz completa de rutas/JSON, pruebas de carga acordadas, redeploy AWS con health checks y comparación de persistencia antes/después.

### PR-20261116-22 — Carga 4: dashboard de reportes y RabbitMQ configurable

- **Fecha:** 2026-11-16.
- **Rama / commit:** `feat/rabbitmq-topologia-configurable` @ `1f198b1` y `feat/dashboard-reportes-academicos` @ `b79dd44`; mergeados a `dev` en `26fe760` mediante PRs #131 y #132.
- **Alcance:** cerrar #20, #21, #24 y #25; #22 ya estaba en Done y #23 se cierra manualmente.
- **Entorno:** Windows + Docker Desktop, stack Compose completo (`siga-project`), MariaDB 11.4, RabbitMQ 2 nodos, Kafka 3 brokers.
- **Pasos de verificación:**
  1. Backend completo con `mvn -f apps/backend/pom.xml -B test "-Dtest=!*ApplicationTests" "-DfailIfNoTests=false" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtypescript.generator.skip=true"`.
  2. Frontend con `npx tsc -p tsconfig.app.json --noEmit`, `npm test -- --watch=false`, `npm run build -- --configuration production` y `npx playwright test -c e2e/playwright.config.ts e2e/tests/reportes.spec.ts`.
  3. Imágenes Docker reconstruidas para los módulos afectados y `docker compose up -d`.
  4. Verificación de colas, bindings y consumidores con `rabbitmqctl list_queues`, `list_bindings` y `list_consumers`.
  5. Regeneración de `docs/openapi/bff-web.json` con `OpenApiSpecificationTest`.
- **Resultado:** `OK`.
  - Backend: CI en verde (build + tests unitarios); local se validó la compilación de los 17 módulos y tests acotados de mensajería/BFF. Las suites `*ApplicationTests` se excluyeron por requerir BD/Azure; Testcontainers se omite en Windows.
  - Frontend: TypeScript OK; Vitest **288/288**; build de producción OK; Playwright de reportes **2/2** (Docente y Administración).
  - Docker: 35 contenedores del stack activos; los servicios con healthcheck quedaron `healthy` (`ms-kafka-admin` no define healthcheck).
  - RabbitMQ: los nombres de colas, DLQ, exchanges, routing keys, bindings y consumers coinciden con los valores por defecto y no cambiaron respecto de la topología previa.
  - OpenAPI: `bff-web.json` incluye `/reportes/filtros`, `/reportes/filtros/estudiantes`, `CursoReporteDTO`, `EstudianteReporteDTO` y `ReporteFiltrosDTO`.
- **Evidencia de configuración:** `common-properties.yaml` y los `application.yaml` de los servicios contienen `siga.mensajeria.registro.*` y `siga.mensajeria.notificaciones.*`; los tests `PropiedadesMensajeriaTest`, `RegistrationMessagingConfigTest` y `ConfiguracionTopologiaNotificacionesTest` demuestran que cambiar una propiedad modifica la cola/binding sin recompilar.
- **Pendientes / notas:** PRs #131 y #132 mergeados; actualizar las tarjetas de Trello (#20, #21, #24 y #25). #22 ya estaba en Done y #23 se cierra manualmente. No se renombraron colas ni se borraron volúmenes; el smoke se ejecutó sobre el stack local ya levantado.

### PR-20261117-01 — Kafka local: 2a corrida (smoke, alertas y failover)

- **Fecha:** 2026-11-17.
- **Rama:** `dev`.
- **Entorno:** Windows + Docker Desktop, stack Compose completo, Kafka 3 brokers, RabbitMQ 2 nodos, MariaDB `siga_reportes_db`.
- **Resultado:** `OK`.
  - Asistencia `AUSENTE` con 62% de inasistencia ⇒ nivel `CRITICO`, con alertas `INASISTENCIA` y `CAMBIO_ESTADO_ACADEMICO`.
  - Rendimiento 3.2 con promedio 4.85 ⇒ nivel `PREVENTIVO`, con alerta `BAJO_RENDIMIENTO`.
  - Lag 0 en ambos consumer groups tras el procesamiento.
  - Failover de 1 broker: ISR se mantuvo en 2 y los offsets pasaron de `0/0/1` a `1/1/2` (escritura aceptada).
  - Failover de 2 brokers: `NOT_ENOUGH_REPLICAS`, `0 records sent` y offsets sin cambio.
  - RabbitMQ con 2 nodos en `Running Nodes`.
- **Pendientes / notas:** los failovers ya quedaron cubiertos en esta corrida y **no se repitieron** en la sesión de cierre; su incorporación a Allure queda fuera de alcance.

### PR-20261117-02 — Errores/duplicados en runtime y carga 3a en Allure

- **Fecha:** 2026-11-17.
- **Rama / commit:** `dev`.
- **Alcance:** cerrar la evidencia EP3 de Kafka local y categorizar los checks en Allure.
- **Entorno:** mismo stack local (35 contenedores; `kafka-ui` y `ms-kafka-admin` sin healthcheck).
- **Pasos y resultado:** `OK`.
  1. **Payload inválido (A1):** evento `local-bad-a-20261117-01` sin `idEstudiante`. El `DefaultErrorHandler` registró `Mensaje Kafka inválido o no procesable` tras 3 intentos (original + 2 reintentos de 1000 ms), el offset del grupo avanzó de 2 a 3 y no se creó proyección ni evento procesado.
  2. **Duplicado (A2):** `local-dup-a-20261117-01` publicado 2 veces. Log `Evento de asistencia duplicado ignorado`; quedaron exactamente 1 proyección, 1 evento procesado y 1 alerta `INASISTENCIA` (`CRITICO`, 45%).
  3. **Política (A3):** 2 reintentos con `FixedBackOff(1000L, 2L)` + log + descarte. Sin DLQ en Kafka; RabbitMQ mantiene su DLQ.
  4. **Carga broker (B1):** topic temporal `siga.e3.local-load-20261117012719` (3/3, min ISR 2, retención 1 h/1 MiB). `kafka-producer-perf-test`: **20 000 records sent**, **14 577,259 rec/s**, 0 errores, offsets 6820/6386/6794 = 20 000.
  5. **Carga consumidor (B2):** 300 eventos de rendimiento únicos (`idCursoAsignatura=null`, puntaje 6.5). Lag 0, 300 filas en `proyecciones_rendimiento`, 300 en `eventos_reportes_procesados`, 0 alertas.
  6. **Recursos (B3):** muestras `docker stats --no-stream` antes/durante/después sin reinicios ni `unhealthy`; CPU de contenedores bajo 5%. **Desviación:** la RAM del host estaba en 87,8% antes de la carga (86,4% durante/después), por encima del umbral <80%, debido al consumo base del host.
  7. **Alcance (B4):** el objetivo de 20 VUs/10 min y p95 de API no se cubre con la carga 3a porque requiere `Bearer token`.
  8. **Limpieza (C):** 4 alertas, 2 proyecciones de asistencia, 302 de rendimiento y 305 eventos `local-%` eliminados; topics `siga.e3.local-load-*` borrados. Verificación final: 0 filas `local-%`, lag 0 y solo los 2 topics de negocio.
  9. **Allure (D):** `tools/ci/allure-command.mjs` acepta `--layer`, `--epic`, `--feature`, `--story`, `--owner` y `--tag` con defaults idénticos; `kafka-error-policy.mjs` y `kafka-topic-policy.mjs` salen con 0; `tools/performance/kafka-load.mjs` publica métricas como attachment (`broker`: 13 633,265 rec/s y 20 000 offsets; `consumer`: lag 0 en 5,173 s, 300/300 filas, 0 alertas).
- **Incidencia operativa:** Docker Desktop dejó de responder a la API durante la verificación inicial. Se reinició Docker Desktop sin `docker compose down -v`; los contenedores auto-reiniciaron y se revalidaron topics, grupos, lag y salud antes de continuar.
- **Evidencia:** `docs/evidencias/E3-PRES-20261117-01/README.md`.

### PR-20261117-03 — Performance Kafka publicado desde CI en GitHub Pages

- **Fecha:** 2026-11-17.
- **Rama:** `dev`.
- **Alcance:** publicar el grupo `Kafka - Alto flujo` en el Allure Report de Pages, no solo en el reporte local.
- **Cambios:**
  1. El job `backend` publica el jar `ms-reportes-*.jar` como artifact `ms-reportes-jar-<sha>`.
  2. Nuevo job `performance` (`needs: backend`, solo push a `dev`/`main`/`deploy`) que ejecuta `tools/performance/kafka-load-ci.sh`.
  3. El script levanta 3 ZooKeeper, 3 Kafka, `mariadb-reportes` y `rabbitmq1`/`rabbitmq2` con `docker compose`, arranca `ms-reportes` desde el jar en la red `siga-network`, espera healthchecks y ejecuta broker + consumidor.
  4. Sube `performance-allure-<sha>`; `allure-report` lo descarga con el patrón existente y lo publica en Pages.
- **Criterios:** broker 20 000 mensajes de 512 B, 0 errores, ≥1000 rec/s y offsets ≥20 000; consumidor 300 eventos, lag 0 en <120 s, 300 filas en proyecciones, 300 en eventos procesados y 0 alertas.
- **Resultado:** `OK` en el run [44105174173](https://github.com/COdelValle/SIGA-Project/actions/runs/44105174173). El job `performance` terminó en ~2 min; broker `20000` records, `5733.945` rec/s, `20000` offsets y 0 errores; consumidor lag 0 en `5.396` s, `300` proyecciones, `300` eventos procesados y 0 alertas. Pages quedó con 1368 tests (1357 passed) e incluye `Kafka - Alto flujo` con `kafka broker throughput` y `kafka consumer throughput` en `passed`.
