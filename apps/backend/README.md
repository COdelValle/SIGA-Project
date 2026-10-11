# Backend SIGA

Backend del sistema de gestión académica **SIGA**. Es un proyecto **Maven
multi-módulo** basado en **Spring Boot 3.5 / Java 21** que separa el dominio
académico en microservicios independientes, expone APIs REST protegidas con
OAuth2/JWT (Azure AD) y centraliza los contratos en la librería `core-share`.

El núcleo académico completo (estudiantes → clases → asignaturas → evaluaciones →
notas, más asistencias) y la orquestación de portales del BFF son funcionales.
La documentación ampliada está en [`docs/backend.md`](../../docs/backend.md).

## Módulos

| Módulo | Puerto | Responsabilidad |
| --- | --- | --- |
| `libs/core-share` | — | DTOs, validadores, seguridad, excepciones y OpenAPI compartidos. |
| `bff-web` | 8080 | Backend For Frontend: `/api/me` y orquestación por Feign. |
| `ms-usuarios-auth` | 8081 | Usuarios, roles, `/me` y sincronización con Microsoft Graph. |
| `ms-estudiantes` | 8083 | Ficha personal y académica del estudiante. |
| `ms-asignaturas` | 8086 | Asignaturas (básicas/electivas), horarios e inscripciones. |
| `ms-notas` | 8089 | Calificaciones; valida estudiante y evaluación por Feign. |
| `ms-docentes` | 8085 | Docentes y certificados. |
| `ms-apoderados` | 8084 | Apoderados, teléfonos y estudiantes a cargo. |
| `ms-clases` | 8087 | Cursos (nivel/letra/año) y docente jefe. |
| `ms-evaluaciones` | 8088 | Evaluaciones por asignatura (tipo y ponderación). |
| `ms-asistencias` | 8090 | Asistencias por estudiante/asignatura (unique por fecha y soft delete). |
| `ms-notificaciones` | 8091 | Consume eventos académicos y mantiene la bandeja in-app de estudiantes y apoderados. |
| `ms-rabbitmq-admin` | 8092 | API REST para administrar la topología RabbitMQ (colas, exchanges y bindings). |

El módulo `ms-auditoria` sigue declarado como futuro (comentado en el POM padre);
su puerto queda **reservado**: `8082`.

### Mensajería (RabbitMQ)

Productor-consumidor sobre el **clúster de dos nodos** (`rabbitmq1`/`rabbitmq2`,
misma EC2) con **intercambio tipo Topic**. Los servicios se conectan con
`SPRING_RABBITMQ_ADDRESSES` y la topología se declara una sola vez desde
`core-share`:

- Intercambio: `intercambio-notificaciones` (compartido para las 3 colas de notificaciones).
- Cola 1: `cola-notificaciones-evaluaciones`, enlazada con el patrón `evaluacion.*`
  (`evaluacion.creada`, `evaluacion.actualizada`, `evaluacion.eliminada`).
- Cola 2: `cola-notificaciones-asistencias`, enlazada con el patrón `asistencia.*`
  (clave `asistencia.registrada`). Se publica solo al **crear** (POST) con estado
  `AUSENTE` o `ATRASADO`; el PUT de justificación no notifica. El mensaje lleva el
  porcentaje de inasistencia del mes (faltas `AUSENTE` / registros del estudiante en
  la dictación) y el campo `superaUmbralInasistencia` (>= 60%).
- Cola 3: `cola-notificaciones-notas`, enlazada con el patrón `nota.*`
  (`nota.creada`, `nota.actualizada`). Se publica al **crear** y **modificar**;
  el DELETE lógico no notifica. El POST que reactiva una nota borrada
  lógicamente se avisa como `ACTUALIZADA` (la fila ya existía).
- Productores: `ms-evaluaciones`, `ms-asistencias` y `ms-notas` guardan el evento en
  un outbox dentro de la misma transacción que el CRUD. El dispatcher publica con
  confirmación RabbitMQ, reintentos acotados e idempotencia por `idEvento`.
- Consumidor: `ms-notificaciones` persiste cada evento una sola vez. Al consultar
  la bandeja, deriva el OID del JWT y resuelve estudiante/apoderado y dictaciones
  autorizadas usando el mismo token propagado por Feign. Las lecturas se guardan
  por OID para que estudiante y apoderado mantengan estados independientes.
