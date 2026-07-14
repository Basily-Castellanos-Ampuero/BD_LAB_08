# ============================================================
#  GestProy — apply-all.ps1
#  Aplica todos los scripts SQL en orden contra la base de
#  datos gestion_proyectos usando psql.
#
#  Uso (desde GestProy\db\scripts):
#    $env:PGPASSWORD = "tu_contrasena"
#    .\apply-all.ps1
#  Parámetros opcionales: -DbHost, -DbPort, -DbUser, -DbName
#
#  Requiere que la BD ya exista:
#    psql -U postgres -c "CREATE DATABASE gestion_proyectos ENCODING 'UTF8'"
# ============================================================
param(
    [string]$DbHost = "localhost",
    [int]   $DbPort = 5432,
    [string]$DbUser = "postgres",
    [string]$DbName = "gestion_proyectos"
)

$ErrorActionPreference = "Stop"
$dbDir = Split-Path -Parent $PSScriptRoot   # ...\GestProy\db

# Orden de ejecución: esquema -> triggers -> funciones -> vistas -> seed
$archivos = @(
    "schema\01_referenciales.sql",
    "schema\02_maestras.sql",
    "schema\03_relacion.sql",
    "schema\04_transaccionales.sql",
    "schema\05_indices.sql",
    "schema\06_usuario.sql",
    "triggers\01_trg_proeqp_valida_percar_activo.sql",
    "triggers\02_trg_promov_valida_eqp_activo.sql",
    "triggers\03_trg_promov_autonumera_sec_etp.sql",
    "triggers\04_trg_procab_valida_fechas.sql",
    "functions\10_sp_ref_grupoa_mant.sql",
    "functions\11_sp_ref_grupob_mant.sql",
    "functions\20_sp_cliente_mant.sql",
    "functions\21_sp_personal_mant.sql",
    "functions\22_sp_per_car_mant.sql",
    "functions\30_sp_proyecto_crear.sql",
    "functions\31_sp_proyecto_editar.sql",
    "functions\32_sp_proyecto_cambiar_estado.sql",
    "functions\40_sp_proyecto_equipo_asignar.sql",
    "functions\41_sp_proyecto_equipo_quitar_reactivar.sql",
    "functions\42_fn_personal_disponible_proyecto.sql",
    "functions\50_sp_proyecto_avance_registrar.sql",
    "functions\51_fn_proyecto_pct_avance.sql",
    "functions\60_fn_usuario_autenticar.sql",
    "views\v_proyecto_resumen.sql",
    "views\v_proyecto_equipo.sql",
    "views\v_proyecto_avance.sql",
    "seed\01_seed_referenciales.sql",
    "seed\02_seed_maestras.sql",
    "seed\03_seed_transaccional.sql",
    "seed\04_seed_usuario.sql"
)

foreach ($archivo in $archivos) {
    $ruta = Join-Path $dbDir $archivo
    Write-Host "==> $archivo" -ForegroundColor Cyan
    & psql -h $DbHost -p $DbPort -U $DbUser -d $DbName -v ON_ERROR_STOP=1 -q -f $ruta
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR al aplicar $archivo (código $LASTEXITCODE)" -ForegroundColor Red
        exit $LASTEXITCODE
    }
}

Write-Host ""
Write-Host "Todos los scripts se aplicaron correctamente sobre '$DbName'." -ForegroundColor Green
