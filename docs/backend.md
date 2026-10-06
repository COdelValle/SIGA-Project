# Documentacion del backend

## 1. Proposito

El backend de SIGA es un proyecto Maven multi-modulo basado en Spring Boot. Separa las responsabilidades del dominio academico en servicios independientes, expone APIs REST protegidas y centraliza los contratos compartidos en la libreria `core-share`.

El nucleo academico es funcional (CRUD, validaciones, busqueda y borrado logico), el BFF ya orquesta `/me` y el perfil de estudiante, y la infraestructura Terraform esta implementada. Las pantallas de negocio del frontend y parte de la orquestacion del BFF siguen pendientes.

## 2. Ubicacion y tecnologias

- Ubicacion: `apps/backend`
- Java 21
- Spring Boot 3.5.0
- Spring Cloud 2025.0.0 (OpenFeign) + Resilience4j
- Maven multi-modulo
- Spring Security y OAuth2 Resource Server con JWT (Azure AD)
- Spring Data JPA e Hibernate
- MapStruct y Lombok
- springdoc 2.8.14 (Swagger UI) + Scalar (starter nativo de springdoc)
- MariaDB (una base por microservicio)
- RabbitMQ 4 (mensajeria asincrona; las variables `SPRING_RABBITMQ_*` las inyecta Docker Compose)

El `pom.xml` padre centraliza versiones, dependencias y modulos. Modulos declarados: `libs/core-share`, `bff-web`, `ms-usuarios-auth`, `ms-estudiantes`, `ms-asignaturas`, `ms-notas`, `ms-docentes`, `ms-apoderados`, `ms-clases`, `ms-evaluaciones`, `ms-asistencias` y `ms-notificaciones` (queda comentado `ms-auditoria`, con puerto reservado `8082`). `ms-notificaciones` es el consumidor de las colas RabbitMQ (actualmente la cola de evaluaciones; solo registra en log).

## 3. Componentes del backend

### 3.1 BFF Web

Ubicacion: `apps/backend/bff-web`

Puerta de entrada pensada para el frontend: oculta la topologia interna, coordina solicitudes y entrega respuestas orientadas a la interfaz.

Responsabilidades previstas:

- Exponer endpoints consumibles por Angular.
- Validar el token y propagar el contexto de seguridad.
- Orquestar llamadas a los microservicios (Feign).
- Unificar respuestas y errores.
- Evitar que el navegador dependa de las direcciones internas.

Estado actual: **orquesta `/me` y el perfil de estudiante**. Incluye:

- `MeController` (`GET /api/me`): resuelve el usuario via `UsuarioClient` y compone `{ id, email, displayName, roles }`.
- `PerfilEstudianteController` (`GET /api/bff/v1/estudiantes/perfil/{idExterno}`): agrega estudiante, asignaturas y notas resolviendo evaluacion -> asignatura, usando `EstudianteClient`, `AsignaturaClient`, `NotaClient` y `EvaluacionClient` con mappers.
- `ApoderadoPupiloController` (`PUT /api/bff/v1/apoderados/pupilos/{idEstudiante}`): valida el vinculo apoderado-estudiante (via `ms-apoderados`) y actualiza el pupilo en `ms-estudiantes`.
- Clientes Feign con fallback para usuarios, estudiantes, asignaturas, notas y evaluaciones (`integration/*`).
- `FeignClientConfig` con propagacion del `Authorization`.

Pendiente: orquestar el resto de recursos de la interfaz.

### 3.2 Servicio de usuarios y autenticacion

Ubicacion: `apps/backend/ms-usuarios-auth` · puerto `8081`

Administra usuarios, roles y estado de las cuentas, en conjunto con los claims del JWT de Azure AD.

Incluye:

