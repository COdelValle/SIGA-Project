# Pruebas y reporte centralizado con Allure 3

Este documento describe como ejecutar las suites y como se publica el reporte
unificado de SIGA. El reporte es la fuente unica de resultados automatizados:

- **GitHub Pages:** `https://codelvalle.github.io/SIGA-Project/`
- **Ramas publicadas:** `main`, `dev` y `deploy`
- **Reporte local:** `allure-report/index.html`

## 1. Backend

```bash
mvn -f apps/backend/pom.xml -B test \
  '-Dtest=!*ApplicationTests' \
  '-DfailIfNoTests=false' \
  '-Dsurefire.failIfNoSpecifiedTests=false' \
  '-Dtypescript.generator.skip=true'
```

Genera:

- Resultados Allure: `apps/backend/**/target/allure-results`
- Cobertura JaCoCo: `apps/backend/**/target/site/jacoco/index.html`
- Reportes JUnit: `apps/backend/**/target/surefire-reports`

Los tests de Testcontainers se omiten automaticamente si Docker no esta
disponible. En GitHub Actions (Ubuntu) se ejecutan con Docker.

## 2. Frontend

```bash
cd apps/frontend
npm test -- --watch=false --coverage
```

Genera:

- Resultados Allure: `apps/frontend/allure-results`
- Cobertura: `apps/frontend/coverage/lcov-report/index.html`
- Reporte JUnit: `apps/frontend/dist/test-results/frontend-junit.xml`

## 3. E2E con autenticacion simulada

```bash
cd apps/frontend
npm run e2e
```

Playwright intercepta `/config.json` con `e2e/config.e2e.json`, por lo que
la app arranca en modo `authMode: mock`, usa un JWT de prueba y no redirige a
Microsoft. Las APIs se simulan con `page.route` para probar la navegacion, el
role guard y la accesibilidad.

- Resultados Allure: `apps/frontend/allure-results-e2e`
- Reporte Playwright: `apps/frontend/playwright-report`
- Trazas/capturas: `apps/frontend/test-results`

## 4. Contratos OpenAPI

Los contratos se generan desde springdoc:

- Especificaciones comprometidas: `docs/openapi/*.json`
- Tests de generacion: `OpenApiSpecificationTest` en cada modulo
- Tests de contrato BFF: `*ClientContractTest` en `bff-web`

Los tests de contrato levantan WireMock con `OpenApiValidationListener`, llaman
al cliente Feign real y validan request/response contra la especificacion del
microservicio. No requieren Docker ni Azure.

Para regenerar las especificaciones despues de un cambio de API:

```bash
mvn -f apps/backend/pom.xml -pl ms-notas -am test -Dtest=OpenApiSpecificationTest
```

Repetir con el modulo modificado. El CI ejecuta todos los `OpenApiSpecificationTest`
en cada PR.

## 5. Generar el reporte unificado

```bash
npx allure generate \
  $(find apps/backend -type d -name allure-results) \
  apps/frontend/allure-results \
  apps/frontend/allure-results-e2e \
  --config allurerc.mjs --clean
```

En PowerShell:

```powershell
$dirs = @(Get-ChildItem apps/backend -Recurse -Directory -Filter allure-results |
  ForEach-Object { $_.FullName })
$dirs += (Resolve-Path apps/frontend/allure-results).Path
$dirs += (Resolve-Path apps/frontend/allure-results-e2e).Path
npx allure generate @dirs --config allurerc.mjs --clean
```

Alternativa desde la raiz: `npm run allure:generate`.

Abrir `allure-report/index.html` o ejecutar `npm run allure:open`.

## 6. CI y publicacion

El workflow `.github/workflows/ci.yml`:

1. Ejecuta backend, frontend y E2E.
2. Sube `allure-results`, cobertura y JUnit como artefactos.
3. Genera un unico reporte Allure con `allurerc.mjs`.
4. Comenta el resumen en el PR con `allure-framework/allure-action`.
5. Publica en GitHub Pages las ramas `main`, `dev` y `deploy`.
6. Publica un indice en la raiz que redirige a `/dev/` y enlaza las demas ramas.

La raiz `https://codelvalle.github.io/SIGA-Project/` abre ese indice; el reporte
de cada rama vive en `/dev/`, `/main/` o `/deploy/`.

El workflow `.github/workflows/cd.yml` ejecuta la misma verificacion antes de
desplegar y publica el reporte en `/deploy/`.

