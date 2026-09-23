# Pruebas de inicio de sesion y sistema (Docker)

Guia paso a paso para validar el flujo de autenticacion con Azure AD (Entra ID) y el
resto de lo implementado, levantando el sistema con Docker Compose.

## 1. Objetivo y alcance

**Se prueba:**

- Login con Azure AD desde el portal publico (`/`).
- `GET /api/me` en el BFF y resolucion del rol.
- Redireccion por rol (`/admin`, `/estudiante`, `/docente`, `/apoderado`) y navegacion por el dashboard.
- Pantalla de rechazo `/sin-acceso`, pantalla de error `/error-acceso` y cierre de sesion de Microsoft.
- CRUD de usuarios en `ms-usuarios-auth` (via Swagger/API).
- Integracion con Microsoft Graph: lookup de `oid`, alta por correo, cambio de rol y
  `sync-roles` (Fase 2, requiere secreto de Azure).

**Pantallas de negocio (datos mock):** los portales (estudiante, apoderado, docente, admin)
ya tienen dashboards con opciones (Inicio, Horarios, Notas, Asistencias, Progreso Academico,
Cursos, Usuarios, etc.), pero usan **datos de ejemplo** en el frontend; la conexion real al
BFF esta pendiente para los recursos sin endpoint.

## 2. Checklist para Azure (responsable: equipo TI / companera)

Antes de probar el login necesitas que exista la app en Entra ID. Valores/config a
solicitar:

| Requerimiento | Descripcion |
| --- | --- |
| Tenant ID | GUID del tenant del colegio. |
| Client ID (SPA) | App registration usada por el frontend (`config.json`). |
| Client ID (API) | App que expone la API y define los app roles. Puede ser la misma que la SPA o una separada (recomendado: separadas). |
| Scope expuesto | Por defecto `Acceso.Base` (el que usa `config.json`). |
| App roles | `ADMIN`, `DOCENTE`, `APODERADO`, `ESTUDIANTE`. |
| Redirect URI `/auth` | `http://localhost:4200/auth` (plataforma SPA). |
| Redirect URI logout `/sin-acceso` | `http://localhost:4200/sin-acceso` (plataforma SPA). |
| Fase 2 - Secreto | Client secret de la app de API. |
| Fase 2 - Permisos Graph | `User.Read.All` y `AppRoleAssignment.ReadWrite.All` (permisos de aplicacion con consentimiento de admin). |

> **Decision importante:** hoy `config.json` y `.env` asumen que la **misma** app se usa
> como SPA y como API (`448f165b-...`). Si TI configura apps separadas, hay que actualizar
> `clientId`/`scopes` en `apps/frontend/public/config.json` y `AZURE_CLIENT_ID`/`AZURE_APP_ID_URI`
> en `.env`.

## 3. Configuracion local

### `.env` (raiz del proyecto)

| Variable | Uso |
| --- | --- |
| `MARIADB_ROOT_PASSWORD` | Password root de MariaDB (solo dev). |
| `DB_USER`, `DB_PASS` | Usuario/password de las bases por microservicio. |
| `AZURE_TENANT_ID` | Tenant del colegio. |
| `AZURE_CLIENT_ID` | App de API (audiencia esperada del token). |
| `AZURE_APP_ID_URI` | `api://<client-id>` de la app de API. |
| `AZURE_CLIENT_SECRET` | (Fase 2) Secreto para llamar a Graph. Si esta vacio, Graph queda deshabilitado. |
| `AZURE_API_APP_ID` | (Fase 2) App id que define los app roles. Si se omite, usa `AZURE_CLIENT_ID`. |

### `apps/frontend/public/config.json`

- `clientId`, `authority`, `redirectUri` (`/auth`), `postLogoutRedirectUri` (`/sin-acceso`)
  y `scopes`.

## 4. Levantar con Docker

```bash
docker compose up -d --build
docker compose ps
```

Servicios esperados:

| Servicio | Puerto host | Descripcion |
| --- | --- | --- |
| `frontend` | 4200 | SPA Angular via Nginx (solo sirve el SPA; el API va por `config.json`). |
| `bff-web` | 8080 | Backend For Frontend (`/api/me` y perfil de estudiante). |
| `ms-usuarios-auth` | 8081 | Usuarios (`/api/v1/usuarios`), lookup y sync con Graph. |
| `ms-estudiantes` | 8082 | Dominio estudiantes. |
| `ms-asignaturas` | 8086 | Dominio asignaturas. |
| `ms-notas` | 8087 | Dominio notas. |
| `mariadb-*` | interno | Una base por microservicio. |

