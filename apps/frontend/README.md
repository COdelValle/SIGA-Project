# Frontend SIGA

SPA Angular del sistema SIGA. El frontend se comunica con el backend **solo a través del BFF** usando `/api`; la arquitectura detallada de librerías, estados y servicios es [`docs/frontend.md`](../../docs/frontend.md), y la arquitectura de extremo a extremo está en [`docs/arquitectura.md`](../../docs/arquitectura.md).

## Tecnologías

Angular 22.1, TypeScript 6, RxJS 7.8, MSAL Angular 6/MSAL Browser 5, Nx, Tailwind CSS 4, Vitest y Prettier. La separación por librerías Nx y sus dependencias está en `libs/*/project.json` y las reglas de `eslint.config.js`.

## Inicio local

Desde `apps/frontend`:

```bash
npm ci
npm start
npx tsc -p tsconfig.app.json --noEmit
npm test -- --watch=false
npm run build -- --configuration production
```

`npm start` usa `proxy.conf.json` para `/api → http://localhost:8080`. Docker sirve el build con Nginx y proxy local al BFF; en AWS el navegador llama `/api` en el mismo origen del API Gateway. Los pasos del stack están en el [README raíz](../../README.md).

## Configuración y autenticación

- `main.ts` carga `/config.json` antes del bootstrap. La misma imagen puede usar configuración distinta en local/AWS.
- `public/config.json` es la plantilla local; en CD se genera un `config.json` runtime. `bffBaseUrl` selecciona la ruta al BFF y `useMocks` está desactivado por defecto.
- Login/logout usan `AuthService` y MSAL redirect; el inicializador procesa el retorno, selecciona la cuenta activa y limpia parámetros MSAL.
- `MsalGuard` protege la sesión y `roleGuard` consulta `GET /api/me` al BFF para resolver el rol.
- `authInterceptor` propio ejecuta `acquireTokenSilent` y agrega el Bearer a llamadas al BFF. La fábrica de configuración de `MsalInterceptor` existe, pero la app registra el interceptor propio; ver [`docs/frontend.md`](../../docs/frontend.md) para el detalle frente a la rúbrica EP1.
- Los guards de cliente no sustituyen los roles/scopes del backend.

## Datos reales y modo demo

`useMocks=false` es el comportamiento normal. Los recursos conectados consumen endpoints BFF; si falla una solicitud se muestra error/vacío y opción de reintento, no un fallback automático a datos ficticios. `useMocks=true` habilita explícitamente demo; algunas librerías exportan constantes reutilizables. El mapa de pantalla → servicio → endpoint y el estado de integración están en [`docs/frontend.md`](../../docs/frontend.md).

## Rutas

`/` portal público; `/auth` retorno MSAL; `/sin-acceso` cuenta no habilitada; `/error-acceso` fallo de sesión/token; `/estudiante`, `/apoderado`, `/docente` y `/admin` están protegidas por sesión y rol, con carga lazy.

## Documentos relacionados

- [`docs/frontend.md`](../../docs/frontend.md) — arquitectura modular detallada.
- [`docs/testing-login.md`](../../docs/testing-login.md) — runbook de autenticación.
- [`docs/microservicios/bff-web.md`](../../docs/microservicios/bff-web.md) — contrato del BFF.
