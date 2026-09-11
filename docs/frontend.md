# Documentacion del frontend

## 1. Proposito

El frontend de SIGA es la interfaz web del sistema de gestion academica. Presenta los flujos de autenticacion, consulta y administracion, y se comunica con el backend a traves del **BFF Web** (nunca directamente con los microservicios).

Actualmente es una aplicacion Angular modular, con autenticacion MSAL y rutas por rol. Las pantallas de negocio reales estan pendientes.

## 2. Ubicacion y tecnologias

- Ubicacion: `apps/frontend`
- Angular 22.1 · TypeScript 6 · RxJS 7.8 · Angular Router
- MSAL Angular v6 + `@azure/msal-browser` (Azure AD)
- Nx (librerias y reglas de frontera)
- Tailwind CSS (framework de estilos; integracion pendiente)
- Vitest para pruebas · Prettier

El diseno de la interfaz se basara en Tailwind CSS; su integracion se realizara en una tarea posterior.

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
│   ├── core/                   # auth (MSAL), guards, interceptores, http, config, modelos
│   ├── shared-ui/              # componentes de UI y layout reutilizables
│   ├── public-portal/          # landing publico de bienvenida
│   ├── academico/              # componentes academicos reutilizables
│   ├── estudiante/             # portal estudiante
│   ├── apoderado/              # portal apoderado
│   ├── docente/                # portal docente
│   └── admin/                  # portal administracion (base)
├── src/
│   ├── app/
│   │   ├── app.ts              # componente raiz (router-outlet)
│   │   ├── app.config.ts       # providers (router, http, MSAL)
│   │   └── app.routes.ts       # rutas por rol con lazy loading
│   ├── main.ts                 # carga config runtime y hace bootstrap
│   ├── index.html
│   └── types/bff-models.d.ts   # tipos generados desde el BFF
├── public/config.json          # configuracion runtime (BFF + MSAL)
├── proxy.conf.json             # proxy /api -> BFF en desarrollo
├── nginx.conf                  # servidor SPA + proxy /api en contenedor
├── Dockerfile
├── angular.json
├── project.json
├── tsconfig.json               # alias @siga/*
└── tsconfig.base.json (raiz)   # paths para el grafo de Nx
```

## 4. Autenticacion y sesion

- **MSAL Angular v6 + Azure AD**. La configuracion (clientId, authority, redirectUri y scopes) se carga en **runtime** desde `public/config.json` antes del bootstrap (`main.ts`).
- `MSAL_INSTANCE`, `MSAL_GUARD_CONFIG` y `MSAL_INTERCEPTOR_CONFIG` se registran en `app.config.ts`.
- `MsalInterceptor` adjunta el token a las llamadas al BFF; `errorInterceptor` centraliza el manejo de errores (p. ej. 401).
- El retorno del login se procesa en la ruta `/auth` (`MsalRedirectComponent`).

## 5. Rutas por rol

| Ruta | Acceso | Descripcion |
| --- | --- | --- |
| `/` | publico | Portal de bienvenida (boton "Ingresar al portal") |
| `/auth` | publico | Retorno de Azure AD (MSAL) |
| `/estudiante` | `ESTUDIANTE` | Informacion academica propia |
| `/apoderado` | `APODERADO` | Seleccion de pupilo y gestiones |
| `/docente` | `DOCENTE` | Panel docente |
| `/admin` | `ADMIN` | Administracion (base) |

- Cada portal se carga con **lazy loading**.
- `MsalGuard` valida la autenticacion y `roleGuard([...])` valida el rol (a partir del rol autoritativo del BFF via `MeService`).

## 6. Librerias Nx

- **`core`**: modelos (`Rol`, `Me`), configuracion (`AppConfig`, `APP_CONFIG`, `loadAppConfig`), autenticacion (`msal.factory`, `AuthService`, `MeService`) y guards (`roleGuard`), e interceptores HTTP.
- **`shared-ui`**: layout compartido (`PortalShell`).
- **`public-portal`**: landing publico con accesos por rol.
- **`academico`**: componentes academicos reutilizables (`ResumenAcademico`), usados por estudiante y apoderado con distinto contexto.
- **`estudiante` / `apoderado` / `docente` / `admin`**: contenedores de cada portal.

### Fronteras

`eslint.config.js` aplica `@nx/enforce-module-boundaries` con `tags` (`scope:core`, `scope:shared`, `scope:academico`, `scope:estudiante`, etc.) y `depConstraints`: los portales pueden depender de `core`, `shared` y `academico`, pero no entre si.

## 7. Contratos con el backend

- El frontend se comunica **solo con el BFF** (`bffBaseUrl` en `config.json`, por defecto `/api`).
- `MeService` consume `GET /api/me` para obtener rol y vinculos (p. ej. estudiantes del apoderado). **Pendiente de implementar en el BFF.**
- Los tipos TypeScript se generan desde los DTOs del BFF con `typescript-generator` (`src/types/bff-models.d.ts`).

## 8. Docker

- Build en dos etapas (`node:24-alpine` → `nginx:alpine`).
- Nginx sirve la SPA con fallback a `index.html` y proxya `/api` hacia `http://bff-web:8080`, de modo que el navegador nunca accede a los microservicios ni hay CORS.
- El contenedor se publica en `http://localhost:4200`.

## 9. Estado y trabajo pendiente

| Area | Situacion |
| --- | --- |
| Bootstrap Angular / Nx | Disponible |
| Autenticacion MSAL + Azure AD | Disponible |
| Rutas por rol y guards | Disponible |
| Portal publico y layout | Disponible (base) |
| Fronteras Nx | Disponible |
| Pantallas de negocio (CRUD) | Pendiente |
| Integracion con BFF (`/me` y recursos) | Pendiente |
| Formularios y validaciones | Pendiente |
| Pruebas funcionales | Pendiente |

El siguiente paso es completar el BFF (`/me` y orquestacion) y luego implementar las pantallas de cada portal sobre contratos estables.
