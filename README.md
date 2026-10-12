# SIGA — Sistema de Gestión Académica

Monorepo del proyecto **SIGA**, un sistema de gestión académica compuesto por un
frontend Angular, un BFF (Backend For Frontend) y microservicios Spring Boot
separados por dominio, con persistencia *database-per-service* en MariaDB.

- **Frontend**: `apps/frontend` (Angular 22 + MSAL + Tailwind CSS 4, librerías Nx).
- **Backend**: `apps/backend` (Maven multi-módulo, Spring Boot 3.5, Java 21).
- **Infraestructura**: `infra/terraform` (AWS Academy Learner Lab: EC2, EBS, ECR y API Gateway HTTP API).
- **Documentación detallada**: [`docs/`](docs/README.md).

> Estado del repositorio: el backend contiene los dominios académicos, el BFF y
> los flujos asíncronos de registro/notificaciones RabbitMQ. El frontend dispone
> de portales por rol y consume datos reales del BFF para los recursos que tienen
> endpoint. `useMocks` está desactivado por defecto; los mocks se usan en modo demo
> explícito o como constantes, no como fallback automático de una petición fallida.
> Algunos recursos/pantallas siguen pendientes de conexión y la observabilidad
> avanzada está pendiente. El estado cloud actual requiere verificación en el entorno.

## Arquitectura

```mermaid
flowchart LR
    U[Usuario] -->|HTTPS| G[API Gateway HTTP API]
    G -->|"$default: SPA"| F[Frontend Angular + Nginx]
    G -->|"/api + Bearer JWT"| B[BFF Web]
    B --> A[MS Usuarios y autenticación]
    B --> E[MS Estudiantes]
    B --> S[MS Asignaturas]
    B --> N[MS Notas]
    B --> V[MS Evaluaciones]
    B --> H[MS Asistencias]
    B --> T[MS Notificaciones]
    B --> R[MS RabbitMQ Admin]
    B -. Feign .-> A
    B -. Feign .-> E
    B -. Feign .-> S
    B -. Feign .-> N
    B -. Feign .-> V
    B -. Feign .-> H
    B -. Feign .-> T
    B -. Feign .-> R
    N -. "Feign: exists" .-> E
    N -. "Feign: exists" .-> S
    V -. "Feign: dictación" .-> S
    H -. "Feign: estudiante/dictación" .-> E
    H -. "Feign: dictación" .-> S
    V -. outbox .-> Q[(RabbitMQ cluster)]
    N -. outbox .-> Q
    H -. outbox .-> Q
    Q -. listeners .-> T
    A --> DA[(BD usuarios)]
    E --> DE[(BD estudiantes)]
    S --> DS[(BD asignaturas)]
    N --> DN[(BD notas)]
    B -. contratos .-> C[core-share]
```

Cada microservicio es dueño de su propia base de datos y expone su API REST
protegida. El frontend **solo** consume el BFF; el BFF orquesta los dominios por
Feign. La documentación completa de la arquitectura está en
[`docs/arquitectura.md`](docs/arquitectura.md).

## Stack

| Capa | Tecnología |
| --- | --- |
| Frontend | Angular 22.1, TypeScript 6, RxJS 7.8, MSAL Angular v6, Tailwind CSS 4, Nx, Vitest |
| Backend | Java 21, Spring Boot 3.5.0, Spring Cloud 2025.0.0 (OpenFeign), Resilience4j, Spring Security OAuth2, JPA/Hibernate, MapStruct, Lombok, springdoc (Swagger/Scalar) |
| Persistencia | MariaDB 11.4 (una base por microservicio), Flyway |
| Mensajería/contratos | HTTP + Feign y RabbitMQ; DTOs/topologías comunes en `core-share` |
| Infra | Docker Compose (local), Terraform (AWS), GitHub Actions (CI/CD) |

## Estructura del repositorio