- Mensajes específicos: los productores agregan el nombre de la asignatura y de la
  evaluación al evento (si el Feign falla, se publica igual y el consumidor usa un
  texto de respaldo). El resumen nunca incluye la nota.
- Canal de esta etapa: campana y bandeja in-app compartida en los portales de
  estudiante y apoderado; **no envía correo ni accede a Gmail/Exchange**. Los
  consumidores usan **ACK/NACK manuales** (`ConfirmadorMensajes`): ACK al
  persistir (o si el evento ya existía, por idempotencia), reintentos acotados en
  memoria con backoff y, al agotarse o ante un evento inválido, NACK sin requeue
  hacia la DLQ del tipo. Cada cola deriva al direct `siga.dlx.direct` y cada DLQ
  está enlazada con su nombre como routing key. Eventos que agoten ocho intentos
  de publicación quedan en estado `FALLIDO` en el outbox para diagnóstico y
  reproceso manual.
- El **retry del contenedor queda desactivado** (`spring.rabbitmq.listener.simple.retry.enabled=false`,
  también `SPRING_RABBITMQ_LISTENER_SIMPLE_RETRY_ENABLED=false` en Compose): con
  ACK manual, el recoverer por defecto no rechaza el mensaje y este quedaría
  retenido sin confirmar. Los reintentos viven en `ConfirmadorMensajes`; un error
  de conversión es fatal y deriva de inmediato a la DLQ.
- API de la bandeja (vía BFF): `GET .../me`, `GET .../me/no-leidas/count`,
  `PATCH .../me/{id}/leida`, `PATCH .../me/leidas` (marcar todas) y
  `DELETE .../me/leidas` (limpiar mis leídas; oculta solo para el OID actual sin
  borrar la fila compartida).
- Retención: las notificaciones se conservan; una purga programada elimina las
  mayores a `siga.notificaciones.retencion-dias` (90 por defecto). Los
  microservicios fijan `TZ=America/Santiago` para que las horas de la bandeja
  queden en hora de Chile.
- La resolución de destinatarios se cachea por OID 3 minutos para que el contador
  (la campana consulta cada 10 s y al volver a la pestaña) no repita llamadas
  Feign; cambios de vínculos/inscripciones pueden tardar hasta ese TTL.
- El PATCH del BFF hacia `ms-notificaciones` usa el cliente Apache HC5
  (`feign-hc5`): el cliente por defecto de Feign no soporta PATCH.
- Nombres en español en `core-share`: `NombresMensajeria` (exchange topic, colas y
  DLQ), `ConfiguracionTopologiaNotificaciones` (exchange directo `siga.dlx.direct`
  y bindings), `ConfiguracionMensajeriaCompartida` (conversor JSON, `RetryTemplate`
  y `ConfirmadorMensajes`) y `EventoEvaluacion`/`AccionEvaluacion`.
- Administración del broker: `ms-rabbitmq-admin` (`/api/v1/rabbitmq/**`) declara y
  elimina colas, exchanges y bindings con `AmqpAdmin` y DTO validados; el BFF
  reexpone la API bajo `/api/bff/v1/admin/rabbitmq/**`.

## Tecnologías

- Java 21 · Spring Boot 3.5.0 · Spring Cloud 2025.0.0 (OpenFeign) + Resilience4j.
- Spring Security / OAuth2 Resource Server con JWT (Azure AD).
- Spring Data JPA + Hibernate · MapStruct · Lombok.
- MariaDB 11.4 (una base por microservicio) + Flyway.
- RabbitMQ 4 en **clúster de dos nodos** (`rabbitmq1`/`rabbitmq2` sobre la misma EC2);
  topología centralizada en `core-share`, ACK/NACK manuales con reintentos y DLQ
  por cola, y `ms-rabbitmq-admin` para operar el broker por REST.
- springdoc 2.8.14 (Swagger UI + Scalar).

## Requisitos

- **JDK 21** y **Maven** (hay wrappers `mvnw` / `mvnw.cmd` en cada módulo).
- **MariaDB** accesible (o Docker Compose, que la provee).

## Comandos

Todos los comandos parten del POM padre `apps/backend/pom.xml`.

