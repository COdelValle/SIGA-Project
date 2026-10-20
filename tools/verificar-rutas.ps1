[CmdletBinding()]
param(
    [string]$BaseUrl = 'https://87hjovxzud.execute-api.us-east-1.amazonaws.com',
    [string]$Token = $env:SIGA_BEARER_TOKEN,
    [ValidateSet('ADMIN', 'OTRO')]
    [string]$Perfil = 'ADMIN'
)

$ErrorActionPreference = 'Stop'
$BaseUrl = $BaseUrl.TrimEnd('/')

if ([string]::IsNullOrWhiteSpace($Token)) {
    throw 'Define SIGA_BEARER_TOKEN con un access token valido de Entra; no lo escribas en el repositorio.'
}

$Token = $Token.Trim()
if ($Token -match '^Bearer\s+') {
    $Token = $Token -replace '^Bearer\s+', ''
}

$headers = @{ Authorization = "Bearer $Token" }

function Get-Respuesta {
    param([string]$Path)

    try {
        $response = Invoke-WebRequest -Uri "$BaseUrl$Path" -Headers $headers -Method Get -UseBasicParsing -ErrorAction Stop
        return [pscustomobject]@{
            Status = [int]$response.StatusCode
            Content = $response.Content
            ContentType = $response.Headers['Content-Type']
        }
    } catch {
        if ($_.Exception.Response) {
            return [pscustomobject]@{
                Status = [int]$_.Exception.Response.StatusCode
                Content = $null
                ContentType = $null
            }
        }
        throw
    }
}

function Get-ResumenJson {
    param([string]$Content)

    if ([string]::IsNullOrWhiteSpace($Content)) {
        return 'vacio'
    }

    try {
        $json = $Content | ConvertFrom-Json
    } catch {
        return "texto($($Content.Length) chars)"
    }

    if ($json -is [System.Array]) {
        $claves = @()
        if ($json.Count -gt 0 -and $json[0]) {
            $claves = @($json[0].PSObject.Properties.Name | Select-Object -First 8)
        }
        return "array(count=$($json.Count); keys=$($claves -join ','))"
    }

    $nombres = @($json.PSObject.Properties.Name)
    if ($nombres -contains 'content') {
        $items = @($json.content)
        return "page(count=$($items.Count); keys=$((@($nombres | Select-Object -First 8)) -join ','))"
    }

    return "objeto(keys=$((@($nombres | Select-Object -First 8)) -join ','))"
}

$casos = @(
    @{ Ruta = '/api/me'; Esperado = '200'; Nota = 'sesion' },
    @{ Ruta = '/api/bff/v1/admin/usuarios'; Esperado = '200'; Nota = 'admin usuarios' },
    @{ Ruta = '/api/bff/v1/admin/asignaturas'; Esperado = '200'; Nota = 'admin asignaturas' },
    @{ Ruta = '/api/bff/v1/admin/malla'; Esperado = '200'; Nota = 'admin malla' },
    @{ Ruta = '/api/bff/v1/admin/clases'; Esperado = '200'; Nota = 'admin clases' },
    @{ Ruta = '/api/bff/v1/admin/estudiantes?q=a'; Esperado = '200'; Nota = 'admin estudiantes' },
    @{ Ruta = '/api/bff/v1/admin/rabbitmq/queues/cola-notificaciones-notas'; Esperado = '200'; Nota = 'admin rabbitmq' },
    @{ Ruta = '/api/bff/v1/notificaciones/me'; Esperado = '200|403'; Nota = 'notificaciones' },
    @{ Ruta = '/api/bff/v1/notificaciones/me/no-leidas/count'; Esperado = '200|403'; Nota = 'notificaciones count' },
    @{ Ruta = '/api/bff/v1/docentes/cursos'; Esperado = '200|403'; Nota = 'docente cursos' },
    @{ Ruta = '/api/bff/v1/apoderados/pupilos'; Esperado = '200|403'; Nota = 'apoderado pupilos' },
    @{ Ruta = '/api/bff/v1/estudiantes/perfil/me'; Esperado = '200|403'; Nota = 'estudiante perfil' },
    @{ Ruta = '/api/bff/v1/asistencias/estudiante/me'; Esperado = '200|403'; Nota = 'asistencias estudiante' }
)

"perfil=$Perfil"
$fallas = 0
foreach ($caso in $casos) {
    $esperado = $caso.Esperado
    if ($Perfil -eq 'OTRO' -and $caso.Ruta.StartsWith('/api/bff/v1/admin/')) {
        $esperado = '403'
    }

    $respuesta = Get-Respuesta $caso.Ruta
    $permitidos = $esperado -split '\|'
    $ok = $permitidos -contains [string]$respuesta.Status

    if ($caso.Ruta -eq '/api/me' -and $respuesta.Status -eq 200) {
        $me = $respuesta.Content | ConvertFrom-Json
        $resumen = "roles=$($me.roles -join ',')"
    } elseif ($respuesta.Status -eq 200) {
        $resumen = Get-ResumenJson $respuesta.Content
    } else {
        $resumen = ''
    }

    if (-not $ok) {
        $fallas++
    }

    "{0} {1} -> {2} (esperado {3}) {4} {5}" -f $(if ($ok) { 'OK' } else { 'FALLA' }), $caso.Ruta, $respuesta.Status, $esperado, $caso.Nota, $resumen
}

if ($fallas -gt 0) {
    exit 1
}
