# GestProy — Métodos y Lógica a Profundidad

> Catálogo exhaustivo de cada función SQL, trigger, vista y método Java del
> proyecto: qué recibe, qué valida, qué hace y qué puede fallar. Si el
> [`00-RESUMEN.md`](00-RESUMEN.md) responde "qué es GestProy", este documento
> responde "qué hace exactamente cada pieza de código".
>
> Convención de esta sección: **§A** = capa SQL/PL-pgSQL (donde vive la
> lógica de negocio), **§B** = capa Java (donde vive la invocación),
> **§C** = capa de seguridad (login/JWT, la única lógica que sí vive en
> Java), **§D** = tabla cruzada Java → SQL.

---

## §A. Capa SQL — PL/pgSQL

### A.1 Vistas (`db/views/`)

#### `v_proyecto_resumen`
Listado de proyectos con nombres legibles en vez de códigos.

- **Fuente**: `g1t_pro_cab` JOIN `g1m_clientes`, `gzz_tip_pro`, `gzz_est_pro`.
- **Columnas añadidas**: `cliente_nombre`, `tipo_descripcion`, `estado_descripcion`.
- **No filtra por `est_reg`**: intencional — la UI necesita ver también los
  proyectos inactivos/eliminados para poder reactivarlos desde el listado.
- **Consumida por**: `ProyectoDao.listar()` / `ProyectoDao.buscar()`.

#### `v_proyecto_equipo`
Equipo asignado a cada proyecto, con nombre de persona y descripción de cargo.

- **Fuente**: `g1t_pro_eqp` JOIN `g1m_personal`, `gzz_car_pro`.
- **Columna calculada**: `estado_descripcion` = `CASE pro_per_car_est_reg_cod
  WHEN 'A' THEN 'Activo' WHEN 'I' THEN 'Retirado' ELSE 'Eliminado' END`.
- Incluye `per_cos_hor` (costo/hora de la persona) para futuros cálculos de
  costo real del equipo.
- **Consumida por**: `ProyectoEquipoDao.listar()`.

#### `v_proyecto_avance`
Vista central del módulo de avance: compara horas trabajadas contra horas
estimadas.

- **`horas_estimadas`**: suma de `etp_tie_est` de las etapas **activas** del
  catálogo global `gzz_etp_pro` (subconsulta independiente del proyecto —
  ver la decisión de diseño en el §A.3, `fn_proyecto_pct_avance`).
- **`horas_trabajadas`**: `SUM(hor_tra_etp + min_tra_etp/60.0)` de los
  movimientos **activos** (`est_reg_cod = 'A'`) del proyecto, vía `LEFT
  JOIN` (un proyecto sin movimientos da 0, no desaparece de la vista).
- **`pct_avance`**: delega en la función `fn_proyecto_pct_avance(...)` para
  no duplicar la fórmula entre la vista y la función invocable directamente.
- **Consumida por**: `ProyectoAvanceDao.resumen()`.

---

### A.2 Triggers (`db/triggers/`)

Los 4 triggers son la "red de seguridad" a nivel de tabla: las funciones de
negocio ya validan antes de escribir (para dar mensajes de error específicos),
pero los triggers garantizan la regla **aunque alguien inserte directamente**
con SQL, sin pasar por las funciones.

#### 1. `trg_proeqp_valida_percar_activo` — `BEFORE INSERT OR UPDATE ON g1t_pro_eqp`
- **Qué evita**: que alguien entre al equipo con un cargo que la FK permite
  (existe la fila en `g1c_per_car`) pero que **no está activo**.
- **Lógica**: solo se dispara si `NEW.pro_per_car_est_reg_cod = 'A'` — es
  decir, al asignar o reactivar. Si se está retirando (`'I'`) o eliminando
  (`'*'`) a alguien, no exige nada (permite retirar aunque la autorización
  ya haya caducado).
- **Error**: `'La persona % no tiene autorizado (activo) el cargo de
  proyecto %'`.

#### 2. `trg_promov_valida_eqp_activo` — `BEFORE INSERT ON g1t_pro_mov`
- **Qué evita**: registrar horas de alguien que ya no está activamente en
  el equipo del proyecto (`g1t_pro_eqp.pro_per_car_est_reg_cod <> 'A'`).
- **Por qué existe pese a la FK**: la FK `fk_promov_eqp` solo exige que la
  fila de equipo *exista*, no que su estado sea `'A'`.
- **Error**: `'La persona % no está activamente asignada al proyecto
  (%,%,%) con el cargo %'`.

#### 3. `trg_promov_autonumera_sec_etp` — `BEFORE INSERT ON g1t_pro_mov`
- **Qué hace**: si `NEW.sec_etp IS NULL OR NEW.sec_etp = 0`, calcula
  `COALESCE(MAX(sec_etp), 0) + 1` para la combinación (proyecto, persona,
  cargo, etapa) y lo asigna a `NEW.sec_etp` **antes** de que se evalúen el
  `NOT NULL` y la restricción de PK.
- **Por qué**: `sec_etp` es el último componente de una PK compuesta de 7
  columnas; la aplicación no necesita (ni debe) calcularlo — lo hace la BD.