```bash
# Compilar e instalar todo (sin tests)
mvn -f apps/backend/pom.xml -DskipTests install

# Build + tests unitarios (excluye contextLoads, que requieren BD y Azure)
mvn -f apps/backend/pom.xml -B package \
  -Dtest='!*ApplicationTests' -DfailIfNoTests=false

# Ejecutar un módulo (compila dependencias con -am)
mvn -f apps/backend/pom.xml -pl ms-notas -am spring-boot:run

# Ejecutar un test concreto
mvn -f apps/backend/pom.xml -pl ms-usuarios-auth test -Dtest=UsuarioServiceTest
```

> `typescript.generator.skip=true` evita que `bff-web` escriba modelos TypeScript
> fuera del workspace (lo usa el CI).

## Endpoints

> Los `GET /search` son **paginados** (`page`, `size`, `sort`); ver
> [`docs/paginacion.md`](../../docs/paginacion.md).

### BFF Web (`:8080`)

| Método | Ruta | Autorización | Descripción |
| --- | --- | --- | --- |
| `GET` | `/api/me` | Autenticado | Usuario actual (id, email, displayName, rol). |
| `GET` | `/api/bff/v1/estudiantes/perfil/me` | `SCOPE_estudiantes:read` + `asignaturas:read` + `notas:read` | Perfil del estudiante autenticado (resuelve el `oid` del token). |
| `GET` | `/api/bff/v1/estudiantes/perfil/{id}` | `SCOPE_estudiantes:read` + `asignaturas:read` + `notas:read` | Perfil agregado: estudiante → clase → asignaturas → evaluaciones → notas (incluye horarios y docente). |
| `GET` | `/api/bff/v1/apoderados/pupilos` | `APODERADO` + `SCOPE_apoderados:read` | Pupilos del apoderado con curso. |
| `PUT` | `/api/bff/v1/apoderados/pupilos/{idEstudiante}` | `APODERADO` + `SCOPE_estudiantes:update` | Valida el vínculo apoderado-estudiante y actualiza el pupilo. |
| `GET` | `/api/bff/v1/docentes/cursos` · `/horario` | `DOCENTE` + `SCOPE_docentes:read` | Cursos con alumnos y horario semanal del docente autenticado. |
| `GET` | `/api/bff/v1/docentes/cursos/{asignaturaId}/notas` | `DOCENTE` + `SCOPE_docentes:read` (ownership) | Evaluaciones y notas del curso para la grilla del docente. |
| `POST` | `/api/bff/v1/notas` | `ADMIN`/`DOCENTE` + `SCOPE_notas:write` (ownership) | Crea una nota validando que la evaluación pertenezca al docente. |
| `PUT` | `/api/bff/v1/notas/{id}` | `ADMIN`/`DOCENTE` + `SCOPE_notas:update` (ownership) | Edita el score de una nota. |
| `DELETE` | `/api/bff/v1/notas/{id}` | `ADMIN`/`DOCENTE` + `SCOPE_notas:delete` (ownership) | Borrado lógico de una nota. |
| `POST` | `/api/bff/v1/evaluaciones` | `ADMIN`/`DOCENTE` + `SCOPE_evaluaciones:write` (ownership) | Crea una evaluación; la ponderación acumulada ≤ 100 la valida ms-evaluaciones. |
| `PUT` | `/api/bff/v1/evaluaciones/{id}` | `ADMIN`/`DOCENTE` + `SCOPE_evaluaciones:update` (ownership) | Edita nombre/tipo/ponderación de una evaluación. |
| `DELETE` | `/api/bff/v1/evaluaciones/{id}` | `ADMIN`/`DOCENTE` + `SCOPE_evaluaciones:delete` (ownership) | Borrado lógico de una evaluación. |
| `GET` | `/api/bff/v1/admin/usuarios` · `/asignaturas` | `ADMIN` + `SCOPE_usuarios:read` / `asignaturas:read` | Usuarios y asignaturas para el portal admin. |
| `GET` | `/api/bff/v1/admin/clases?anioAcademico=` | `ADMIN` + `SCOPE_clases:read` | Clases activas del año para el select del formulario. |
| `GET` | `/api/bff/v1/admin/estudiantes?q=` | `ADMIN` + `SCOPE_estudiantes:read` | Busca alumnos por RUT (con/sin puntos) o nombre (mínimo 2 caracteres). |
| `GET` | `/api/bff/v1/admin/usuarios/{idUsuario}` | `ADMIN` + `SCOPE_usuarios:read` | Detalle de la cuenta + resumen del perfil del rol (estudiante/docente/apoderado). |
| `DELETE` | `/api/bff/v1/admin/usuarios/{idUsuario}` | `ADMIN` + `SCOPE_usuarios:delete` | Soft delete en SIGA y deshabilitación/revocación en Entra ID. |
| `POST` | `/api/bff/v1/admin/registraciones` | `ADMIN` + `SCOPE_usuarios:write` | Inicia el registro asíncrono (`202` + `processId`). |
| `GET` | `/api/bff/v1/admin/registraciones/{processId}` | `ADMIN` + `SCOPE_usuarios:read` | Estado del proceso (polling). |
| `GET` | `/api/bff/v1/admin/registraciones/{processId}/credencial` | `ADMIN` + `SCOPE_usuarios:read` | Credencial temporal de un solo uso (solo el admin iniciador). |
| `POST` | `/api/bff/v1/admin/usuarios/{idUsuario}/reset-password` | `ADMIN` + `SCOPE_usuarios:update` | Regenera la clave temporal (requiere el rol User Administrator en Graph). |
| `GET` | `/api/bff/v1/asistencias/estudiante/me` · `/{id}` | `SCOPE_asistencias:read` (ownership para `{id}`) | Asistencias del estudiante autenticado, de un pupilo vinculado o de un docente. |
| `GET` | `/api/bff/v1/asistencias/asignatura/{id}?fecha=` | `DOCENTE` + `SCOPE_asistencias:read` (ownership) | Asistencias de una asignatura en una fecha, para el docente dueño. |
| `POST` | `/api/bff/v1/asistencias` | `ADMIN`/`DOCENTE` + `SCOPE_asistencias:write` (ownership) | Registra asistencia. |
| `PUT` | `/api/bff/v1/asistencias/{id}` | `ADMIN`/`DOCENTE` + `SCOPE_asistencias:update` (ownership) | Actualiza justificación/observación y, opcionalmente, el estado. |

