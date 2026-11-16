# Arquitectura y funcionamiento del frontend

## 1. Rol en el sistema y estado actual

`apps/frontend` es una SPA Angular servida por Nginx. Solo conoce el BFF en `/api`; no contiene URLs de microservicios ni abre conexiones RabbitMQ. Implementa portales público, estudiante, apoderado, docente y administrador, con layout reutilizable, tema claro/oscuro y navegación lazy.

Hay integraciones reales del BFF para identidad, perfil académico/pupilos, cursos del docente, notas/evaluaciones/asistencias, administración de usuarios y bandeja de notificaciones. La cobertura no es uniforme para todos los recursos del dominio. La configuración runtime trae `useMocks=false`; los datos de `libs/mocks` se habilitan deliberadamente como demo/constantes. Un error de HTTP se representa como estado vacío/error con reintento, no como fallback automático a mock. El estado por vista se resume más abajo y puede variar por recurso.

## 2. Stack, límites y configuración

| Área | Tecnologías/fuentes |
|---|---|
| App | Angular 22.1, TypeScript 6, RxJS 7.8, Angular Router. |
| Identidad | `@azure/msal-angular` 6 y `@azure/msal-browser` 5. |
| Monorepo | Nx y `@nx/enforce-module-boundaries`; `scope:*` tags restringen dependencias entre librerías. |
| UI | Tailwind CSS 4 + PostCSS, tokens CSS/tema. |
| Pruebas | Vitest + `HttpTestingController`/mocks de providers donde aplica. |
| Config runtime | `src/main.ts`, `libs/core/.../app-config.loader.ts`, `public/config.json`. |

Node carga `/config.json` antes del bootstrap, luego construye providers MSAL y router. El archivo contiene `bffBaseUrl`, `useMocks`, `clientId`, `authority`, redirect URIs y scopes. La plantilla está en `apps/frontend/public/config.json`; CD crea configuración de runtime para AWS. Los IDs no son secretos; client secret, contraseñas y tokens no pertenecen al bundle.

## 3. Flujo de autenticación y autorización del cliente

1. `AuthService.login()` inicia redirect MSAL; `PublicClientApplication` procesa el regreso en `/auth` mediante el inicializador de `app.config.ts`.
2. El inicializador procesa `handleRedirectObservable`, registra errores normalizados, fija cuenta activa y limpia parámetros MSAL de URL.
3. `MsalGuard` exige una cuenta en rutas privadas. `roleGuard` llama `MeService.getMe()` (`GET /api/me`) y enruta con el rol autoritativo que devuelve BFF/servicio usuarios.
4. `MsalInterceptor` usa `protectedResourceMap` para limitar solicitudes al BFF; llama `acquireTokenSilent` con scopes runtime y adjunta `Authorization: Bearer <accessToken>`.
5. `AccessTokenClaimsService` decodifica `roles` y `scp` del access token para navegación/diagnóstico; exige coincidencia con el perfil del BFF y los scopes solicitados. No valida firma ni se usa como autorización: eso corresponde a Gateway/backend.
6. `errorInterceptor` y `AuthErrorService` normalizan errores de retorno/HTTP; los errores de autorización y los flows interactivos los maneja MSAL.
7. Logout limpia caché de cuenta antes de `logoutRedirect`.

`app.config.ts` registra explícitamente `MSAL_INTERCEPTOR_CONFIG` y `MsalInterceptor` como `HTTP_INTERCEPTORS` con `withInterceptorsFromDi()`. `msal.factory.spec.ts` comprueba el mapa del BFF y que el Bearer se adjunte solo a recursos protegidos.

El SDK MSAL para SPA usa Authorization Code con PKCE como flujo de navegador; la app invoca redirect sin implementar a mano `code_verifier`. Para demostrarlo se debe inspeccionar la solicitud OIDC (code challenge S256/state/nonce) o evidencia del proveedor; no mostrar el token ni el verifier.

## 4. Rutas y límites por rol

`src/app/app.routes.ts` enruta lazy:

| Ruta raíz | Control | Librería |
|---|---|---|
| `/` | Pública | `public-portal` |
| `/auth` | Callback | componente de retorno raíz |
| `/sin-acceso`, `/error-acceso` | Públicas de resultado/error | public portal / raíz |
| `/estudiante` | `MsalGuard` + `roleGuard(ESTUDIANTE)` | `estudiante` |
| `/apoderado` | `MsalGuard` + `roleGuard(APODERADO)` | `apoderado` |
| `/docente` | `MsalGuard` + `roleGuard(DOCENTE)` | `docente` |
| `/admin` | `MsalGuard` + `roleGuard(ADMIN)` | `admin` |

Los hijos por portal están registrados en `libs/{portal}/src/lib/*-routes.ts`; `DashboardShell` aporta header/sidebar. En el backend cada operación debe repetir autorización por scope/rol/ownership.