- **Nota de diseño**: usa `MAX+1` sin bloqueo explícito (`FOR UPDATE`), por
  lo que dos inserciones concurrentes sobre la misma combinación podrían
  colisionar en la PK. Ver `03-DOCUMENTACION-GENERAL.md` § Concurrencia.

#### 4. `trg_procab_valida_fechas` — `BEFORE INSERT OR UPDATE ON g1t_pro_cab`
- **Qué valida** (solo si ambos extremos no son `NULL`):
  1. `pro_fec_con <= pro_fec_pac` (contrato no posterior a lo pactado)
  2. `pro_fec_ini <= pro_fec_ent` (inicio no posterior a la entrega)
  3. `pro_fec_ini <= pro_fec_cer` (inicio no posterior al cierre)
- **Hueco conocido** (ver auditoría): no valida `pro_fec_ent <= pro_fec_cer`
  ni `pro_fec_con <= pro_fec_ini`. No es un bug de sintaxis, es una regla
  que falta agregar si se quiere blindar del todo la coherencia temporal.

---

### A.3 Funciones — mantenimiento de referenciales (`functions/10_*`, `functions/11_*`)

#### `sp_ref_grupoa_mant(p_tabla TEXT, p_operacion TEXT, p_cod VARCHAR, p_des VARCHAR) RETURNS void`
Una sola función para las **6 tablas del Grupo A** (`gzz_est_reg`,
`gzz_tip_cli`, `gzz_est_cli`, `gzz_est_pro`, `gzz_car_per`, `gzz_car_pro`),
todas con la forma idéntica `cod/des/est_reg`.

- **Por qué una sola función y no 6**: el SQL de cada tabla es literal en
  una rama `IF p_tabla = '...' THEN ... ELSIF ...` — **no** hay `EXECUTE
  format()` dinámico. Esto es deliberado: evita inyección SQL por nombre de
  tabla y mantiene cada sentencia visible y auditable, al costo de código
  repetitivo (6 ramas casi idénticas).
- **`p_operacion`** acepta: `ADICIONAR`, `MODIFICAR`, `ELIMINAR`,
  `INACTIVAR`, `REACTIVAR` (cualquier otro valor → `RAISE EXCEPTION`).
- **Validaciones comunes** (antes de entrar a las ramas por tabla):
  - `p_cod` no puede ser `NULL` ni vacío.
  - En `ADICIONAR`/`MODIFICAR`, `p_des` no puede ser `NULL` ni vacío.
- **Por operación**:
  | Operación | Efecto |
  |---|---|
  | `ADICIONAR` | `INSERT` con `est_reg = 'A'`; error si `p_cod` ya existe |
  | `MODIFICAR` | `UPDATE` de la descripción; error si `p_cod` no existe |
  | `ELIMINAR` | `UPDATE est_reg = '*'` |
  | `INACTIVAR` | `UPDATE est_reg = 'I'` |
  | `REACTIVAR` | `UPDATE est_reg = 'A'` |
- **Nota de riesgo conocida**: nada impide `ELIMINAR`/`INACTIVAR` sobre los
  códigos base `'A'/'I'/'*'` de `gzz_est_reg`, lo que corrompe el mecanismo
  de eliminación lógica de todo el esquema (ver auditoría de seguridad).

#### `sp_gzz_tip_pro_mant(p_operacion, p_cod SMALLINT, p_des VARCHAR, p_tam CHAR) RETURNS void`
Grupo B — `gzz_tip_pro`. Mismo ciclo ADICIONAR/MODIFICAR/ELIMINAR/INACTIVAR/REACTIVAR,
más: `p_tam` debe ser `'P'`, `'M'` o `'G'` (si no, error).

#### `sp_gzz_lin_pro_mant(p_operacion, p_cod SMALLINT, p_nom VARCHAR, p_tam CHAR) RETURNS void`
Grupo B — `gzz_lin_pro`. Igual patrón; valida `p_nom` no vacío y `p_tam ∈ {P,M,G}`.

#### `sp_gzz_etp_pro_mant(p_operacion, p_cod SMALLINT, p_des VARCHAR, p_tie_est NUMERIC) RETURNS void`
Grupo B — `gzz_etp_pro`. Valida `p_tie_est > 0` (el tiempo estimado de una
etapa no puede ser cero ni negativo, porque es el denominador del % de avance).

---

### A.4 Funciones — mantenimiento de maestras (`functions/20-22`)

#### `sp_cliente_mant(...) RETURNS void`
```
(p_operacion, p_cod, p_nom=NULL, p_tip_cod=NULL, p_est_cod=NULL,
 p_fec_ing=NULL, p_fec_ces=NULL, p_fec_ult_pro_cer=NULL)
```
- **En ADICIONAR/MODIFICAR** valida:
  - `p_nom` no vacío.
  - `p_tip_cod` existe y está **activo** en `gzz_tip_cli`.
  - `p_est_cod` existe y está **activo** en `gzz_est_cli`.
  - `p_fec_ing <= p_fec_ces` (si ambas no son `NULL`).
- **ADICIONAR**: error si `p_cod` ya existe; inserta con `cli_est_reg_cod='A'`.
- **MODIFICAR**: reemplaza todos los campos editables (no toca `cli_cod`).
- **ELIMINAR/INACTIVAR/REACTIVAR**: solo cambia `cli_est_reg_cod`.
- **Nota**: no verifica si el cliente tiene proyectos activos antes de
  ELIMINAR/INACTIVAR (hueco conocido, ver auditoría).