### Usuarios y autenticación (`:8081`, `/api/v1/usuarios`)

| Método | Ruta | Autorización |
| --- | --- | --- |
| `GET` | `/me` | Autenticado |
| `GET` | `/lookup?email=` | `ADMIN` + `SCOPE_usuarios:read` |
| `GET` | `/{id}` | `ADMIN` + `SCOPE_usuarios:read` |
| `GET` | `/search?email=&rol=&state=&page=&size=&sort=` | `ADMIN` + `SCOPE_usuarios:read` |
| `POST` | `/` | `ADMIN` + `SCOPE_usuarios:write` |
| `PUT` | `/{id}` | `ADMIN` + `SCOPE_usuarios:update` (no puede desactivarse a sí mismo) |
| `DELETE` | `/{id}` | `ADMIN` + `SCOPE_usuarios:delete` (borrado lógico + offboarding en Entra) |
| `POST` | `/{id}/sync-roles` | `ADMIN` + `SCOPE_usuarios:update` |
| `POST` | `/registraciones/async` | `ADMIN` + `SCOPE_usuarios:write` (`202` + `processId`) |
| `GET` | `/registraciones/{processId}` | `ADMIN` + `SCOPE_usuarios:read` |
| `GET` | `/registraciones/{processId}/credencial` | `ADMIN` + `SCOPE_usuarios:read` (una sola lectura; `409`/`410`) |
| `POST` | `/{id}/reset-password` | `ADMIN` + `SCOPE_usuarios:update` |

### Estudiantes (`:8083`, `/api/v1/estudiantes`)

| Método | Ruta | Autorización |
| --- | --- | --- |
| `GET` | `/{id}` | `SCOPE_estudiantes:read` |
| `GET` | `/idUsuario/{idUsuario}` | `SCOPE_estudiantes:read` |
| `GET` | `/search?rut=&firstName=&...&from=&to=&state=&page=&size=&sort=` | `SCOPE_estudiantes:read` |
| `GET` | `/exists/{id}` | `SCOPE_estudiantes:read` |
| `POST` | `/` | `ADMIN` + `SCOPE_estudiantes:write` |
| `PUT` | `/{id}` | `ADMIN` o el propio estudiante (ownership por `oid`); el estado no se cambia por PUT |
| `DELETE` | `/{id}` | `ADMIN` + `SCOPE_estudiantes:delete` (borrado lógico) |

