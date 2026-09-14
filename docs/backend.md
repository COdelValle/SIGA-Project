# Documentacion del backend

## 1. Proposito

El backend de SIGA es un proyecto Maven multi-modulo basado en Spring Boot. Separa las responsabilidades del dominio academico en servicios independientes, expone APIs REST protegidas y centraliza los contratos compartidos en la libreria `core-share`.

El nucleo academico es funcional (CRUD, validaciones, busqueda y borrado logico), el BFF ya orquesta `/me` y el perfil de estudiante, y la infraestructura Terraform esta implementada. Las pantallas de negocio del frontend y parte de la orquestacion del BFF siguen pendientes.

## 2. Ubicacion y tecnologias

- Ubicacion: `apps/backend`
- Java 21
- Spring Boot 3.5.0
- Spring Cloud 2025.0.0 (OpenFeign) + Resilience4j
- Maven multi-modulo
- Spring Security y OAuth2 Resource Server con JWT (Azure AD)
- Spring Data JPA e Hibernate
- MapStruct y Lombok
- springdoc 2.8.14 (Swagger UI) + Scalar (starter nativo de springdoc)
- MariaDB (una base por microservicio)

El `pom.xml` padre centraliza versiones, dependencias y modulos. Modulos declarados: `libs/core-share`, `bff-web`, `ms-usuarios-auth`, `ms-estudiantes`, `ms-asignaturas` y `ms-notas`.

## 3. Componentes del backend

### 3.1 BFF Web

Ubicacion: `apps/backend/bff-web`

Puerta de entrada pensada para el frontend: oculta la topologia interna, coordina solicitudes y entrega respuestas orientadas a la interfaz.

Responsabilidades previstas:

- Exponer endpoints consumibles por Angular.
- Validar el token y propagar el contexto de seguridad.
- Orquestar llamadas a los microservicios (Feign).
- Unificar respuestas y errores.
- Evitar que el navegador dependa de las direcciones internas.

Estado actual: **orquesta `/me` y el perfil de estudiante**. Incluye:

- `MeController` (`GET /api/me`): resuelve el usuario via `UsuarioClient` y compone `{ id, email, displayName, roles }`.
- `PerfilEstudianteController` (`GET /api/bff/v1/estudiantes/perfil/{idExterno}`): agrega estudiante, asignaturas y notas usando `EstudianteClient`, `AsignaturaClient` y `NotaClient` con mappers.
- Clientes Feign con fallback para usuarios, estudiantes, asignaturas y notas (`integration/*`).
- `FeignClientConfig` con propagacion del `Authorization`.

Pendiente: orquestar el resto de recursos de la interfaz.

### 3.2 Servicio de usuarios y autenticacion

Ubicacion: `apps/backend/ms-usuarios-auth` · puerto `8081`

Administra usuarios, roles y estado de las cuentas, en conjunto con los claims del JWT de Azure AD.

Incluye:

- Entidad `Usuario` (PK = `id` de Azure, rol, estado).
- `UsuarioRepository` (JPA + Specifications), `UsuarioSpecifications`.
- `UsuarioService` y `UsuarioMapper` (MapStruct).
- `UsuarioController` en `/api/v1/usuarios`: CRUD, `GET /me`, `GET /lookup?email=` y `POST /{id}/sync-roles`.
- Integracion con **Microsoft Graph** (opcional via `AZURE_CLIENT_SECRET`): pre-registro por correo, cambio de rol y sincronizacion de app roles.
- DTOs de registro, actualizacion y respuesta en `core-share`.

Comportamiento:

- `POST` crea el usuario con estado `ACTIVO`.
- `DELETE` aplica **borrado logico** (estado `INACTIVO`); los inactivos no se devuelven en consultas.
- `GET /search` filtra por `email`, `rol` y `state`.

Seguridad: `hasRole('ADMIN')` combinado con `hasAuthority('SCOPE_usuarios:read|write|update|delete')`.

Pendientes: contrato final de identidad (Azure AD vs SIGA) y pruebas.

### 3.3 Servicio de estudiantes

Ubicacion: `apps/backend/ms-estudiantes` · puerto `8082`

Administra la ficha personal y academica de los estudiantes.

Incluye:

- Entidad `Estudiante` (RUT validado, nombres/apellidos, fecha de nacimiento, alergias, estado).
- Repositorio, service, mapper y `EstudianteSpecifications`.
- `EstudianteController` en `/api/v1/estudiantes`.

Endpoints:

- `GET /{id}`, `GET /idUsuario/{idUsuario}`, `GET /search` (rut, nombres, apellidos, rango de fecha, estado).
- `GET /exists/{id}` (verificacion de existencia; usado por otros servicios).
- `POST`, `PUT /{id}`, `DELETE /{id}`.

Comportamiento y reglas:

- Normaliza RUT y nombres a mayusculas.
- `DELETE` aplica **borrado logico** (`State.INACTIVO`); `State` se centraliza en `core-share`.
- Valida RUT chileno con `@RUT`.

### 3.4 Servicio de asignaturas

Ubicacion: `apps/backend/ms-asignaturas` · puerto `8086`

Gestiona asignaturas. La entidad usa nombres en ingles (`name`, `description`).

Incluye:

