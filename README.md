# SIGA-Project

## Documentacion detallada

Este README resume el estado general del proyecto. El detalle tecnico esta en documentos independientes:

- [Documentacion del frontend](docs/frontend.md): estructura, librerias Nx, autenticacion y trabajo pendiente.
- [Documentacion del backend](docs/backend.md): microservicios, BFF, biblioteca compartida, seguridad, datos y estado.
- [Arquitectura del sistema](docs/arquitectura.md): arquitectura actual y objetivo, flujo de solicitudes, contratos y despliegue.

## Resumen ejecutivo

SIGA es un sistema de gestion academica organizado como monorepo (Nx) con un frontend Angular, un BFF Web y microservicios Spring Boot separados por responsabilidad.

El **nucleo academico ya es funcional**: los microservicios de usuarios, estudiantes, asignaturas y notas exponen APIs REST con operaciones CRUD, validaciones, busqueda y borrado logico, protegidas con OAuth2/JWT (Azure AD). El frontend ya es una aplicacion Angular modular con autenticacion MSAL y rutas por rol. La orquestacion del BFF y la infraestructura Terraform siguen pendientes.

## Objetivo del proyecto

Construir un sistema integral de gestion academica institucional, con separacion por modulos y capacidad de crecer en microservicios. El enfoque busca:

- gestionar usuarios y autenticacion
- administrar estudiantes
- gestionar asignaturas y docentes
- registrar y consultar notas
- centralizar la capa de frontend mediante un BFF
- preparar una base reproducible de despliegue y operacion

## Estado actual

| Componente | Estado |
| --- | --- |
| Frontend Angular | App modular (Nx + MSAL) con rutas por rol; pantallas de negocio pendientes |
| Microservicios | `ms-usuarios-auth`, `ms-estudiantes`, `ms-asignaturas`, `ms-notas` con CRUD funcional |
| BFF Web | Esqueleto (app + seguridad); orquestacion y `/me` pendientes |
| Biblioteca `core-share` | DTOs, validadores, seguridad, excepciones y OpenAPI compartidos |
| Seguridad | OAuth2/JWT con Azure AD y autorizacion por rol/scope |
| Documentacion API | Swagger UI y Scalar servidos en cada servicio |
| Docker | Compose completo: 4 MariaDB + 4 MS + BFF + Frontend (database-per-service) |
| Terraform | Pendiente (`terraform/main.tf` sin contenido) |

## Estructura del proyecto

```text
SIGA-Project/
├── apps/
│   ├── backend/
│   │   ├── pom.xml
│   │   ├── bff-web/                 # BFF (esqueleto)
│   │   ├── ms-usuarios-auth/        # usuarios, roles y estado de cuenta
│   │   ├── ms-estudiantes/          # ficha academica del estudiante
│   │   ├── ms-asignaturas/          # asignaturas
│   │   ├── ms-notas/                # calificaciones
│   │   └── libs/
│   │       └── core-share/          # DTOs, validadores, seguridad, excepciones
│   └── frontend/
│       ├── libs/                    # librerias Nx: core, shared-ui, features...
│       ├── src/                     # shell, rutas, configuracion
│       ├── public/config.json       # configuracion runtime (MSAL/BFF)
│       ├── nginx.conf
│       ├── Dockerfile
│       └── project.json
├── docs/                            # backend.md, frontend.md, arquitectura.md
├── docker-compose.yml
├── .env.example
├── terraform/main.tf
├── nx.json
├── tsconfig.base.json
├── eslint.config.js
└── README.md
```

## Stack tecnologico

**Backend**
- Java 21 · Spring Boot 3.5.0 · Maven
- Spring Cloud 2025.0.0 (OpenFeign) · Resilience4j
- Spring Security + OAuth2 Resource Server (JWT / Azure AD)
- Spring Data JPA / Hibernate · MariaDB
- MapStruct · Lombok
- springdoc 2.8.14 (Swagger UI) + Scalar (nativo)

**Frontend**
- Angular 22 · TypeScript 6 · RxJS 7.8 · Angular Router
- Nx (librerias y fronteras)
- MSAL Angular v6 + Azure AD
- Tailwind CSS (framework de estilos; integracion pendiente)
- Vitest · Prettier

**Infraestructura**
- Docker / Docker Compose · Nginx · Terraform (pendiente)

## Backend

Detalle en [docs/backend.md](docs/backend.md).