- Entidad `Usuario` (PK = `id` de Azure, rol, estado).
- Entidad `InvitacionUsuario` (`invitaciones_usuarios`: correo, rol, estado, quién invitó y cuándo).
- `UsuarioRepository` (JPA + Specifications), `UsuarioSpecifications`, `InvitacionUsuarioRepository`.
- `UsuarioService`, `InvitacionVinculacionService` y `UsuarioMapper` (MapStruct).
- `UsuarioController` en `/api/v1/usuarios`: CRUD, `GET /me`, `GET /lookup?email=`,
  `POST /invitaciones/lote` (JSON), `POST /invitaciones/lote/csv` (multipart),
  `POST /{id}/sync-roles` y los endpoints del registro asincrono
  (`POST /registraciones/async`, `GET /registraciones/{processId}`,
  `GET /registraciones/{processId}/credencial`, `POST /{id}/reset-password`).
- Integracion con **Microsoft Graph** (opcional via `AZURE_CLIENT_SECRET`): pre-registro por correo, cambio de rol, sincronizacion de app roles y **aprovisionamiento de cuentas** (`POST /users` + clave temporal con cambio obligatorio).
- DTOs de registro, invitacion, actualizacion y respuesta en `core-share`.
- **Registro asincrono** (migraciones `V5`-`V9`): `UserRegistrationProcess`,
  `UserRegistrationAttempt`, `ProcessedRegistrationEvent` y `RegistrationOutboxEvent`
  (V5-V7); `usuarios.full_name` (V8) y la normalizacion de nombres de seed (V9);
  `AsyncUserRegistrationService` (orquestacion), `RegistrationStateService`
  (transacciones `REQUIRES_NEW` para no perder resultados ante reintentos),
  `RegistrationOutboxPublisher` (confirmaciones del broker + backoff),
  `CredentialCipher` (AES-256-GCM para la clave temporal en reposo) y
  `EmailInstitucionalGenerator` (correo y nombre completo derivados).
- **Integracion Graph ampliada**: `ProvisionedUser`, `PasswordGenerator`,
  `GraphTransientException`/`GraphPermanentException` (clasificacion de errores
  para reintento vs fallo permanente) y `RegistroAsyncProperties` (flags, dominio
  de correo y parametros del flujo).

Comportamiento:

- `POST` sin `id` resuelve el `oid` con Graph si esta configurado; si no, crea una
  **invitacion** (`202`) que se vincula en el primer login. Con `id` explicito el
  usuario queda `ACTIVO` (`201`); esa via es solo de compatibilidad (el oid no
  deberia ingresarse a mano).
- **Vinculacion automatica**: en `GET /me`, si el `oid` del token no tiene fila,
  se busca una invitacion por el correo del JWT (`preferred_username`/`email`/`upn`)
  y se crea el usuario `ACTIVO` con el rol invitado. Sin invitacion se responde
  `404` (acceso cerrado: nadie se auto-registra).
- `DELETE` aplica **borrado logico** (estado `INACTIVO`); los inactivos no se devuelven en consultas.
- `GET /search` filtra por `email`, `rol` y `state`.

Seguridad: `hasRole('ADMIN')` combinado con `hasAuthority('SCOPE_usuarios:read|write|update|delete')`.

#### Registro asincrono de usuarios

Flujo unico SPA -> BFF -> ms-usuarios-auth -> RabbitMQ -> Graph + microservicio de rol:

1. `POST /api/bff/v1/admin/registraciones` (ADMIN + `usuarios:write`) valida el
   payload tipado por rol, que el correo este libre y que no exista otro proceso
   en curso; para APODERADO valida los pupilos por Feign con el token del
   administrador. Responde `202` con `processId`.
2. El evento inicial `v1` se persiste en el **outbox**
   (`user_registration_outbox`) en la misma transaccion del proceso y
   `RegistrationOutboxPublisher` lo publica con confirmacion del broker,
   backoff exponencial y limite de intentos (`user.registration.azure`).
3. `ms-usuarios-auth` consume: crea la cuenta en Entra ID si no existe
   (idempotente por correo/UPN), fija la clave temporal con
   `forceChangePasswordNextSignIn=true`, asigna el app role y persiste
   `Usuario` `ACTIVO` + la invitacion previa como `VINCULADA`. La clave se
   cifra (AES-256-GCM) y vence a las 48 h.
4. En la misma transaccion de exito se encola el evento de dominio (ya con
   `userId`) hacia `user.register.estudiante|docente|apoderado`. Los
   consumidores son idempotentes por `id_usuario` (reentrega = exito) y
   responden a `user.registration.status`; sus DLQ reportan el fallo agotado.