#### `sp_personal_mant(...) RETURNS void`
```
(p_operacion, p_cod, p_nom=NULL, p_car_cod=NULL, p_cos_hor=NULL, p_fec_ing=NULL)
```
- **En ADICIONAR/MODIFICAR** valida:
  - `p_nom` no vacío.
  - `p_car_cod` existe y está activo en `gzz_car_per`.
  - `p_cos_hor > 0`.
  - `p_fec_ing` no es `NULL` y no es futura (`<= CURRENT_DATE`).
- Mismo patrón ADICIONAR/MODIFICAR/ELIMINAR/INACTIVAR/REACTIVAR que clientes.
- **Nota**: eliminar/inactivar personal no lo retira de los equipos donde ya
  esté asignado activo (hueco conocido — puede seguir registrando horas).

#### `sp_per_car_mant(p_operacion, p_per_cod INTEGER, p_car_pro_cod SMALLINT) RETURNS void`
Mantenimiento de `g1c_per_car` (autorización persona↔cargo). Solo admite
`ADICIONAR`, `ELIMINAR`, `INACTIVAR`, `REACTIVAR` (**no** `MODIFICAR`: la PK
compuesta *es* el dato, no hay nada más que editar).

- **ADICIONAR**:
  1. Valida que la persona exista y esté activa (`g1m_personal`).
  2. Valida que el cargo de proyecto exista y esté activo (`gzz_car_pro`).
  3. Si la fila `(per_cod, car_pro_cod)` **no existe** → `INSERT` con `est_reg='A'`.
  4. Si existe con `est_reg='A'` → error ("ya tiene autorizado el cargo").
  5. Si existe con `est_reg IN ('I','*')` → **se reactiva** con `UPDATE`
     (no se re-inserta): evita el error de PK duplicada al querer "volver a
     dar de alta" algo que solo estaba inactivo.
- **ELIMINAR/INACTIVAR/REACTIVAR**: exige que la fila ya exista; cambia
  `per_car_pro_est_reg_cod` al valor correspondiente.

---

### A.5 Funciones — negocio de proyectos (`functions/30-51`)

#### `sp_proyecto_crear(p_cli_cod, p_tip_cod, p_fec_con=NULL, p_fec_pac=NULL, p_mon_pre=NULL, p_cos_pre=NULL, p_gas_pre=NULL) RETURNS SMALLINT`
Crea la cabecera (`g1t_pro_cab`) y retorna el `pro_sec` generado.

- Valida cliente activo y tipo de proyecto activo.
- **Cálculo de secuencia**: `pro_sec = COALESCE(MAX(pro_sec), 0) + 1` para
  `(pro_cli_cod, pro_tip_cod)`. Es "el proyecto N de este tipo para este
  cliente" — por eso no se usa `SERIAL` (que sería global, no por cliente+tipo).
- **Utilidad presupuestada**: `pro_uti_pre = monto - costo - gasto` (solo si
  `p_mon_pre` no es `NULL`); nunca se ingresa a mano.
- Inserta con `pro_est_cod = '01'` (Planificado) y `pro_est_reg_cod = 'A'`.
- La coherencia de fechas la valida el trigger `trg_procab_valida_fechas`,
  no esta función (evita duplicar la regla).
- **Riesgo de concurrencia**: el cálculo de secuencia no usa bloqueo; dos
  llamadas simultáneas para el mismo cliente+tipo podrían calcular el mismo
  `pro_sec` y una de las dos fallaría por PK duplicada (no corrupción de
  datos, solo un error que el usuario vería y podría reintentar).

#### `sp_proyecto_editar(p_cli_cod, p_tip_cod, p_sec, ...) RETURNS void`
Actualiza fechas y montos de una cabecera **no cerrada**.

- Falla si el proyecto no existe, no está activo (`est_reg`), o está
  `pro_est_cod = '04'` (Cerrado).
- Recalcula `pro_uti_pre` y `pro_uti_rea` igual que en la creación
  (monto − costo − gasto); nunca se reciben como parámetro directo.
- **No valida signo de montos**: acepta negativos (hueco conocido).

#### `sp_proyecto_cambiar_estado(p_cli_cod, p_tip_cod, p_sec, p_nuevo_est_cod) RETURNS void`
El corazón del ciclo de vida del proyecto. Estados:
`01 Planificado · 02 En Ejecución · 03 Entregado · 04 Cerrado · 05 Suspendido`.

- Falla si el proyecto no existe, no está activo, el estado destino no
  existe/no está activo, o el destino es igual al estado actual.
- **Matriz de transiciones permitidas** (cualquier otra combinación → error):

  | Desde | Hacia |
  |---|---|
  | `01` Planificado | `02` En Ejecución, `05` Suspendido |
  | `02` En Ejecución | `03` Entregado, `05` Suspendido |
  | `05` Suspendido | `02` En Ejecución |
  | `03` Entregado | `04` Cerrado |