```text
SIGA-Project/
├── apps/
│   ├── backend/                  # Maven multi-módulo (ver apps/backend/README.md)
│   │   ├── libs/core-share/      # DTOs, validadores, seguridad y OpenAPI compartidos
│   │   ├── bff-web/              # BFF: /me y orquestación de portales por Feign
│   │   ├── ms-usuarios-auth/     # usuarios, roles y Microsoft Graph
│   │   ├── ms-estudiantes/       # ficha del estudiante y curso
│   │   ├── ms-asignaturas/       # asignaturas, horarios e inscripciones
│   │   ├── ms-notas/             # calificaciones + Feign con fallback
│   │   ├── ms-docentes/          # docentes y certificados
│   │   ├── ms-apoderados/        # apoderados y estudiantes a cargo
│   │   ├── ms-clases/            # cursos y docente jefe
│   │   ├── ms-evaluaciones/      # evaluaciones por asignatura
│   │   ├── ms-asistencias/       # asistencias por estudiante y asignatura
│   │   ├── ms-notificaciones/    # bandeja in-app que consume eventos académicos
│   │   └── ms-rabbitmq-admin/    # API REST de administración de RabbitMQ
│   └── frontend/                 # Angular + Nx (ver apps/frontend/README.md)
├── docs/                         # documentación (ver docs/README.md)
│   └── microservicios/           # fichas técnicas por servicio, BFF y core-share
├── infra/terraform/              # infraestructura AWS (ver infra/terraform/README.md)
├── .github/workflows/            # CI (ci.yml) y CD (cd.yml)
├── docker-compose.yml            # stack local completo
└── .env.example                  # plantilla de variables de entorno
```

## Requisitos

- **Docker** y **Docker Compose** (para el entorno local completo).
- **Node.js 22+** y **npm** (frontend).
- **JDK 21** y **Maven** (backend; hay wrappers `mvnw` por módulo).
- **Terraform >= 1.5** y **AWS CLI** (solo para desplegar en AWS).

## Inicio rápido (Docker Compose)

```bash
# 1. Variables de entorno
cp .env.example .env          # Windows PowerShell: Copy-Item .env.example .env
#    Completar MARIADB_ROOT_PASSWORD, DB_USER, DB_PASS, RABBITMQ_USER/PASS,
#    RABBITMQ_ERLANG_COOKIE (clúster) y las variables AZURE_*

# 2. Levantar todo el stack (bases, microservicios, BFF, RabbitMQ y frontend)
docker compose up -d --build

# 3. Ver estado y logs
docker compose ps
docker compose logs -f bff-web
```

Accesos del entorno local:

| Recurso | URL |
| --- | --- |
| Frontend (SPA) | http://localhost:4200 |
| BFF Web | http://localhost:8080 |
| RabbitMQ (management) | http://localhost:15672 |
| Swagger BFF | http://localhost:8080/docs/swagger |
| Scalar BFF | http://localhost:8080/docs/scalar |
| Swagger usuarios | http://localhost:8081/docs/swagger |
| Swagger admin RabbitMQ | http://localhost:8092/docs/swagger |
| Health BFF | http://localhost:8080/actuator/health |

> Guía paso a paso de login y pruebas: [`docs/testing-login.md`](docs/testing-login.md).

## Servicios y puertos

| Servicio | Puerto host | Descripción |
| --- | --- | --- |
| `frontend` | 4200 | SPA Angular/Nginx; usa BFF por defecto y no cae automáticamente a mocks ante error. |
| `bff-web` | 8080 | Backend For Frontend: `/api/me`, perfil académico, pupilos, cursos y admin. |
| `ms-usuarios-auth` | 8081 | Usuarios, roles y sincronización con Microsoft Graph. |
| `ms-estudiantes` | 8083 | Dominio estudiantes (incluye `id_clase`). |
| `ms-asignaturas` | 8086 | Asignaturas (básicas/electivas), horarios e inscripciones. |
| `ms-notas` | 8089 | Notas (valida estudiante y evaluación por Feign). |
| `ms-docentes` | 8085 | Docentes y certificados. |
| `ms-apoderados` | 8084 | Apoderados, teléfonos y estudiantes a cargo. |
| `ms-clases` | 8087 | Cursos (nivel/letra/año) y docente jefe. |
| `ms-evaluaciones` | 8088 | Evaluaciones por asignatura (tipo y ponderación). |
| `ms-asistencias` | 8090 | Asistencias por estudiante/asignatura (soft delete y unique por fecha). |
| `ms-notificaciones` | 8091 | Bandeja de notificaciones in-app para estudiantes y apoderados; consume eventos académicos. |
| `ms-rabbitmq-admin` | 8092 | API REST para administrar colas, exchanges y bindings de RabbitMQ. |
| `rabbitmq1` / `rabbitmq2` | interno 5672; host 5672/15672 en `rabbitmq1` | Clúster de dos nodos en la misma EC2; Management UI solo se publica desde `rabbitmq1`. |
| `mariadb-*` | interno | Una instancia MariaDB por microservicio (10 bases). |

