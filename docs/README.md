# Documentación de SIGA

Índice de la documentación técnica del proyecto. Para una visión general y el
inicio rápido, ver el [`README.md`](../README.md) de la raíz.

## Guías por tema

| Documento | Contenido |
| --- | --- |
| [`arquitectura.md`](arquitectura.md) | Visión general, capas, flujo de una solicitud, seguridad, comunicación, despliegue y estado actual frente a la arquitectura objetivo. |
| [`backend.md`](backend.md) | Proyecto Maven multi-módulo, microservicios, endpoints, `core-share`, seguridad y datos. |
| [`frontend.md`](frontend.md) | Aplicación Angular, MSAL, rutas por rol, librerías Nx y contratos con el BFF. |
| [`testing-login.md`](testing-login.md) | Guía paso a paso para validar login con Azure AD y el sistema completo con Docker Compose. |
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
3. [`testing-login.md`](testing-login.md) — levantar y probar localmente.
4. [`branch-cleanup.md`](branch-cleanup.md) — flujo de trabajo con Git.
