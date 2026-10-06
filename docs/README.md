# Documentación de SIGA

Índice de la documentación técnica del proyecto. Para una visión general y el
inicio rápido, ver el [`README.md`](../README.md) de la raíz.

## Guías por tema

| Documento | Contenido |
| --- | --- |
| [`arquitectura.md`](arquitectura.md) | Visión general, capas, flujo de una solicitud, seguridad, comunicación, despliegue y estado actual frente a la arquitectura objetivo. |
| [`backend.md`](backend.md) | Proyecto Maven multi-módulo, microservicios, endpoints, `core-share`, seguridad y datos. |
| [`frontend.md`](frontend.md) | Aplicación Angular, MSAL, rutas por rol, librerías Nx y contratos con el BFF. |
| [`paginacion.md`](paginacion.md) | Contrato de paginación (`Page<T>`) de los `GET /search`, ejemplos y guía para backend y frontend. |
| [`testing-login.md`](testing-login.md) | Guía paso a paso para validar login con Azure AD y el sistema completo con Docker Compose. |
| [`registro-pruebas.md`](registro-pruebas.md) | Bitácora reutilizable de pruebas y pasos por sesión: plantilla, entradas registradas, runbook (comandos, SQL y colas) y checklist de cierre. |
| [`auditoria-backend.md`](auditoria-backend.md) | Hallazgos de la auditoría del backend (P0–P3), inventario de scopes y la decisión pendiente de autorización (roles vs scopes granulares). |
| [`branch-cleanup.md`](branch-cleanup.md) | Modelo de ramas (`feature -> dev -> main -> deploy`) y limpieza de ramas ejecutada. |

## Guías de componentes

| Documento | Contenido |
| --- | --- |
| [`../apps/backend/README.md`](../apps/backend/README.md) | Backend: módulos, comandos Maven, puertos y endpoints. |
| [`../apps/frontend/README.md`](../apps/frontend/README.md) | Frontend: comandos, estilos, configuración runtime y rutas. |
| [`../infra/terraform/README.md`](../infra/terraform/README.md) | Infraestructura AWS: topología, uso, persistencia y secretos. |

## Orden de lectura recomendado

1. [`arquitectura.md`](arquitectura.md) — entender el sistema completo.
2. [`backend.md`](backend.md) y [`frontend.md`](frontend.md) — profundizar por capa.
3. [`paginacion.md`](paginacion.md) — contrato de las búsquedas paginadas.
4. [`testing-login.md`](testing-login.md) — levantar y probar localmente.
5. [`registro-pruebas.md`](registro-pruebas.md) — registrar y reproducir pruebas entre sesiones.
6. [`branch-cleanup.md`](branch-cleanup.md) — flujo de trabajo con Git.
