# Frontend SIGA

Aplicacion Angular del sistema de gestion academica SIGA. Es una aplicacion modular que se comunica **solo con el BFF** (a traves de `/api`) y organiza su codigo en librerias Nx con fronteras entre portales por rol.

Detalle general del proyecto en el [README raiz](../../README.md) y [docs/frontend.md](../../docs/frontend.md).

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
│   ├── core/            # auth (MSAL), guards, interceptores, http, config, modelos
│   ├── shared-ui/       # layout y componentes reutilizables
│   ├── public-portal/   # landing publico
│   ├── academico/       # componentes academicos reutilizables
│   ├── estudiante/      # portal estudiante
│   ├── apoderado/       # portal apoderado
│   ├── docente/         # portal docente
│   └── admin/           # portal administracion (base)
├── src/
│   ├── app/             # app.ts, app.config.ts, app.routes.ts (rutas por rol)
│   ├── main.ts          # carga config runtime y hace bootstrap
│   └── types/           # tipos generados desde el BFF
├── public/config.json   # configuracion runtime (BFF + MSAL)
├── proxy.conf.json      # proxy /api -> BFF en desarrollo
├── nginx.conf           # servidor SPA + proxy /api en contenedor
├── Dockerfile
├── angular.json
└── project.json
```

## Comandos

Desde `apps/frontend`:

```bash
npm install
npm start              # servidor de desarrollo en http://localhost:4200
npm run build          # build de produccion en dist/frontend/browser
npm test               # pruebas unitarias (Vitest)
```

Linting con Nx (desde la raiz):

```bash
npx nx lint frontend
```

## Estilos (Tailwind CSS)

- **Tailwind CSS 4** integrado via PostCSS (`.postcssrc.json` con el plugin `@tailwindcss/postcss`).
- Import en `src/styles.css`: `@import 'tailwindcss';` y `@source "../libs"` para incluir las librerias Nx.
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

Autenticacion con **MSAL Angular v6 + Azure AD**. El retorno del login se procesa en `/auth`.

| Ruta | Acceso |
| --- | --- |
| `/` | Portal publico de bienvenida |
| `/auth` | Retorno de Azure AD (MSAL) |
| `/estudiante` | Rol `ESTUDIANTE` |
| `/apoderado` | Rol `APODERADO` |
| `/docente` | Rol `DOCENTE` |
| `/admin` | Rol `ADMIN` |

Cada portal se carga con lazy loading; `MsalGuard` valida la sesion y `roleGuard([...])` valida el rol. El rol autoritativo se obtiene del BFF (`GET /api/me`).

## Docker

- Build en dos etapas: `node:24-alpine` (build) → `nginx:alpine` (servido).
- Nginx sirve la SPA con fallback a `index.html` y proxya `/api` hacia `bff-web:8080`.
- Publicado en `http://localhost:4200` (mapeo `4200:80`).

```bash
# desde la raiz del repositorio
docker compose up -d --build frontend
```

## Estado

- Disponible: bootstrap, autenticacion MSAL, rutas por rol, guards, layout base y portal publico.
- Pendiente: pantallas de negocio (CRUD), integracion completa con el BFF y pruebas funcionales.