- **Efectos secundarios automáticos** (solo si el campo de fecha está `NULL`):
  - → `02`: fija `pro_fec_ini = CURRENT_DATE` si no estaba fijada.
  - → `03`: fija `pro_fec_ent = CURRENT_DATE`.
  - → `04`: fija `pro_fec_cer = CURRENT_DATE` **y** actualiza
    `g1m_clientes.cli_fec_ult_pro_cer` del cliente dueño del proyecto.
- Este es el único lugar de todo el sistema donde una operación sobre
  `g1t_pro_cab` también escribe en `g1m_clientes` — documentado aquí porque
  no es obvio a simple vista leyendo solo el nombre de la función.

#### `sp_proyecto_equipo_asignar(p_cli_cod, p_tip_cod, p_sec, p_per_cod, p_car_pro_cod) RETURNS void`
- Falla si el proyecto no existe, no está activo, o está cerrado (`'04'`).
- Falla si `(p_per_cod, p_car_pro_cod)` no está **activo** en `g1c_per_car`
  (misma regla que impone el trigger `trg_proeqp_valida_percar_activo` —
  aquí se valida primero para dar un mensaje más específico; el trigger
  queda como red de seguridad).
- Si la fila de equipo no existe → `INSERT` con `est_reg='A'`.
- Si existe activa → error ("ya está asignada").
- Si existe inactiva/eliminada → se **reactiva** con `UPDATE` (mismo patrón
  que `sp_per_car_mant`: nunca se re-inserta sobre una PK que ya existió).

#### `sp_proyecto_equipo_quitar(...) RETURNS void` / `sp_proyecto_equipo_reactivar(...) RETURNS void`
Par de funciones complementarias sobre `g1t_pro_eqp`:
- `quitar`: exige que la fila exista y esté `'A'`; la pasa a `'I'` (baja
  lógica — el historial de movimientos de esa persona se conserva intacto).
- `reactivar`: exige que exista y **no** esté `'A'`; la pasa a `'A'`. El
  trigger `trg_proeqp_valida_percar_activo` vuelve a comprobar en este punto
  que la autorización en `g1c_per_car` siga activa (pudo haber caducado
  mientras la persona estaba retirada).

#### `fn_personal_disponible_proyecto(p_cli_cod, p_tip_cod, p_sec) RETURNS TABLE(per_cod, per_nom, car_pro_cod, car_pro_des)`
Función de **lectura** (`LANGUAGE sql STABLE`, no `plpgsql`): alimenta el
`<select>` del formulario "Asignar persona".

- Criterio: personas con autorización activa en `g1c_per_car`, personal
  activo, y que **no** estén ya asignadas activamente a este proyecto con
  ese mismo cargo (las asignaciones retiradas sí se ofrecen — al elegirlas
  de nuevo, `sp_proyecto_equipo_asignar` las reactiva en vez de duplicar).

#### `sp_proyecto_avance_registrar(p_cli_cod, p_tip_cod, p_sec, p_per_cod, p_car_pro_cod, p_etp_cod, p_fec_reg, p_hor_tra, p_min_tra) RETURNS SMALLINT`
Inserta un movimiento en `g1t_pro_mov` y retorna el `sec_etp` asignado.

- Valida: proyecto existente/activo/no cerrado; etapa existente y activa;
  persona+cargo **activamente** asignados al proyecto (mismo criterio que
  el trigger `trg_promov_valida_eqp_activo`, validado antes para dar un
  mensaje más claro); `p_fec_reg` no nula y no futura; `p_hor_tra ∈ [0,23]`;
  `p_min_tra ∈ [0,59]`; el par (horas, minutos) no puede ser `(0,0)`.
- El `INSERT` pasa `sec_etp = NULL`: lo calcula el trigger
  `trg_promov_autonumera_sec_etp` antes de que se apliquen las restricciones.
- **Nota**: valida rangos en la función, pero la tabla `g1t_pro_mov` **no
  tiene `CHECK`** — un `INSERT` directo con horas fuera de rango pasaría sin
  error (hueco conocido: la protección solo existe si se entra por esta función).

#### `fn_proyecto_pct_avance(p_cli_cod, p_tip_cod, p_sec) RETURNS NUMERIC`
Función de lectura (`LANGUAGE sql STABLE`):

```
pct = ROUND(
  SUM(horas trabajadas en movimientos activos del proyecto)
  / NULLIF(SUM(etp_tie_est de TODAS las etapas activas del catálogo), 0)
  * 100
, 2)
```

- **Decisión de diseño explícita** (confirmada en el código fuente):
  `gzz_etp_pro` es un catálogo único y global — no existe un "plan de
  etapas por proyecto". El denominador es el mismo para todos los
  proyectos del sistema. Esto simplifica el modelo a costa de no poder
  tener, por ejemplo, un proyecto que solo pase por 3 de las 6 etapas.
- Puede superar el 100% si se trabajó más horas de las estimadas (se
  muestra en la UI, ver `ProyectoAvance.excedido()` en §B.1).
- Sin etapas activas → división entre `NULLIF(...,0)` → `NULL` →
  `COALESCE(...,0)` → retorna `0`.

---

### A.6 Función de autenticación (`functions/60_fn_usuario_autenticar.sql`)

