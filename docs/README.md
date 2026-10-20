# Documentación de SIGA

Índice y reglas de navegación. El [README raíz](../README.md) es la entrada rápida; esta carpeta contiene la documentación técnica canónica del repositorio.

## Arquitectura y componentes

| Documento | Es responsabilidad de este documento |
|---|---|
| [`arquitectura.md`](arquitectura.md) | Vista del sistema, fronteras, flujos síncronos/asíncronos, seguridad y estado actual de alto nivel. |
| [`backend.md`](backend.md) | Convenciones compartidas, stack y entrada al backend; no duplica cada endpoint de cada dominio. |
| [`microservicios/README.md`](microservicios/README.md) | Catálogo por servicio, dependencias y grafo de acoplamientos. |
| [`frontend.md`](frontend.md) | Arquitectura Angular/Nx, autenticación, consumo del BFF y estado de datos. |
| [`../infra/terraform/README.md`](../infra/terraform/README.md) | Recursos AWS, Compose, persistencia, RabbitMQ, seguridad operativa y CI/CD. |
| [`paginacion.md`](paginacion.md) | Contrato común `Page<T>` y consultas paginadas. |

## Pruebas e historial

| Documento | Uso |
|---|---|
| [`testing-login.md`](testing-login.md) | Runbook para login/Entra, Docker y comprobaciones funcionales actuales. |
| [`registro-pruebas.md`](registro-pruebas.md) | Bitácora cronológica de verificaciones ejecutadas; conservar como evidencia histórica. |
| [`evidencias/README.md`](evidencias/README.md) | Convención para guardar pruebas positivas/negativas y vincular artefactos. |
| [`auditoria-backend.md`](auditoria-backend.md) | Auditoría histórica y trazabilidad de remediaciones; no usar sus hallazgos antiguos como estado vigente sin cotejar con las fichas actuales. |
| [`branch-cleanup.md`](branch-cleanup.md) | Convenciones/historial de ramas. |

## Guías de repositorio

| Documento | Contenido |
|---|---|
| [`../apps/backend/README.md`](../apps/backend/README.md) | Comandos, build, pruebas y enlaces técnicos del backend. |
| [`../apps/frontend/README.md`](../apps/frontend/README.md) | Inicio rápido y comandos Angular. |
| [`../tools/performance/README.md`](../tools/performance/README.md) | Prueba k6 de rendimiento mínima, autenticada y sin escrituras. |
| [`../README.md`](../README.md) | Resumen del producto, inicio local y mapa de documentos. |

## Orden recomendado

1. `README.md` raíz e [`arquitectura.md`](arquitectura.md) para entender el sistema.
2. [`backend.md`](backend.md) y [`microservicios/README.md`](microservicios/README.md) para backend y dependencias.
3. [`frontend.md`](frontend.md) e infraestructura para límites de cliente y despliegue.
4. [`testing-login.md`](testing-login.md) para reproducir validaciones; registrar resultados nuevos al final de `registro-pruebas.md`.

Los informes de rúbricas EP1/EP2 se mantienen en la carpeta externa `Desktop/SIGA-Evaluacion` y por eso no se versionan ni enlazan desde este índice.
