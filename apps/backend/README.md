# Backend SIGA

Backend del sistema de gestión académica **SIGA**. Es un proyecto **Maven
multi-módulo** basado en **Spring Boot 3.5 / Java 21** que separa el dominio
académico en microservicios independientes, expone APIs REST protegidas con
OAuth2/JWT (Azure AD) y centraliza los contratos en la librería `core-share`.

El núcleo académico y la orquestación del BFF (`/me` y perfil de estudiante) son
funcionales. La documentación ampliada está en [`docs/backend.md`](../../docs/backend.md).

## Módulos

| Módulo | Puerto | Responsabilidad |
| --- | --- | --- |
| `libs/core-share` | — | DTOs, validadores, seguridad, excepciones y OpenAPI compartidos. |
| `bff-web` | 8080 | Backend For Frontend: `/api/me` y orquestación por Feign. |
| `ms-usuarios-auth` | 8081 | Usuarios, roles, `/me` y sincronización con Microsoft Graph. |
| `ms-estudiantes` | 8082 | Ficha personal y académica del estudiante. |
| `ms-asignaturas` | 8086 | Asignaturas. |
| `ms-notas` | 8087 | Calificaciones; valida estudiante/asignatura por Feign. |

## Tecnologías

- Java 21 · Spring Boot 3.5.0 · Spring Cloud 2025.0.0 (OpenFeign) + Resilience4j.
- Spring Security / OAuth2 Resource Server con JWT (Azure AD).
- Spring Data JPA + Hibernate · MapStruct · Lombok.
- MariaDB 11.4 (una base por microservicio) + Flyway.
- springdoc 2.8.14 (Swagger UI + Scalar).

## Requisitos

- **JDK 21** y **Maven** (hay wrappers `mvnw` / `mvnw.cmd` en cada módulo).
- **MariaDB** accesible (o Docker Compose, que la provee).

## Comandos

Todos los comandos parten del POM padre `apps/backend/pom.xml`.

```bash
# Compilar e instalar todo (sin tests)
mvn -f apps/backend/pom.xml -DskipTests install

# Build + tests unitarios (excluye contextLoads, que requieren BD y Azure)
mvn -f apps/backend/pom.xml -B package \
  -Dtest='!*ApplicationTests' -DfailIfNoTests=false

# Ejecutar un módulo (compila dependencias con -am)
mvn -f apps/backend/pom.xml -pl ms-notas -am spring-boot:run

# Ejecutar un test concreto
mvn -f apps/backend/pom.xml -pl ms-usuarios-auth test -Dtest=UsuarioServiceTest
```

> `typescript-generator.skip=true` evita que `bff-web` escriba modelos TypeScript
> fuera del workspace (lo usa el CI).

## Endpoints

### BFF Web (`:8080`)

| Método | Ruta | Autorización | Descripción |
| --- | --- | --- | --- |
| `GET` | `/api/me` | Autenticado | Usuario actual (id, email, displayName, rol). |
| `GET` | `/api/bff/v1/estudiantes/perfil/{idExterno}` | `SCOPE_estudiantes:read` + `asignaturas:read` + `notas:read` | Perfil agregado: estudiante + asignaturas + notas. |

### Usuarios y autenticación (`:8081`, `/api/v1/usuarios`)

| Método | Ruta | Autorización |
| --- | --- | --- |
| `GET` | `/me` | Autenticado |
| `GET` | `/lookup?email=` | `ADMIN` + `SCOPE_usuarios:read` |
| `GET` | `/{id}` | `ADMIN` + `SCOPE_usuarios:read` |
| `GET` | `/search?email=&rol=&state=` | `ADMIN` + `SCOPE_usuarios:read` |
| `POST` | `/` | `ADMIN` + `SCOPE_usuarios:write` |
| `PUT` | `/{id}` | `ADMIN` + `SCOPE_usuarios:update` |
| `DELETE` | `/{id}` | `ADMIN` + `SCOPE_usuarios:delete` (borrado lógico) |
| `POST` | `/{id}/sync-roles` | `ADMIN` + `SCOPE_usuarios:update` |

### Estudiantes (`:8082`, `/api/v1/estudiantes`)

| Método | Ruta | Autorización |
| --- | --- | --- |
| `GET` | `/{id}` | `SCOPE_estudiantes:read` |
| `GET` | `/idUsuario/{idUsuario}` | `SCOPE_estudiantes:read` |
| `GET` | `/search?rut=&firstName=&...&from=&to=&state=` | `SCOPE_estudiantes:read` |
| `GET` | `/exists/{id}` | `SCOPE_estudiantes:read` |
| `POST` | `/` | `ADMIN` + `SCOPE_estudiantes:write` |
| `PUT` | `/{id}` | `ADMIN` o `APODERADO` + `SCOPE_estudiantes:update` |
| `DELETE` | `/{id}` | `ADMIN` + `SCOPE_estudiantes:delete` (borrado lógico) |

