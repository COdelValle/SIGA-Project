# `ms-estudiantes`

**Módulo:** `apps/backend/ms-estudiantes` · **Puerto:** 8083 · **BD:** `siga_estudiantes_db`.

## Responsabilidad y datos

Es dueño de la ficha personal/académica básica del estudiante, RUT, nombres, fecha de nacimiento, alergias, vínculo a usuario y clase, y estado del perfil. `Estudiante` se persiste con JPA; Flyway gestiona esquema e índices. La eliminación es lógica y el formato RUT/nombre se normaliza con utilidades compartidas.

Fuentes principales: `controller/EstudianteController.java`, `service/EstudianteService.java`, `repository/EstudianteRepository.java`, `model/entity/Estudiante.java`, `client/ApoderadoClient.java` y `messaging/EstudianteRegistrationListener.java`.

## Dependencias declaradas

[`pom.xml`](../../apps/backend/ms-estudiantes/pom.xml): Web, Validation, AMQP, Data JPA/MariaDB, Flyway, Security/OAuth2 Resource Server, OpenFeign, Resilience4j, Springdoc, Actuator, MapStruct, `core-share` y Testcontainers MariaDB.

## Patrones y dependencias

- Capas Controller/Service/Repository con `EstudianteSpecifications` para búsqueda filtrada y paginada.
- `GET /exists/{id}` sirve como contrato de existencia a otros servicios; `/search` y texto libre `q` soportan listados.
- `EstudianteRegistrationListener` consume la etapa de registro `ms.estudiantes.queue`, idempotente por usuario y emite estado.
- Feign a `ms-apoderados` apoya validaciones de propiedad/vínculo. Es una dependencia síncrona runtime; `ms-apoderados` también llama a `ms-estudiantes`, por lo que existe acoplamiento de ida/vuelta (no necesariamente ciclo de arranque).
- Consume RabbitMQ para perfil de registro; no es productor de notificaciones académicas.

## Contrato, seguridad y verificación

Base `/api/v1/estudiantes`: lectura, búsqueda, existencia, alta/actualización y soft delete; roles/scopes y ownership están en el controlador/servicio. `common-properties.yaml` aporta JWT/CORS y el POM aporta resilience. Tests en `src/test/java`; integración con otros servicios requiere Feign y token propagado. Health `/actuator/health`; ejecutar Flyway contra su BD propia.
