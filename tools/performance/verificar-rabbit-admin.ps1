[CmdletBinding()]
param(
    [string]$BaseUrl = 'https://87hjovxzud.execute-api.us-east-1.amazonaws.com',
    [string]$Token = $env:SIGA_BEARER_TOKEN,
    [int]$Iterations = 20,
    [double]$P95Ms = 3000
)

$ErrorActionPreference = 'Stop'
$BaseUrl = $BaseUrl.TrimEnd('/')

if ([string]::IsNullOrWhiteSpace($Token)) {
    throw 'Define SIGA_BEARER_TOKEN con un access token ADMIN; no lo escribas en el repositorio.'
}

$Token = $Token.Trim()
if ($Token -match '^Bearer\s+') {
    $Token = $Token -replace '^Bearer\s+', ''
}

$headers = @{ Authorization = "Bearer $Token" }
$runId = '{0}{1}' -f (Get-Date -Format 'yyyyMMddHHmmss'), (Get-Random -Minimum 1000 -Maximum 9999)
$writes = New-Object System.Collections.Generic.List[double]
$reads = New-Object System.Collections.Generic.List[double]
$fallas = 0

function Invoke-Step {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body,
        [int[]]$Expected,
        [string]$Kind
    )

    $watch = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $parameters = @{
            Uri = "$BaseUrl$Path"
            Method = $Method
            Headers = $headers
            UseBasicParsing = $true
            ErrorAction = 'Stop'
        }
        if ($null -ne $Body) {
            $parameters.Body = ($Body | ConvertTo-Json -Compress)
            $parameters.ContentType = 'application/json'
        }
        $response = Invoke-WebRequest @parameters
        $status = [int]$response.StatusCode
    } catch {
        if ($_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
        } else {
            $status = 0
        }
    } finally {
        $watch.Stop()
    }

    if ($Kind -eq 'write') {
        $writes.Add($watch.Elapsed.TotalMilliseconds)
    } elseif ($Kind -eq 'read') {
        $reads.Add($watch.Elapsed.TotalMilliseconds)
    }

    if ($Expected -notcontains $status) {
        $script:fallas++
        Write-Warning ("FALLA {0} {1} -> {2} (esperado {3})" -f $Method, $Path, $status, ($Expected -join '|'))
    }

    return $status
}

function Get-Percentile {
    param([System.Collections.Generic.List[double]]$Values, [double]$Percentile)

    if ($Values.Count -eq 0) {
        return 0
    }
    $sorted = @($Values | Sort-Object)
    $index = [Math]::Ceiling($sorted.Count * $Percentile) - 1
    if ($index -lt 0) {
        $index = 0
    }
    return [Math]::Round($sorted[$index], 1)
}

for ($i = 1; $i -le $Iterations; $i++) {
    $sufijo = '{0}.{1}' -f $runId, $i
    $queue = "perf.$sufijo.q"
    $exchange = "perf.$sufijo.x"
    $routingKey = "perf.$sufijo.*"
    $creadaCola = $false
    $creadoExchange = $false
    $creadoBinding = $false

    try {
        Invoke-Step -Method 'POST' -Path '/api/bff/v1/admin/rabbitmq/exchanges' -Body @{ nombre = $exchange; tipo = 'TOPIC' } -Expected @(201) -Kind 'write' | Out-Null
        $creadoExchange = $true
        Invoke-Step -Method 'POST' -Path '/api/bff/v1/admin/rabbitmq/queues' -Body @{ nombre = $queue } -Expected @(201) -Kind 'write' | Out-Null
        $creadaCola = $true
        Invoke-Step -Method 'POST' -Path '/api/bff/v1/admin/rabbitmq/bindings' -Body @{ cola = $queue; exchange = $exchange; routingKey = $routingKey } -Expected @(201) -Kind 'write' | Out-Null
        $creadoBinding = $true
        Invoke-Step -Method 'GET' -Path "/api/bff/v1/admin/rabbitmq/queues/$queue" -Expected @(200) -Kind 'read' | Out-Null
    } finally {
        if ($creadoBinding) {
            $query = '?cola={0}&exchange={1}&routingKey={2}' -f [uri]::EscapeDataString($queue), [uri]::EscapeDataString($exchange), [uri]::EscapeDataString($routingKey)
            Invoke-Step -Method 'DELETE' -Path "/api/bff/v1/admin/rabbitmq/bindings$query" -Expected @(204, 404) -Kind 'write' | Out-Null
        }
        if ($creadaCola) {
            Invoke-Step -Method 'DELETE' -Path "/api/bff/v1/admin/rabbitmq/queues/$queue" -Expected @(204, 404) -Kind 'write' | Out-Null
            Invoke-Step -Method 'GET' -Path "/api/bff/v1/admin/rabbitmq/queues/$queue" -Expected @(404) -Kind 'read' | Out-Null
        }
        if ($creadoExchange) {
            Invoke-Step -Method 'DELETE' -Path "/api/bff/v1/admin/rabbitmq/exchanges/$exchange" -Expected @(204, 404) -Kind 'write' | Out-Null
        }
    }
}

$p95Escrituras = Get-Percentile -Values $writes -Percentile 0.95
$p95Lecturas = Get-Percentile -Values $reads -Percentile 0.95
$maxEscrituras = if ($writes.Count -gt 0) { [Math]::Round(($writes | Measure-Object -Maximum).Maximum, 1) } else { 0 }
$maxLecturas = if ($reads.Count -gt 0) { [Math]::Round(($reads | Measure-Object -Maximum).Maximum, 1) } else { 0 }

"run_id=$runId"
"iteraciones=$Iterations"
"fallas=$fallas"
"escrituras_n=$($writes.Count)"
"escrituras_p95_ms=$p95Escrituras"
"escrituras_max_ms=$maxEscrituras"
"lecturas_n=$($reads.Count)"
"lecturas_p95_ms=$p95Lecturas"
"lecturas_max_ms=$maxLecturas"

if ($fallas -gt 0 -or $p95Escrituras -ge $P95Ms) {
    exit 1
}