Logs utiles:

```bash
docker compose logs -f ms-usuarios-auth
docker compose logs -f bff-web
```

## 5. Verificar infraestructura (sin Azure)

```bash
# Flyway aplico la migracion
docker compose logs ms-usuarios-auth | findstr /I "flyway Successfully"

# La tabla usuarios existe
docker exec -it mariadb-usuarios mariadb -u<DB_USER> -p<DB_PASS> siga_usuarios_db \
  -e "SHOW TABLES; SELECT * FROM flyway_schema_history;"

# Health y documentacion (endpoints publicos)
curl http://localhost:8080/actuator/health
curl http://localhost:8080/docs/swagger
curl http://localhost:8081/docs/swagger

# Seguridad activa: /api/me sin token debe responder 401
curl -i http://localhost:8080/api/me
```

## 6. Bootstrap del primer admin

Sin un usuario `ADMIN` registrado no se puede crear ningun usuario via API.

1. Obtener el `oid` (Object ID) de tu cuenta:
   - Azure Portal > Microsoft Entra ID > Users > seleccionar usuario > Object ID, o
   - decodificar el JWT tras el primer login.
2. Insertar la fila (reemplazando `<OID>` y `<email>`):

```bash
docker exec -it mariadb-usuarios mariadb -u<DB_USER> -p<DB_PASS> siga_usuarios_db \
  -e "INSERT INTO usuarios (id,email,rol,state) VALUES ('<OID>','<email>','ADMIN','ACTIVO');"
```

3. En Azure: **Enterprise applications > <tu app> > Users and groups > Add** y asignar el
   App role `ADMIN` a tu usuario.
   - Sin este paso, el login funciona y te lleva a `/admin`, pero las APIs con
     `hasRole('ADMIN')` responden 403.
   - Alternativa: `POST /api/v1/usuarios/{oid}/sync-roles` (requiere Graph configurado).

## 7. Casos de prueba de login

Abrir `http://localhost:4200` y hacer clic en **Ingresar al portal**.

| Caso | Cuenta | Resultado esperado |
| --- | --- | --- |
| A | Valida en Microsoft, pero **no** esta en la tabla `usuarios`. | Redirige a `/sin-acceso`, muestra "Cuenta no habilitada" y **cierra la sesion** de Microsoft. |
| B | Registrada como `ADMIN` y con App role `ADMIN` asignado. | Vuelve de `/auth`, `GET /me` resuelve el rol y redirige a `/admin`. |
| C | Registrada en la tabla pero **sin** App role asignado. | Entra a su portal (rol desde la BD), pero las APIs admin responden 403. |
| D | Cuenta que no existe en Entra ID. | Microsoft rechaza el login en su propia pantalla; SIGA no interviene. |

Verificaciones de DevTools (F12):

- `Application > Local Storage`: tras el Caso A, el cache de `msal` debe quedar limpio.
- `Network`: la request a `/api/me` lleva `Authorization: Bearer ...` y devuelve 404 (Caso A)
  o 200 con `roles` (Caso B/C).

## 8. Pruebas de API

Swagger: `http://localhost:8080/docs/swagger` (BFF) y `http://localhost:8081/docs/swagger`.

Obtener token: DevTools > Network > request a `/api/me` > header `Authorization`, o
`Application > Local Storage` (claves `msal`).

```bash
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/me
```

Respuesta esperada:

```json
{ "id": "<oid>", "email": "<email>", "displayName": "<nombre>", "roles": ["ADMIN"] }
```

CRUD como admin (Swagger de `ms-usuarios-auth`):

- `GET /api/v1/usuarios/{id}` y `GET /api/v1/usuarios/search`.
- `POST /api/v1/usuarios` (con `id` o solo `email` si Graph esta activo).
- `PUT /api/v1/usuarios/{id}` (permite cambiar `rol`).
- `DELETE /api/v1/usuarios/{id}` (soft delete: pasa a `INACTIVO`).
- `POST /api/v1/usuarios/{id}/sync-roles`.

