[CmdletBinding()]
param(
    [string]$BaseUrl = 'https://87hjovxzud.execute-api.us-east-1.amazonaws.com',
    [string]$Token = $env:SIGA_BEARER_TOKEN
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

function Get-HttpStatus {
    param(
        [string]$Path,
        [hashtable]$Headers = @{}
    )

    try {
        $response = Invoke-WebRequest -Uri "$BaseUrl$Path" -Headers $Headers -Method Get -UseBasicParsing -ErrorAction Stop
        return [int]$response.StatusCode
    } catch {
        if ($_.Exception.Response) {
            return [int]$_.Exception.Response.StatusCode
        }
        throw
    }
}

$segments = $Token.Split('.')
if ($segments.Length -ne 3) {
    throw 'El token no tiene formato JWT.'
}

$payload = $segments[1].Replace('-', '+').Replace('_', '/')
$payload = $payload.PadRight([int][Math]::Ceiling($payload.Length / 4) * 4, '=')
$claims = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($payload)) | ConvertFrom-Json
$now = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()

"iss=$($claims.iss)"
"aud=$($claims.aud -join ',')"
"scp=$($claims.scp)"
"roles=$($claims.roles -join ',')"
"exp_remaining_seconds=$([int]$claims.exp - $now)"

$sinToken = Get-HttpStatus '/api/me'
$tokenInvalido = Get-HttpStatus '/api/me' @{ Authorization = 'Bearer token-invalido' }
$headers = @{ Authorization = "Bearer $Token" }
$meStatus = Get-HttpStatus '/api/me' $headers
$adminStatus = Get-HttpStatus '/api/bff/v1/admin/rabbitmq/queues/cola-notificaciones-notas' $headers

"sin_token=$sinToken"
"token_invalido=$tokenInvalido"
"me=$meStatus"
"admin_rabbitmq=$adminStatus"

if ($meStatus -eq 200) {
    $me = Invoke-RestMethod -Uri "$BaseUrl/api/me" -Headers $headers -Method Get -ErrorAction Stop
    "me_roles=$($me.roles -join ',')"
}

if ($sinToken -ne 401 -or $tokenInvalido -ne 401 -or $meStatus -ne 200) {
    exit 1
}
