# `ms-notas`

**Módulo:** `apps/backend/ms-notas` · **Puerto:** 8089 · **BD:** `siga_notas_db`.

## Responsabilidad y datos

Dueño de la calificación (`Nota`: estudiante, evaluación, score y estado). Aplica validador chileno 1.0–7.0, soft delete y búsquedas paginadas; entidad y migraciones permanecen en su propia MariaDB.

Fuentes principales: `controller/NotaController.java`, `service/NotaService.java`, `repository/NotaRepository.java`, `model/entity/Nota.java`, clients `EstudianteClient`, `EvaluacionClient`, `AsignaturaClient` y dispatcher/outbox de evento académico.

## Dependencias declaradas

[`pom.xml`](../../apps/backend/ms-notas/pom.xml): Web, Validation, JPA/MariaDB, Flyway, Security/OAuth2 Resource Server, OpenFeign/Resilience4j, Springdoc, Actuator, MapStruct, `core-share` y Testcontainers MariaDB/JUnit.

## Integraciones y patrones

- `EstudianteClient` y `EvaluacionClient` validan IDs antes de persistir; `AsignaturaClient` apoya la resolución de nombres/datos para eventos.
- Feign propaga Bearer y usa timeouts/circuit breaker/fallback; una dependencia fuera de servicio puede devolver 503.
- Productor de eventos académicos usa **outbox transaccional**; cada nota nueva/actualizada produce evento versionado, con `idEvento` para deduplicar en `ms-notificaciones`. El borrado lógico no emite el mismo evento de creación.
- El BFF ofrece operaciones al docente con scope/ownership; el frontend no llama este servicio directo.

## Pruebas y operación

`NotaServiceTest` y pruebas de mappers/serialización cubren reglas; Testcontainers cubre migraciones donde Docker lo permite. Para demostrar evento extremo a extremo, crear nota sintética, observar outbox→Rabbit→notificación y eliminar dato de prueba.