- **`ms-usuarios-auth`** (`/api/v1/usuarios`): CRUD de usuarios, busqueda por email/rol/estado y borrado logico (`INACTIVO`).
- **`ms-estudiantes`** (`/api/v1/estudiantes`): CRUD, busqueda, consulta por `idUsuario` y endpoint `exists`; borrado logico.
- **`ms-asignaturas`** (`/api/v1/asignaturas`): CRUD, listado, busqueda y `exists`; borrado logico.
- **`ms-notas`** (`/api/v1/notas`): CRUD y busqueda; valida existencia de estudiante y asignatura via Feign (con fallback Resilience4j).
- **`bff-web`**: esqueleto; pendiente la orquestacion y el endpoint `/me`.
- **`core-share`**: DTOs, enums (`Rol`, `StateUsuario`, `State`), validadores (`RUT`, `Phone`, `ChileanGrade`), seguridad compartida, manejo de errores y OpenAPI.

## Frontend

Detalle en [docs/frontend.md](docs/frontend.md).

- App Angular modular con **librerias Nx** (`core`, `shared-ui`, `public-portal`, `academico`, `estudiante`, `apoderado`, `docente`, `admin`).
- **Autenticacion MSAL v6 + Azure AD** con configuracion runtime (`public/config.json`).
- **Rutas por rol** con lazy loading y guards; portal publico de bienvenida.
- **Fronteras Nx** (`tags` + `depConstraints`) para separar responsabilidades.
- Servida por **Nginx** en contenedor, con proxy `/api` hacia el BFF.

## Seguridad y documentacion API

- OAuth2/JWT con **Azure AD** como proveedor de identidad.
- Autorizacion por rol y scope (`hasRole(...)` / `hasAuthority('SCOPE_...')`).
- El frontend solo habla con el **BFF**; el BFF y los servicios propagan el token.
- Documentacion publica por servicio:

| Ruta | Descripcion |
| --- | --- |
| `/v3/api-docs` | Especificacion OpenAPI (JSON) |
| `/v3/api-docs.yaml` | Especificacion OpenAPI (YAML) |
| `/docs/swagger` | Swagger UI (redirige a `/docs/swagger-ui/index.html`) |
| `/docs/scalar` | Scalar UI |

## Docker y despliegue (database-per-service)

Cada microservicio tiene su propia base de datos MariaDB en `siga-network`.

| Servicio | Puerto host | Base de datos | Volumen |
| --- | --- | --- | --- |
| `ms-usuarios-auth` | 8081 | `siga_usuarios_db` | `mariadb_usuarios_data` |
| `ms-estudiantes` | 8082 | `siga_estudiantes_db` | `mariadb_estudiantes_data` |
| `ms-asignaturas` | 8086 | `siga_asignaturas_db` | `mariadb_asignaturas_data` |
| `ms-notas` | 8087 | `siga_notas_db` | `mariadb_notas_data` |
| `bff-web` | 8080 | — | — |
| `frontend` | 4200 | — | — |

## Variables de entorno

Copiar `.env.example` a `.env` y completar los valores (`.env` no se versiona):

```dotenv
MARIADB_ROOT_PASSWORD=change_me_root
DB_USER=siga
DB_PASS=change_me

AZURE_TENANT_ID=
AZURE_CLIENT_ID=
AZURE_APP_ID_URI=api://<client-id>
```

El frontend se configura en runtime via `apps/frontend/public/config.json` (clientId, authority, scopes y URL del BFF).

## Como levantar el entorno

```bash
cp .env.example .env     # completar credenciales de Azure y MariaDB
docker compose up -d --build
```

URLs:
- Frontend: http://localhost:4200
- BFF: http://localhost:8080
- Servicios: 8081 / 8082 / 8086 / 8087
- Docs (ej. estudiantes): http://localhost:8082/docs/swagger y http://localhost:8082/docs/scalar

## Pendientes

- **BFF**: orquestacion de llamadas a los microservicios y endpoint `/me` para el enrutamiento por rol.
- **Frontend**: pantallas de negocio reales consumiendo el BFF; formularios y validaciones.
- **Terraform**: infraestructura declarativa.
- **Pruebas**: unitarias, de integracion y de contrato.
- **Servicios futuros**: `ms-docentes`, `ms-apoderados`, `ms-asistencias`, `ms-auditoria`.

## Conclusion

La base tecnica esta solida: dominio academico funcional en microservicios, contratos y componentes transversales compartidos, seguridad con Azure AD, documentacion de API y un entorno Docker reproducible con base de datos por servicio. Lo siguiente es completar la capa de orquestacion (BFF + `/me`), conectar las pantallas del frontend y preparar la infraestructura y las pruebas.