## 5. Librerías Nx y responsabilidades

| Librería | Función / dependencias |
|---|---|
| `core` | Config runtime, MSAL, `AuthService`, `MeService`, guards, interceptores, modelos, `recursoRemoto`, tema y refresco compartido. No depende de portales. |
| `shared-ui` | Dashboard/header/nav, campana in-app, selector, tarjetas, tabs y paginador. Consume servicios/modelos transversales, no implementa reglas del backend. |
| `public-portal` | Landing y estado sin acceso/login público. |
| `academico` | Modelos/mappers del perfil, tablas reutilizables y servicios de perfil/asistencias. |
| `estudiante` | Composición del portal estudiante y páginas hijas. Consume servicios académicos y core. |
| `apoderado` | Selección de pupilo y composición de horarios/notas/asistencias/progreso/solicitudes. Reutiliza servicios académicos. |
| `docente` | Cursos, horarios, registro de notas/evaluaciones y asistencias. Servicios de estado agrupan requests al BFF y autosalvado. |
| `admin` | CRUD de usuarios, altas asíncronas/polling, credenciales temporales, clases/alumnos, asignaturas y malla. |
| `mocks` | Datos demo explícitos y constantes para reglas/visualización; no son sustituto silencioso ante errores HTTP. |

Las restricciones `depConstraints` están en `eslint.config.js`; portales no deben depender entre sí. Contratos TypeScript del BFF se generan en `src/types/bff-models.d.ts` con TypeScript Generator Maven, salvo que se configure `typescript.generator.skip=true`.

## 6. Consumo del BFF

La tabla es una guía de relaciones UI/API, no un inventario de cada ruta. El catálogo completo de métodos, roles y scopes está en [`apps/backend/README.md`](../apps/backend/README.md) y los detalles del dueño en [`microservicios/bff-web.md`](microservicios/bff-web.md).

| Flujo UI | Contrato BFF observado/documentado | Nota de integración |
|---|---|---|
| Identidad/roles | `GET /api/me` | `MeService` cachea respuesta; 404 produce perfil sin roles para acceso denegado. |
| Estudiante | `/api/bff/v1/estudiantes/perfil/me`, `/perfil/{id}` | Perfil agregado, usado también para pupilos autorizados. |
| Apoderado | `GET .../apoderados/pupilos`, perfiles por estudiante, actualización validada | Ownership se valida en backend; no confiar en ID elegido en pantalla. |
| Docente | cursos, horario, grilla de notas y asistentes por asignatura | Escritura via BFF con scopes/ownership. |
| Administración | usuarios, roles, asignaturas/malla, selectores, detalle y registro asíncrono | Alta retorna processId; UI consulta estado y recibe credencial de un solo uso. |
| Notificaciones | lista, contador, marcar individual/todas y ocultar leídas | Campana compartida por estudiante/apoderado; polling, refresco de foco y estado por OID. |

## 7. Estado, caché, errores y mocks

- `recursoRemoto`/servicios modelan carga, éxito, error y reintento; error BFF no selecciona `libs/mocks` automáticamente.
- `MeService` usa `shareReplay(1)` para `/api/me`; invalida con `clear()` al cambiar sesión.
- Servicios académicos cachean por clave, comparten requests y reciben señal global `RefrescoDatosService`; campana solicita refresco cuando llega notificación o se marca.
- El consumidor debe diferenciar error de autenticación 401, autorización 403, recurso ausente 404 e indisponibilidad 503; la UI muestra estado/mensaje y no asumir dato vacío como cero.
- Las constantes de mocks pueden usarse para presentación controlada (`useMocks=true`); no evidencian integración cloud.

## 8. Presentación, paginación y pruebas

Tailwind 4 se importa en `src/styles.css`; `ThemeService` usa `data-theme` y guarda preferencia en localStorage. `shared-ui` alberga el paginador; el contrato `Page<T>` y el uso cliente/backend se especifican en [`paginacion.md`](paginacion.md).

Comandos desde `apps/frontend`:

```bash
npm ci
npm start
npx tsc -p tsconfig.app.json --noEmit
npm test -- --watch=false
npm run build -- --configuration production
```

Las pruebas Vitest de services/componentes están junto a las fuentes `*.spec.ts`; cubren HTTP, estados, normalización y acciones de UI. La cobertura no sustituye una comprobación manual con sesión real de Entra.

## 9. Verificación del estado de datos por vista

Antes de afirmar que una pantalla usa backend, verificar su service (URL construida), `useMocks`, ruta BFF y caso de error. Los recursos conectados listados en la tabla anterior tienen implementación en el repo; vistas demo o constantes se deben marcar expresamente. Una respuesta exitosa en mocks no demuestra que Gateway, token o backend funcionen.