5. El estado se consulta con polling (`GET /registraciones/{processId}`) y la
   clave se entrega **una sola vez** al administrador iniciador
   (`GET /registraciones/{processId}/credencial`; `409` si aun no esta lista o
   ya se entrego, `410` si expiro) con `POST /usuarios/{id}/reset-password`
   como respaldo.

Estados del proceso: `PENDIENTE`, `EN_PROCESO`, `COMPLETADO`, `FALLIDO`; por
etapa: `azureState` y `domainState` (`PENDIENTE|COMPLETADO|FALLIDO`).

Configuracion: `REGISTRO_ASYNC_ENABLED` (default `false`),
`REGISTRO_CRED_KEY` (clave AES-256 en Base64 de 32 bytes, obligatoria al
habilitar), `REGISTRO_NOTIFY_CREDENTIALS_ENABLED` (emite
`user.credentials.notify` para el futuro envio de la clave por correo) y
`REGISTRO_EMAIL_DOMAIN` (default `platformsiga.onmicrosoft.com`). Requiere
Graph con permisos de escritura de directorio y consentimiento de administrador;
si falta `AZURE_CLIENT_SECRET` el proceso falla de forma permanente y visible.

Correo y nombre completo (automaticos): si la solicitud no trae `email`/`fullName`
se derivan del rol. El correo usa la **primera palabra** del primer nombre y del
primer apellido, sin tildes/ñ (`jose.munoz@…`), y ante colisiones intenta
`nombre.apellido1apellido2@` y luego un sufijo numerico (`…2@`, `…3@` hasta 99),
truncando el local part a 64 caracteres. Una cuenta **deshabilitada** no cuenta
como colision: se reutiliza para reactivar. Si el nombre no deja caracteres
utilizables se responde 400.

Offboarding y reactivacion: `DELETE /usuarios/{id}` marca `INACTIVO` y en Entra
ID revoca sesiones y app roles y deshabilita el login (`accountEnabled=false`),
sin borrar el objeto de directorio (los datos y el correo se conservan). Al
reactivar el usuario (`ACTIVO`) o re-registrar el mismo correo en el flujo
asincrono, se rehabilita la cuenta y se reasigna el app role; los perfiles de
dominio (`ms-estudiantes`, `ms-docentes`, `ms-apoderados`) se **reactivan** en
vez de rechazarse si estaban inactivos.

Selectores del admin (BFF): `GET /api/bff/v1/admin/clases?anioAcademico=`
(clases activas del año), `GET /api/bff/v1/admin/estudiantes?q=` (busqueda por
RUT con o sin puntos y por nombres), `GET /api/bff/v1/admin/usuarios/{id}`
(detalle de la cuenta + resumen del perfil del rol) y
`DELETE /api/bff/v1/admin/usuarios/{id}`. La tabla de usuarios muestra el nombre
completo (`usuarios.full_name`, derivado en el registro asincrono) y "Sin nombre"
cuando la cuenta es legacy sin nombre.

### Estandar de datos (RUT y nombres)

- **RUT**: se almacena canonico, sin puntos y con `K` mayuscula (`13789943-2`).
  La mascara del formulario muestra puntos y agrega el guion al escribir el DV;
  la validacion `@RUT` sigue activa en la API.
- **Nombres**: capitalizacion inteligente (`core-share`
  `format.NombrePropio`): Titulo con particulas en minuscula (`Maria del
  Carmen`), prefijos `Mc`/`Mac`, apostrofes y guiones. Se aplica en el frontend
  (en vivo) y en el backend antes de guardar (`prePersist`/servicios y
  `EmailInstitucionalGenerator.nombreCompleto`).
- **Data de ejemplo**: las migraciones `ms-estudiantes` V7, `ms-docentes` V7,
  `ms-apoderados` V5 y `ms-usuarios-auth` V9 normalizan los datos de seed (y
  quitan puntos de los RUT en todas las filas) en cualquier entorno nuevo o
  existente.
