# Evidencia de pruebas y aceptación

Este directorio acompaña la matriz de rúbricas externa preparada por el equipo (fuera del repositorio) con evidencia versionable de pruebas. La bitácora [`../registro-pruebas.md`](../registro-pruebas.md) conserva resultados cronológicos y fallos; este índice define cómo adjuntar resultados nuevos sin duplicar logs.

## Identificador y registro

Cada ejecución debe incluir: ID `E1-ENC-*`, `E1-PRES-*`, `E2-ENC-*` o `E2-PRES-*`; fecha/hora; commit SHA; entorno (`local`, Testcontainers, CI o AWS); caso positivo/negativo; esperado/obtenido; estado; enlace al run y referencia al artefacto.

Para EP3 usar los prefijos `E3-ENC-*` (código/build/pruebas) y `E3-PRES-*` (despliegue y demostración operativa). La guía reproducible está en [`../pruebas-manuales-ep3-aws.md`](../pruebas-manuales-ep3-aws.md).

Para un fallo, conservar el intento `FALLA`, diagnóstico y corrección. Crear una nueva verificación `OK` para demostrar la solución; no reemplazar ni borrar el fallo inicial. No declarar un indicador 100% si queda un caso requerido sin probar o sin evidencia vigente.

## Fuentes de artefactos

- **CI:** reporte JUnit Surefire como artifact del workflow; Vitest/build y advertencias en el log de GitHub Actions. Al registrar evidencia enlazar el run correspondiente al SHA.
- **API/MSAL:** matriz de rutas/status y JSON esperado, con Bearer/token redactado; capturas DevTools/Entra sin secretos ni datos personales.
- **RabbitMQ:** snapshot de `cluster_status`, exchanges, colas, bindings, consumers y conteo DLQ; publicar eventos sintéticos y purgar solo los IDs creados por la prueba.
- **Base de datos:** versión Flyway y conteos antes/después de despliegue; no incluir dumps, filas personales ni credenciales.
- **AWS:** commit/tag desplegado, estado health/containers, respuesta SPA y códigos de prueba del API Gateway; identificar fecha/ambiente.
- **Kafka:** estado de brokers, metadatos de los dos topics, líderes/ISR, grupos/offsets/lag, publicaciones sintéticas y recuperación del broker; incluir captura de Kafka UI autenticada sin credenciales visibles.

Los artefactos/binarios deben subirse a GitHub Actions o guardarse como capturas saneadas bajo `docs/evidencias/<ID>/`. No versionar access tokens, cookies Erlang, `.env`, claves privadas, dumps de producción ni secretos.

## Puerta de aceptación

Un punto solo pasa a `100% demostrado` cuando están verdes las pruebas automáticas aplicables y la evidencia manual/cloud necesaria. El estado del código se registra por separado del estado de infraestructura: un unit test no demuestra que AWS/Entra/RabbitMQ estén activos.

## EP3 Kafka — verificación vigente

- `E3-ENC-20261115-01`: Maven backend `BUILD SUCCESS` (los tests dependientes de Testcontainers se omitieron por el Docker Desktop local), 4/4 pruebas de seguridad del administrador Kafka, 283/283 pruebas frontend, build Angular de producción y configuración de ambos Compose válidas. Ver detalle en [`E3-ENC-20261115-01/README.md`](E3-ENC-20261115-01/README.md).
- `E3-ENC-20261115-02`: PR #121 para promover a `deploy`; CodeQL corregido y checks de backend/frontend/E2E/infra/CodeQL verdes. GitGuardian continúa bloqueando el PR con un hallazgo que requiere revisar su detalle en el dashboard. Ver [`E3-ENC-20261115-02/README.md`](E3-ENC-20261115-02/README.md).
- `E3-PRES-20261115-01`: la EC2 observada usa una plantilla antigua sin Kafka, Kafka UI, `ms-kafka-admin` ni `ms-reportes`; por eso no se ejecutaron pruebas de broker ni autenticación UI en AWS. Ver detalle en [`E3-PRES-20261115-01/README.md`](E3-PRES-20261115-01/README.md).
- Los nombres de los secrets `KAFKA_UI_USER` y `KAFKA_UI_PASSWORD_HASH` se confirmaron en GitHub; sus valores no se leyeron ni se adjuntan.

