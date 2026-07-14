# GestProy — Scripts SQL (PostgreSQL 16)

Todo el trabajo de base de datos del proyecto vive en esta carpeta como scripts
SQL versionados: DDL, triggers, funciones/procedimientos PL/pgSQL, vistas y
datos semilla. La aplicación Java (Spring Boot, JDBC sin ORM) **invoca** estas
funciones; nunca arma su propio DML para las operaciones con lógica de negocio.

## Cómo aplicar los scripts

1. Crear la base de datos (una sola vez):

   ```
   psql -U postgres -c "CREATE DATABASE gestion_proyectos ENCODING 'UTF8'"
   ```

2. Aplicar todo en orden con el script de PowerShell:

   ```powershell
   cd GestProy\db\scripts
   $env:PGPASSWORD = "tu_contrasena"
   .\apply-all.ps1
   ```

   O manualmente con `psql -d gestion_proyectos -v ON_ERROR_STOP=1 -f <archivo>`
   siguiendo el orden de la sección siguiente.

## Orden de ejecución

| # | Carpeta | Contenido |
|---|---------|-----------|
| 1 | `schema/01..04` | Las 15 tablas de negocio: referenciales (gzz_*), maestras (g1m_*), relación (g1c_per_car), transaccionales (g1t_*) |
| 2 | `schema/05` | Índices explícitos sobre columnas FK (PostgreSQL no indexa FKs automáticamente) |
| 3 | `schema/06` | `g1s_usuario`: la única cuenta admin del panel web (login + hash de contraseña con pgcrypto) |
| 4 | `triggers/` | 4 triggers: validación de autorización activa al asignar equipo, validación de miembro activo al registrar horas, autonumeración de `sec_etp`, coherencia de fechas de cabecera |
| 5 | `functions/1x` | Mantenimiento de referenciales: `sp_ref_grupoa_mant` (una función para las 6 tablas de forma idéntica) y las 3 del Grupo B (`sp_gzz_tip_pro_mant`, `sp_gzz_lin_pro_mant`, `sp_gzz_etp_pro_mant`) |
| 6 | `functions/2x` | Maestras: `sp_cliente_mant`, `sp_personal_mant`, `sp_per_car_mant` |
| 7 | `functions/3x-5x` | Negocio: `sp_proyecto_crear/editar/cambiar_estado`, `sp_proyecto_equipo_asignar/quitar/reactivar`, `fn_personal_disponible_proyecto`, `sp_proyecto_avance_registrar`, `fn_proyecto_pct_avance` |
| 8 | `functions/60` | `fn_usuario_autenticar`: verifica login+contraseña contra el hash de `g1s_usuario` (la usa el login de la app, no un CRUD de usuarios) |
| 9 | `views/` | `v_proyecto_resumen`, `v_proyecto_equipo`, `v_proyecto_avance` |
| 10 | `seed/01..03` | Datos semilla idempotentes (`ON CONFLICT DO NOTHING`): catálogos, maestras y un proyecto de ejemplo con equipo y movimientos |
| 11 | `seed/04` | Cuenta admin `admi` (solo el hash de su contraseña; ver comentario del archivo para cambiarla) |

## Tests de humo

`tests/smoke_tests.sql` verifica de punta a punta las reglas de negocio de la
BD: mantenimiento de referenciales/maestras, matriz de estados del proyecto,
triggers (autorización, autonumeración de `sec_etp`, fechas), registro de
avance, las 3 vistas y la autenticación de la cuenta admin
(`fn_usuario_autenticar`). Todo corre dentro de una transacción con `ROLLBACK`
final, así que **no modifica los datos** y puede ejecutarse contra la BD de
desarrollo tantas veces como se quiera:

```powershell
cd GestProy\db\scripts
$env:PGPASSWORD = "tu_contrasena"
.\run-tests.ps1
```