- **Data real previa** (filas no seed creadas antes del cambio, si existen):
  ejecutar una vez `tools/normalizar-datos.ps1` por entorno; lee `.env`, usa los
  contenedores de docker y aplica las mismas reglas (nombres, RUT y
  `usuarios.full_name`).

Topologia RabbitMQ (declarada desde `core-share` con `RegistrationMessagingConfig`
solo en los servicios con AMQP): exchange `user.topic.exchange`; colas
`user.azure.sync.queue`, `ms.estudiantes.queue`, `ms.docentes.queue`,
`ms.apoderados.queue`, `user.registration.status.queue`,
`user.credentials.notify.queue` y una DLQ por cola.

Pendientes: reconciliacion de OIDs de los datos mock y carga masiva por CSV en la UI.

### 3.3 Servicio de estudiantes

Ubicacion: `apps/backend/ms-estudiantes` · puerto `8083`

Administra la ficha personal y academica de los estudiantes.

Incluye:

- Entidad `Estudiante` (RUT validado, nombres/apellidos, fecha de nacimiento, alergias, estado).
- Repositorio, service, mapper y `EstudianteSpecifications`.
- `EstudianteController` en `/api/v1/estudiantes`.

Endpoints:

- `GET /{id}`, `GET /idUsuario/{idUsuario}`, `GET /search` (rut, nombres, apellidos, rango de fecha, estado) y `GET /search?q=` (texto libre por RUT con o sin puntos o por nombre, minimo 2 caracteres).
- `GET /exists/{id}` (verificacion de existencia; usado por otros servicios).
- `POST`, `PUT /{id}`, `DELETE /{id}`.

Comportamiento y reglas:

- Normaliza el RUT a canonico sin puntos (`13789943-2`, `K` mayuscula) con `RutNormalizer` y capitaliza los nombres con `NombrePropio` (particulas en minuscula, `Mc`/`Mac`, apostrofes y guiones) antes de guardar; la busqueda por RUT usa el formato canonico.
- Si `saveEstudiante` recibe un `idUsuario` con perfil `INACTIVO`, lo **reactiva** actualizando sus datos en vez de rechazarlo.
- `DELETE` aplica **borrado logico** (`State.INACTIVO`); `State` se centraliza en `core-share`.
- Valida RUT chileno con `@RUT`.

### 3.4 Servicio de asignaturas

Ubicacion: `apps/backend/ms-asignaturas` · puerto `8086`

Modela el catalogo de asignaturas separado de su dictacion por curso:

- `Asignatura`: catalogo general (nombre oficial unico, descripcion, area, `calificable`, `active`).
- `MallaCurricular`: que asignatura aplica a cada `Nivel`, con `caracter` (OBLIGATORIA/OPTATIVA/ELECTIVA), `plan` (COMUN o diferenciada) y horas.
- `CursoAsignatura`: dictacion concreta (asignatura + clase + docente + semestre + `caracter` + `cupo_maximo`); de ella cuelgan `horarios` e `inscripciones`.

Endpoints:

- `GET/DELETE /api/v1/asignaturas/{id}`, `GET /search?nombre=&area=&calificable=`, `GET /exists/{id}`, `POST`, `PUT /{id}`.
- `GET /api/v1/malla?nivel=&plan=`, `POST`, `PUT /{id}`, `DELETE /{id}`.
- `GET/DELETE /api/v1/curso-asignaturas/{id}`, `GET /search?idClase=&idDocente=&idAsignatura=&caracter=&semestre=&area=&nombre=&conCupoDisponible=`, `GET /exists/{id}`, `POST`, `PUT /{id}` y `GET /validar-mineduc`.
- `horarios` e `inscripciones` referencian `curso_asignatura_id`.

Comportamiento:

- La dictacion valida por Feign que la clase (`ms-clases`) y el docente (`ms-docentes`) existan, y que la asignatura este en la malla del nivel de la clase.
- Solo las dictaciones OPTATIVA/ELECTIVA admiten cupo e inscripciones; `calificable = false` (Orientacion) no admite evaluaciones.
- `DELETE` aplica **borrado logico** (`active = false`); listados y `exists` solo consideran activos.

### 3.5 Servicio de notas

Ubicacion: `apps/backend/ms-notas` · puerto `8089`

