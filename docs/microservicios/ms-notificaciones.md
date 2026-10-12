# `ms-notificaciones`

**Módulo:** `apps/backend/ms-notificaciones` · **Puerto:** 8091 · **BD:** `siga_notificaciones_db`.

## Responsabilidad y datos

Consume eventos académicos y guarda una bandeja in-app; no envía correo ni lee buzones. `Notificacion` persiste el evento deduplicado y `NotificacionLectura` guarda estado/ocultamiento por OID para que estudiante y apoderado no compartan la marca de lectura. Retención programada elimina registros antiguos (90 días por defecto).

Fuentes principales: `mensajeria/EscuchadorNota.java`, `EscuchadorEvaluacion.java`, `EscuchadorAsistencia.java`, `service/NotificacionService.java`, `ResolverDestinatariosNotificacion.java`, `controller/NotificacionController.java`, `repository/NotificacionRepository.java` y entidades `Notificacion`/`NotificacionLectura`.

## Dependencias declaradas

[`pom.xml`](../../apps/backend/ms-notificaciones/pom.xml): Web, Validation, JPA/MariaDB, Flyway, Security/OAuth2 Resource Server, OpenFeign/Resilience4j, Springdoc, Actuator, `core-share` y Testcontainers MariaDB. AMQP llega vía `core-share` aunque no aparece como dependencia directa de este módulo.

## Flujos, patrones y dependencias

- Tres listeners por dominio reciben nota, evaluación y asistencia desde colas topic. `ConfirmadorMensajes` coordina ACK/NACK, reintentos y DLQ.
- Persistencia idempotente por identidad de evento evita duplicar bandeja al recibir redelivery.
- `ResolverDestinatariosNotificacion` llama vía Feign a estudiantes, apoderados, asignaturas/inscripciones para decidir quién puede ver cada evento; propaga token donde aplica y cachea destinatarios por OID con TTL.
- El BFF expone lista, contador, marcar individual/todas y ocultar leídas; Angular muestra campana compartida en portales de estudiante/apoderado.
- La identidad/propiedad se deriva de token Entra; Rabbit transporta el evento, no autoriza por sí mismo al lector final.

## Verificación

Tests en `src/test/java` cubren idempotencia, eventos, destinatarios y endpoints. La bitácora `PR-20261007-04`, `PR-20261008-07` y `PR-20261011-11` registra prueba real de eventos válidos/erróneos y DLQ. `TZ=America/Santiago` se inyecta por Compose; API protegida por JWT/scopes.
