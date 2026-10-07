# Normaliza data real previa al estandar del proyecto (nombres en Titulo con
# particulas y RUT sin puntos) en las bases de dominio y en usuarios.full_name.
#
# Las migraciones Flyway V7 (estudiantes/docentes), V5 (apoderados) y V9
# (usuarios) ya normalizan los datos de ejemplo en cualquier entorno. Este
# script es para filas NO seed creadas antes del cambio; se ejecuta una vez
# por entorno (lee .env para credenciales y usa los contenedores de docker).
#
# Uso:  powershell -ExecutionPolicy Bypass -File tools/normalizar-datos.ps1

[Console]::OutputEncoding = [Text.Encoding]::UTF8
$OutputEncoding = New-Object System.Text.UTF8Encoding($false)

$raiz = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $raiz '.env'
if (-not (Test-Path -LiteralPath $envPath)) {
    throw "No se encontro .env en $raiz"
}
$cfg = @{}
Get-Content -LiteralPath $envPath | Where-Object { $_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$' } |
    ForEach-Object { $cfg[$matches[1]] = $matches[2] }
$root = $cfg['MARIADB_ROOT_PASSWORD']

function Normalizar-Nombre([string]$texto) {
    if ($null -eq $texto) { return $null }
    $limpio = ($texto.Trim() -replace '\s+', ' ')
    if ($limpio -eq '') { return '' }
    $particulas = @('de', 'del', 'la', 'las', 'los', 'y', 'e')
    $palabras = $limpio.ToLower().Split(' ')
    $resultado = New-Object System.Collections.Generic.List[string]
    for ($i = 0; $i -lt $palabras.Count; $i++) {
        $palabra = $palabras[$i]
        if ($palabra -eq '') { continue }
        if ($particulas -contains $palabra) { $resultado.Add($palabra); continue }
        $sb = New-Object System.Text.StringBuilder
        $inicio = $true
        foreach ($ch in $palabra.ToCharArray()) {
            if ($ch -eq '-' -or $ch -eq "'" -or $ch -eq [char]0x2019) {
                [void]$sb.Append($ch); $inicio = $true; continue
            }
            if ($inicio) { [void]$sb.Append([char]::ToUpper($ch)) } else { [void]$sb.Append($ch) }
            $inicio = $false
        }
        $w = $sb.ToString()
        $lo = $w.ToLower()
        if ($lo.StartsWith('mc') -and $w.Length -gt 2) {
            $w = $w.Substring(0, 2) + [char]::ToUpper($w[2]) + $w.Substring(3)
        } elseif ($lo.StartsWith('mac') -and $w.Length -gt 3 -and 'aeiou'.IndexOf($lo[3]) -lt 0) {
            $w = $w.Substring(0, 3) + [char]::ToUpper($w[3]) + $w.Substring(4)
        }
        $resultado.Add($w)
    }
    return ($resultado -join ' ')
}

function Sql-Literal([string]$valor) {
    if ($null -eq $valor -or $valor -eq '') { return 'NULL' }
    return "'" + $valor.Replace("'", "''") + "'"
}

function Base64-Texto([string]$valor) {
    if ($valor -eq 'NULL') { return $null }
    if ($valor -eq '') { return '' }
    return [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($valor))
}

function Ejecutar-Sql([string]$contenedor, [string]$base, [System.Collections.Generic.List[string]]$sentencias) {
    if ($sentencias.Count -eq 0) { return }
    ($sentencias -join "`n") | docker exec -i -e MYSQL_PWD="$root" $contenedor mariadb -uroot -D $base --default-character-set=utf8mb4
}

$tablas = @(
    @{ cont = 'mariadb-estudiantes'; db = 'siga_estudiantes_db'; tabla = 'estudiantes' },
    @{ cont = 'mariadb-docentes';     db = 'siga_docentes_db';     tabla = 'docentes' },
    @{ cont = 'mariadb-apoderados';   db = 'siga_apoderados_db';   tabla = 'apoderados' }
)

$nombresPorOid = @{}
foreach ($t in $tablas) {
    $consulta = "SELECT id, CASE WHEN first_name IS NULL THEN 'NULL' ELSE REPLACE(TO_BASE64(first_name),CHAR(10),'') END, " +
        "CASE WHEN middle_name IS NULL THEN 'NULL' ELSE REPLACE(TO_BASE64(middle_name),CHAR(10),'') END, " +
        "CASE WHEN first_surname IS NULL THEN 'NULL' ELSE REPLACE(TO_BASE64(first_surname),CHAR(10),'') END, " +
        "CASE WHEN second_surname IS NULL THEN 'NULL' ELSE REPLACE(TO_BASE64(second_surname),CHAR(10),'') END, " +
        "CASE WHEN id_usuario IS NULL THEN 'NULL' ELSE REPLACE(TO_BASE64(id_usuario),CHAR(10),'') END " +
        "FROM $($t.db).$($t.tabla);"
    $filas = docker exec -e MYSQL_PWD="$root" $t.cont mariadb -uroot -N --default-character-set=utf8mb4 -e $consulta
    $sentencias = New-Object System.Collections.Generic.List[string]
    foreach ($linea in $filas) {
        $p = $linea -split "`t"
        if ($p.Count -lt 6) { continue }
        $n1 = Normalizar-Nombre (Base64-Texto $p[1])
        $n2 = Normalizar-Nombre (Base64-Texto $p[2])
        $a1 = Normalizar-Nombre (Base64-Texto $p[3])
        $a2 = Normalizar-Nombre (Base64-Texto $p[4])
        $oid = Base64-Texto $p[5]
        if ($oid) {
            $partes = @($n1, $n2, $a1, $a2) | Where-Object { $_ }
            if ($partes.Count -gt 0) { $nombresPorOid[$oid] = ($partes -join ' ') }
        }
        $sentencias.Add("UPDATE $($t.tabla) SET first_name=$(Sql-Literal $n1), middle_name=$(Sql-Literal $n2), first_surname=$(Sql-Literal $a1), second_surname=$(Sql-Literal $a2) WHERE id=$($p[0]);")
    }
    $sentencias.Add("UPDATE $($t.tabla) SET rut = REPLACE(rut, '.', '') WHERE rut LIKE '%.%';")
    Ejecutar-Sql $t.cont $t.db $sentencias
    Write-Host "Normalizado: $($t.db).$($t.tabla) ($($sentencias.Count - 1) filas)"
}

$usuarios = docker exec -e MYSQL_PWD="$root" mariadb-usuarios mariadb -uroot -N --default-character-set=utf8mb4 -e "SELECT CASE WHEN id IS NULL THEN 'NULL' ELSE REPLACE(TO_BASE64(id),CHAR(10),'') END, CASE WHEN email IS NULL THEN 'NULL' ELSE REPLACE(TO_BASE64(email),CHAR(10),'') END FROM siga_usuarios_db.usuarios;"
$sentenciasUsuarios = New-Object System.Collections.Generic.List[string]
foreach ($linea in $usuarios) {
    $p = $linea -split "`t"
    if ($p.Count -lt 2) { continue }
    $oid = Base64-Texto $p[0]
    $email = Base64-Texto $p[1]
    if ($oid -and $email -and $nombresPorOid.ContainsKey($oid)) {
        $sentenciasUsuarios.Add("UPDATE usuarios SET full_name=$(Sql-Literal $nombresPorOid[$oid]) WHERE email=$(Sql-Literal $email);")
    }
}
$sentenciasUsuarios.Add("UPDATE usuarios SET full_name='Administrador' WHERE rol='ADMIN' AND full_name IS NULL;")
Ejecutar-Sql 'mariadb-usuarios' 'siga_usuarios_db' $sentenciasUsuarios
Write-Host "Normalizado: siga_usuarios_db.usuarios ($($sentenciasUsuarios.Count) sentencias)"
Write-Host 'Listo.'
