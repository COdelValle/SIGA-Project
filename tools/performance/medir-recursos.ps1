[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$SshHost,

    [Parameter(Mandatory = $true)]
    [string]$KeyPath,

    [string]$User = 'ubuntu',
    [int]$DurationMinutes = 15,
    [int]$IntervalSeconds = 10,
    [double]$MemoryThresholdPercent = 80,
    [string]$OutFile
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path -LiteralPath $KeyPath)) {
    throw "No existe la clave SSH: $KeyPath"
}
if ($DurationMinutes -lt 1) {
    throw 'DurationMinutes debe ser mayor o igual a 1.'
}
if ($IntervalSeconds -lt 1) {
    throw 'IntervalSeconds debe ser mayor o igual a 1.'
}

if ([string]::IsNullOrWhiteSpace($OutFile)) {
    $OutFile = Join-Path ([System.IO.Path]::GetTempPath()) (
        'siga-recursos-{0}.json' -f (Get-Date -Format 'yyyyMMdd-HHmmss')
    )
}

$remoteCommand = @'
free -m
echo __DOCKER_STATS__
docker stats --no-stream --format '{{.Name}}|{{.MemUsage}}|{{.CPUPerc}}'
'@

$samples = New-Object System.Collections.Generic.List[object]
$deadline = (Get-Date).AddMinutes($DurationMinutes)

while ((Get-Date) -lt $deadline) {
    $muestra = [ordered]@{
        timestamp = (Get-Date).ToString('o')
        memoryPercent = $null
        memoryLine = $null
        docker = @()
        error = $null
    }

    try {
        $salida = & ssh -i $KeyPath -o BatchMode=yes -o StrictHostKeyChecking=accept-new "$User@$SshHost" $remoteCommand 2>&1
        $texto = ($salida | Out-String)
        $lineaMemoria = ($texto -split "`r?`n" | Where-Object { $_ -match '^Mem:\s+' } | Select-Object -First 1)

        if ($lineaMemoria -and $lineaMemoria -match '^Mem:\s+(\d+)\s+(\d+)') {
            $totalMb = [double]$Matches[1]
            $usadaMb = [double]$Matches[2]
            if ($totalMb -gt 0) {
                $muestra.memoryPercent = [math]::Round(($usadaMb / $totalMb) * 100, 2)
            }
        }
        $muestra.memoryLine = $lineaMemoria

        $enDocker = $false
        foreach ($linea in ($texto -split "`r?`n")) {
            if ($linea -match '__DOCKER_STATS__') {
                $enDocker = $true
                continue
            }
            if ($enDocker -and -not [string]::IsNullOrWhiteSpace($linea)) {
                $muestra.docker += $linea.Trim()
            }
        }
    } catch {
        $muestra.error = $_.Exception.Message
    }

    $samples.Add([pscustomobject]$muestra)
    Start-Sleep -Seconds $IntervalSeconds
}

$memorias = @($samples | Where-Object { $null -ne $_.memoryPercent } | ForEach-Object { [double]$_.memoryPercent })
$resumen = [ordered]@{
    generatedAt = (Get-Date).ToString('o')
    host = $SshHost
    durationMinutes = $DurationMinutes
    intervalSeconds = $IntervalSeconds
    thresholdPercent = $MemoryThresholdPercent
    samples = $samples.Count
    maxMemoryPercent = $null
    passed = $false
    inconclusive = $false
}

if ($memorias.Count -eq 0) {
    $resumen.inconclusive = $true
} else {
    $maximo = ($memorias | Measure-Object -Maximum).Maximum
    $resumen.maxMemoryPercent = $maximo
    $resumen.passed = ($maximo -lt $MemoryThresholdPercent)
}

$resultado = [pscustomobject]@{
    summary = [pscustomobject]$resumen
    samples = $samples
}

$resultado | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $OutFile -Encoding UTF8

Write-Output ("Resultado: {0}" -f $OutFile)
if ($resumen.inconclusive) {
    Write-Error 'No se pudo interpretar free -m en ninguna muestra; resultado no concluyente.' -ErrorAction Continue
    exit 2
}

Write-Output ("Memoria maxima: {0}% (limite {1}%)" -f $resumen.maxMemoryPercent, $MemoryThresholdPercent)
if (-not $resumen.passed) {
    exit 1
}
