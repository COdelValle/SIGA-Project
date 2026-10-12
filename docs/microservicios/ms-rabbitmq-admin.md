# `ms-rabbitmq-admin`

**Módulo:** `apps/backend/ms-rabbitmq-admin` · **Puerto:** 8092 · **Base de datos:** ninguna.

## Responsabilidad

Microservicio de control plane para crear/eliminar colas, exchanges y bindings. No es consumidor de eventos de negocio. El BFF lo expone para UI/admin; la UI no recibe conexión, credenciales AMQP ni dirección de RabbitMQ.

## Dependencias declaradas

[`pom.xml`](../../apps/backend/ms-rabbitmq-admin/pom.xml): Web, Validation, AMQP (`AmqpAdmin`), Security/OAuth2 Resource Server, Springdoc, Actuator y `core-share`. No declara JPA, MariaDB ni Feign.

## Arquitectura, seguridad y endpoints

`RabbitAdminController` valida bodies con `@Valid`, autoriza ADMIN + scope y delega a `RabbitAdminService`, que encapsula `AmqpAdmin`. Rutas bajo `/api/v1/rabbitmq`: POST/GET/DELETE `/queues`, POST/DELETE `/exchanges` y POST/DELETE `/bindings`. BFF reexpone `/api/bff/v1/admin/rabbitmq/**` usando Feign.

Los DTO validan campos centrales como nombre, tipo de exchange y routing key. **Punto abierto:** `@PathVariable` de cola/exchange y varios `@RequestParam` de DELETE no tienen restricciones `@NotBlank`/equivalentes visibles; la validación completa de rutas/query queda por probar/mejorar.

## Dependencias operacionales

- RabbitMQ management/AMQP del entorno y credenciales inyectadas mediante `SPRING_RABBITMQ_*`.
- BFF hace llamada HTTP interna a la API admin, propaga autenticación y agrega su control de rol/scope.
- No administra usuarios ni conexiones para el navegador. Evitar exponer Management UI o puerto 5672 como sustituto de esta API.

## Pruebas y evidencia

`RabbitAdminServiceTest` usa `AmqpAdmin` mock; `RabbitAdminControllerMappingsTest` inspecciona mapeos; pruebas DTO verifican validaciones. Complementar con peticiones válidas e inválidas contra broker desechable y revisar exchanges/colas resultantes.
