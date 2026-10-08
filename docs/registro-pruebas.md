# Registro de pruebas y pasos

Bitácora reutilizable para dejar constancia de las pruebas ejecutadas y de los pasos
necesarios para reproducirlas, sesión a sesión. Complementa a
[`testing-login.md`](testing-login.md) (guía de login/infraestructura) y a
[`backend.md`](backend.md) (contratos y comportamiento).

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
| Frontend | specs Vitest de `app`, `docente-notas.service`, `perfil-estudiante.service`, `asistencia.service`, `notification.service` y `NotificationBellComponent` |

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
