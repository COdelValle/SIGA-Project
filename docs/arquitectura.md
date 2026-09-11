# Arquitectura de SIGA

## 1. Vision general

SIGA es un sistema de gestion academica con frontend Angular, un BFF Web y microservicios Spring Boot separados por responsabilidad. El nucleo academico es funcional; la capa de orquestacion (BFF) y la infraestructura estan en construccion incremental.

```mermaid
flowchart LR
    U[Usuario] --> F[Frontend Angular + Nginx]
    F -->|/api| B[BFF Web (esqueleto)]
    B --> A[MS Usuarios y autenticacion]
    B --> E[MS Estudiantes]
    B --> S[MS Asignaturas]
    B --> N[MS Notas]
    N -. Feign: exists .-> E
    N -. Feign: exists .-> S
    A --> DA[(BD usuarios)]
    E --> DE[(BD estudiantes)]
    S --> DS[(BD asignaturas)]
    N --> DN[(BD notas)]
    B -. contratos .-> C[core-share]
```

Cada microservicio es dueno de su propia base de datos (database-per-service). El BFF aun no orquesta: los microservicios ya exponen sus APIs y el frontend esta preparado para consumirlas a traves del BFF.

## 2. Capas

### Presentacion

`apps/frontend` contiene la aplicacion Angular (librerias Nx, rutas por rol, autenticacion MSAL, servicios HTTP). En contenedor se sirve con Nginx, que proxya `/api` al BFF. El navegador no conoce la topologia interna. Los estilos del frontend se gestionan con Tailwind CSS 4.

### Entrada y orquestacion

`bff-web` sera el punto de entrada para la interfaz: aplicara seguridad, coordinara solicitudes y combinara informacion de varios servicios. Actualmente es un esqueleto (app + seguridad compartida); la orquestacion y `/me` estan pendientes.

### Dominio distribuido

Cada microservicio es dueno de un contexto funcional:

- Usuarios y autenticacion: identidad, roles y estado de cuentas.
- Estudiantes: datos personales y academicos del estudiante.
- Asignaturas: asignaturas.
- Notas: calificaciones y sus relaciones.

### Compartidos transversales

`core-share` ofrece DTOs, validaciones, excepciones, seguridad y configuracion OpenAPI reutilizables. No contiene logica de un dominio especifico.

### Persistencia

**Database-per-service implementado**: cada servicio tiene su propia instancia MariaDB y su propio volumen en Docker. El acoplamiento entre dominios se reduce al no compartir tablas.

## 3. Flujo de una solicitud

1. El usuario inicia una accion en Angular.
2. El frontend envia la solicitud a `/api` (Nginx la proxya al BFF) con el token JWT.
3. El BFF valida la autenticacion y la autorizacion.
4. El BFF llama al microservicio responsable (Feign).
5. El microservicio valida la entrada, ejecuta la regla de negocio y persiste.
6. La respuesta se transforma a un DTO y vuelve al BFF.
7. El frontend muestra el resultado o un error normalizado.

Nota: los pasos 3 y 4 (orquestacion del BFF) estan pendientes; hoy los microservicios ya responden a sus propias APIs.

## 4. Seguridad

La estrategia es OAuth2/JWT con **Azure AD** como proveedor de identidad.

- Autenticacion: confirma quien es el usuario.
- Autorizacion: roles (`hasRole`) y scopes (`hasAuthority('SCOPE_...')`).
- Rutas publicas limitadas a salud y documentacion tecnicas.
- **Propagacion del token**: cuando un microservicio llama a otro por Feign, reenvia el `Authorization` entrante (`RequestInterceptor`), de modo que la autorizacion se evalue en destino.
- El frontend solo se comunica con el BFF.

Aspectos a completar: contrato final de roles/permisos y validacion de audiencia/emisor en todos los flujos.

## 5. Comunicacion y contratos

- Comunicacion interna HTTP; **Feign** es el cliente declarativo.
- Implementado en `ms-notas`, que valida la existencia de estudiante y asignatura (`exists`) con fallback **Resilience4j**.
- El BFF usara Feign para orquestar el resto (pendiente).
- Los DTOs compartidos viven en `core-share` y no deben contener logica de dominio.

## 6. Despliegue

- **Dockerfiles** por servicio (multi-stage).
- `docker-compose.yml` levanta: 4 MariaDB, los 4 microservicios, `bff-web` y `frontend` sobre la red `siga-network`.
- Configuracion centralizada por `.env` (credenciales MariaDB y Azure AD).
- `frontend` se sirve con Nginx y proxya `/api` al BFF.
- **Terraform** (`terraform/main.tf`) sigue pendiente.

## 7. Observabilidad y operacion

- Health checks: `/actuator/health` en cada servicio.
- Documentacion OpenAPI por servicio (Swagger UI y Scalar).
- Pendiente: logs estructurados, correlation ID, metricas y tracing.

## 8. Estado actual frente a la arquitectura objetivo

| Componente | Estado actual | Objetivo |
| --- | --- | --- |
| Frontend Angular | App modular con MSAL y rutas por rol | Pantallas academicas conectadas al BFF |
| BFF Web | Esqueleto (app + seguridad) | Orquestacion y API para la interfaz (`/me`) |
| Usuarios/Auth | CRUD funcional + soft delete | Identidad y permisos completos |
| Estudiantes | CRUD, busqueda, `exists`, soft delete | Matricula y relaciones academicas |
| Asignaturas | CRUD, listado, busqueda, `exists`, soft delete | Relacion con docentes y cursos |
| Notas | CRUD, busqueda, Feign, soft delete | Reglas de periodo y calculo |
| core-share | DTOs, validadores, seguridad, errores, OpenAPI | Contratos versionados estables |
| Docker Compose | Completo (database-per-service) | Entorno local reproducible |
| Terraform | Archivo vacio | Infraestructura declarativa |
| Pruebas | Base de proyecto | Cobertura unitaria, integracion y contratos |

## 9. Principios de implementacion

- Mantener una responsabilidad clara por microservicio.
- Evitar que el frontend dependa de servicios internos.
- Compartir contratos y componentes transversales, no entidades de dominio.
- Validar entradas en el limite de cada servicio.
- Proteger todos los flujos que modifiquen informacion.
- Automatizar compilacion, pruebas y despliegue progresivamente.