Registra y consulta calificaciones asociadas a evaluaciones.

Incluye:

- Entidad `Nota` (`idEstudiante`, `idEvaluacion`, `score`, `active`).
- Repositorio, service, mapper y `NotaSpecifications`.
- `NotaController` en `/api/v1/notas`.

Endpoints:

- `GET /{id}`, `GET /search` (por estudiante, evaluacion y rango de nota).
- `POST`, `PUT /{id}`, `DELETE /{id}`.

Comportamiento e integracion:

- Valida el rango de la nota (1.0 a 7.0) con `@ChileanGrade`.
- **Integracion Feign**: antes de guardar, verifica la existencia del estudiante (`ms-estudiantes`) y de la evaluacion (`ms-evaluaciones`) usando sus endpoints `exists`.
- **Resilience4j**: circuit breaker y timeouts configurados; si un servicio no responde, se aplica el fallback y se responde `503`.
- Propagacion del token: un `RequestInterceptor` reenvia el `Authorization` entrante en las llamadas Feign.
- `DELETE` aplica **borrado logico** (`active = false`).

### 3.6 Servicios de docentes, apoderados, clases, evaluaciones y asistencias

- **`ms-docentes`** · puerto `8085`: entidades `Docente` (idUsuario, RUT, nombres, fecha de contratacion, area academica, activo) y `Certificado`; CRUD + busqueda + `exists` + certificados como subrecurso (`/api/v1/docentes/{id}/certificados`). Scopes `docentes:*`.
- **`ms-apoderados`** · puerto `8084`: entidad `Apoderado` (telefonos y estudiantes a cargo con parentesco); CRUD + `idUsuario` + busqueda + `exists` + alta/baja de estudiantes; Feign a `ms-estudiantes`. Scopes `apoderados:*`.
- Los tres dominios consumen su cola del registro asincrono (`messaging/`, idempotencia por `id_usuario`), publican el resultado a `user.registration.status` y **reactivan** el perfil si existia `INACTIVO` (docentes y apoderados reemplazan sus colecciones: certificados, telefonos y vinculos).
- **`ms-clases`** · puerto `8087`: entidad `Clase` (nivel, letra, anio academico, docente jefe, activo) con unicidad nivel+letra+anio; CRUD + busqueda + `exists` + `PUT /{id}/docente-jefe`; Feign a `ms-docentes`. Scopes `clases:*`.
- **`ms-evaluaciones`** · puerto `8088`: entidad `Evaluacion` (nombre, tipo, ponderacion, `idCursoAsignatura`, activa); CRUD + busqueda + `exists`; Feign a `ms-asignaturas` (valida dictacion activa y calificable). Scopes `evaluaciones:*`.
- **`ms-asistencias`** · puerto `8090`: entidad `Asistencia` (idEstudiante, `idCursoAsignatura`, fecha, estado, justificacion, observacion, activa) con unique `(estudiante, dictacion, fecha)`; CRUD + busqueda por estudiante/dictacion/rango/estado; Feign a `ms-estudiantes` y `ms-asignaturas`. Scopes `asistencias:*`. Seed con Camila y Lilith (03-08 a 02-10-2026, solo dias habiles y segun horario).

### 3.7 Biblioteca compartida

Ubicacion: `apps/backend/libs/core-share`

Centraliza elementos reutilizables, sin logica de un dominio especifico:

