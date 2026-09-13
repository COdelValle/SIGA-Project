# Limpieza de ramas

Documento de trabajo. **Nada se elimina todavia**: sirve como inventario para
ejecutar la limpieza cuando el equipo lo autorice.

## Modelo adoptado

```
feature/*  ->  dev  ->  main  ->  deploy
                                (dispara CD)
```

- **dev**: integracion.
- **main**: estable / entrega.
- **deploy**: rama que dispara el CD (`push: deploy` + `workflow_dispatch`).
- **feature/*** y **fix/***: ramas cortas, se eliminan al integrar.

## Estado actual (resumen)

`main` esta ~97 commits por detras de `dev` y es la rama por defecto del repo.
Casi todas las ramas ya estan contenidas en `dev`.

## 1. Ramas ya integradas en `dev` (seguras de eliminar)

Tienen 0 commits propios respecto a `dev`:

- `docs/readme`
- `feature/docker-infra`
- `feature/frontend`
- `feature/ms-estudiantes`
- `feature/bff-web`
- `feature/common-security`
- `feature/core-share`
- `feature/dtos`
- `feature/java-common-security`
- `feature/ms-asignaturas`
- `feature/ms-notas`
- `feature/ms-usuarios-auth`
- `feature/nxForCD`
- `feature/workflow`
- `fix/backend-contracts`
- `fix/backend-data`
- `fix/backend-errors-config`
- `fix/backend-feign-tuning`
- `fix/backend-pom`
- `fix/backend-security`
- `fix/backend-soft-delete`
- `fix/backend-swagger`
- `fix/bff-microservices-config`
- `fix/frontend-docker-build`
- `fix/login-e2e`
- `fix/msal-logout-redirect`
- `fix/msal-url-cleanup`

## 2. Ramas con commits propios (revisar antes de eliminar)

| Rama | Contenido | Decision |
| --- | --- | --- |
| `feature/infrasEc2` | Unico Terraform real, pero invalido y basado en un `dev` viejo | **Rescatar solo la infra conceptual** (ya reescrita en `infra/terraform`). **NO mergear**: revierte el `.gitignore` de `.env` y renombra `ci.yml`. Eliminar. |
| `feature/infraestructure` | Terraform antiguo (`main.tf` vacio) | Eliminar. |
| `exp/bff-web-structure` | Experimento de estructura BFF | Revisar; probablemente eliminar. |
| `feature/azure` | Commit obsoleto + `package.json.backup` | Eliminar. |
| `deploy` | Divergida y obsoleta; el CD apunta aqui | Recrear desde `main`. |

## 3. Comandos sugeridos (ejecutar por separado, con el equipo de acuerdo)

Actualizar `main` a `dev`:

```bash
git checkout main
git merge --ff-only dev
git push origin main
```

Recrear `deploy` desde `main`:

```bash
git branch -f deploy main
git push origin deploy --force-with-lease
```

Eliminar ramas locales integradas:

```bash
git branch -d docs/readme feature/docker-infra feature/frontend feature/ms-estudiantes \
  feature/bff-web feature/common-security feature/core-share feature/dtos \
  feature/java-common-security feature/ms-asignaturas feature/ms-notas \
  feature/ms-usuarios-auth feature/nxForCD feature/workflow \
  fix/backend-contracts fix/backend-data fix/backend-errors-config \
  fix/backend-feign-tuning fix/backend-pom fix/backend-security \
  fix/backend-soft-delete fix/backend-swagger fix/bff-microservices-config \
  fix/frontend-docker-build fix/login-e2e fix/msal-logout-redirect fix/msal-url-cleanup
```

Eliminar ramas remotas integradas (una por una, requiere permiso de push):

```bash
git push origin --delete <nombre-rama>
```

## 4. Protecciones recomendadas en GitHub

- Rama por defecto: `dev` (o `main` si se avanza primero).
- Requerir PR y CI verde hacia `dev` y `main`.
- `deploy`: protegida, solo merges desde `main`.