> Puerto reservado para el servicio futuro `ms-auditoria`: `8082`.

## Comandos habituales

### Docker Compose (desde la raíz)

```bash
docker compose up -d --build            # levantar/actualizar el stack
docker compose up -d --build frontend   # reconstruir un servicio
docker compose logs -f ms-notas         # seguir logs
docker compose down                     # detener (conserva volúmenes)
docker compose down -v                  # detener y borrar datos (¡destructivo!)
```

### Backend (Maven)

```bash
# Compilar y ejecutar tests unitarios de todos los módulos
mvn -f apps/backend/pom.xml -B package -Dtest='!*ApplicationTests' -DfailIfNoTests=false

# Compilar sin tests e instalar en el repo local
mvn -f apps/backend/pom.xml -DskipTests install

# Levantar un servicio concreto (incluye dependencias con -am)
mvn -f apps/backend/pom.xml -pl ms-estudiantes -am spring-boot:run
```

Detalle de módulos, endpoints y configuración en
[`apps/backend/README.md`](apps/backend/README.md) y [`docs/backend.md`](docs/backend.md).

### Frontend (npm / Nx)

```bash
# Desde apps/frontend
npm install
npm start              # ng serve en http://localhost:4200 (proxy /api -> BFF)
npm run build          # build de producción
npm test               # pruebas unitarias (Vitest)

# Desde la raíz
npx nx lint frontend
npx nx graph           # explorar el grafo de proyectos
npx prettier --write "apps/frontend/**/*.{ts,html,css}"
```

Más información en [`apps/frontend/README.md`](apps/frontend/README.md) y
[`docs/frontend.md`](docs/frontend.md).

### Infraestructura (Terraform)

```bash
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars
terraform init && terraform validate && terraform plan && terraform apply
terraform output api_gateway_invoke_url   # URL HTTPS del SPA
```

Detalles de la topología AWS y secretos en
[`infra/terraform/README.md`](infra/terraform/README.md).

## Documentación

| Documento | Contenido |
| --- | --- |
| [`docs/README.md`](docs/README.md) | Índice general de la documentación. |
| [`docs/arquitectura.md`](docs/arquitectura.md) | Visión general, capas, seguridad y despliegue. |
| [`docs/backend.md`](docs/backend.md) | Microservicios, endpoints, contratos y datos. |
| [`docs/microservicios/README.md`](docs/microservicios/README.md) | Dependencias, patrones y acoplamientos por servicio. |
| [`docs/frontend.md`](docs/frontend.md) | Angular, rutas por rol, MSAL y librerías Nx. |
| [`docs/paginacion.md`](docs/paginacion.md) | Contrato de paginación (`Page<T>`) de las búsquedas. |
| [`docs/auditoria-backend.md`](docs/auditoria-backend.md) | Auditoría histórica del backend y trazabilidad de remediaciones. |
| [`docs/testing-login.md`](docs/testing-login.md) | Runbook de login y comprobaciones del sistema con Docker. |
| [`docs/registro-pruebas.md`](docs/registro-pruebas.md) | Bitácora histórica de pruebas reproducibles. |
| [`docs/branch-cleanup.md`](docs/branch-cleanup.md) | Modelo de ramas y limpieza ejecutada. |
| [`apps/backend/README.md`](apps/backend/README.md) | Guía del backend. |
| [`apps/frontend/README.md`](apps/frontend/README.md) | Guía del frontend. |
| [`infra/terraform/README.md`](infra/terraform/README.md) | Guía de infraestructura AWS. |

## Flujo de trabajo (ramas)

```text
feature/*  ->  dev  ->  main  ->  deploy
                                 (dispara CD)
```

- `dev`: integración. `main`: estable. `deploy`: dispara el despliegue (CD).
- Ramas cortas `feature/*` y `fix/*` se eliminan al integrar.
- CI corre en push a `dev`, `main` y `deploy`, y en PR a `dev`/`main`.
- CD se dispara por push a `deploy` o manualmente con `workflow_dispatch`. Un push
  de solo documentación a `deploy` también dispara CD mientras el workflow esté
  activo; consultar `docs/branch-cleanup.md`/README de infraestructura antes de
  sincronizar ramas sin despliegue.

Ver [`docs/branch-cleanup.md`](docs/branch-cleanup.md).
