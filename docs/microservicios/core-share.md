# `core-share` — biblioteca transversal

**Módulo:** `apps/backend/libs/core-share` · **Tipo:** JAR compartido, no servicio ejecutable.

## Propósito

Centraliza capacidades técnicas y contratos que deben ser compatibles entre microservicios sin convertirse en dueño de una regla de dominio: DTOs/versiones de eventos, enums comunes, validadores, normalización de RUT/nombres, formato de fecha, errores, seguridad, OpenAPI, trazabilidad y auto-configuración de RabbitMQ.

## Dependencias declaradas

[`pom.xml`](../../apps/backend/libs/core-share/pom.xml) incluye Validation API, Spring Boot starter, Web, Security, OAuth2 Resource Server, Springdoc Swagger/Scalar, Spring TX/JDBC, OpenFeign opcional, AMQP y Spring Retry/Spring Data Commons. El Resource Server se configura explícitamente con Spring Security/Nimbus; la integración Graph usa Azure Identity solo en `ms-usuarios-auth`. `core-share` no debe incluir entidades JPA de negocio ni depender de un microservicio concreto.

## Patrones y responsabilidades

- **Spring Boot auto-configuration** registrada en `META-INF/spring/...AutoConfiguration.imports`; `SharedSecurityConfig` usa `@ConditionalOnMissingBean` para permitir override explícito.
- **Seguridad JWT común:** `JwtResourceServerConfiguration` deriva issuer, JWKS y audiences de Entra (con override por entorno); `NimbusJwtDecoder` comprueba firma, issuer, timestamp y audience. `JwtClaimAuthoritiesConverter` mapea `scp → SCOPE_` y `roles → ROLE_`; `SharedSecurityConfig` exige autenticación y activa method security.
- **Contratos compartidos:** DTOs API/Feign y eventos Rabbit versionados; validadores personalizados (`@RUT`, teléfono, nota) y manejador global de errores.
- **RabbitMQ:** `ConfiguracionTopologiaNotificaciones` declara topic/direct, colas, DLQs/bindings; `RegistrationMessagingConfig` define el flujo de registro bajo property; `ConfirmadorMensajes` comparte ACK/NACK y reintentos.
- **Trazabilidad/formato:** correlation ID en MDC y mensajes AMQP; fechas ISO y configuración equivalente para OpenFeign.

Código representativo: `security/JwtResourceServerConfiguration.java`, `security/JwtAudienceValidator.java`, `security/JwtClaimAuthoritiesConverter.java`, `security/SharedSecurityConfig.java`, `security/SecurityUtils.java`, `exception/GlobalExceptionHandler.java`, `config/SharedFeignAuthConfig.java`, `config/CommonDateFormatConfig.java`, `mensajeria/ConfiguracionNombresRabbitMQ.java`, `mensajeria/ConfiguracionTopologiaNotificaciones.java`, `mensajeria/ConfiguracionMensajeriaCompartida.java`, `mensajeria/ConfirmadorMensajes.java` y `messaging/RegistrationMessagingConfig.java`. La carga automática se registra en `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.

## Dependencias implícitas y riesgos de cambio

Al depender de `core-share`, cada servicio que incluya el JAR hereda clases de auto-configuración y parte del classpath, incluyendo AMQP en módulos donde no aparece declarado directamente en su POM. En despliegue los overrides opcionales son `AZURE_JWT_ISSUER`, `AZURE_JWKS_URI` y `AZURE_JWT_AUDIENCES`; los defaults corresponden al issuer v1 del tenant, sus JWKS y audiencias App ID URI/client ID. Esto reduce duplicación, pero un cambio en seguridad, DTO o topología afecta a varios servicios. La versión del contrato es la versión del reactor Maven; al evolucionar un evento, coordinar productor/consumidor, compatibilidad de esquema y pruebas.

## Pruebas

Las pruebas del módulo están bajo `apps/backend/libs/core-share/src/test/java`; destacan `ConfirmadorMensajesTest`, `ConfiguracionTopologiaNotificacionesTest`, validadores y configuración Feign/fechas. Los tests unitarios no prueban por sí solos un broker real ni un JWT firmado por Entra.