## Casos previstos para cierre EP1/EP2

La matriz concreta por indicador (incluidos archivos y pasos de demostración) está en los informes externos. Para el cierre técnico se deben registrar, al menos:

- login/logout/renovación MSAL, scopes/roles, llamadas Gateway con token válido, sin token y con token inválido;
- rutas del BFF/Gateway con código HTTP y JSON esperado;
- creación, eliminación y rechazo de parámetros inválidos de colas/exchanges/bindings;
- evento RabbitMQ válido (ACK + persistencia idempotente), transitorio (retry) e inválido (NACK/DLQ);
- dos nodos activos, tres colas académicas y sus DLQ, exchanges topic/direct, y Compose desplegado;
- persistencia antes/después del despliegue, Flyway y respaldo verificado si hubo migración.

## Trazabilidad local EP1/EP2 — 2026-10-19

Estado de la rama `feature/rubric-100-ep1-ep2` sobre `1be2c3b`; los cambios aún no tienen SHA publicado ni artifact CI. La columna **Estado local** no reemplaza la evidencia de Entra/AWS/CI.

| Indicador de rúbrica | Implementación | Prueba o evidencia local | Estado local |
|---|---|---|---|
| EP1: `MsalInterceptor` literal | `app.config.ts`, `msal.factory.ts` | `msal.factory.spec.ts` (mapa + Bearer solo al BFF) | Verde |
| EP1: roles/scopes desde claims | `access-token-claims.service.ts`, `role.guard.ts` | `access-token-claims.service.spec.ts`, `role.guard.spec.ts` | Verde |
| EP1: camino de error de token | `role.guard.ts` + `/error-acceso` | Caso `InteractionRequiredAuthError` en `role.guard.spec.ts` | Verde |
| EP1: JWT firma/issuer/audience/exp | `JwtResourceServerConfiguration`, `JwtAudienceValidator`, `JwtClaimAuthoritiesConverter` | `JwtResourceServerConfigurationTest`, `SharedSecurityFilterTest` (200/401/403) | Verde |
| EP1: validación en Gateway/BFF real | Terraform + resource server | CD smoke 401/SPA/CORS; token real pendiente | Pendiente de entorno |
| EP1: PKCE y renovación silenciosa | MSAL SPA | Sin captura de `code_challenge`/renovación en esta sesión | Pendiente de entorno |
| EP2: nombres centralizados | `ConfiguracionNombresRabbitMQ` + aliases | `ConfiguracionNombresRabbitMQTest` | Verde |
| EP2: beans y bindings | `RegistrationMessagingConfig`, `ConfiguracionTopologiaNotificaciones` | `RegistrationMessagingConfigTest`, `ConfiguracionTopologiaNotificacionesTest` | Verde |
| EP2: ACK/NACK/reintentos/DLQ | `ConfirmadorMensajes` y listeners | `ConfirmadorMensajesTest`, `ConfirmadorMensajesRabbitTest`, `RabbitAdminServiceRabbitTest` | Unitario e integración CI verdes (run `39356000842`) |
| EP2: API admin y validación de path/query | `RabbitAdminController`, DTOs | `RabbitAdminControllerValidationTest`, `SolicitudRabbitValidationTest`, `RabbitAdminServiceTest` | Verde |
| EP2: topología desplegada y datos | `cd.yml` | Chequeos de 18 colas/13 consumidores/exchanges/cluster y conteos pre/post | Pendiente de AWS |
| Rendimiento acordado | `tools/performance/` | `siga-api.js` (lectura), `siga-escrituras.js` (escritura opt-in) y `medir-recursos.ps1` (RAM/CPU por SSH); sintaxis validada | Guiones listos; ejecución pendiente de ventana/AWS |

Los totales locales de esta sesión están en [`../registro-pruebas.md`](../registro-pruebas.md), sección 9. Cualquier resultado nuevo debe conservar el fallo original, la corrección y una ejecución `OK` separada.
