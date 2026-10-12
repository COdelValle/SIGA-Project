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

`deploy` quedó alineada con `main` en la limpieza documentada. Verificar ambos
heads antes de sincronizarlos: la alineación histórica no garantiza que sigan
iguales hoy.

## Flujo de trabajo

1. Crear `feature/<algo>` o `fix/<algo>` desde `dev`.
2. PR a `dev` (merge `--no-ff`).
3. PR `dev` -> `main` (merge `--no-ff`).
4. Actualizar `deploy` solo desde `main`. Este push **dispara el CD** por
   `.github/workflows/cd.yml` (`push` en `deploy`):
   ```bash
   git fetch origin
   git switch deploy
   git merge --ff-only origin/main
   git push origin deploy
   ```
5. Borrar la rama corta tras el merge.

### Sincronización documental sin despliegue

Si se necesita actualizar `deploy` solo con documentación y no desplegar:

1. Comprobar que no haya un CD activo o pendiente.
2. Deshabilitar temporalmente el workflow **CD** en GitHub Actions (no modificar
   su YAML).
3. Sincronizar el commit documental hacia `deploy` con fast-forward; CI puede
   ejecutarse, pero CD debe permanecer deshabilitado.
4. Verificar que no se creó un run CD y reactivar el workflow para cambios futuros.

No ejecutar `workflow_dispatch` en esta operación. Un push exclusivamente `.md`
no se excluye automáticamente del disparador de despliegue.

## Protecciones recomendadas en GitHub

- Requerir PR y CI verde hacia `dev` y `main`.
- `deploy`: protegida, solo actualizaciones desde `main`.
- Activar "Automatically delete head branches" para las ramas cortas.