### Asignaturas (`:8086`)

| Método | Ruta | Autorización |
| --- | --- | --- |
| `GET` | `/api/v1/asignaturas/{id}` | `SCOPE_asignaturas:read` |
| `GET` | `/api/v1/asignaturas/search?name=&tipo=&semestre=&area=&idDocente=&idClase=&page=&size=&sort=` | `SCOPE_asignaturas:read` |
| `GET` | `/api/v1/asignaturas/exists/{id}` | `SCOPE_asignaturas:read` |
| `DELETE` | `/api/v1/asignaturas/{id}` | `ADMIN` + `SCOPE_asignaturas:delete` (borrado lógico) |
| `POST` | `/api/v1/asignaturas/basicas` | `ADMIN` + `SCOPE_asignaturas:write` |
| `PUT` | `/api/v1/asignaturas/basicas/{id}` | `ADMIN` + `SCOPE_asignaturas:update` |
| `POST` | `/api/v1/asignaturas/electivas` | `ADMIN` + `SCOPE_asignaturas:write` |
| `PUT` | `/api/v1/asignaturas/electivas/{id}` | `ADMIN` + `SCOPE_asignaturas:update` |
| `GET` | `/api/v1/asignaturas/electivas/validar-mineduc` | `ADMIN` o `SCOPE_asignaturas:read` |
| `POST` | `/api/v1/horarios/asignatura/{asignaturaId}` | `ADMIN` + `SCOPE_horarios:write` |
| `PUT` | `/api/v1/horarios/{id}` | `ADMIN` + `SCOPE_horarios:update` |
| `DELETE` | `/api/v1/horarios/{id}` | `ADMIN` + `SCOPE_horarios:delete` (borrado lógico; mantiene al menos 1 activo) |
| `GET` | `/api/v1/inscripciones/{id}` | `SCOPE_inscripciones:read` |
| `GET` | `/api/v1/inscripciones/search?idAlumno=&idAsignatura=&estados=&page=&size=&sort=` | `SCOPE_inscripciones:read` |
| `POST` | `/api/v1/inscripciones` | `ADMIN` o el propio ESTUDIANTE (ownership por `oid`) + `SCOPE_inscripciones:write` |
| `PUT` | `/api/v1/inscripciones/{id}/estado` | `ADMIN` + `SCOPE_inscripciones:update` |

### Notas (`:8089`, `/api/v1/notas`)

| Método | Ruta | Autorización |
| --- | --- | --- |
| `GET` | `/{id}` | `SCOPE_notas:read` |
| `GET` | `/search?idEstudiante=&idEvaluacion=&lessThanScore=&greaterThanScore=&page=&size=&sort=` | `SCOPE_notas:read` |
| `POST` | `/` | `ADMIN` o `DOCENTE` + `SCOPE_notas:write` |
| `PUT` | `/{id}` | `ADMIN` o `DOCENTE` + `SCOPE_notas:update` |
| `DELETE` | `/{id}` | `ADMIN` o `DOCENTE` + `SCOPE_notas:delete` (borrado lógico) |

### Docentes (`:8085`, `/api/v1/docentes`)

| Método | Ruta | Autorización |
| --- | --- | --- |
| `GET` | `/{id}` · `/idUsuario/{idUsuario}` · `/exists/{id}` | `SCOPE_docentes:read` |
| `GET` | `/search?rut=&firstName=&firstSurname=&from=&to=&area=&page=&size=&sort=` | `SCOPE_docentes:read` |
| `POST` | `/` | `ADMIN` + `SCOPE_docentes:write` |
| `PUT` | `/{id}` | `ADMIN` o `SCOPE_docentes:update` |
| `DELETE` | `/{id}` | `ADMIN` + `SCOPE_docentes:delete` (borrado lógico) |
| `GET` | `/{docenteId}/certificados` | `SCOPE_docentes:read` |
| `POST` | `/{docenteId}/certificados` | `ADMIN` o el propio DOCENTE (ownership) + `SCOPE_docentes:write` |
| `DELETE` | `/{docenteId}/certificados/{certificadoId}` | `ADMIN` o el propio DOCENTE (ownership) + `SCOPE_docentes:delete` |

