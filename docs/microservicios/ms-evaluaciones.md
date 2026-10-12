# `ms-evaluaciones`

**Módulo:** `apps/backend/ms-evaluaciones` · **Puerto:** 8088 · **BD:** `siga_evaluaciones_db`.

## Responsabilidad y modelo

Administra evaluaciones de una dictación (`Evaluacion`): nombre, tipo, ponderación y estado activo. Es dueño de la persistencia y migraciones de su base.

Fuentes principales: `controller/EvaluacionController.java`, `service/EvaluacionService.java`, `repository/EvaluacionRepository.java`, `model/entity/Evaluacion.java`, `client/AsignaturaClient.java` y la configuración/outbox de notificaciones académicas.

## Dependencias declaradas

[`pom.xml`](../../apps/backend/ms-evaluaciones/pom.xml): Web, Validation, JPA/MariaDB, Flyway, Security/OAuth2 Resource Server, OpenFeign/Resilience4j, Springdoc, Actuator, MapStruct y `core-share`.

## Integraciones y patrones

- `AsignaturaClient` consulta `ms-asignaturas` para validar la dictación/condición calificable.
- La ponderación se valida en el servicio con locks/consultas del agregado; solo evaluaciones SUMATIVA ponderan el promedio según regla vigente descrita en `docs/registro-pruebas.md`.
- Productor de notificaciones: outbox académico en la misma transacción del CRUD; dispatch con confirm publisher y reintentos; la notificación se materializa asíncronamente en `ms-notificaciones`.
- La API REST ofrece CRUD, búsqueda y `exists`; BFF expone operaciones del docente con ownership/role/scope.

## Pruebas y fallo

Tests del servicio cubren ponderaciones, validación y casos concurrentes; revisar Testcontainers cuando el entorno Docker esté disponible. Si la validación Feign de asignatura falla, el servicio usa Resilience4j/fallback y el cliente recibe un error de indisponibilidad, no debe asumir evaluación válida.
