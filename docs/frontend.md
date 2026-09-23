# Documentacion del frontend

## 1. Proposito

El frontend de SIGA es la interfaz web del sistema de gestion academica. Presenta los flujos de autenticacion, consulta y administracion, y se comunica con el backend a traves del **BFF Web** (nunca directamente con los microservicios).

Actualmente es una aplicacion Angular modular con autenticacion MSAL, **dashboards por rol** (layout con header y sidebar) y **tema oscuro/claro**. Las pantallas academicas ya estan implementadas con **datos mock**; la conexion real al BFF esta pendiente para los recursos que aun no tienen endpoint.

## 2. Ubicacion y tecnologias

- Ubicacion: `apps/frontend`
- Angular 22.1 · TypeScript 6 · RxJS 7.8 · Angular Router
- MSAL Angular v6 + `@azure/msal-browser` (Azure AD)
- Nx (librerias y reglas de frontera)
- Tailwind CSS 4 (framework de estilos) + `prettier-plugin-tailwindcss`
- Vitest para pruebas · Prettier

Estilos: Tailwind CSS 4 se integra via PostCSS (`.postcssrc.json` con el plugin `@tailwindcss/postcss`) y se importa con `@import 'tailwindcss';` en `src/styles.css` (que ademas declara `@source "../libs"` para escanear las librerias Nx). Las clases se ordenan automaticamente con `prettier-plugin-tailwindcss` (configurado en `.prettierrc` con `tailwindStylesheet` apuntando a `src/styles.css`); conviene ejecutar `npx prettier --write` al cerrar cambios de UI.

Comandos (desde `apps/frontend`):

```bash
npm install
npm start        # servidor de desarrollo (http://localhost:4200)
npm run build
npm test
```

Linting (desde la raiz, con Nx):

```bash
npx nx lint frontend
```

## 3. Estructura actual

```text
apps/frontend/
├── libs/                       # librerias Nx (cada una con project.json y tags)
│   ├── core/                   # auth (MSAL), guards, interceptores, tema, http, config, modelos
│   ├── shared-ui/              # layout (DashboardShell, PortalHeader, menu) y UI reutilizable
│   ├── public-portal/          # landing publico de bienvenida
│   ├── academico/              # componentes academicos (horario, asistencia, notas, periodo) y mocks
│   ├── estudiante/             # portal estudiante (inicio, horarios, notas, asistencias, progreso)
│   ├── apoderado/              # portal apoderado (multipupilo)
│   ├── docente/                # portal docente (inicio, cursos, horarios, registrar-notas/asistencias)
│   └── admin/                  # portal administracion (inicio, usuarios, roles, asignaturas)
├── src/
│   ├── app/
│   │   ├── app.ts              # componente raiz (router-outlet)
│   │   ├── app.config.ts       # providers (router, http con authInterceptor, MSAL)
│   │   ├── app.routes.ts       # rutas por rol con lazy loading
│   │   └── auth-error/         # pantalla /error-acceso (fallo de sesion/token)
│   ├── main.ts                 # carga config runtime y hace bootstrap
│   ├── index.html              # <html data-theme="dark"> (tema oscuro por defecto)
│   └── types/bff-models.d.ts   # tipos generados desde el BFF
├── public/config.json          # configuracion runtime (BFF + MSAL)
├── proxy.conf.json             # proxy /api -> BFF en desarrollo
├── nginx.conf                  # servidor SPA + proxy /api (local)
├── Dockerfile
├── angular.json
├── project.json
├── tsconfig.json               # alias @siga/*
└── tsconfig.base.json (raiz)   # paths para el grafo de Nx
```

## 4. Autenticacion y sesion

- **MSAL Angular v6 + Azure AD**. La configuracion (clientId, authority, redirectUri y scopes) se carga en **runtime** desde `public/config.json` antes del bootstrap (`main.ts`).
- `MSAL_INSTANCE` y `MSAL_GUARD_CONFIG` se registran en `app.config.ts`.
- **`authInterceptor` (propio)** adjunta el token de acceso a las llamadas al BFF (`acquireTokenSilent`). Ante un fallo de token, registra el error en `AuthErrorService` y lo **re-lanza sin `loginRedirect`**, evitando el bucle de login. Ya **no** se usa `MsalInterceptor`.
- `errorInterceptor` complementa el manejo de errores HTTP.
- El retorno del login se procesa en la ruta `/auth` (`AuthRedirectComponent`); el error de sesion se muestra en `/error-acceso` (`AuthErrorComponent`).
- **Temas**: `ThemeService` (oscuro por defecto magenta/dorado, claro institucional) aplica `data-theme` en `<html>` y persiste la preferencia en `localStorage`.

## 5. Rutas por rol

