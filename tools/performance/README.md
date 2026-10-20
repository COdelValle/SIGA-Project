# Pruebas de carga y recursos SIGA

Objetivos acordados: 20 usuarios concurrentes durante 10 minutos; lecturas p95 <2 s, escrituras p95 <3 s, evento RabbitMQ <5 s, sin pérdida/duplicación y memoria sostenida <80%. Login/Graph se mide aparte.

## Lectura: `siga-api.js`

Mide `GET /api/me` sin cambiar datos. Es la referencia para lecturas; el token solo se lee de `SIGA_BEARER_TOKEN` y no se escribe en el repositorio ni en los resultados.

```powershell
$env:SIGA_BASE_URL = 'https://<api-gateway-url>'
# SIGA_BEARER_TOKEN se carga desde el gestor de secretos/variable de proceso.
k6 run tools/performance/siga-api.js
Remove-Item Env:SIGA_BEARER_TOKEN
```

## Escritura: `siga-escrituras.js`

Mide una operación de escritura configurable con p95 <3 s y errores <1%. **No se ejecuta por defecto**: exige `SIGA_ALLOW_WRITES=true` y un entorno descartable con datos de prueba aprobados. Un `POST` repetido crea recursos; definir un plan de limpieza antes de correr.

Variables:

| Variable | Obligatoria | Uso |
|---|---|---|
| `SIGA_BASE_URL`, `SIGA_BEARER_TOKEN` | sí | Gateway y token de prueba. |
| `SIGA_WRITE_PATH` | sí | Ruta del endpoint descartable, por ejemplo `/api/bff/v1/...`. |
| `SIGA_WRITE_METHOD` | no | `POST` (default), `PUT`, `PATCH` o `DELETE`. |
| `SIGA_WRITE_BODY` | no | JSON del DTO. Admite `{{correlationId}}`, `{{runId}}`, `{{vu}}` y `{{iteration}}` dentro de valores string. |
| `SIGA_WRITE_EXPECTED_STATUSES` | no | CSV; default `200,201,204`. |
| `SIGA_WRITE_P95_MS` | no | Umbral p95; default `3000`. |
| `SIGA_ALLOW_WRITES` | sí | Debe ser `true` para confirmar que el destino es descartable. |

Ejemplo (ajustar cuerpo/ruta al endpoint autorizado):

```powershell
$env:SIGA_BASE_URL = 'https://<api-gateway-url>'
$env:SIGA_BEARER_TOKEN = '<desde gestor de secretos>'
$env:SIGA_WRITE_PATH = '/api/bff/v1/<recurso>'
$env:SIGA_WRITE_METHOD = 'POST'
$env:SIGA_WRITE_BODY = '{"campo":"{{correlationId}}"}'
$env:SIGA_ALLOW_WRITES = 'true'
k6 run tools/performance/siga-escrituras.js
Remove-Item Env:SIGA_BEARER_TOKEN, Env:SIGA_WRITE_BODY, Env:SIGA_ALLOW_WRITES
```

## Escritura aislada RabbitMQ: `verificar-rabbit-admin.ps1`

Mide el camino de escritura del API admin creando, consultando y eliminando una cola, un exchange y un binding con nombres únicos. No toca datos de negocio ni deja topología residual. Requiere un token ADMIN; el default es 20 iteraciones y p95 <3 s.

```powershell
$env:SIGA_BEARER_TOKEN = 'Bearer ...'
powershell -ExecutionPolicy Bypass -File tools\performance\verificar-rabbit-admin.ps1 -Iterations 20
Remove-Item Env:SIGA_BEARER_TOKEN
```

Cada iteración verifica que la cola desaparezca al final; el script falla si algún status esperado no coincide o si el p95 de escritura alcanza el umbral. Usarlo en lugar de `siga-escrituras.js` cuando no exista un endpoint de negocio descartable.

## Memoria y contenedores: `medir-recursos.ps1`

Ejecuta por SSH, en modo lectura, `free -m` y `docker stats --no-stream` durante la ventana de carga. No reinicia ni modifica contenedores. Guarda muestras JSON y falla si la memoria máxima alcanza el umbral (default 80%) o si no pudo interpretar ninguna muestra.

```powershell
powershell -File tools/performance/medir-recursos.ps1 `
  -SshHost <eip> -KeyPath <ruta-a-vockey.pem> `
  -DurationMinutes 15 -IntervalSeconds 10 `
  -OutFile "$env:TEMP\siga-recursos.json"
```

El archivo de salida incluye porcentaje de memoria, línea `free -m`, memoria/CPU por contenedor y errores de muestreo. Revisar los nombres de contenedores antes de adjuntarlo como evidencia.

## Evento RabbitMQ e integridad

El objetivo de <5 s y la ausencia de pérdida/duplicación requieren un evento sintético y datos desechables. Usar el procedimiento ya documentado en `docs/registro-pruebas.md` (evento de outbox sintético, PR-20261011-11) o un destino centinela (`idEstudiante=999999999`) con un marcador único en `nombreEvaluacion`/`nombreAsignatura`: medir el tiempo hasta la fila en `notificaciones`, verificar que exista **exactamente una** fila con ese marcador, confirmar el outbox en `ENVIADO` y borrar el evento y la notificación. No usar estudiantes reales ni borrar colas compartidas.

## Registro

Ejecutar solo después de que los smoke checks pasen y en una ventana aprobada. Guardar en `docs/registro-pruebas.md`: fecha, ambiente, commit, resumen de k6, máximo de memoria y resultado de integridad, sin tokens, cookies, credenciales ni filas personales. Si no se cumplen los umbrales, perfilar antes de cambiar índices, pools o concurrencia y repetir con idéntico volumen.
