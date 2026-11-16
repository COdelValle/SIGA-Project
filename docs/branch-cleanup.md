# Limpieza de ramas

Estado: **ejecutada** (2026-09-14). El repositorio quedo con solo tres ramas
remotas: `dev`, `main` y `deploy`.

## Modelo adoptado

```
feature/*  ->  dev  ->  deploy  ->  main
                       AWS       release estable
```

- **dev**: integración de desarrollo; CI valida pull requests y pushes.
- **deploy**: candidato de release. El push dispara CD, que debe completar su
  puerta de calidad, el despliegue AWS y los smoke tests.
- **main**: release estable; solo se actualiza después de que el mismo SHA pasó
  aceptación post-deploy en AWS.
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

## Flujo de trabajo y aceptación

1. Crear `feature/<tema>` o `fix/<tema>` desde `dev`; vincular cada cambio a los
   IDs de la matriz de rúbrica y a pruebas/evidencias.
2. PR a `dev`: requiere CI backend/frontend completo y pruebas de contrato.
   No mezclar cambios que no estén relacionados con la matriz de evaluación.
3. PR `dev` → `deploy` solo si todos los indicadores de código están aprobados
   y la evidencia local/CI está indexada. CI también debe correr en este PR.
4. El push de merge a `deploy` dispara `.github/workflows/cd.yml`. Su job `verify`
   vuelve a ejecutar pruebas antes de construir/desplegar. Desplegar imágenes por
   SHA inmutable y conservar el tag de la versión previa para rollback.
5. Ejecutar smoke/E2E AWS: Gateway/401/200/403, frontend/MSAL, clúster RabbitMQ,
   colas/DLQ, CRUD admin, salud de servicios y verificación de persistencia.
6. Solo con todos los resultados aprobados, PR/fast-forward `deploy` → `main` con
   **el mismo SHA desplegado**; no cambiar código entre aceptación y promoción.
7. Si falla una validación, no promover a `main`: guardar el resultado fallido,
   corregir en una nueva rama desde `dev`, repetir pruebas y volver a candidatear.
8. Borrar la rama corta tras integrar.

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
- Requerir CI verde también en PR hacia `deploy`; `deploy` solo recibe desde `dev`.
- `main`: solo recibe el SHA de `deploy` después de la aceptación AWS.
- Proteger `deploy` para que el CD no se dispare con un push accidental.
- Activar "Automatically delete head branches" para las ramas cortas.