| Ruta | Acceso | Descripcion |
| --- | --- | --- |
| `/` | publico | Portal de bienvenida (boton "Ingresar al portal") |
| `/auth` | publico | Retorno de Azure AD (MSAL) |
| `/sin-acceso` | publico | Cuenta autenticada pero no habilitada (cierra sesion) |
| `/error-acceso` | publico | Error de sesion/token (muestra el detalle de Azure) |
| `/estudiante` | `ESTUDIANTE` | Dashboard del estudiante |
| `/apoderado` | `APODERADO` | Dashboard del apoderado (multipupilo) |
| `/docente` | `DOCENTE` | Dashboard del docente |
| `/admin` | `ADMIN` | Dashboard de administracion |

Cada portal se carga con **lazy loading** y define **rutas hijas** bajo un layout `DashboardShell`
(header fijo + sidebar con menu por rol):

| Portal | Rutas hijas |
| --- | --- |
| Estudiante | `inicio`, `horarios`, `notas`, `asistencias`, `asistencias/:id`, `progreso` |
| Apoderado | `inicio`, `pupilos`, `horarios`, `notas`, `asistencias`, `asistencias/:id`, `progreso`, `solicitudes` |
| Docente | `inicio`, `cursos`, `horarios`, `registrar-notas`, `registrar-asistencias` |
| Admin | `inicio`, `usuarios`, `roles`, `asignaturas` |

- `MsalGuard` valida la autenticacion y `roleGuard([...])` valida el rol (a partir del rol autoritativo del BFF via `MeService`).

## 6. Librerias Nx

- **`core`**: modelos (`Rol`, `Me`), configuracion (`AppConfig`, `APP_CONFIG`, `loadAppConfig`), autenticacion (`msal.factory`, `AuthService`, `MeService`, `AuthErrorService`), tema (`ThemeService`), guards (`roleGuard`) e interceptores HTTP (`authInterceptor`, `errorInterceptor`).
- **`shared-ui`**: layout (`DashboardShell`, `PortalHeader`, `PortalShell`, menu con iconos SVG) y UI reutilizable (`SeccionCard`, `DayTabs`, `Paginador`).
- **`public-portal`**: landing publico con accesos por rol.
- **`academico`**: componentes academicos reutilizables (horario, asistencia, notas, periodo) y datos mock.
- **`estudiante` / `apoderado` / `docente` / `admin`**: contenedores de cada portal con sus rutas hijas y paginas.

### Fronteras

`eslint.config.js` aplica `@nx/enforce-module-boundaries` con `tags` (`scope:core`, `scope:shared`, `scope:academico`, `scope:estudiante`, etc.) y `depConstraints`: los portales pueden depender de `core`, `shared` y `academico`, pero no entre si.

## 7. Contratos con el backend

- El frontend se comunica **solo con el BFF** (`bffBaseUrl` en `config.json`, por defecto `/api`).
- `MeService` consume `GET /api/me` para obtener rol y vinculos (p. ej. estudiantes del apoderado). **El BFF ya implementa `/me`**; el resto de recursos se conectara de forma incremental.
- Los tipos TypeScript se generan desde los DTOs del BFF con `typescript-generator` (`src/types/bff-models.d.ts`).

## 8. Docker

- Build en dos etapas (`node:24-alpine` → `nginx:alpine`).
- Nginx sirve la SPA con fallback a `index.html` y los assets, y **proxya `/api` al BFF** (`http://bff-web:8080`) para el entorno local (Docker), donde `bffBaseUrl` es relativo (`/api`). En **AWS** esa ruta no se usa: el API Gateway intercepta `/api/{proxy+}` y lo envia directo al BFF (alli `config.json` se monta con la URL absoluta del API Gateway).
- El contenedor se publica en `http://localhost:4200`.

## 9. Estado y trabajo pendiente

| Area | Situacion |
| --- | --- |
| Bootstrap Angular / Nx | Disponible |
| Autenticacion MSAL + Azure AD | Disponible |
| Rutas por rol, guards y rutas hijas | Disponible |
| Resolucion del rol via BFF (`GET /api/me`) | Disponible |
| Portal publico, `/sin-acceso` y `/error-acceso` | Disponible |
| Tema oscuro/claro (oscuro por defecto) | Disponible |
| Dashboards y pantallas por rol | Disponible (con **datos mock**) |
| Fronteras Nx | Disponible |
| Conexion real al BFF del resto de recursos | Pendiente |
| Formularios y validaciones | Pendiente |
| Pruebas funcionales | Pendiente |

Las pantallas academicas (horarios, notas, asistencias, progreso, cursos, usuarios) usan
**datos mock** en `libs/*/src/lib/mocks`; el siguiente paso es conectarlas a los contratos
del BFF a medida que se expongan los endpoints.
