# Backend SIGA

## Propósito y estructura

El backend en `apps/backend` es un reactor Maven Spring Boot/Java que contiene 11 microservicios, el BFF Web y `core-share`. Cada servicio ejecutable es dueño de su modelo de negocio/API; el BFF compone respuestas para la SPA; `core-share` aporta capacidades transversales. Las fichas por servicio son la referencia de propósito, POM, patrones y acoplamientos: [`docs/microservicios/README.md`](microservicios/README.md).

El inventario de módulos/puertos, comandos y catálogo de endpoints permanece en [`apps/backend/README.md`](../apps/backend/README.md); los endpoints generados están disponibles en `/v3/api-docs`, `/docs/swagger` y `/docs/scalar` al levantar cada servicio.

## Stack y dependencias administradas

El POM padre [`apps/backend/pom.xml`](../apps/backend/pom.xml) fija Java 21 y Spring Boot 3.5.0. Gestiona Spring Cloud 2025.0.0, Azure Spring 5.18.0, Springdoc 2.8.14 y MapStruct 1.5.5; configura procesamiento de anotaciones Lombok/MapStruct y dependencias de test comunes. Cada POM de módulo declara sus propios starters; no asumir que todo el classpath común está declarado directamente.

Tecnologías transversales: Spring MVC, Spring Security + OAuth2 Resource Server, Spring Data JPA/Hibernate, MariaDB, Flyway, OpenFeign/Resilience4j donde aplica, MapStruct, Lombok, Actuator, OpenAPI y RabbitMQ AMQP en componentes participantes. El resource server compartido usa `NimbusJwtDecoder` con JWKS Entra, issuer/audience y validación temporal explícitos, y convierte claims `scp`/`roles` a authorities Spring.

## Patrones de arquitectura

- **Database-per-service:** cada dominio tiene esquema separado; en local hay contenedor MariaDB por base y en AWS un motor MariaDB con varias bases lógicas. Nunca unir tablas de distintos dominios en una transacción/consulta directa.
- **REST + Feign:** llamadas síncronas para validación de referencias y composición BFF. Ver el grafo actualizado en [`microservicios/README.md`](microservicios/README.md).
- **Resilience4j:** timeouts/circuit breakers/fallbacks para dependencias Feign declaradas; un fallback no equivale a disponibilidad de datos correctos.
- **Capas por servicio:** controller/DTO para HTTP, service para regla de dominio, repository/entity para persistencia, mapper para contratos. El grado de separación concreto se documenta en cada ficha.
- **Seguridad stateless:** `SharedSecurityConfig` configura resource server, rutas técnicas públicas y method security; `SecurityUtils` apoya ownership por `oid`. Roles/scopes se derivan del JWT mediante configuración común. Revisar `@PreAuthorize` del controlador antes de asumir cobertura.
- **Errores/trazabilidad:** `core-share` centraliza excepciones/respuestas y correlation ID; fechas API ISO (`yyyy-MM-dd`) también para conversiones Feign.
- **Migraciones:** Flyway crea/evoluciona el esquema y Hibernate valida, en lugar de generar DDL automáticamente.

## Comunicación HTTP y acoplamientos

El navegador solo conoce `/api`; API Gateway dirige a BFF y el BFF llama los dominios. Existen llamadas servicio-a-servicio para validar IDs y reglas de negocio. `SharedFeignAuthConfig` propaga Bearer entrante en clientes que lo usan. Hay dependencia runtime en ambas direcciones entre estudiantes/apoderados para algunos chequeos de vínculo/ownership: debe preservarse como llamada acotada, no como FK o ciclo de arranque.

Los endpoints exactos, autoridades y parámetros se mantienen en [`apps/backend/README.md`](../apps/backend/README.md). La paginación `Page<T>` está descrita en [`paginacion.md`](paginacion.md).

## Dos flujos RabbitMQ

La topología no es una sola cola genérica. Consulte [`microservicios/core-share.md`](microservicios/core-share.md) para configuración y las fichas de productores/consumidores.

1. **Registro de usuarios:** `ms-usuarios-auth` + outbox/Graph; eventos `user.topic.exchange`; perfiles de estudiante/docente/apoderado y estado por colas; idempotencia por OID/proceso; DLQ por etapa.
2. **Notificaciones académicas:** outbox de evaluaciones/notas/asistencias → topic `intercambio-notificaciones` → tres colas de `ms-notificaciones`; direct `siga.dlx.direct` enruta fallos a DLQ. Consumidores ACK manual, retry acotado, NACK sin requeue al agotar o al recibir payload no recuperable.

`ms-rabbitmq-admin` administra topología en tiempo de ejecución por REST; sus cambios pueden diferir de la declaración estática si no se coordinan. El dashboard sirve para verificar broker; no es contrato canónico de nombres.

## Configuración y operación

Compose y Terraform inyectan variables en tiempo de ejecución (`SPRING_DATASOURCE_*`, `AZURE_*`, `MS_*_URL`, `SPRING_RABBITMQ_*`, `REGISTRO_*`). Los valores reales permanecen en `.env`/GitHub Secrets; la documentación solo enumera nombres. Health en `/actuator/health`; una respuesta healthy no demuestra por sí sola que todos los flujos externos funcionan.

Comandos de build/tests, ejecución por módulo y restricciones del generador TypeScript están en [`apps/backend/README.md`](../apps/backend/README.md). Las verificaciones ejecutadas se anotan cronológicamente en [`registro-pruebas.md`](registro-pruebas.md).

## Pendientes y límites conocidos

- Validar en entorno real issuer/audience, firma, expiración y authorities para los JWT de Entra.
- Probar las llamadas Feign en escenarios de timeout/fallback e identificar acoplamientos bidireccionales.
- La API de administración RabbitMQ necesita completar/comprobar restricciones de parámetros de ruta/consulta, además de validar bodies.
- Logs, métricas y tracing no están completos; correlation ID ya permite correlación básica.
- `ms-auditoria` es futuro y no forma parte de los módulos compilados del POM.