#### `fn_usuario_autenticar(p_login TEXT, p_pass TEXT) RETURNS BOOLEAN`
Única función relacionada con el acceso al panel admin. Function de lectura
(`LANGUAGE sql STABLE`): no escribe nada, solo compara.

- **Tabla que consulta**: `g1s_usuario` (`schema/06_usuario.sql`), con
  columnas `usu_login` (PK), `usu_pass_hash` (hash Blowfish de pgcrypto) y
  `usu_est_reg_cod` (el mismo patrón de eliminación lógica del resto del
  esquema: solo un usuario `'A'` puede autenticarse).
- **Cómo compara la contraseña**: `usu_pass_hash = crypt(p_pass,
  usu_pass_hash)`. Esto no es una comparación de texto plano: `crypt()`
  vuelve a hashear `p_pass` usando la **misma sal** que ya está codificada
  dentro de `usu_pass_hash` (así almacena las sales pgcrypto/Blowfish) y
  compara los dos hashes resultantes. La contraseña en texto plano nunca se
  guarda ni se compara directamente en ningún punto del sistema.
- **Devuelve `TRUE` solo si**: el login existe, `usu_est_reg_cod = 'A'`, y
  el hash coincide. Cualquier otra combinación (login inexistente,
  contraseña incorrecta, o usuario inactivado) devuelve `FALSE` — a
  propósito **no** distingue estos casos con distintos mensajes de error,
  para no filtrarle a un atacante si un login existe o no.
- **Quién la invoca**: `UsuarioDao.autenticar(login, password)` (única
  llamada en todo el sistema), desde `AuthController.iniciarSesion(...)`.
  No existe ninguna función `sp_usuario_mant`: no hay alta/edición/baja de
  usuarios porque solo existe la cuenta `admi`, sembrada una vez por
  `seed/04_seed_usuario.sql`.

---

## §B. Capa Java

### B.1 Modelos (`model/`)

Todos son POJOs/`record` de transporte, sin lógica de negocio — son el
"molde" en el que caen las filas de las vistas/tablas. Regla general: si
tiene mutabilidad y se llena por partes en un formulario (Cliente, Personal,
Proyecto), es clase con setters; si es de solo lectura (resultado de una
vista o consulta), es un `record` inmutable.

| Clase | Representa | Notas |
|---|---|---|
| `Cliente` | Fila de `g1m_clientes` + JOIN | Clase mutable (formulario) |
| `Personal` | Fila de `g1m_personal` + JOIN | Clase mutable (formulario) |
| `Proyecto` | Fila de `v_proyecto_resumen` | Clase mutable (formulario) |
| `PerCar` (record) | Autorización persona↔cargo | Solo lectura |
| `PersonalDisponible` (record) | Fila de `fn_personal_disponible_proyecto` | Expone `clave()` → `"perCod\|carProCod"` para el `<select>` HTML, y `etiqueta()` para mostrar |
| `ProyectoEquipoItem` (record) | Fila de `v_proyecto_equipo` | Solo lectura |
| `ProyectoMovimiento` (record) | Fila de movimiento con nombres resueltos | Solo lectura |
| `ProyectoAvance` (record) | Fila de `v_proyecto_avance` | Expone `pctBarra()` (acota a [0,100] para el ancho de la barra CSS) y `excedido()` (`true` si `pctAvance > 100`) |
| `RegistroReferencial` | Fila genérica de cualquier catálogo | `tam`/`tieEst` solo se usan si la tabla es Grupo B |
| `ReferencialTabla` (enum) | Metadatos de las 9 tablas de catálogo | Ver detalle abajo |

**`ReferencialTabla`** merece nota aparte: es el enum que permite que **un
solo** DAO/Service/Controller/vista Thymeleaf sirvan para las 9 tablas de
catálogo. Cada constante (`TIP_CLI`, `EST_CLI`, ... `ETP_PRO`) sabe su
`slug` de URL, el nombre real de tabla y columnas, si es Grupo A o B
(`esGrupoA()`), y si tiene columna `tam` o `tieEst`. El método estático
`porSlug(String)` resuelve el enum a partir del segmento de la URL
(`/referenciales/{tabla}`) — si no coincide con ninguna constante, el
controlador responde `404`.

### B.2 DAOs (`dao/`) — capa de acceso a datos

Regla de oro de todo el proyecto, cumplida sin excepciones: **las lecturas
usan `SELECT` con JOIN directo sobre tablas o vistas; las escrituras
siempre invocan una función PL/pgSQL** (`SELECT sp_x(?, ?, ...)`), nunca un
`INSERT`/`UPDATE` armado en Java.

#### `ClienteDao`
| Método | SQL que ejecuta |
|---|---|
| `listar()` | `SELECT ... FROM g1m_clientes JOIN gzz_tip_cli JOIN gzz_est_cli ORDER BY cli_cod` |
| `buscar(cod)` | Igual + `WHERE c.cli_cod = ?` |
| `mantener(operacion, Cliente)` | `SELECT sp_cliente_mant(?,?,?,?::char(2),?::char(1),?::date,?::date,?::date)` |
| `cambiarEstado(cod, operacion)` | `SELECT sp_cliente_mant(?, ?)` (sobrecarga de la función con solo 2 parámetros para ELIMINAR/INACTIVAR/REACTIVAR) |