### Asignaturas (`:8086`, `/api/v1/asignaturas`)

| Método | Ruta | Autorización |
| --- | --- | --- |
| `GET` | `/{id}` | `SCOPE_asignaturas:read` |
| `GET` | `/` | `SCOPE_asignaturas:read` |
| `GET` | `/search?name=` | `SCOPE_asignaturas:read` |
| `GET` | `/name/{name}` | `SCOPE_asignaturas:read` |
| `GET` | `/exists/{id}` | `SCOPE_asignaturas:read` |
| `POST` | `/` | `ADMIN` + `SCOPE_asignaturas:write` |
| `PUT` | `/{id}` | `ADMIN` + `SCOPE_asignaturas:update` |
| `DELETE` | `/{id}` | `ADMIN` + `SCOPE_asignaturas:delete` (borrado lógico) |

### Notas (`:8087`, `/api/v1/notas`)

| Método | Ruta | Autorización |
| --- | --- | --- |
| `GET` | `/{id}` | `SCOPE_notas:read` |
| `GET` | `/search?idEstudiante=&idAsignatura=&lessThanScore=&greaterThanScore=` | `SCOPE_notas:read` |
| `POST` | `/` | `ADMIN` o `DOCENTE` + `SCOPE_notas:write` |
| `PUT` | `/{id}` | `ADMIN` o `DOCENTE` + `SCOPE_notas:update` |
| `DELETE` | `/{id}` | `ADMIN` o `DOCENTE` + `SCOPE_notas:delete` (borrado lógico) |

## Comunicación entre servicios

- **Feign** es el cliente declarativo (p. ej. `bff-web` orquesta estudiantes,
  asignaturas y notas; `ms-notas` verifica existencia de estudiante y asignatura).
- **Resilience4j** aporta circuit breaker y timeouts; si un servicio no responde
  se activa el fallback correspondiente (`*Fallback`).
- **Propagación del token**: un `RequestInterceptor` reenvía el `Authorization`
  entrante en las llamadas Feign, de modo que la autorización se evalúa en destino.

## Seguridad

- OAuth2/JWT con Azure AD (`spring.cloud.azure.active-directory`).
- Autorización por método: roles (`hasRole`) y scopes (`hasAuthority('SCOPE_...')`).
- Rutas públicas limitadas a salud y documentación técnica (`/actuator/**`, `/docs/**`, `/v3/api-docs/**`).
- `core-share` aporta `SharedSecurityConfig` (filtro stateless y conversión de
  claims `scp`/`roles`) y `SecurityUtils`.
- El frontend nunca accede a los microservicios directamente: lo hace a través del BFF.

## Datos y configuración

- Patrón **database-per-service**: `siga_usuarios_db`, `siga_estudiantes_db`,
  `siga_asignaturas_db` y `siga_notas_db`.
- **Flyway** crea y evoluciona el esquema (`ddl-auto: validate`); una base vacía
  se auto-inicializa al arrancar.
- En Docker las variables se inyectan desde `.env`
  (`SPRING_DATASOURCE_*`, `SERVER_PORT`, Azure, `MS_*_URL`).

Variables principales: `MARIADB_ROOT_PASSWORD`, `DB_USER`, `DB_PASS`,
`AZURE_TENANT_ID`, `AZURE_CLIENT_ID`, `AZURE_APP_ID_URI`, `AZURE_CLIENT_SECRET`,
`AZURE_API_APP_ID`, `MS_USUARIOS_URL`, `MS_ESTUDIANTES_URL`,
`MS_ASIGNATURAS_URL`, `MS_NOTAS_URL`.

## Documentación de la API

Cada servicio expone (rutas públicas): `/actuator/health`, `/v3/api-docs`,
`/docs/swagger` (Swagger UI) y `/docs/scalar` (Scalar).

## Pruebas

```bash
mvn -f apps/backend/pom.xml -B package -Dtest='!*ApplicationTests' -DfailIfNoTests=false
```

Los `*ApplicationTests` (contextLoads) se excluyen en CI porque requieren base de
datos y Azure AD.

## Enlaces

- [README raíz](../../README.md)
- [`docs/backend.md`](../../docs/backend.md) · [`docs/arquitectura.md`](../../docs/arquitectura.md)
- [`docs/testing-login.md`](../../docs/testing-login.md) · [Índice de docs](../../docs/README.md)