### Apoderados (`:8084`, `/api/v1/apoderados`)

| Método | Ruta | Autorización |
| --- | --- | --- |
| `GET` | `/{id}` · `/idUsuario/{idUsuario}` · `/exists/{id}` | `SCOPE_apoderados:read` |
| `GET` | `/search?rut=&firstName=&firstSurname=&idEstudiante=&page=&size=&sort=` | `SCOPE_apoderados:read` |
| `POST` | `/` | `ADMIN` + `SCOPE_apoderados:write` |
| `PUT` | `/{id}` | `ADMIN` o el propio APODERADO (ownership por `oid`) + `SCOPE_apoderados:update` |
| `POST` | `/{id}/estudiantes` | `ADMIN` + `SCOPE_apoderados:update` |
| `DELETE` | `/{id}/estudiantes/{idEstudiante}` | `ADMIN` + `SCOPE_apoderados:update` |
| `DELETE` | `/{id}` | `ADMIN` + `SCOPE_apoderados:delete` (borrado lógico) |

### Clases (`:8087`, `/api/v1/clases`)

| Método | Ruta | Autorización |
| --- | --- | --- |
| `GET` | `/{id}` · `/exists/{id}` | `SCOPE_clases:read` |
| `GET` | `/search?nivel=&letra=&anioAcademico=&idDocenteJefe=&page=&size=&sort=` | `SCOPE_clases:read` |
| `POST` | `/` | `ADMIN` + `SCOPE_clases:write` |
| `PUT` | `/{id}/docente-jefe` | `ADMIN` + `SCOPE_clases:update` (permite limpiar con `null`) |
| `DELETE` | `/{id}` | `ADMIN` + `SCOPE_clases:delete` (borrado lógico) |

### Evaluaciones (`:8088`, `/api/v1/evaluaciones`)

| Método | Ruta | Autorización |
| --- | --- | --- |
| `GET` | `/{id}` · `/exists/{id}` | `SCOPE_evaluaciones:read` |
| `GET` | `/search?nombre=&tipo=&idAsignatura=&page=&size=&sort=` | `SCOPE_evaluaciones:read` |
| `POST` | `/` | `ADMIN` o `DOCENTE` + `SCOPE_evaluaciones:write` |
| `PUT` | `/{id}` | `ADMIN` o `DOCENTE` + `SCOPE_evaluaciones:update` |
| `DELETE` | `/{id}` | `ADMIN` o `DOCENTE` + `SCOPE_evaluaciones:delete` (borrado lógico) |

### Asistencias (`:8090`, `/api/v1/asistencias`)

| Método | Ruta | Autorización |
| --- | --- | --- |
| `GET` | `/{id}` · `/exists/{id}` | `SCOPE_asistencias:read` |
| `GET` | `/search?idEstudiante=&idAsignatura=&from=&to=&estado=&page=&size=&sort=` | `SCOPE_asistencias:read` |
| `POST` | `/` | `ADMIN` o `DOCENTE` + `SCOPE_asistencias:write` (valida estudiante y asignatura por Feign) |
| `PUT` | `/{id}` | `ADMIN` o `DOCENTE` + `SCOPE_asistencias:update` (justificación/observación) |
| `DELETE` | `/{id}` | `ADMIN` + `SCOPE_asistencias:delete` (borrado lógico) |

### Administración RabbitMQ (`:8092`, `/api/v1/rabbitmq`)

| Método | Ruta | Autorización |
| --- | --- | --- |
| `POST` | `/queues` | `ADMIN` + `SCOPE_usuarios:write` |
| `GET` | `/queues/{nombre}` | `ADMIN` + `SCOPE_usuarios:read` (mensajes y consumidores) |
| `DELETE` | `/queues/{nombre}` | `ADMIN` + `SCOPE_usuarios:delete` |
| `POST` | `/exchanges` | `ADMIN` + `SCOPE_usuarios:write` (`DIRECT`, `TOPIC`, `FANOUT`, `HEADERS`) |
| `DELETE` | `/exchanges/{nombre}` | `ADMIN` + `SCOPE_usuarios:delete` |
| `POST` | `/bindings` | `ADMIN` + `SCOPE_usuarios:write` |
| `DELETE` | `/bindings?cola=&exchange=&routingKey=` | `ADMIN` + `SCOPE_usuarios:delete` |

