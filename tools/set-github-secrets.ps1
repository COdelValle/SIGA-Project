# Restaura TODOS los secrets requeridos por el CD, sin imprimir valores.
#
# Fuentes:
#  - AWS_*: ~/.aws/credentials (perfil -AwsProfile; por defecto "default").
#  - EC2_USER / EC2_SSH_KEY: parametros (-Ec2KeyPath para el PEM).
#  - Configuracion general y flags: .env del repo.
#  - Secretos ESPECIFICOS DEL DESPLIEGUE (DB, RabbitMQ, Azure, cred key): NO
#    tienen por que coincidir con el .env local (que es de desarrollo). Se
#    prefieren desde -RecoveryJson (dump de la env de los contenedores vigentes,
#    ver mas abajo) y si falta el valor ahi se usa el .env con advertencia.
#
# Dump de recuperacion (en la EC2, con los contenedores corriendo):
#   for c in bff-web ms-usuarios-auth ms-notas ms-notificaciones; do
#     docker inspect "$c" --format '{{json .Config.Env}}'
#   done > recuperacion.json
#
# Uso (desde la raiz del repo):
#   powershell -ExecutionPolicy Bypass -File tools/set-github-secrets.ps1
#   ... -Ec2KeyPath "$env:USERPROFILE\Downloads\Vockey.pem"
#   ... -RecoveryJson "C:\ruta\recuperacion.json"
#   ... -AwsProfile siga
#   ... -WhatIf
[CmdletBinding()]
param(
    [string]$EnvFile = '',
    [string]$AwsCredentials = '',
    [string]$AwsProfile = 'default',
    [string]$AwsRegion = 'us-east-1',
    [string]$Ec2User = 'ubuntu',
    [string]$Ec2KeyPath = '',
    [string]$RecoveryJson = '',
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
if ($RecoveryJson -and -not (Test-Path -LiteralPath $RecoveryJson)) {
    throw "No existe el dump de recuperacion: $RecoveryJson"
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

# Lee el dump de `docker inspect ... {{json .Config.Env}}` (una linea por
# contenedor) y devuelve todas las claves encontradas.
function Leer-DumpContenedores([string]$ruta) {
    $mapa = @{}
    foreach ($linea in Get-Content -LiteralPath $ruta) {
        if (-not $linea.Trim()) { continue }
        foreach ($entrada in (ConvertFrom-Json $linea)) {
            $partes = $entrada -split '=', 2
            if ($partes.Count -eq 2) {
                $mapa[$partes[0]] = $partes[1]
            }
        }
    }
    return $mapa
}

$envVars = Leer-ClaveValor $EnvFile ''
$aws = Leer-ClaveValor $AwsCredentials $AwsProfile
$dump = @{}
if ($RecoveryJson) {
    $dump = Leer-DumpContenedores $RecoveryJson
}

if (-not $aws['aws_access_key_id']) {
    throw "El perfil [$AwsProfile] no tiene aws_access_key_id en $AwsCredentials"
}

$avisos = New-Object System.Collections.Generic.List[string]

# Prefiere el valor del despliegue (dump); si no esta, el del .env con aviso.
function Elegir-Valor([string]$nombre, [string]$delDump, [string]$delEnv) {
    if (-not [string]::IsNullOrWhiteSpace($delDump)) { return $delDump }
    if (-not [string]::IsNullOrWhiteSpace($delEnv)) {
        $script:avisos.Add("$nombre se tomo del .env LOCAL (no estaba en el dump): verifica que sea el valor del despliegue")
        return $delEnv
    }
    return ''
}

$secrets = [ordered]@{
    'AWS_ACCESS_KEY_ID'                   = $aws['aws_access_key_id']
    'AWS_SECRET_ACCESS_KEY'               = $aws['aws_secret_access_key']
    'AWS_SESSION_TOKEN'                   = $aws['aws_session_token']
    'AWS_REGION'                          = $AwsRegion
    'EC2_USER'                            = $Ec2User
    'DB_USER'                             = Elegir-Valor 'DB_USER' $dump['SPRING_DATASOURCE_USERNAME'] $envVars['DB_USER']
    'DB_PASSWORD'                         = Elegir-Valor 'DB_PASSWORD' $dump['SPRING_DATASOURCE_PASSWORD'] $envVars['DB_PASS']
    'MARIADB_ROOT_PASSWORD'               = Elegir-Valor 'MARIADB_ROOT_PASSWORD' '' $envVars['MARIADB_ROOT_PASSWORD']
    'AZURE_TENANT_ID'                     = Elegir-Valor 'AZURE_TENANT_ID' $dump['AZURE_TENANT_ID'] $envVars['AZURE_TENANT_ID']
    'AZURE_CLIENT_ID'                     = Elegir-Valor 'AZURE_CLIENT_ID' $dump['AZURE_CLIENT_ID'] $envVars['AZURE_CLIENT_ID']
    'AZURE_APP_ID_URI'                    = Elegir-Valor 'AZURE_APP_ID_URI' $dump['AZURE_APP_ID_URI'] $envVars['AZURE_APP_ID_URI']
    'AZURE_CLIENT_SECRET'                 = Elegir-Valor 'AZURE_CLIENT_SECRET' $dump['AZURE_CLIENT_SECRET'] $envVars['AZURE_CLIENT_SECRET']
    'AZURE_API_APP_ID'                    = Elegir-Valor 'AZURE_API_APP_ID' $dump['AZURE_API_APP_ID'] $envVars['AZURE_API_APP_ID']
    'RABBITMQ_USER'                       = Elegir-Valor 'RABBITMQ_USER' $dump['SPRING_RABBITMQ_USERNAME'] $envVars['RABBITMQ_USER']
    'RABBITMQ_PASSWORD'                   = Elegir-Valor 'RABBITMQ_PASSWORD' $dump['SPRING_RABBITMQ_PASSWORD'] $envVars['RABBITMQ_PASS']
    'RABBITMQ_ERLANG_COOKIE'              = Elegir-Valor 'RABBITMQ_ERLANG_COOKIE' '' $envVars['RABBITMQ_ERLANG_COOKIE']
    'REGISTRO_ASYNC_ENABLED'              = $envVars['REGISTRO_ASYNC_ENABLED']
    'REGISTRO_CRED_KEY'                   = Elegir-Valor 'REGISTRO_CRED_KEY' $dump['REGISTRO_CRED_KEY'] $envVars['REGISTRO_CRED_KEY']
    'REGISTRO_NOTIFY_CREDENTIALS_ENABLED' = $envVars['REGISTRO_NOTIFY_CREDENTIALS_ENABLED']
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

if ($avisos.Count -gt 0) {
    'ADVERTENCIAS:'
    $avisos | ForEach-Object { " - $_" }
}
if ($sinValor.Count -gt 0) {
    "SIN VALOR en el origen (no se subieron): $($sinValor -join ', ')"
}
if (-not $WhatIf) {
    "Listo: secrets restaurados (AWS: perfil [$AwsProfile]; dump: $(if ($RecoveryJson) { $RecoveryJson } else { 'no indicado' }))"
}
