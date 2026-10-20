# Evidencia de pruebas y aceptación

Este directorio acompaña la matriz de rúbricas externa preparada por el equipo (fuera del repositorio) con evidencia versionable de pruebas. La bitácora [`../registro-pruebas.md`](../registro-pruebas.md) conserva resultados cronológicos y fallos; este índice define cómo adjuntar resultados nuevos sin duplicar logs.

## Identificador y registro

Cada ejecución debe incluir: ID `E1-ENC-*`, `E1-PRES-*`, `E2-ENC-*` o `E2-PRES-*`; fecha/hora; commit SHA; entorno (`local`, Testcontainers, CI o AWS); caso positivo/negativo; esperado/obtenido; estado; enlace al run y referencia al artefacto.

Para un fallo, conservar el intento `FALLA`, diagnóstico y corrección. Crear una nueva verificación `OK` para demostrar la solución; no reemplazar ni borrar el fallo inicial. No declarar un indicador 100% si queda un caso requerido sin probar o sin evidencia vigente.

## Fuentes de artefactos

- **CI:** reporte JUnit Surefire como artifact del workflow; Vitest/build y advertencias en el log de GitHub Actions. Al registrar evidencia enlazar el run correspondiente al SHA.
- **API/MSAL:** matriz de rutas/status y JSON esperado, con Bearer/token redactado; capturas DevTools/Entra sin secretos ni datos personales.
- **RabbitMQ:** snapshot de `cluster_status`, exchanges, colas, bindings, consumers y conteo DLQ; publicar eventos sintéticos y purgar solo los IDs creados por la prueba.
- **Base de datos:** versión Flyway y conteos antes/después de despliegue; no incluir dumps, filas personales ni credenciales.
- **AWS:** commit/tag desplegado, estado health/containers, respuesta SPA y códigos de prueba del API Gateway; identificar fecha/ambiente.

Los artefactos/binarios deben subirse a GitHub Actions o guardarse como capturas saneadas bajo `docs/evidencias/<ID>/`. No versionar access tokens, cookies Erlang, `.env`, claves privadas, dumps de producción ni secretos.

## Puerta de aceptación

Un punto solo pasa a `100% demostrado` cuando están verdes las pruebas automáticas aplicables y la evidencia manual/cloud necesaria. El estado del código se registra por separado del estado de infraestructura: un unit test no demuestra que AWS/Entra/RabbitMQ estén activos.

## Casos previstos para cierre EP1/EP2

La matriz concreta por indicador (incluidos archivos y pasos de demostración) está en los informes externos. Para el cierre técnico se deben registrar, al menos:

- login/logout/renovación MSAL, scopes/roles, llamadas Gateway con token válido, sin token y con token inválido;
- rutas del BFF/Gateway con código HTTP y JSON esperado;
- creación, eliminación y rechazo de parámetros inválidos de colas/exchanges/bindings;
- evento RabbitMQ válido (ACK + persistencia idempotente), transitorio (retry) e inválido (NACK/DLQ);
- dos nodos activos, tres colas académicas y sus DLQ, exchanges topic/direct, y Compose desplegado;
- persistencia antes/después del despliegue, Flyway y respaldo verificado si hubo migración.

## Trazabilidad EP1/EP2 — actualizada 2026-10-20

Estado del release `2104a266` desplegado en AWS desde `deploy`; CI y smoke checks verdes. La columna **Estado** distingue lo demostrado en el entorno de lo que aún falta.

| Indicador de rúbrica | Implementación | Prueba o evidencia | Estado |
|---|---|---|---|
| EP1: `MsalInterceptor` literal | `app.config.ts`, `msal.factory.ts` | `msal.factory.spec.ts` (mapa + Bearer solo al BFF) | Verde |
| EP1: roles/scopes desde claims | `access-token-claims.service.ts`, `role.guard.ts` | specs de claims/guard; tokens reales ADMIN y DOCENTE con 43 scopes | Verde |
| EP1: camino de error de token | `role.guard.ts` + `/error-acceso` | Caso `InteractionRequiredAuthError` en `role.guard.spec.ts` | Verde |
| EP1: JWT firma/issuer/audience/exp | `JwtResourceServerConfiguration`, `JwtAudienceValidator`, `JwtClaimAuthoritiesConverter` | tests unitarios/filtro; `iss`/`aud` reales alineados con el JWT Authorizer | Verde |
| EP1: validación en Gateway/BFF real | Terraform + resource server | smoke AWS: SPA 200, sin token/inválido 401; ADMIN `/api/me` y admin RabbitMQ 200; DOCENTE admin RabbitMQ 403 | Verde |
| EP1: PKCE y renovación silenciosa | MSAL SPA | `EP1-PRES-PKCE/authorize.png`: `response_type=code`, `code_challenge`, `S256`; renovación cubierta por specs MSAL | Verde PKCE; renovación con token real pendiente |
| EP2: nombres centralizados | `ConfiguracionNombresRabbitMQ` + aliases | `ConfiguracionNombresRabbitMQTest` | Verde |
| EP2: beans y bindings | `RegistrationMessagingConfig`, `ConfiguracionTopologiaNotificaciones` | `RegistrationMessagingConfigTest`, `ConfiguracionTopologiaNotificacionesTest` | Verde |
| EP2: ACK/NACK/reintentos/DLQ | `ConfirmadorMensajes` y listeners | `ConfirmadorMensajesTest`, `ConfirmadorMensajesRabbitTest`, `RabbitAdminServiceRabbitTest` | Verde en CI (run `39356000842`) |
| EP2: API admin y validación de path/query | `RabbitAdminController`, DTOs | `RabbitAdminControllerValidationTest`, `SolicitudRabbitValidationTest`, `RabbitAdminServiceTest`; consulta admin real 200 | Verde |
| EP2: topología desplegada y datos | `cd.yml` | 18 colas, 13 consumidores, dos nodos, exchanges y conteos pre/post idénticos; respaldo 2026-10-20 | Verde en AWS |
| Rendimiento acordado | `tools/performance/` | `siga-api.js`, `siga-escrituras.js` y `medir-recursos.ps1`; sintaxis validada | Pendiente de ejecución |

Los totales locales de esta sesión están en [`../registro-pruebas.md`](../registro-pruebas.md), sección 9. Cualquier resultado nuevo debe conservar el fallo original, la corrección y una ejecución `OK` separada.

## Evidencias adjuntas

- [`EP1-ENC-TOKEN-ADMIN.png`](EP1-ENC-TOKEN-ADMIN.png): token real ADMIN; `roles=ADMIN`, sin token/inválido 401, `/api/me` 200 y admin RabbitMQ 200.
- [`EP1-ENC-TOKEN-DOCENTE-403.png`](EP1-ENC-TOKEN-DOCENTE-403.png): token real DOCENTE; `roles=DOCENTE`, `/api/me` 200 y admin RabbitMQ 403.
- [`EP1-PRES-PKCE/authorize.png`](EP1-PRES-PKCE/authorize.png) y [`README.md`](EP1-PRES-PKCE/README.md): `authorize` con `response_type=code`, `code_challenge` y `S256`; `state`/`nonce` tapados.

Las capturas fueron recortadas para excluir los tokens; no se versionan tokens, cookies ni credenciales. Si una captura contiene un token, debe eliminarse o taparse antes de commitearla.

## Evidencia de ejecución pendiente

### Carga

Ejecutar `tools/performance/README.md` en una ventana aprobada con datos descartables; registrar fecha, commit, ambiente, p95, errores, evento RabbitMQ, integridad y memoria máxima. No adjuntar tokens.
