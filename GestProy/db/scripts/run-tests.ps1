# ============================================================
#  GestProy — run-tests.ps1
#  Ejecuta los tests de humo de la base de datos
#  (db\tests\smoke_tests.sql) contra la BD indicada.
#
#  Los tests corren dentro de una transacción con ROLLBACK
#  final: NO modifican los datos de la base de datos.
#
#  Uso (desde GestProy\db\scripts):
#    $env:PGPASSWORD = "tu_contrasena"
#    .\run-tests.ps1
#  Parámetros opcionales: -DbHost, -DbPort, -DbUser, -DbName
#
#  Requiere haber aplicado antes los scripts con .\apply-all.ps1
# ============================================================
param(
    [string]$DbHost = "localhost",
    [int]   $DbPort = 5432,
    [string]$DbUser = "postgres",
    [string]$DbName = "gestion_proyectos"
)

$ErrorActionPreference = "Stop"
$dbDir = Split-Path -Parent $PSScriptRoot   # ...\GestProy\db
$tests = Join-Path $dbDir "tests\smoke_tests.sql"

Write-Host "==> Ejecutando tests de humo sobre '$DbName'..." -ForegroundColor Cyan
& psql -h $DbHost -p $DbPort -U $DbUser -d $DbName -v ON_ERROR_STOP=1 -f $tests
if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "TESTS FALLIDOS (código $LASTEXITCODE): revisar el primer 'TEST FALLIDO' de arriba." -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host ""
Write-Host "Tests completados sin errores; la BD no fue modificada (ROLLBACK)." -ForegroundColor Green