- **DTOs** de usuario, estudiante, asignatura (catalogo y dictacion), malla, notas y asistencias (incluye `PageResponseDTO` para clientes Feign) y los contratos del registro asincrono (`UserRegistrationEventDTO`, estado del proceso, credencial y payloads tipados por rol).
- **Enums**: `Rol`, `StateUsuario`, `StateInvitacion`, `RegistrationProcessState`/`RegistrationStepState`/`RegistrationStepType` (registro asincrono), `State` (estudiante) y `State`/`Justificacion` (asistencia).
- **Validadores**: `@RUT` (RUT chileno), `@Phone` y `@ChileanGrade` (nota 1.0 a 7.0).
- **Utilidades de formato**: `format.RutNormalizer` (RUT canonico sin puntos) y `format.NombrePropio` (capitalizacion inteligente usada por backend y espejada en el frontend).
- **Seguridad**: `SharedSecurityConfig` (filtro stateless, rutas publicas de salud/documentacion y conversion de claims `scp`/`roles` a scopes/roles) y `SecurityUtils`.
- **Excepciones**: `BusinessException`, `ResourceNotFoundException`, `BadRequestException`, `ServiceUnavailableException`, `GlobalExceptionHandler` y `ErrorResponseDTO`.
- **OpenAPI**: `SharedOpenApiConfig` (esquema `bearerAuth`).
- **Fechas**: `CommonDateFormatConfig` (ISO 8601 `yyyy-MM-dd` para JSON y parametros) y `SharedFeignFormatConfig` (registra el mismo formato en el conversion service de OpenFeign, que no usa los formatters de MVC; sin el, Feign serializaba `from`/`to` con el formato corto localizado `6/10/26`). El frontend normaliza a `dd/MM/yyyy` solo para mostrar.
- **Mensajeria**: `RegistrationMessagingConfig` (exchange, colas por etapa/rol y DLQ), `RegistrationMessagingConstants` (nombres y routing keys), `RegistrationCorrelation` (correlation id en headers AMQP/MDC) y `RegistrationStatusPublisher` (resultado de cada paso).

Se registra mediante `META-INF/spring/...AutoConfiguration.imports`.

## 4. Seguridad y contratos

- OAuth2/JWT con Azure AD (`spring.cloud.azure.active-directory`).
- Autorizacion por metodo: roles (`hasRole`) y scopes (`hasAuthority('SCOPE_...')`).
- **Ownership por `oid`**: APODERADO/DOCENTE/ESTUDIANTE solo modifican sus propios recursos (`SecurityUtils.requireOwnerOrAdmin`).
- Rutas publicas limitadas a salud y documentacion tecnicas.
- El frontend no accede a los microservicios: lo hara a traves del BFF.
- Los contratos se definen en cada servicio y se reflejan en los DTOs de `core-share` y en los modelos TypeScript del frontend.
- **Autorizacion unificada**: lecturas con `hasAuthority('SCOPE_x:read')`; escrituras con `hasRole(...) and hasAuthority('SCOPE_x:write|update|delete')` (ADMIN incluido). Los 38 scopes granulares se mantienen y deben exponerse/consentirse en Entra ID (ver [`testing-login.md`](testing-login.md)).

## 5. Datos y configuracion

