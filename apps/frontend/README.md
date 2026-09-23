# Frontend SIGA

Aplicacion Angular del sistema de gestion academica SIGA. Es una aplicacion modular que se comunica **solo con el BFF** (a traves de `/api`) y organiza su codigo en librerias Nx con fronteras entre portales por rol.

Detalle general del proyecto en el [README raiz](../../README.md); guia del backend en
[apps/backend/README.md](../backend/README.md); indice completo en [docs/README.md](../../docs/README.md)
y detalle del frontend en [docs/frontend.md](../../docs/frontend.md).

## Tecnologias

- Angular 22.1 · TypeScript 6 · RxJS 7.8 · Angular Router
- MSAL Angular v6 + `@azure/msal-browser` (Azure AD)
- Nx (librerias y reglas de frontera)
- Tailwind CSS 4 (framework de estilos) + `prettier-plugin-tailwindcss`
- Vitest (pruebas) · Prettier

## Estructura

```text
apps/frontend/
├── libs/
│   ├── core/            # auth (MSAL), guards, interceptores, tema (ThemeService), config, modelos
│   ├── shared-ui/       # layout (DashboardShell, PortalHeader, menu) y UI reutilizable (SeccionCard, DayTabs, Paginador)
│   ├── public-portal/   # landing publico
│   ├── academico/       # componentes academicos (horario, asistencia, notas, periodo) y mocks
│   ├── estudiante/      # portal estudiante (inicio, horarios, notas, asistencias, progreso)
│   ├── apoderado/       # portal apoderado (multipupilo: pupilos, horarios, notas, asistencias, progreso, solicitudes)
│   ├── docente/         # portal docente (inicio, cursos, horarios, registrar-notas, registrar-asistencias)
│   └── admin/           # portal administracion (inicio, usuarios, roles, asignaturas)
├── src/
│   ├── app/             # app.ts, app.config.ts, app.routes.ts (rutas por rol) y auth-error
│   ├── main.ts          # carga config runtime y hace bootstrap
│   └── types/           # tipos generados desde el BFF
├── public/config.json   # configuracion runtime (BFF + MSAL)
├── proxy.conf.json      # proxy /api -> BFF en desarrollo
├── nginx.conf           # servidor SPA + proxy /api en contenedor (local)
├── Dockerfile
├── angular.json
└── project.json
```

## Comandos

Desde `apps/frontend`:

```bash
npm install
npm start                        # servidor de desarrollo en http://localhost:4200
npm run build                    # build de produccion en dist/frontend/browser
npm test                         # pruebas unitarias (Vitest)
npm test -- --watch=false        # ejecucion unica (como en CI)
npx tsc -p tsconfig.app.json --noEmit   # typecheck (como en CI)
```

Linting y formateo con Nx / Prettier (desde la raiz):

```bash
npx nx lint frontend
npx prettier --write "apps/frontend/**/*.{ts,html,css}"
```

`npm start` usa `proxy.conf.json` para redirigir `/api` hacia el BFF en
`http://localhost:8080`; en Docker el SPA llama al API Gateway via `config.json`.

## Estilos (Tailwind CSS)

- **Tailwind CSS 4** integrado via PostCSS (`.postcssrc.json` con el plugin `@tailwindcss/postcss`).
- Import en `src/styles.css`: `@import 'tailwindcss';` y `@source "../libs"` para incluir las librerias Nx.
- Los **temas** (oscuro por defecto magenta/dorado y claro institucional) se definen como tokens CSS (`--siga-*`) expuestos a Tailwind con `@theme inline`; el estado lo maneja `ThemeService` (`data-theme` en `<html>` + persistencia en `localStorage`).
- Las clases se **ordenan automaticamente** con `prettier-plugin-tailwindcss` (configurado en `.prettierrc`).
- Formatear estilos/clases:
  ```bash
  npx prettier --write "src/**/*.{ts,html,css}" "libs/**/*.ts"
  ```
- Los componentes usan utilidades de Tailwind; se evitan estilos inline.

## Configuracion runtime

La configuracion se carga antes del bootstrap desde `public/config.json` (servido en `/config.json`):