El BFF reexpone la misma API bajo `/api/bff/v1/admin/rabbitmq/**` (el navegador
nunca se conecta al broker). Los DTO se validan con Bean Validation.

## Comunicación entre servicios

- **Feign** es el cliente declarativo: `bff-web` orquesta usuarios, estudiantes,
  clases, asignaturas, evaluaciones, notas, docentes y asistencias; `ms-notas`
  valida estudiante y evaluación; `ms-asistencias` valida estudiante y asignatura;
  `ms-asignaturas` valida docente, clase y estudiante; `ms-clases` valida docente;
  `ms-apoderados` valida estudiante; `ms-evaluaciones` valida asignatura.
- **Resilience4j** aporta circuit breaker y timeouts; si un servicio no responde
  se activa el fallback correspondiente (`*Fallback`, respuesta 503).
- **Propagación del token**: `SharedFeignAuthConfig` (en `core-share`) reenvía el
  `Authorization` entrante en todas las llamadas Feign; el BFF usa su propio
  `FeignClientConfig`.
- **Mensajería RabbitMQ**: topología centralizada en `core-share`
  (`ConfiguracionTopologiaNotificaciones`): topic `intercambio-notificaciones`
  reparte `nota.*`/`asistencia.*`/`evaluacion.*` a una cola por tipo, cada una con
  su DLQ enlazada al direct `siga.dlx.direct`. Los consumidores confirman con
  ACK/NACK manuales (`ConfirmadorMensajes`): reintentos acotados en memoria y, al
  agotarse o ante un evento inválido, NACK sin requeue hacia la DLQ. El registro
  asíncrono de usuarios usa `user.topic.exchange` con las mismas reglas
  (`siga.mensajeria.registro.topology-enabled=true` en los cuatro servicios del
  flujo). Los servicios se conectan con `SPRING_RABBITMQ_ADDRESSES` a los dos
  nodos del clúster.

## Seguridad

- OAuth2/JWT con Azure AD (`spring.cloud.azure.active-directory`).
- Autorización por método: roles (`hasRole`) y scopes (`hasAuthority('SCOPE_...')`).
- **Ownership por `oid`**: APODERADO/DOCENTE/ESTUDIANTE solo modifican sus
  propios recursos (`SecurityUtils.requireOwnerOrAdmin`).
- Rutas públicas limitadas a salud y documentación técnica (`/actuator/**`, `/docs/**`, `/v3/api-docs/**`).
- `core-share` aporta `SharedSecurityConfig` (filtro stateless y conversión de
  claims `scp`/`roles`) y `SecurityUtils`.
- El frontend nunca accede a los microservicios directamente: lo hace a través del BFF.
- **Autorización unificada**: lecturas con `hasAuthority('SCOPE_x:read')`; escrituras
  con `hasRole(...) and hasAuthority('SCOPE_x:write|update|delete')` (ADMIN incluido).
  Los 42 scopes granulares se mantienen y deben exponerse/consentirse en Entra ID
  (ver [`docs/testing-login.md`](../../docs/testing-login.md)).

## Datos y configuración

- Patrón **database-per-service** (10 bases): `siga_usuarios_db`,
  `siga_estudiantes_db`, `siga_asignaturas_db`, `siga_notas_db`,
  `siga_docentes_db`, `siga_apoderados_db`, `siga_clases_db`,
  `siga_evaluaciones_db`, `siga_asistencias_db` y `siga_notificaciones_db`.
- **Flyway** crea y evoluciona el esquema (`ddl-auto: validate`); una base vacía
  se auto-inicializa al arrancar.
- **Paginación**: los `GET /search` devuelven `Page<T>` (`page`, `size`, `sort`;
  default 20, máximo 100). Detalle en [`docs/paginacion.md`](../../docs/paginacion.md).