#### `PersonalDao`
| Método | SQL que ejecuta |
|---|---|
| `listar()` / `buscar(cod)` | `SELECT ... FROM g1m_personal JOIN gzz_car_per` |
| `mantener(operacion, Personal)` | `SELECT sp_personal_mant(?,?,?,?::smallint,?::numeric,?::date)` |
| `cambiarEstado(cod, operacion)` | `SELECT sp_personal_mant(?, ?)` |
| `listarCargos(perCod)` | `SELECT ... FROM g1c_per_car JOIN gzz_car_pro WHERE pc.per_cod = ?` |
| `mantenerCargo(operacion, perCod, carProCod)` | `SELECT sp_per_car_mant(?, ?, ?::smallint)` |

#### `ProyectoDao`
| Método | SQL que ejecuta |
|---|---|
| `listar()` / `buscar(cli,tip,sec)` | `SELECT * FROM v_proyecto_resumen [WHERE ...]` |
| `crear(Proyecto)` | `SELECT sp_proyecto_crear(...)` → retorna `Integer` (el `pro_sec`) |
| `editar(Proyecto)` | `SELECT sp_proyecto_editar(...)` |
| `cambiarEstado(cli,tip,sec,estado)` | `SELECT sp_proyecto_cambiar_estado(?,?::smallint,?::smallint,?::char(2))` |

#### `ProyectoEquipoDao`
| Método | SQL que ejecuta |
|---|---|
| `listar(cli,tip,sec)` | `SELECT ... FROM v_proyecto_equipo WHERE ...` |
| `disponibles(cli,tip,sec)` | `SELECT * FROM fn_personal_disponible_proyecto(?,?::smallint,?::smallint)` |
| `asignar/quitar/reactivar(...)` | `SELECT sp_proyecto_equipo_{asignar,quitar,reactivar}(...)` |

#### `ProyectoAvanceDao`
| Método | SQL que ejecuta |
|---|---|
| `resumen(cli,tip,sec)` | `SELECT horas_estimadas, horas_trabajadas, pct_avance FROM v_proyecto_avance WHERE ...` |
| `movimientos(cli,tip,sec)` | `SELECT ... FROM g1t_pro_mov JOIN g1m_personal JOIN gzz_car_pro JOIN gzz_etp_pro ORDER BY fec_reg_etp DESC, etp_cod, sec_etp DESC` |
| `registrar(...)` | `SELECT sp_proyecto_avance_registrar(...)` → retorna `Integer` (el `sec_etp` asignado) |

#### `ReferencialDao` (genérico para las 9 tablas)
- `listar(ReferencialTabla)` / `buscar(ReferencialTabla, cod)`: arman el
  `SELECT` **concatenando** nombres de columna/tabla — seguro porque esos
  nombres **solo** provienen del enum `ReferencialTabla`, nunca de
  entrada del usuario (el comentario en el código lo deja explícito).
- `mantener(ReferencialTabla, operacion, RegistroReferencial)`: `switch`
  sobre el enum que decide a qué función invocar —
  `sp_gzz_tip_pro_mant` / `sp_gzz_lin_pro_mant` / `sp_gzz_etp_pro_mant`
  para Grupo B, `sp_ref_grupoa_mant(tabla.getTabla(), ...)` para las 6 del
  Grupo A (aquí el nombre de tabla sí viaja como parámetro bind de texto
  hacia la función SQL, que lo compara en `IF/ELSIF`, no lo ejecuta).

#### `UsuarioDao`
| Método | SQL que ejecuta |
|---|---|
| `autenticar(login, password)` | `SELECT fn_usuario_autenticar(?, ?)` → `boolean` |

### B.3 Services (`service/`) — orquestación

Todos los `Service` son **envoltorios delgados** sobre su DAO: no agregan
reglas de negocio (esas viven en PL/pgSQL), solo dan nombres de método más
expresivos al controlador y aíslan al controlador del DAO concreto.
`ClienteService`, `PersonalService`, `ProyectoService`,
`ProyectoAvanceService`, `ReferencialService` — cada uno expone
`listar/buscar/adicionar/modificar/cambiarEstado` (más los métodos propios
de su dominio: `ProyectoService` añade `equipo/disponibles/asignarEquipo/...`;
`PersonalService` añade `listarCargos/adicionarCargo/...`).

### B.4 Controllers (`web/`) — capa MVC

Patrón uniforme en todos los controladores de mantenimiento (heredan de
`MantenimientoControllerBase`):

- **GET sin sufijo** (`/entidad`) → listado.
- **GET `/nuevo`** → formulario vacío (Adicionar).
- **GET `/{cod}/editar`** → formulario lleno (Modificar) — 404 vía
  `ResponseStatusException` si el código no existe.
- **POST `/entidad`** → confirma el alta.
- **POST `/{cod}`** → confirma la edición.
- **POST `/{cod}/{eliminar|inactivar|reactivar}`** → cambia el estado.

`MantenimientoControllerBase.ejecutar(Runnable, mensajeExito, RedirectAttributes)`
es el único punto que traduce una operación de escritura en mensaje flash:
ejecuta el `Runnable`, y si lanza `DataAccessException`/`ReglaNegocioException`,
extrae el mensaje real con `ErroresBd.extraerMensaje()` y lo guarda como
`error` flash en vez de dejar que reviente en un 500.