```jsonc
{
  "bffBaseUrl": "/api",
  "msal": {
    "clientId": "<AZURE_FRONTEND_CLIENT_ID>",
    "authority": "https://login.microsoftonline.com/<AZURE_TENANT_ID>",
    "redirectUri": "/auth",
    "scopes": ["api://<AZURE_BFF_APP_ID>/access_as_user"]
  }
}
```

Gracias a esto, la misma imagen se puede desplegar en distintos entornos sin recompilar.

## Autenticacion y rutas

Autenticacion con **MSAL Angular v6 + Azure AD**. El retorno del login se procesa en `/auth`
(`AuthRedirectComponent`). Un **`authInterceptor` propio** adjunta el token de acceso a las
llamadas al BFF y, ante un fallo de token, registra el error y lo re-lanza **sin** disparar
`loginRedirect` (evita el loop de login); `AuthErrorService` + la ruta `/error-acceso`
muestran el motivo.

| Ruta | Acceso |
| --- | --- |
| `/` | Portal publico de bienvenida |
| `/auth` | Retorno de Azure AD (MSAL) |
| `/sin-acceso` | Cuenta autenticada pero no habilitada (cierra sesion) |
| `/error-acceso` | Error de sesion/token (incluye el detalle de Azure) |
| `/estudiante` | Rol `ESTUDIANTE` |
| `/apoderado` | Rol `APODERADO` |
| `/docente` | Rol `DOCENTE` |
| `/admin` | Rol `ADMIN` |

Cada portal se carga con lazy loading y **rutas hijas** (layout `DashboardShell` con sidebar):

| Portal | Rutas hijas |
| --- | --- |
| Estudiante | `inicio`, `horarios`, `notas`, `asistencias`, `asistencias/:id`, `progreso` |
| Apoderado | `inicio`, `pupilos`, `horarios`, `notas`, `asistencias`, `asistencias/:id`, `progreso`, `solicitudes` |
| Docente | `inicio`, `cursos`, `horarios`, `registrar-notas`, `registrar-asistencias` |
| Admin | `inicio`, `usuarios`, `roles`, `asignaturas` |

`MsalGuard` valida la sesion y `roleGuard([...])` valida el rol. El rol autoritativo se obtiene del BFF (`GET /api/me`).

## Docker

- Build en dos etapas: `node:24-alpine` (build) → `nginx:alpine` (servido).
- Nginx sirve la SPA con fallback a `index.html` y los assets, y **proxya `/api` al BFF**
  (`http://bff-web:8080`) para el entorno local (Docker), donde `bffBaseUrl` de `config.json` es relativo (`/api`).
  En AWS esa ruta **no se usa**: el API Gateway intercepta `/api/{proxy+}` y lo envia directo al BFF;
  alli `config.json` se monta con la URL absoluta del API Gateway.
- Publicado en `http://localhost:4200` (mapeo `4200:80`).

```bash
# desde la raiz del repositorio
docker compose up -d --build frontend
```

## Estado

- Disponible: bootstrap, autenticacion MSAL, rutas por rol con guards, layout con header y sidebar, tema oscuro/claro y portal publico.
- Disponible: consumo de `GET /api/me` (el BFF ya lo implementa) para resolver el rol autoritativo.
- Disponible: **pantallas de los portales** (Inicio, Horarios, Notas, Asistencias, Progreso Academico, Cursos, Registrar notas/asistencias, Usuarios/Roles/Asignaturas) y componentes reutilizables.
- **Datos mock**: esas pantallas usan datos de ejemplo en `libs/*/src/lib/mocks`; la conexion real al BFF esta pendiente para los recursos que aun no tienen endpoint.
- Pendiente: integracion completa con el resto de recursos del BFF, formularios/validaciones y pruebas funcionales.

## Documentacion

- [README raiz](../../README.md) · [Indice de docs](../../docs/README.md)
- [docs/frontend.md](../../docs/frontend.md) · [docs/arquitectura.md](../../docs/arquitectura.md)
- [docs/testing-login.md](../../docs/testing-login.md) · [apps/backend/README.md](../backend/README.md)