- Entidad `Asignatura` (nombre unico, descripcion y `active`).
- Repositorio, service, mapper.
- `AsignaturaController` en `/api/v1/asignaturas`.

Endpoints:

- `GET /{id}`, `GET` (listado), `GET /search?name=`, `GET /name/{name}`, `GET /exists/{id}`.
- `POST`, `PUT /{id}`, `DELETE /{id}`.

Comportamiento:

- `DELETE` aplica **borrado logico** (`active = false`); listados y `exists` solo consideran activos.
- Normaliza el nombre (mayusculas) para busquedas y unicidad.

### 3.5 Servicio de notas

Ubicacion: `apps/backend/ms-notas` · puerto `8087`

Registra y consulta calificaciones relacionadas con estudiantes y asignaturas.

Incluye:

- Entidad `Nota` (`idEstudiante`, `idAsignatura`, `score`, `active`).
- Repositorio, service, mapper y `NotaSpecifications`.
- `NotaController` en `/api/v1/notas`.

Endpoints:

- `GET /{id}`, `GET /search` (por estudiante, asignatura y rango de nota).
- `POST`, `PUT /{id}`, `DELETE /{id}`.

Comportamiento e integracion:

- Valida el rango de la nota (1.0 a 7.0) con `@ChileanGrade`.
- **Integracion Feign**: antes de guardar, verifica la existencia del estudiante (`ms-estudiantes`) y de la asignatura (`ms-asignaturas`) usando sus endpoints `exists`.
- **Resilience4j**: circuit breaker y timeouts configurados; si un servicio no responde, se aplica el fallback y se responde `503`.
- Propagacion del token: un `RequestInterceptor` reenvia el `Authorization` entrante en las llamadas Feign.
- `DELETE` aplica **borrado logico** (`active = false`).

### 3.6 Biblioteca compartida

Ubicacion: `apps/backend/libs/core-share`

Centraliza elementos reutilizables, sin logica de un dominio especifico:

- **DTOs** de usuario, estudiante, asignatura y notas.
- **Enums**: `Rol`, `StateUsuario` y `State` (estudiante).
- **Validadores**: `@RUT` (RUT chileno), `@Phone` y `@ChileanGrade` (nota 1.0 a 7.0).
- **Seguridad**: `SharedSecurityConfig` (filtro stateless, rutas publicas de salud/documentacion y conversion de claims `scp`/`roles` a scopes/roles) y `SecurityUtils`.
- **Excepciones**: `BusinessException`, `ResourceNotFoundException`, `BadRequestException`, `ServiceUnavailableException`, `GlobalExceptionHandler` y `ErrorResponseDTO`.
- **OpenAPI**: `SharedOpenApiConfig` (esquema `bearerAuth`).
- **Fechas**: `CommonDateFormatConfig` (`dd/MM/yyyy` para JSON y parametros).

Se registra mediante `META-INF/spring/...AutoConfiguration.imports`.

## 4. Seguridad y contratos

- OAuth2/JWT con Azure AD (`spring.cloud.azure.active-directory`).
- Autorizacion por metodo: roles (`hasRole`) y scopes (`hasAuthority('SCOPE_...')`).
- Rutas publicas limitadas a salud y documentacion tecnicas.
- El frontend no accede a los microservicios: lo hara a traves del BFF.
- Los contratos se definen en cada servicio y se reflejan en los DTOs de `core-share` y en los modelos TypeScript del frontend.

## 5. Datos y configuracion

- Patron **database-per-service**: cada microservicio tiene su propia MariaDB.
- `docker-compose.yml` levanta 4 MariaDB, los 4 microservicios, el BFF y el frontend.
- En desarrollo, `docker-compose` inyecta `SPRING_DATASOURCE_*`, `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` (Flyway gestiona el esquema) y las variables de Azure; no se requieren ficheros `application-*.yml` extra.
- Variables principales: `DB_HOST`, `DB_USER`, `DB_PASS`, `AZURE_TENANT_ID`, `AZURE_CLIENT_ID`, `AZURE_APP_ID_URI`, `MS_ESTUDIANTES_URL`, `MS_ASIGNATURAS_URL`, `MS_NOTAS_URL`.

## 6. Documentacion API

Servida por cada servicio:

- `/v3/api-docs` y `/v3/api-docs.yaml`: especificacion OpenAPI.
- `/docs/swagger`: Swagger UI (redirige a `/docs/swagger-ui/index.html`).
- `/docs/scalar`: Scalar UI.

Nota de version: se usa **springdoc 2.8.14** por compatibilidad con Spring Boot 3.5 / Spring Framework 6.2 (2.6.0 fallaba al generar el OpenAPI y 2.8.15+ tenia una regresion de patrones de ruta).

## 7. Estado y orden recomendado

1. Completar la orquestacion del BFF para el resto de recursos (`/me` y perfil de estudiante ya estan implementados).
2. Conectar las pantallas del frontend a traves del BFF.
3. Anadir pruebas unitarias, de integracion y de contrato.
4. Definir migraciones de esquema para produccion.
5. Completar la observabilidad (logs estructurados, correlation ID, metricas, tracing).
6. Incorporar servicios futuros (`ms-docentes`, `ms-apoderados`, `ms-asistencias`, `ms-auditoria`).
