# Restaura TODOS los secrets requeridos por el CD desde el .env del repo y las
# credenciales AWS locales (~/.aws/credentials), sin imprimir valores. Es
# idempotente: volver a correrlo re-sube lo mismo.
#
# Uso (desde la raiz del repo):
#   powershell -ExecutionPolicy Bypass -File tools/set-github-secrets.ps1
#   ... -Ec2KeyPath "$env:USERPROFILE\Downloads\Vockey.pem"   # sube EC2_SSH_KEY
#   ... -AwsProfile siga                                     # otro perfil de ~/.aws
#   ... -WhatIf                                               # solo lista que se subiria
[CmdletBinding()]
param(
    [string]$EnvFile = '',
    [string]$AwsCredentials = '',
    [string]$AwsProfile = 'default',
    [string]$AwsRegion = 'us-east-1',
    [string]$Ec2User = 'ubuntu',
    [string]$Ec2KeyPath = '',
    [switch]$WhatIf
)

$ErrorActionPreference = 'Stop'

# $PSScriptRoot no esta disponible al evaluar los valores por defecto del bloque
# param en PowerShell 5.1: las rutas se resuelven aqui.
if (-not $EnvFile) {
    $EnvFile = Join-Path (Split-Path -Parent $PSScriptRoot) '.env'
}
if (-not $AwsCredentials) {
    $AwsCredentials = Join-Path $env:USERPROFILE '.aws\credentials'
}

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    throw 'gh no esta instalado o no esta en el PATH.'
}
if (-not (Test-Path -LiteralPath $EnvFile)) {
    throw "No existe el archivo .env: $EnvFile"
}
if (-not (Test-Path -LiteralPath $AwsCredentials)) {
    throw "No existen las credenciales AWS: $AwsCredentials"
}

# Lee un archivo clave=valor. Con -perfil solo toma las claves del bloque INI
# indicado (p. ej. [default] en ~/.aws/credentials, que puede tener mas de un
# perfil); sin perfil toma todas las lineas (formato .env).
function Leer-ClaveValor([string]$ruta, [string]$perfil) {
    $mapa = @{}
    $activo = [string]::IsNullOrEmpty($perfil)
    foreach ($linea in Get-Content -LiteralPath $ruta) {
        if ($linea -match '^\s*\[(.+?)\]\s*$') {
            $activo = (-not [string]::IsNullOrEmpty($perfil)) -and
                ($Matches[1].Trim().ToLowerInvariant() -eq $perfil.ToLowerInvariant())
            continue
        }
        if ($activo -and $linea -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$') {
            $mapa[$Matches[1]] = $Matches[2].Trim()
        }
    }
    return $mapa
}

$envVars = Leer-ClaveValor $EnvFile ''
$aws = Leer-ClaveValor $AwsCredentials $AwsProfile

if (-not $aws['aws_access_key_id']) {
    throw "El perfil [$AwsProfile] no tiene aws_access_key_id en $AwsCredentials"
}

$secrets = [ordered]@{
    'AWS_ACCESS_KEY_ID'                    = $aws['aws_access_key_id']
    'AWS_SECRET_ACCESS_KEY'                = $aws['aws_secret_access_key']
    'AWS_SESSION_TOKEN'                    = $aws['aws_session_token']
    'AWS_REGION'                           = $AwsRegion
    'EC2_USER'                             = $Ec2User
    'DB_USER'                              = $envVars['DB_USER']
    'DB_PASSWORD'                          = $envVars['DB_PASS']
    'MARIADB_ROOT_PASSWORD'                = $envVars['MARIADB_ROOT_PASSWORD']
    'AZURE_TENANT_ID'                      = $envVars['AZURE_TENANT_ID']
    'AZURE_CLIENT_ID'                      = $envVars['AZURE_CLIENT_ID']
    'AZURE_APP_ID_URI'                     = $envVars['AZURE_APP_ID_URI']
    'AZURE_CLIENT_SECRET'                  = $envVars['AZURE_CLIENT_SECRET']
    'AZURE_API_APP_ID'                     = $envVars['AZURE_API_APP_ID']
    'RABBITMQ_USER'                        = $envVars['RABBITMQ_USER']
    'RABBITMQ_PASSWORD'                    = $envVars['RABBITMQ_PASS']
    'RABBITMQ_ERLANG_COOKIE'               = $envVars['RABBITMQ_ERLANG_COOKIE']
    'REGISTRO_ASYNC_ENABLED'               = $envVars['REGISTRO_ASYNC_ENABLED']
    'REGISTRO_CRED_KEY'                    = $envVars['REGISTRO_CRED_KEY']
    'REGISTRO_NOTIFY_CREDENTIALS_ENABLED'  = $envVars['REGISTRO_NOTIFY_CREDENTIALS_ENABLED']
}

if ($Ec2KeyPath) {
    if (-not (Test-Path -LiteralPath $Ec2KeyPath)) {
        throw "No existe la clave EC2: $Ec2KeyPath"
    }
    $secrets['EC2_SSH_KEY'] = (Get-Content -LiteralPath $Ec2KeyPath -Raw)
}

$sinValor = @()
foreach ($nombre in @($secrets.Keys)) {
    if ([string]::IsNullOrWhiteSpace($secrets[$nombre])) {
        $sinValor += $nombre
        continue
    }
    if ($WhatIf) {
        "se subiria: $nombre"
        continue
    }
    $secrets[$nombre] | gh secret set $nombre
    if ($LASTEXITCODE -ne 0) {
        throw "Fallo 'gh secret set $nombre'."
    }
    "OK $nombre"
}

if ($sinValor.Count -gt 0) {
    "AVISO: sin valor en el origen (no se subieron): $($sinValor -join ', ')"
}
if (-not $WhatIf) {
    "Listo: secrets restaurados desde $EnvFile y $AwsCredentials (perfil [$AwsProfile])"
}
