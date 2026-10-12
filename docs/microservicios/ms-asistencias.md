# `ms-asistencias`

**Módulo:** `apps/backend/ms-asistencias` · **Puerto:** 8090 · **BD:** `siga_asistencias_db`.

## Responsabilidad y modelo

Dueño de registros de asistencia por estudiante, dictación y fecha (`Asistencia`), estado, justificación y observación. Una restricción única evita duplicar alumno/dictación/fecha; la baja es lógica y los reportes consultan rangos/estado.

Fuentes principales: `controller/AsistenciaController.java`, `service/AsistenciaService.java`, `repository/AsistenciaRepository.java`, `model/entity/Asistencia.java`, clients `EstudianteClient`/`AsignaturaClient` y dispatcher/outbox académico.

## Dependencias declaradas

[`pom.xml`](../../apps/backend/ms-asistencias/pom.xml): Web, Validation, JPA/MariaDB, Flyway, Security/OAuth2 Resource Server, OpenFeign/Resilience4j, Springdoc, Actuator, MapStruct, `core-share` y Testcontainers MariaDB/JUnit.

## Integraciones y patrones

- `EstudianteClient` comprueba estudiante y `AsignaturaClient` la dictación; ambas son llamadas síncronas con Feign, auth propagada y fallback.
- Productor de eventos/notificaciones usa outbox dentro de la transacción de dominio; en la regla actual notifica alta de AUSENTE/ATRASADO, no cada actualización de justificación.
- Los eventos incluyen `idEvento` y el porcentaje/umbral de inasistencia; `ms-notificaciones` los deduplica y aplica privacidad por OID.
- BFF compone operaciones para estudiante/apoderado y docente; `GET /search` pagina.

## Pruebas

`AsistenciaServiceTest`, mappers y Testcontainers de esquema/unique prueban reglas. Para E2E, registrar evento sintético en entorno de prueba y correlacionar outbox, routing, fila de bandeja y ACK. No usar datos reales en la DLQ.
