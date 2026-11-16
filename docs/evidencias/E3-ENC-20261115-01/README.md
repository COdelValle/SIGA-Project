# E3-ENC-20261115-01 — Pruebas de código y Compose

- **Fecha local:** 2026-11-15.
- **Rama/SHA:** `dev` / `ed64504cd82408de2658d427966aedf357b7df90`.
- **Entorno:** Windows 11, JDK 21.0.11, Maven 3.9.9, Node/npm local.
- **Bitácora:** [`../../registro-pruebas.md`](../../registro-pruebas.md), entrada `PR-20261115-14`.
- **CI Linux del mismo SHA:** [run 43761010668](https://github.com/COdelValle/SIGA-Project/actions/runs/43761010668), `success`.

| Caso | Esperado | Obtenido | Estado |
|---|---|---|---|
| Backend Maven, 17 módulos | Compilar y ejecutar suites sin errores | `BUILD SUCCESS`; suites unitarias sin fallas | `PARCIAL`: Testcontainers omitió pruebas de integración porque Docker Desktop devolvió `BadRequest 400`/Docker info vacío |
| Seguridad de `ms-kafka-admin` | 401/403/200 según token, rol y scope | 4 tests aprobados | `OK` |
| Frontend Vitest | Suite completa verde | 35 archivos, 283 tests aprobados; 94.17% cobertura de líneas | `OK` |
| Build Angular producción | Bundle de producción exitoso | `Application bundle generation complete` | `OK` |
| Compose local | YAML/configuración válida | `docker compose --env-file .env.example config -q` sin error | `OK` |
| Plantilla Compose AWS | YAML/configuración válida | `config -q` aprobado con `REGISTRY` y `IMAGE_TAG` ficticios | `OK` |

El primer intento Maven falló por el parsing de flags con PowerShell; se corrigió entrecomillando cada `-D...` y la ejecución final terminó en verde. Testcontainers se omitió localmente por el named pipe; el CI Ubuntu del mismo SHA está verde. Revisar sus reportes JUnit antes de afirmar que cada prueba de integración se ejecutó, y no considerar el resultado local como integración Kafka verificada.

Los tests de OpenAPI reescribieron temporalmente diez JSON por reordenamiento de propiedades. Se restauraron esos artefactos generados; no forman parte de la evidencia ni del cambio documental EP3.