Cada test imprime `OK Txx: ...`; si algo está roto, el primer
`TEST FALLIDO ...` explica qué regla se violó y `psql` termina con código
distinto de 0 (los tests usan códigos `9xxx` que no chocan con datos reales).

## Convenciones

- **`sp_`** = función de escritura/mantenimiento; **`fn_`** = función de lectura/cálculo;
  **`trgfn_`/`trg_`** = función de trigger / trigger; **`v_`** = vista.
- Se usan `FUNCTION` (no `PROCEDURE`): cada operación es una sola transacción y
  se invocan de forma directa desde `JdbcTemplate` con `SELECT sp_x(...)`.
- **Errores**: toda validación fallida lanza `RAISE EXCEPTION` con mensaje en
  español; la aplicación lo recibe como `DataAccessException` y lo muestra al
  usuario tal cual.
- **Eliminación lógica** (patrón del curso): ninguna operación borra filas;
  el estado vive en la columna `*est_reg*` FK a `gzz_est_reg`
  (`'A'` activo, `'I'` inactivo, `'*'` eliminado).
- Operaciones de mantenimiento con el vocabulario del docente:
  `ADICIONAR`, `MODIFICAR`, `ELIMINAR`, `INACTIVAR`, `REACTIVAR`.

## Migración desde MySQL (sqllabbd.sql)

El esquema original (`sqllabbd.sql`, raíz del repo) fue generado por MySQL
Workbench. Reglas aplicadas en la conversión:

- Sin backticks; identificadores en `snake_case` minúsculas (abajo el mapeo).
- Tipos compatibles directos: `SMALLINT`/`INT`/`CHAR(n)`/`VARCHAR(n)`/`DECIMAL(p,s)`→`numeric`/`DATE`.
- `INDEX ... VISIBLE` inline (sintaxis MySQL) → `CREATE INDEX` explícitos en `schema/05_indices.sql`.
- Se eliminaron los pragmas de MySQL (`SET @OLD_...`, `SQL_MODE`, `FOREIGN_KEY_CHECKS`);
  cada archivo va envuelto en `BEGIN; ... COMMIT;`.
- `INSERT IGNORE` → `INSERT ... ON CONFLICT DO NOTHING` en los seeds.
- Ninguna PK usa `SERIAL`: los códigos los ingresa el usuario (como en el
  laboratorio original); la excepción es `pro_sec`, que calcula
  `sp_proyecto_crear` como `MAX+1` por (cliente, tipo).
- `gzz_est_reg.est_reg_est_reg` queda sin FK a sí misma (bootstrap del catálogo).

### Mapeo de nombres MySQL → PostgreSQL

