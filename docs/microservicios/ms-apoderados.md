# `ms-apoderados`

**Módulo:** `apps/backend/ms-apoderados` · **Puerto:** 8084 · **BD:** `siga_apoderados_db`.

## Responsabilidad y datos

Administra datos del apoderado, teléfonos y vínculo con estudiantes (`Apoderado`, `ApoderadoEstudiante` y parentesco). Es dueño de su esquema; sus referencias a estudiante son IDs lógicos y se verifican por API, no con una tabla compartida.

Fuentes principales: `controller/ApoderadoController.java`, `service/ApoderadoService.java`, `repository/ApoderadoRepository.java`, `model/entity/Apoderado.java`, `model/entity/ApoderadoEstudiante.java`, `client/EstudianteClient.java` y `messaging/ApoderadoRegistrationListener.java`.

## Dependencias declaradas

[`pom.xml`](../../apps/backend/ms-apoderados/pom.xml): Web, Validation, AMQP, JPA/MariaDB, Flyway, Security/OAuth2 Resource Server, OpenFeign, Resilience4j, Springdoc, Actuator, MapStruct y `core-share`.

## Patrones y acoplamientos

- Controller/Service/Repository y búsqueda por especificaciones; `DELETE` es lógico.
- `EstudianteClient` consulta `ms-estudiantes` para verificar alumno/vínculo. `ms-estudiantes` también usa al apoderado para algunas validaciones de propiedad: dependencia HTTP bidireccional que debe mantenerse en interfaces de existencia/autorización simples.
- `ApoderadoRegistrationListener` consume `ms.apoderados.queue`, valida versión/rol/datos, comprueba idempotencia por `idUsuario` y publica resultado a la cola de estado. Reentregas tras persistencia exitosa se confirman sin duplicar.
- La solicitud de alta valida estudiantes de manera síncrona desde usuarios antes de publicar el evento de perfil.

## Contrato y operación

API `/api/v1/apoderados`: CRUD, búsqueda, existencia, lookup por usuario y asociación/desasociación de estudiantes; requiere los scopes/roles definidos por controlador. Usa su MariaDB/Flyway y Rabbit compartido para registro. Pruebas unitarias en `src/test/java`; E2E de alta necesita RabbitMQ, `ms-usuarios-auth`, estudiantes y Graph según el flujo.