## 9. Fase 2 - Microsoft Graph

1. Agregar `AZURE_CLIENT_SECRET` y `AZURE_API_APP_ID` a `.env`.
2. Recrear el servicio:
   ```bash
   docker compose up -d ms-usuarios-auth
   ```
3. Pruebas:
   - `GET /api/v1/usuarios/lookup?email=<correo>` -> devuelve `{ oid, email, displayName }`.
   - `POST /api/v1/usuarios` con `{ "email": "...", "rol": "DOCENTE" }` -> resuelve el `oid`
     y lo registra.
   - Cambiar rol con `PUT` -> en Azure se reasigna el App role.
   - `DELETE` -> se revocan los App roles del usuario.
4. Si Graph no esta configurado, estos endpoints responden error de negocio/503 y el CRUD
   basico sigue funcionando.

## 10. Troubleshooting

| Sintoma | Causa probable | Solucion |
| --- | --- | --- |
| `AADSTS50011` (redirect mismatch) | Falta registrar `/auth` o `/sin-acceso`. | Agregar ambos redirect URIs a la app SPA. |
| 401 en `/api/me` | Token con audiencia incorrecta. | Revisar `AZURE_APP_ID_URI` / `AZURE_CLIENT_ID`. |
| 403 en endpoints admin | El token no trae `roles: ["ADMIN"]`. | Asignar el App role al usuario (o `sync-roles`). |
| `/api/me` 404 | Usuario no registrado en `usuarios`. | Pre-registrar por admin (seccion 6). |
| 503 / error de negocio en lookup/sync | Graph deshabilitado o sin permisos. | Configurar `AZURE_CLIENT_SECRET` + permisos Graph. |
| 403 en perfil de estudiante | Los controllers piden `SCOPE_estudiantes:read`, etc., y el token solo trae `Acceso.Base`. | Exponer y solicitar esos scopes (pendiente conocido). |
| `/error-acceso` tras iniciar sesion | Fallo al adquirir el token (scope no expuesto/consentido, redirect como Web en vez de SPA, o app role sin asignar). | Revisar el detalle que muestra la pantalla (AADSTS); corregir en Entra ID y limpiar el cache MSAL (`msal.*` en Local/Session Storage). |
| Fechas `dd/MM/yyyy` rechazadas por la API | La API usa **ISO 8601** (`yyyy-MM-dd`) para `LocalDate` (JSON y parametros). | Enviar/esperar `yyyy-MM-dd` (p. ej. `birthDate`, `from`/`to`); el frontend normaliza a `dd/MM/yyyy` solo para mostrar. |
| Frontend no llama al BFF | `bffBaseUrl` mal configurado o API inaccesible. | En AWS, usar el API Gateway; en `ng serve`, `proxy.conf.json`; en Docker local, el proxy `/api` de Nginx. |

## 11. Que se puede validar sin Azure

Mientras no haya acceso al tenant se puede comprobar:

- Build de backend (`mvn -f apps/backend/pom.xml -DskipTests install`) y frontend (`ng build`).
- `docker compose up -d --build` y que todos los servicios queden arriba.
- Migracion Flyway y existencia de las tablas (`usuarios`, `estudiantes`, etc.).
- Endpoints publicos (`/actuator/health`, `/docs/swagger`) y que `/api/me` sin token da 401.
- UI de `http://localhost:4200`: portal publico, `/sin-acceso`, `/error-acceso` y la navegacion del dashboard (con datos mock) en cada rol.

**Requiere Azure:** login real, `/api/me` con rol, redireccion por rol y CRUD autenticado.

## 12. Checklist de resultados

- [ ] `docker compose up -d --build` sin errores.
- [ ] MariaDB healthy y servicios arriba.
- [ ] Flyway aplico `V1__crear_tabla_usuarios`.
- [ ] `/actuator/health` y `/docs/swagger` responden.
- [ ] `/api/me` sin token responde 401.
- [ ] Caso A: `sin-acceso` + logout.
- [ ] Caso B: redireccion a `/admin`.
- [ ] Caso C: portal si, API admin 403.
- [ ] `/api/me` con token devuelve el rol.
- [ ] CRUD de usuarios (crear/listar/editar/baja).
- [ ] Fase 2: lookup, alta por correo y `sync-roles`.