| MySQL | PostgreSQL |
|-------|-----------|
| `GZZ_EST_REG` (`EstRegCod`, `EstRegDes`, `EstRegEstReg`) | `gzz_est_reg` (`est_reg_cod`, `est_reg_des`, `est_reg_est_reg`) |
| `GZZ_TIP_CLI` (`TipCliCod`, `TipCliDes`, `TipCliEstReg`) | `gzz_tip_cli` (`tip_cli_cod`, `tip_cli_des`, `tip_cli_est_reg`) |
| `GZZ_EST_CLI` (`EstCliCod`, `EstCliDes`, `EstCliEstReg`) | `gzz_est_cli` (`est_cli_cod`, `est_cli_des`, `est_cli_est_reg`) |
| `GZZ_LIN_PRO` (`LinProCod`, `LinProNom`, `LinProTam`, `LinProEstRegCod`) | `gzz_lin_pro` (`lin_pro_cod`, `lin_pro_nom`, `lin_pro_tam`, `lin_pro_est_reg_cod`) |
| `GZZ_TIP_PRO` (`TipProCod`, `TipProDes`, `TipProTam`, `TipProEstReg`) | `gzz_tip_pro` (`tip_pro_cod`, `tip_pro_des`, `tip_pro_tam`, `tip_pro_est_reg`) |
| `GZZ_EST_PRO` (`EstProCod`, `EstProDes`, `EstProEstReg`) | `gzz_est_pro` (`est_pro_cod`, `est_pro_des`, `est_pro_est_reg`) |
| `GZZ_CAR_PER` (`CarPerCod`, `CarPerDes`, `CarPerEstReg`) | `gzz_car_per` (`car_per_cod`, `car_per_des`, `car_per_est_reg`) |
| `GZZ_CAR_PRO` (`CarProCod`, `CarProDes`, `CarProEstReg`) | `gzz_car_pro` (`car_pro_cod`, `car_pro_des`, `car_pro_est_reg`) |
| `GZZ_ETP_PRO` (`EtpCod`, `EtpDes`, `EtpTieEst`, `EtpEstReg`) | `gzz_etp_pro` (`etp_cod`, `etp_des`, `etp_tie_est`, `etp_est_reg`) |
| `G1M_CLIENTES` (`CliCod`, `CliNom`, `CliTipCod`, `CliFecIng`, `CliFecCes`, `CliFecUltProCer`, `CliEstCod`, `CliEstRegCod`) | `g1m_clientes` (`cli_cod`, `cli_nom`, `cli_tip_cod`, `cli_fec_ing`, `cli_fec_ces`, `cli_fec_ult_pro_cer`, `cli_est_cod`, `cli_est_reg_cod`) |
| `G1M_PERSONAL` (`PerCod`, `PerNom`, `PerCarCod`, `PerCosHor`, `PerFecIng`, `PerEstReg`) | `g1m_personal` (`per_cod`, `per_nom`, `per_car_cod`, `per_cos_hor`, `per_fec_ing`, `per_est_reg_cod`) |
| `G1C_PER_CAR` (`PerCod`, `CarProCod`, `PerCarProEstReg`) | `g1c_per_car` (`per_cod`, `car_pro_cod`, `per_car_pro_est_reg_cod`) |
| `G1T_PRO_CAB` (`ProCliCod`, `ProTipCod`, `ProSec`, `ProFecCon`, `ProFecPac`, `ProFecIni`, `ProFecEnt`, `ProFecCer`, `ProMonPre`, `ProMonRea`, `ProCosPre`, `ProCosRea`, `ProGasPre`, `ProGasRea`, `ProUtiPre`, `ProUtiRea`, `ProEstCod`, `ProEstRegCod`) | `g1t_pro_cab` (`pro_cli_cod`, `pro_tip_cod`, `pro_sec`, `pro_fec_con`, `pro_fec_pac`, `pro_fec_ini`, `pro_fec_ent`, `pro_fec_cer`, `pro_mon_pre`, `pro_mon_rea`, `pro_cos_pre`, `pro_cos_rea`, `pro_gas_pre`, `pro_gas_rea`, `pro_uti_pre`, `pro_uti_rea`, `pro_est_cod`, `pro_est_reg_cod`) |
| `G1T_PRO_EQP` (`ProCliCod`, `ProTipCod`, `ProSec`, `PerCod`, `CarProCod`, `ProPerCarEstRegCod`) | `g1t_pro_eqp` (`pro_cli_cod`, `pro_tip_cod`, `pro_sec`, `per_cod`, `car_pro_cod`, `pro_per_car_est_reg_cod`) |
| `G1T_PRO_MOV` (`ProCliCod`, `ProTipCod`, `ProSec`, `PerCod`, `CarProCod`, `EtpCod`, `SecEtp`, `FecRegEtp`, `HorTraEtp`, `MinTraEtp`, `EstRegCod`) | `g1t_pro_mov` (`pro_cli_cod`, `pro_tip_cod`, `pro_sec`, `per_cod`, `car_pro_cod`, `etp_cod`, `sec_etp`, `fec_reg_etp`, `hor_tra_etp`, `min_tra_etp`, `est_reg_cod`) |
