# Exporta las 10 bases del stack local (database-per-service) a dumps SQL
# listos para importar en la MariaDB de AWS (una sola instancia, 10 bases).
#
# El dump se ejecuta DENTRO de cada contenedor con --result-file para evitar
# el problema de encoding de la redireccion de PowerShell en Windows; luego se
# copia con `docker cp`.
#
# Uso:
#   powershell -ExecutionPolicy Bypass -File tools/exportar-bd-local.ps1
#   powershell -ExecutionPolicy Bypass -File tools/exportar-bd-local.ps1 -Destino D:\dumps
#
# Los dumps contienen datos personales: mantenerlos FUERA del repositorio.

param(
    [string]$Destino = (Join-Path $env:USERPROFILE 'siga-dumps')
)

$ErrorActionPreference = 'Stop'

$bases = @(
    @{ cont = 'mariadb-usuarios';       db = 'siga_usuarios_db' },
    @{ cont = 'mariadb-estudiantes';    db = 'siga_estudiantes_db' },
    @{ cont = 'mariadb-asignaturas';    db = 'siga_asignaturas_db' },
    @{ cont = 'mariadb-notas';          db = 'siga_notas_db' },
    @{ cont = 'mariadb-docentes';       db = 'siga_docentes_db' },
    @{ cont = 'mariadb-apoderados';     db = 'siga_apoderados_db' },
    @{ cont = 'mariadb-clases';         db = 'siga_clases_db' },
    @{ cont = 'mariadb-evaluaciones';   db = 'siga_evaluaciones_db' },
    @{ cont = 'mariadb-asistencias';    db = 'siga_asistencias_db' },
    @{ cont = 'mariadb-notificaciones'; db = 'siga_notificaciones_db' }
)

New-Item -ItemType Directory -Force -Path $Destino | Out-Null

$resumen = @()
foreach ($b in $bases) {
    $cont = $b.cont
    $db = $b.db

    $corriendo = docker inspect -f '{{.State.Running}}' $cont 2>$null
    if ($corriendo -ne 'true') {
        throw "El contenedor $cont no esta corriendo. Levanta el stack local antes de exportar."
    }

    # Dump dentro del contenedor: el resultado queda en /tmp y luego se copia.
    $cmd = 'mariadb-dump -uroot -p"$MARIADB_ROOT_PASSWORD" --single-transaction --default-character-set=utf8mb4 --routines --triggers --result-file=/tmp/' + $db + '.sql ' + $db
    docker exec $cont sh -c $cmd
    if ($LASTEXITCODE -ne 0) {
        throw "Fallo mariadb-dump en $cont ($db)"
    }

    $archivo = Join-Path $Destino "$db.sql"
    docker cp "${cont}:/tmp/$db.sql" $archivo | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Fallo docker cp del dump $db"
    }
    docker exec $cont sh -c "rm -f /tmp/$db.sql"

    $tamanoMb = [math]::Round((Get-Item -LiteralPath $archivo).Length / 1MB, 2)
    $ultimaLinea = Get-Content -LiteralPath $archivo -Tail 1
    $completo = $ultimaLinea -match 'Dump completed'

    $resumen += [pscustomobject]@{
        Base       = $db
        Contenedor = $cont
        MB         = $tamanoMb
        Completo   = $completo
    }
}

$resumen | Format-Table -AutoSize

if ($resumen.Completo -contains $false) {
    throw 'Algun dump no quedo completo (no aparece el footer "Dump completed").'
}

"OK: $($bases.Count) dumps en $Destino"
