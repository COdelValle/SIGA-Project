# `ms-asignaturas`

**Módulo:** `apps/backend/ms-asignaturas` · **Puerto:** 8086 · **BD:** `siga_asignaturas_db`.

## Responsabilidad y modelo

Separa catálogo (`Asignatura`), aplicación curricular por nivel (`MallaCurricular`) y dictación concreta por clase/docente/semestre (`CursoAsignatura`). Horarios e inscripciones referencian la dictación; el servicio administra cupos y estado. El esquema es propio, con JPA/Flyway.

Fuentes principales: `controller/asignatura/AsignaturaController.java`, `MallaCurricularController.java`, `CursoAsignaturaController.java`, `controller/HorarioController.java`, `controller/InscripcionController.java`, sus servicios homónimos bajo `service/` y clientes `client/DocenteClient.java`, `ClaseClient.java`, `EstudianteClient.java`.

## Dependencias declaradas

[`pom.xml`](../../apps/backend/ms-asignaturas/pom.xml): Web, Validation, Data JPA/MariaDB, Flyway, Security/OAuth2 Resource Server, OpenFeign, Resilience4j, Springdoc, Actuator, MapStruct y `core-share`; Testcontainers MariaDB/JUnit. AMQP no figura en este POM; si una clase lo ve por classpath, llega transitivamente por `core-share`, no es una declaración directa.

## Dependencias y reglas

- `DocenteClient` y `ClaseClient` verifican los responsables de la dictación; `EstudianteClient` interviene en las inscripciones. URLs por `services.*` y llamadas Feign con propagación de token/resilience.
- Reglas: malla válida para el nivel, cupos/inscripciones solo para asignaturas electivas/optativas y orientación no admite evaluación.
- `HorarioService` e `InscripcionService` manejan subdominios con soft delete/validaciones propias; lista de endpoints y scopes en `apps/backend/README.md`.
- No es productor ni consumidor principal de eventos académicos. `ms-notificaciones` lo consulta para resolver dictaciones/destinatarios; notas y asistencias lo llaman para validar referencias.

## Pruebas

Las pruebas de servicio cubren catálogo, malla, cupos y dependencias mockeadas. `GET /exists` es contrato de integración. Para E2E deben existir docentes/clases/estudiantes asociados y URLs Feign correctas. DB/Flyway es independiente de las demás.