| Controlador | Ruta base | Particularidad |
|---|---|---|
| `HomeController` | `/` | Cuenta clientes/personal/proyectos activos para el dashboard; sirve de smoke-test de la conexión a BD |
| `ClienteController` | `/clientes` | Patrón estándar |
| `PersonalController` | `/personal` | Patrón estándar + sub-rutas `/{cod}/cargos` (autorizaciones) |
| `ReferencialController` | `/referenciales/{tabla}` | **Un solo controlador para 9 tablas**; resuelve `{tabla}` a `ReferencialTabla` con `@ModelAttribute` (404 si el slug no existe) |
| `ProyectoController` | `/proyectos` | Además de alta/edición/estado, expone `/equipo` (asignar/quitar/reactivar) |
| `ProyectoAvanceController` | `/proyectos/{cli}/{tip}/{sec}/avance` | El `<select>` de miembro llega como `"perCod\|carProCod"` y se parte con `split("\\|")` — **sin validar longitud del array**, ver hueco en `03-DOCUMENTACION-GENERAL.md` |
| `AuthController` | `/login`, `/logout` | Único controlador que escribe cookies directamente (`HttpServletResponse` como parámetro) en vez de devolver solo una vista; no extiende `MantenimientoControllerBase` porque su manejo de errores es distinto (usuario/contraseña incorrectos no es una `DataAccessException`) |

### B.5 Manejo de errores (`exception/`)

- **`ErroresBd.extraerMensaje(Throwable)`**: dado un `DataAccessException`,
  navega a `getMostSpecificCause()`. Si es un `SQLException` (viene de
  Postgres), toma solo la **primera línea** del mensaje (el driver antepone
  `"ERROR: "` y agrega líneas `"Where:"` con el contexto PL/pgSQL que no le
  interesa al usuario final) y le quita el prefijo `"ERROR: "`.
- **`ReglaNegocioException`**: `RuntimeException` para reglas detectadas en
  Java (no en SQL) — hoy no hay ningún caso de uso activo, existe como
  extensión prevista.
- **`NoAutorizadoException`**: se lanza desde `AutorizacionInterceptor`
  (§D.3) cuando llega una escritura sin sesión admin. A diferencia de
  `ReglaNegocioException`, no representa una regla de negocio violada sino
  un intento de mutación sin permiso — por eso tiene su propio
  `@ExceptionHandler` con `@ResponseStatus(HttpStatus.FORBIDDEN)` (403), en
  vez del 200 implícito que usan los demás manejadores.
- **`GlobalExceptionHandler`** (`@ControllerAdvice`): red de seguridad para
  cualquier `DataAccessException`/`ReglaNegocioException`/`NoAutorizadoException`
  que **no** haya sido capturada por `MantenimientoControllerBase.ejecutar(...)`
  (típicamente errores en un GET, o cualquier bloqueo del interceptor, que
  nunca llega a pasar por ese método) — muestra la página `error.html`.

---

## §C. Capa de seguridad (`security/` + `web/GlobalModelAttributes`)

Esta capa es la única parte de la aplicación Java que **sí** contiene
lógica propia (no solo invocaciones a SQL): decidir quién es el usuario de
esta petición no es una regla de negocio de datos, así que no tiene
sentido implementarla en PL/pgSQL. La verificación de la contraseña en sí
(lo único que *sí* es una comparación de datos) permanece en la BD
(`fn_usuario_autenticar`, ver §A.6).

#### `JwtService`
Firma y valida los JWT. Dos tipos de token distinguidos por el claim
`"tipo"` (`"access"` / `"refresh"`), firmados con HMAC-SHA256 usando la
clave de `app.jwt.secret` (cargada desde `db.properties`, nunca committeada).

| Método | Qué hace |
|---|---|
| `generarAccessToken(login)` | JWT con `tipo=access`, expira en `app.jwt.access-minutos` (15 por defecto) |
| `generarRefreshToken(login)` | JWT con `tipo=refresh`, expira en `app.jwt.refresh-dias` (7 por defecto) |
| `validarYObtenerLogin(token, tipoEsperado)` | Verifica firma + expiración + que el claim `tipo` coincida; retorna el `subject` (login) o `null` ante cualquier problema |
| `accessMaxAgeSegundos()` / `refreshMaxAgeSegundos()` | Duración en segundos, para fijar el `Max-Age` de la cookie |

El claim `tipo` evita que un refresh token robado sirva directamente como
access token (o viceversa): aunque ambos son JWT válidos y firmados con la
misma clave, `validarYObtenerLogin` los rechaza si el tipo no es el esperado.

#### `CookieUtil`
Construye (`ResponseCookie` de Spring, no la `Cookie` clásica de Servlet,
porque esta última no soporta `SameSite`) y limpia las dos cookies de
sesión. Ambas cookies son `HttpOnly` (JavaScript no puede leerlas) y
`SameSite=Lax`; `Secure` se controla con `app.jwt.cookie-secure` (`false`
en desarrollo sobre `http://localhost`, debería ser `true` en producción
con HTTPS).

