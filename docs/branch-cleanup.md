# Limpieza de ramas

Estado: **ejecutada** (2026-09-14). El repositorio quedo con solo tres ramas
remotas: `dev`, `main` y `deploy`.

## Modelo adoptado

```
feature/*  ->  dev  ->  main  ->  deploy
                                (dispara CD)
```

- **dev**: integracion.
- **main**: estable / entrega.
- **deploy**: rama que dispara el CD (`push: deploy` + `workflow_dispatch`).
- **feature/*** y **fix/***: ramas cortas, se eliminan al integrar.

## Resultado

- Se eliminaron **28 ramas ya integradas** en `dev` (locales y remotas).
- Se eliminaron **4 ramas con commits propios** que estaban obsoletas, dejando
  un tag de respaldo por cada una antes de borrarlas:

| Rama eliminada | Tag de respaldo |
| --- | --- |
| `feature/infrasEc2` | `archive/infrasEc2` |
| `feature/infraestructure` | `archive/infraestructure` |
| `feature/azure` | `archive/azure` |
| `exp/bff-web-structure` | `archive/bff-web-structure` |

Los tags se pueden consultar con `git tag -l "archive/*"` y
`git show archive/<nombre>`. Para recuperar una rama:
`git branch <nueva-rama> archive/<nombre>`.

`deploy` quedo alineada con `main` (ya no es el scaffold viejo).

## Flujo de trabajo

1. Crear `feature/<algo>` o `fix/<algo>` desde `dev`.
2. PR a `dev` (merge `--no-ff`).
3. PR `dev` -> `main` (merge `--no-ff`).
4. Actualizar `deploy` desde `main` para disparar el CD:
   ```bash
   git fetch origin
   git branch -f deploy origin/main
   git push origin deploy --force-with-lease
   ```
5. Borrar la rama corta tras el merge.

## Protecciones recomendadas en GitHub

- Requerir PR y CI verde hacia `dev` y `main`.
- `deploy`: protegida, solo actualizaciones desde `main`.
- Activar "Automatically delete head branches" para las ramas cortas.