- **Formato de fecha (API): ISO 8601 `yyyy-MM-dd`** para `LocalDate` (JSON y
  parámetros de URL), definido en `CommonDateFormatConfig` y en
  `SharedFeignFormatConfig` (OpenFeign no usa los formatters de MVC; sin esta
  configuración las llamadas Feign enviaban `from`/`to` con formatos localizados).
  *Cambio de contrato:* antes era `dd/MM/yyyy`; los consumidores deben usar
  `yyyy-MM-dd` (`birthDate`, `from`/`to`). El frontend normaliza a `dd/MM/yyyy`
  solo para mostrar.
- **Estándar de datos**: RUT canónico sin puntos (`13789943-2`) vía
  `RutNormalizer` y nombres capitalizados con `NombrePropio` (partículas en
  minúscula, `Mc`/`Mac`, apóstrofes y guiones). El backend normaliza antes de
  guardar; el frontend lo espeja en vivo. Las migraciones `V7`/`V7`/`V5`/`V9`
  normalizan los seeds y `tools/normalizar-datos.ps1` cubre data previa no seed.
- En Docker las variables se inyectan desde `.env`
  (`SPRING_DATASOURCE_*`, `SERVER_PORT`, Azure, `MS_*_URL`, `SPRING_RABBITMQ_*`,
  `REGISTRO_*`).

Variables principales: `MARIADB_ROOT_PASSWORD`, `DB_USER`, `DB_PASS`,
`AZURE_TENANT_ID`, `AZURE_CLIENT_ID`, `AZURE_APP_ID_URI`, `AZURE_CLIENT_SECRET`,
`AZURE_API_APP_ID`, `MS_USUARIOS_URL`, `MS_ESTUDIANTES_URL`,
`MS_ASIGNATURAS_URL`, `MS_NOTAS_URL`, `MS_DOCENTES_URL`, `MS_CLASES_URL`,
`MS_EVALUACIONES_URL`, `MS_ASISTENCIAS_URL`, `RABBITMQ_USER`, `RABBITMQ_PASS`,
`REGISTRO_ASYNC_ENABLED`, `REGISTRO_CRED_KEY`,
`REGISTRO_NOTIFY_CREDENTIALS_ENABLED` y `REGISTRO_EMAIL_DOMAIN`.

## Documentación de la API

Cada servicio expone (rutas públicas): `/actuator/health`, `/v3/api-docs`,
`/docs/swagger` (Swagger UI) y `/docs/scalar` (Scalar).

## Pruebas

```bash
mvn -f apps/backend/pom.xml -B package -Dtest='!*ApplicationTests' -DfailIfNoTests=false
```

- Tests unitarios (Mockito) de validadores, cupos, ponderación, docente jefe,
  horarios, asistencias y de los servicios del BFF (`perfil`, `pupilos`, `cursos`,
  `admin`, `asistencias`, `detalle/eliminación de usuarios`) en `core-share`,
  `bff-web`, `ms-asignaturas`, `ms-clases`, `ms-evaluaciones`, `ms-asistencias` y
  `ms-usuarios-auth`.
- Registro asíncrono: `AsyncUserRegistrationServiceTest`, `CredentialCipherTest`,
  `EmailInstitucionalGeneratorTest` y pruebas de normalización de formatos
  (`RutNormalizerTest`, `NombrePropioTest`, `SharedFeignFormatConfigTest`).
  Reactivación de perfiles y búsqueda `q` en los dominios (`BusquedaTextoTest`).
- Contrato de errores (`GlobalExceptionHandlerTest`) y migraciones con
  **Testcontainers** (`MigracionesTest`/`MigracionesRegistroAsyncTest`: unicidad de
  `id_usuario`/asignatura, outbox, credenciales y soft delete). Los IT se omiten si
  no hay Docker (`disabledWithoutDocker`).
- Los `*ApplicationTests` (contextLoads) se excluyen en CI porque requieren base de
  datos y Azure AD.

## Enlaces

- [README raíz](../../README.md)
- [`docs/backend.md`](../../docs/backend.md) · [`docs/arquitectura.md`](../../docs/arquitectura.md)
- [`docs/paginacion.md`](../../docs/paginacion.md) · [`docs/auditoria-backend.md`](../../docs/auditoria-backend.md)
- [`docs/testing-login.md`](../../docs/testing-login.md) · [`docs/registro-pruebas.md`](../../docs/registro-pruebas.md)
- [Índice de docs](../../docs/README.md)