| Método | Qué hace |
|---|---|
| `escribirAccessToken(response, token, maxAge)` | `Set-Cookie: gp_access=...` |
| `escribirRefreshToken(response, token, maxAge)` | `Set-Cookie: gp_refresh=...` |
| `borrarCookiesSesion(response)` | Ambas cookies con `Max-Age=0` (logout) |

#### `JwtAuthFilter` (`OncePerRequestFilter`, se autorregistra como bean)
Corre en **cada** petición, antes que cualquier controlador. Responde una
sola pregunta: *¿esta petición trae la sesión admin?*

1. Lee la cookie `gp_access`; si es válida (`validarYObtenerLogin(...,
   "access")`), usa ese login.
2. Si no — falta, expiró, o la firma no valida — lee la cookie `gp_refresh`;
   si **esa** es válida, la petición igual se trata como autenticada, **y**
   se emite un access token nuevo vía `Set-Cookie` en la misma respuesta
   (renovación silenciosa: el usuario nunca nota que su access token venció).
3. Guarda el resultado en el atributo de request `gestproy.admin`
   (`true`/`false`) para que el resto del pipeline no vuelva a tocar
   cookies ni JWT.

Como GestProy tiene una sola cuenta, "autenticado" y "es admi" son la
misma pregunta — no hay niveles de permiso que resolver más allá de sí/no.

#### `AutorizacionInterceptor` (`HandlerInterceptor`, registrado en `WebConfig`)
Lee el atributo `gestproy.admin` que dejó el filtro y decide si la petición
puede seguir. Es la aplicación **real** de "modo solo vista": esconder un
botón en Thymeleaf es cosmético, esto es lo que de verdad impide escribir
aunque alguien arme el `POST` a mano con `curl`/Postman.

- `"/login"` y `"/logout"` siempre pasan (son las únicas rutas de escritura
  que alguien sin sesión necesita usar).
- Si `admin == true`, todo pasa.
- Si no, se considera "escritura" (y se lanza `NoAutorizadoException`):
  cualquier `POST`, o un `GET` a una ruta terminada en `/nuevo` o `/editar`
  (esas páginas no tienen ningún valor de solo lectura).

#### `WebConfig` (`WebMvcConfigurer`)
Registra `AutorizacionInterceptor` para todas las rutas. `JwtAuthFilter` no
necesita registrarse aquí: al ser un bean `Filter`, Spring Boot lo agrega
solo a la cadena de servlets.

#### `GlobalModelAttributes` (`@ControllerAdvice`, en `web/`)
Agrega el booleano `admin` al modelo de **todas** las vistas Thymeleaf,
leyendo el mismo atributo de request que puso `JwtAuthFilter`. Gracias a
esto, cualquier plantilla puede hacer `th:if="${admin}"` para
mostrar/ocultar botones de Adicionar/Modificar/Eliminar/etc. sin que cada
controlador tenga que agregarlo a mano a su propio `Model`.

---

## §D. Tabla cruzada: método Java → función/vista SQL

| Método Java | Función/vista SQL invocada | Triggers que pueden dispararse |
|---|---|---|
| `ClienteDao.mantener` | `sp_cliente_mant` | — |
| `PersonalDao.mantener` | `sp_personal_mant` | — |
| `PersonalDao.mantenerCargo` | `sp_per_car_mant` | — |
| `ReferencialDao.mantener` (Grupo A) | `sp_ref_grupoa_mant` | — |
| `ReferencialDao.mantener` (Grupo B) | `sp_gzz_tip_pro_mant` / `sp_gzz_lin_pro_mant` / `sp_gzz_etp_pro_mant` | — |
| `ProyectoDao.crear` | `sp_proyecto_crear` | `trg_procab_valida_fechas` |
| `ProyectoDao.editar` | `sp_proyecto_editar` | `trg_procab_valida_fechas` |
| `ProyectoDao.cambiarEstado` | `sp_proyecto_cambiar_estado` | `trg_procab_valida_fechas` (por el `UPDATE` interno) |
| `ProyectoEquipoDao.asignar` | `sp_proyecto_equipo_asignar` | `trg_proeqp_valida_percar_activo` |
| `ProyectoEquipoDao.quitar` | `sp_proyecto_equipo_quitar` | — (no dispara el trigger: pasa a `'I'`, no a `'A'`) |
| `ProyectoEquipoDao.reactivar` | `sp_proyecto_equipo_reactivar` | `trg_proeqp_valida_percar_activo` |
| `ProyectoEquipoDao.disponibles` | `fn_personal_disponible_proyecto` | — (solo lectura) |
| `ProyectoAvanceDao.registrar` | `sp_proyecto_avance_registrar` | `trg_promov_valida_eqp_activo`, `trg_promov_autonumera_sec_etp` |
| `ProyectoAvanceDao.resumen` | `v_proyecto_avance` (usa `fn_proyecto_pct_avance` internamente) | — (solo lectura) |
| `ProyectoDao.listar/buscar` | `v_proyecto_resumen` | — (solo lectura) |
| `ProyectoEquipoDao.listar` | `v_proyecto_equipo` | — (solo lectura) |
| `UsuarioDao.autenticar` | `fn_usuario_autenticar` | — (solo lectura; sin triggers, `g1s_usuario` no tiene) |