- Patron **database-per-service**: cada microservicio tiene su propia MariaDB.
- `docker-compose.yml` levanta 8 MariaDB, los 8 microservicios, el BFF, el frontend y **RabbitMQ** (mensajeria; credenciales por `RABBITMQ_USER`/`RABBITMQ_PASS`).
- **Formato de fecha (API): ISO 8601 `yyyy-MM-dd`** para `LocalDate` (JSON y parametros de URL), definido en `CommonDateFormatConfig`. *Cambio de contrato:* antes se usaba `dd/MM/yyyy`; los consumidores deben enviar/esperar `yyyy-MM-dd` (p. ej. `birthDate`, `from`/`to`). El frontend normaliza a `dd/MM/yyyy` solo para mostrar.
- **Busquedas paginadas**: los `GET /search` devuelven `Page<T>` (`page`, `size`, `sort`; default 20, maximo 100). Contrato, ejemplos y guia en [`paginacion.md`](paginacion.md).
- **Borrado logico**: todos los `DELETE` son idempotentes. La mayoria usa un booleano `active`/`activo` (asignaturas, horarios, notas, docentes, apoderados, clases, evaluaciones); estudiantes y usuarios usan el enum `State`/`StateUsuario`. Convencion a unificar en el futuro.
- **Enums** (ubicacion actual, deuda de consistencia): `Rol`/`StateUsuario` en `dto/usuario/enums`, `State` en `dto/estudiante/enums`, `AreaAcademica` en `coreshare/enums`.
- **Seeds**: las migraciones `V2` insertan datos de ejemplo con IDs fijos e `INSERT IGNORE`; tenerlo en cuenta antes de cargar datos reales.
- **Specifications**: cada servicio mantiene su `isActive()` local (core-share no depende de JPA); un helper compartido queda como mejora futura.
- En desarrollo, `docker-compose` inyecta `SPRING_DATASOURCE_*`, `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` (Flyway gestiona el esquema) y las variables de Azure; no se requieren ficheros `application-*.yml` extra.
- Variables principales: `DB_HOST`, `DB_USER`, `DB_PASS`, `AZURE_TENANT_ID`, `AZURE_CLIENT_ID`, `AZURE_APP_ID_URI`, `AZURE_CLIENT_SECRET`, `AZURE_API_APP_ID`, `MS_ESTUDIANTES_URL`, `MS_ASIGNATURAS_URL`, `MS_NOTAS_URL`, `MS_DOCENTES_URL`, `MS_CLASES_URL`, `MS_EVALUACIONES_URL`, `RABBITMQ_USER`, `RABBITMQ_PASS` (estas dos se exponen como `SPRING_RABBITMQ_*`) y, para el registro asincrono, `REGISTRO_ASYNC_ENABLED`, `REGISTRO_CRED_KEY`, `REGISTRO_NOTIFY_CREDENTIALS_ENABLED` y `REGISTRO_EMAIL_DOMAIN` (ver `.env.example`; generar la clave con `openssl rand -base64 32`).
- **Prerrequisitos de Graph para el flujo real**: permisos de aplicacion `User.Read.All`, `User.ReadWrite.All` y `AppRoleAssignment.ReadWrite.All` con consentimiento de administrador, y el rol de directorio **User Administrator** asignado a la app (lo exige Graph para actualizar `passwordProfile` en cuentas existentes: `reset-password` y reutilizacion). Para asignar ese rol se necesita temporalmente `RoleManagement.ReadWrite.Directory`.

## 6. Documentacion API

Servida por cada servicio:

- `/v3/api-docs` y `/v3/api-docs.yaml`: especificacion OpenAPI.
- `/docs/swagger`: Swagger UI (redirige a `/docs/swagger-ui/index.html`).
- `/docs/scalar`: Scalar UI.

Nota de version: se usa **springdoc 2.8.14** por compatibilidad con Spring Boot 3.5 / Spring Framework 6.2 (2.6.0 fallaba al generar el OpenAPI y 2.8.15+ tenia una regresion de patrones de ruta).

## 7. Estado y orden recomendado

1. ~~Completar la orquestacion del BFF~~ (hecho para `/me`, perfil de estudiante, pupilos, cursos, notas/asistencias del docente y el modulo admin: registro asincrono, selectores de clases/alumnos, detalle y eliminacion).
2. ~~Conectar las pantallas del frontend a traves del BFF~~ (hecho para los portales y el admin; quedan recursos sin endpoint).
3. ~~Registro asincrono de usuarios~~ (hecho: outbox, aprovisionamiento Graph, credencial de un solo uso, consumidores por rol, BFF y UI con polling). Pendiente acotado: propagar la desactivacion del usuario a los perfiles de dominio por evento (`user.account.disabled/enabled`) y la carga masiva por CSV en la UI.
4. ~~Definir migraciones de esquema para produccion~~ (hecho: Flyway en los 8 microservicios + indices; el registro asincrono agrega `V5`-`V9` en `ms-usuarios-auth` y migraciones de normalizacion en los dominios).
5. Ampliar pruebas unitarias, de integracion y de contrato (nuevas suites de orquestacion asincrona, credencial, correo, normalizacion, busqueda `q`, admin BFF y migraciones `V5`-`V9`; ver [`registro-pruebas.md`](registro-pruebas.md)).
6. Completar la observabilidad (logs estructurados, metricas, tracing); el correlation ID ya esta implementado (HTTP, Feign y AMQP).
7. Incorporar el servicio futuro `ms-auditoria`; docentes, apoderados, clases, evaluaciones y asistencias ya estan implementados.
8. ~~Resolver la decision de autorizacion~~ (hecho: se mantienen los 38 scopes con politica unificada; falta exponerlos/consentirlos en Azure).
9. Reconciliacion de OIDs de los datos mock.
