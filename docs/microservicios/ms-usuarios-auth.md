# `ms-usuarios-auth`

**Módulo:** `apps/backend/ms-usuarios-auth` · **Puerto:** 8081 · **BD:** `siga_usuarios_db`.

## Responsabilidad y datos

Dueño del registro local de usuarios/roles, invitaciones y ciclo de vida de cuenta. Entidades principales: `Usuario`, `InvitacionUsuario`, `UserRegistrationProcess`, `UserRegistrationAttempt`, `RegistrationOutboxEvent` y `ProcessedRegistrationEvent`. Flyway mantiene el esquema y los estados; DELETE local es soft delete y la vinculación del OID se hace desde claims/Invitación, no copiando OID a mano.

Fuentes principales: `controller/UsuarioController.java`, `service/UsuarioService.java`, `service/AsyncUserRegistrationService.java`, `service/RegistrationOutboxPublisher.java`, `service/AsyncUserRegistrationListeners.java`, `integration/graph/GraphUserDirectory.java`, `repository/UsuarioRepository.java` y `src/main/resources/db/migration/`.

## Dependencias declaradas

[`pom.xml`](../../apps/backend/ms-usuarios-auth/pom.xml): Web, Validation, JPA + MariaDB runtime, Flyway, Azure Identity (Microsoft Graph), Security/OAuth2 Resource Server, AMQP, OpenFeign, Springdoc, Actuator, MapStruct, `core-share` y Testcontainers MariaDB/JUnit.

## Patrones

- **Outbox transaccional**: solicitud/proceso y evento se escriben juntos; `RegistrationOutboxPublisher` publica con confirms/backoff.
- **Proceso asíncrono orquestado**: estados por etapa Azure/dominio; endpoint inicia `202`, polling y recuperación visible.
- **Idempotencia y reintentos**: eventos repetidos no crean dos perfiles; errores Graph se clasifican transitorios/permanentes.
- **Credencial de un solo uso cifrada** con AES-256-GCM y expiración; Graph se usa para provisión/offboarding, con secretos fuera del repo.
- **RabbitMQ**: produce eventos hacia Azure/profiles y consume resultado; listeners usan `ConfirmadorMensajes`/correlation ID.

## Dependencias de otros sistemas

| Dependencia | Tipo | Motivo |
|---|---|---|
| Microsoft Graph / Entra ID | HTTP externo (Azure Identity client credentials) | Buscar/provisionar usuarios, sincronizar roles, deshabilitar/revocar acceso. |
| `ms-estudiantes` | Feign síncrono | Validar referencias/perfil asociadas a una creación de apoderado cuando aplica. |
| RabbitMQ | AMQP asíncrono | Etapas de registro y retorno de estado. |
| MariaDB propia | JPA/Flyway | Usuarios, invitaciones, procesos, intentos y outbox. |

## Endpoints, seguridad y pruebas

`/api/v1/usuarios` ofrece CRUD, `/me`, lookup, invitaciones y registro asíncrono; catálogo ampliado en [`apps/backend/README.md`](../../apps/backend/README.md). Operaciones admin requieren ADMIN + scope. Variables `AZURE_*`, `REGISTRO_*`, Feign y Rabbit se inyectan en ejecución. Tests en `src/test/java`, incluyendo servicio, Graph/registro/cifrado y migraciones; las operaciones reales de Graph requieren tenant y permisos externos.
