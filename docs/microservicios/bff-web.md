# BFF Web

**Módulo:** `apps/backend/bff-web` · **Puerto:** 8080 · **Base de datos:** ninguna.

## Responsabilidad y límites

Es la única API prevista para Angular. Oculta las direcciones internas, autentica requests, aplica autorización por endpoint y agrega respuestas de dominio para cada portal. No es dueño de las entidades académicas ni reemplaza la seguridad de los microservicios: propaga el Bearer entrante y cada servicio destino vuelve a validar permisos.

## Endpoints y orquestación

Los controladores se organizan en `domain/`: `/api/me`, perfil agregado de estudiante, pupilos, cursos/notas/asistencias docentes, administración de usuarios y registro asíncrono, bandeja de notificaciones y administración RabbitMQ. La lista vigente con método, ruta y autoridad está en [`apps/backend/README.md`](../../apps/backend/README.md), sección BFF.

Código representativo: `domain/me/MeController.java` + `MeService.java`; `domain/estudiantes/PerfilEstudianteController.java` + `PerfilEstudianteService.java`; `integration/*/*Client.java` (Feign); `domain/admin/rabbitmq/RabbitMqAdminController.java`; configuración en `src/main/resources/application.yaml`.

Flujos centrales:

- `MeService` compone identidad/roles a partir de `ms-usuarios-auth`.
- `PerfilEstudianteService` combina estudiante, curso, dictaciones, evaluaciones y notas por clientes Feign; BFF usa mappers/DTO orientados a UI.
- Funcionalidad de docente escribe notas/evaluaciones/asistencias mediante BFF y después los dominios publican eventos.
- Admin RabbitMQ se expone como fachada; el frontend nunca abre conexión AMQP al broker.
- Notificaciones consultan `ms-notificaciones`; la autorización de destinatario se vuelve a calcular en el servicio dueño de bandeja.

## Dependencias declaradas

Ver [`pom.xml`](../../apps/backend/bff-web/pom.xml): Spring MVC + WebFlux, Validation, Spring Security y OAuth2 Resource Server, OpenFeign, Resilience4j, `feign-hc5`, Springdoc Swagger/Scalar, Actuator, Azure Spring, MapStruct y `core-share`. `feign-hc5` es necesario para que Feign emita PATCH hacia la bandeja. El plugin TypeScript Generator crea modelos desde DTOs bajo `apps/frontend/src/types`; `-Dtypescript.generator.skip=true` evita escritura cruzada en CI.

## Patrones, seguridad y fallas

- **BFF/fachada + composición de consultas:** endpoints del frontend no exponen topología de servicios.
- **Feign con fallbacks/circuit breakers:** fallos de dependencias se traducen a errores de servicio, típicamente 503. El comportamiento concreto está en cada `FallbackFactory`.
- **Propagación de contexto:** `FeignClientConfig` reenvía `Authorization`; correlation ID se propaga a logs/requests.
- **Resource server y method security:** `core-share` exige autenticación por defecto; endpoints aplican `@PreAuthorize` con rol/scope.
- No hay transacción distribuida que cubra varios microservicios; las operaciones agregadas deben admitir fallos parciales y exponer errores normalizados.

## Acoplamientos

El BFF llama sincrónicamente a `ms-usuarios-auth`, `ms-estudiantes`, `ms-apoderados`, `ms-docentes`, `ms-asignaturas`, `ms-clases`, `ms-evaluaciones`, `ms-notas`, `ms-asistencias`, `ms-notificaciones` y `ms-rabbitmq-admin`. URLs de ejecución se inyectan por `MS_*_URL` en Compose/plantilla Terraform. El BFF no publica directamente eventos de negocio; los productores son los servicios dueños del dato.

## Pruebas y operación

Tests del BFF están en `apps/backend/bff-web/src/test/java`; el POM tiene `spring-boot-starter-test` heredado. Revisar Swagger/Scalar, `/actuator/health`, respuestas 401/403 y payload JSON. Construcción sugerida: `mvn -f apps/backend/pom.xml -pl bff-web -am package -Dtypescript.generator.skip=true`.
