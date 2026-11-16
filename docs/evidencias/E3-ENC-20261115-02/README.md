# E3-ENC-20261115-02 — Checks de promoción y seguridad

- **PR:** [#121, `dev` → `deploy`](https://github.com/COdelValle/SIGA-Project/pull/121).
- **SHA actual de `dev`:** `c651331b3b9d114eab2581277f6ed97817ef9b56`.
- **Bitácora:** [`../../registro-pruebas.md`](../../registro-pruebas.md), entrada `PR-20261115-15`.

## Resultado

| Comprobación | Resultado |
|---|---|
| `CredentialCipherErroresTest` | 2/2 aprobadas |
| Sintaxis `tools/performance/allure-summary.js` | `node --check` aprobado |
| CI backend/frontend/E2E/infra | Aprobado en el PR (runs `43788574518` y `43788582398`) |
| CodeQL Java/JavaScript y check CodeQL | Aprobados; se corrigió `js/insecure-randomness` |
| GitGuardian | **FALLA/BLOQUEA**: `Generic Password` en `docker-compose.yml`, commit `06d5d666`; el check no expone la línea/valor |
| Merge/deploy | Pendiente; no se fusionó ni se disparó CD |

Para desbloquear, revisar ese hallazgo en GitGuardian y confirmar si el default histórico se usó en algún entorno. La plantilla actual no contiene esos valores por defecto. Si se usaron como credenciales, rotarlas; si es un placeholder de desarrollo, documentar el falso positivo en GitGuardian. No se adjuntan valores secretos.