## 7. Convenciones de labels

| Label | Valores | Uso |
| --- | --- | --- |
| `layer` | `unit`, `integration`, `e2e`, `performance` | Piramide de pruebas |
| `epic` | Modulo o portal | Agrupacion principal |
| `feature` | Funcionalidad | Agrupacion secundaria |
| `story` | Caso de uso | Detalle |
| `severity` | `blocker`, `critical`, `normal`, `minor`, `trivial` | Riesgo |
| `owner` | `backend`, `frontend`, `qa` | Responsable |
| `tag` | `smoke`, `contract`, `a11y`, `regression` | Filtros |

En Java se agregan con `@Epic`, `@Feature`, `@Story`, `@Severity`, `@Owner`,
`@Tag`. En Vitest y Playwright se agregan con `allure.epic()`, `allure.feature()`,
etc., o con `globalLabels` en el reporter.

## 8. Seguridad del reporte publico

El reporte de Pages es publico. Antes de generarlo, el CI ejecuta:

```bash
node tools/allure/sanitize-results.mjs <directorios-allure-results>
```

El sanitizador redacta JWT, `Bearer`, passwords, tokens y claves en archivos
JSON. Aun asi, no adjuntar datos personales ni secretos en nuevos tests.

## 9. Estado de cobertura actual

- Backend: **69.71% de lineas de negocio** (67.63% global bruto), JaCoCo 13 modulos.
- Frontend: **50.96% de lineas de negocio** (51.41% global bruto), Vitest v8.
- Objetivo: **80%+ de codigo de negocio**, con umbrales progresivos y gate en CI.
- El reporte Allure incluye una suite sintetica de cobertura por modulo/lib.

## 10. Alcance del reporte y detalles

El reporte unificado debe contener:

| Suite | Resultados esperados |
|---|---|
| Backend JUnit | 636 |
| Frontend Vitest | 107 |
| E2E Playwright | 5 |
| Infraestructura | 3 |
| Cobertura (sintetica) | 25 |
| **Total** | **776** |

El job de reporte ejecuta `tools/allure/verify-results.mjs` y falla si falta
cualquiera de las suites.

El porcentaje grande del dashboard (por ejemplo 100%) es la **tasa de exito de
las pruebas**, no la cobertura de codigo.

- Backend: `allure-assertj` convierte las aserciones AssertJ en pasos. Los tests
  de contrato adjuntan la especificacion OpenAPI, el stub y la validacion.
- Frontend: Vitest registra los expect como pasos.
- E2E: Playwright registra navegacion, expectativas y adjuntos; los tests
  agregan pasos y respuestas de API con `allure.step`/`allure.attachment`.

## 11. Taxonomia y nombres de pruebas

| Label | Valores | Uso |
|---|---|---|
| `layer` | `unit`, `component`, `integration`, `contract`, `e2e`, `security`, `performance`, `infra`, `coverage` | Tipo de prueba y grafico de piramide |
| `epic` | Modulo o portal | Agrupacion principal |
| `feature` | Funcionalidad | Agrupacion secundaria |
| `story` | Caso de uso o clase | Detalle |
| `owner` | `backend`, `frontend`, `qa` | Responsable |
| `severity` | `blocker`, `critical`, `normal`, `minor`, `trivial` | Riesgo |
| `tag` | `unit`, `smoke`, `a11y`, `regression`, etc. | Filtros |

Reglas:

- Los nombres visibles deben ser frases en espanol, sin `()` ni nombres de metodo.
- `layer` se agrupa antes de `epic > feature > story` en Allure.
- Backend: la extension `test-support` completa labels y nombres legibles si no hay anotaciones.
- Frontend/E2E: `tools/allure/enrich-results.mjs` completa labels por archivo y framework.
- `tools/allure/verify-taxonomy.mjs` falla el reporte si falta un label o el nombre no es legible.

Ejemplos:

- `[unit] Gestion Academica > Notas > Registrar nota`
- `[integration] Gestion Academica > Estudiantes > GET /api/v1/estudiantes/{id}`
- `[contract] Contratos BFF > ms-notas > GET /api/v1/notas/{id}`
- `[security] Seguridad > JWT y roles > Ownership`
- `[e2e] Portal Docente > Notas > Dashboard del docente`
- `[coverage] Calidad > Cobertura backend > 69.71% de lineas`
