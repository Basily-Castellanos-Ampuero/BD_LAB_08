# GestProy — Vistas, Triggers y Procedimientos Almacenados: dónde y por qué se usan

Este documento complementa [03-DOCUMENTACION-GENERAL.md](03-DOCUMENTACION-GENERAL.md) con el
detalle exacto de **cada objeto de base de datos** definido en `db/views/`, `db/triggers/` y
`db/functions/`: qué hace, por qué existe, y el punto exacto del código Java (archivo y línea)
donde se invoca. Es el mapa de trazabilidad BD ↔ aplicación.

## Idea central

GestProy pone la lógica de negocio **dentro de PostgreSQL**, no en Java. La capa `dao/` de Spring
Boot es delgada: para lecturas hace `SELECT` directos (a menudo sobre las vistas) y para toda
escritura con reglas de negocio llama a una función `SELECT sp_x(...)` vía `JdbcTemplate`. Los
triggers son la última línea de defensa: validan invariantes de la tabla incluso si alguien
escribe SQL directo sin pasar por una función `sp_`.

```
Navegador → Controller (web/) → DAO (dao/) → JdbcTemplate → función/vista PostgreSQL → tablas
                                                                    ↑
                                                        triggers (validan cada INSERT/UPDATE)
```

---

## 1. Vistas (`db/views/`)

Las 3 vistas existen para que el DAO no tenga que hacer los mismos `JOIN` repetidos en cada
consulta de lectura, y para traducir códigos a descripciones legibles antes de llegar a Java.
Ninguna vista se escribe (no hay `INSERT`/`UPDATE` sobre vistas); todas se consumen con `SELECT`.

### 1.1 `v_proyecto_resumen`

**Archivo:** `db/views/v_proyecto_resumen.sql`

**Qué hace:** une `g1t_pro_cab` (cabecera de proyecto) con `g1m_clientes`, `gzz_tip_pro` y
`gzz_est_pro` para devolver el proyecto completo con nombres legibles (`cliente_nombre`,
`tipo_descripcion`, `estado_descripcion`) en vez de solo códigos. Deliberadamente **no filtra**
por `est_reg`: la UI necesita ver también proyectos inactivos/eliminados para poder reactivarlos.

**Por qué existe:** sin la vista, cada pantalla que lista o muestra un proyecto tendría que
repetir 3 `JOIN` a mano en Java. Centralizarlo en una vista es una sola fuente de verdad para "qué
es un proyecto, mostrado al usuario".

**Dónde se usa:**
- `src/main/java/edu/unsa/eps/gestproy/dao/ProyectoDao.java:15` — constante
  `SELECT_BASE = "SELECT * FROM v_proyecto_resumen "`, usada por `listar()` y `buscar(...)`
  (los métodos de lectura del DAO de proyectos). Es la fuente de datos del listado de proyectos y
  de la pantalla de detalle.
- Referenciada en el Javadoc de `src/main/java/edu/unsa/eps/gestproy/model/Proyecto.java:9`.

**Consumida por (flujo completo):** `ProyectoController` (`GET /proyectos`, `GET /proyectos/{cli}/{tip}/{sec}`) → `ProyectoDao.listar()`/`buscar()` → `v_proyecto_resumen`.

### 1.2 `v_proyecto_equipo`

**Archivo:** `db/views/v_proyecto_equipo.sql`

**Qué hace:** une `g1t_pro_eqp` (equipo asignado) con `g1m_personal` y `gzz_car_pro` para devolver,
por cada miembro, su nombre, el cargo que ejerce en el proyecto, su costo/hora, y una columna
calculada `estado_descripcion` (`CASE` que traduce `'A'→Activo`, `'I'→Retirado`, `else→Eliminado`).

**Por qué existe:** evita repetir el `JOIN` persona+cargo en cada pantalla de equipo, y centraliza
la traducción de estado a texto (antes la hacía cada vista Java por separado).

**Dónde se usa:**
- `src/main/java/edu/unsa/eps/gestproy/dao/ProyectoEquipoDao.java:24` — método `listar(cliCod,
  tipCod, sec)`: `SELECT ... FROM v_proyecto_equipo WHERE pro_cli_cod = ? AND pro_tip_cod = ? AND
  pro_sec = ? ORDER BY per_nom, car_pro_des`.
- Referenciada en el Javadoc de `src/main/java/edu/unsa/eps/gestproy/model/ProyectoEquipoItem.java:5`.

**Consumida por (flujo completo):** `ProyectoController` (`GET /proyectos/{cli}/{tip}/{sec}/equipo`) → `ProyectoEquipoDao.listar()` → `v_proyecto_equipo`.

### 1.3 `v_proyecto_avance`

**Archivo:** `db/views/v_proyecto_avance.sql`

**Qué hace:** por cada proyecto calcula tres números agregados:
- `horas_estimadas`: suma de `etp_tie_est` de las etapas **activas** del catálogo global
  `gzz_etp_pro` (subconsulta independiente del proyecto — ver nota de modelado en §3.14).
- `horas_trabajadas`: suma de `hor_tra_etp + min_tra_etp/60.0` de los movimientos activos
  (`g1t_pro_mov`) de ese proyecto.
- `pct_avance`: invoca la función `fn_proyecto_pct_avance(...)` (ver §3.14) por fila.

**Por qué existe:** es la vista "central del flujo de negocio de avance por etapas" (comentario
textual del propio archivo). Sin ella, la pantalla de avance tendría que ejecutar 3 consultas y
combinarlas en Java; la vista lo resuelve en una sola fila por proyecto.

**Dónde se usa:**
- `src/main/java/edu/unsa/eps/gestproy/dao/ProyectoAvanceDao.java:24` — método `resumen(cliCod,
  tipCod, sec)`: `SELECT horas_estimadas, horas_trabajadas, pct_avance FROM v_proyecto_avance
  WHERE pro_cli_cod = ? AND pro_tip_cod = ? AND pro_sec = ?`.
- Referenciada en el Javadoc de `src/main/java/edu/unsa/eps/gestproy/model/ProyectoAvance.java:5`
  y en un comentario de `src/main/java/edu/unsa/eps/gestproy/web/ProyectoAvanceController.java:25`.

**Consumida por (flujo completo):** `ProyectoAvanceController` (`GET /proyectos/{cli}/{tip}/{sec}/avance`, implícito en el listado de movimientos) → `ProyectoAvanceDao.resumen()` → `v_proyecto_avance` → internamente llama a `fn_proyecto_pct_avance`.

---

## 2. Triggers (`db/triggers/`)

Los 4 triggers son la **red de seguridad a nivel de tabla**: nunca se llaman explícitamente desde
Java (no hay ningún `CALL` ni `SELECT` a una función de trigger); se disparan automáticamente
cuando las funciones `sp_*` hacen su `INSERT`/`UPDATE` sobre `g1t_pro_eqp`, `g1t_pro_mov` o
`g1t_pro_cab`. Varias funciones `sp_*` **validan lo mismo por adelantado** para dar un mensaje de
error más específico al usuario; el trigger es el respaldo que garantiza la regla incluso si en el
futuro alguien escribe SQL directo sin pasar por la función.

### 2.1 `trg_proeqp_valida_percar_activo`

**Archivo:** `db/triggers/01_trg_proeqp_valida_percar_activo.sql`
**Tabla / evento:** `BEFORE INSERT OR UPDATE ON g1t_pro_eqp`

**Qué valida:** si la fila nueva/actualizada llega con `pro_per_car_est_reg_cod = 'A'` (se está
asignando o reactivando a alguien), exige que exista una fila **activa** en `g1c_per_car` para esa
combinación (persona, cargo). La FK sola solo garantiza que la combinación exista, no que esté
vigente. **No** se aplica al retirar/inactivar a alguien (`'I'` o `'*'`), para poder dar de baja a
un miembro aunque su autorización de cargo ya haya caducado.

**Por qué existe:** evita que alguien sea asignado a un proyecto con un cargo que nunca tuvo
autorizado o que ya le fue retirado — la regla "solo puedo ejercer un cargo de proyecto si tengo
esa autorización activa" es un invariante de negocio central del sistema.

**Se dispara indirectamente desde:**
- `sp_proyecto_equipo_asignar` (`db/functions/40_sp_proyecto_equipo_asignar.sql`) — que ya valida
  lo mismo en líneas 46-52 antes de insertar, para dar un mensaje más claro; el trigger es el
  respaldo.
- `sp_proyecto_equipo_reactivar` (`db/functions/41_sp_proyecto_equipo_quitar_reactivar.sql:44-77`)
  — aquí el trigger es la **única** validación (el comentario del archivo, línea 71-72, lo dice
  explícitamente: "el trigger... verifica que la autorización siga activa").
- Invocadas desde Java en `src/main/java/edu/unsa/eps/gestproy/dao/ProyectoEquipoDao.java:52` (asignar) y `:64` (reactivar).

### 2.2 `trg_promov_valida_eqp_activo`

**Archivo:** `db/triggers/02_trg_promov_valida_eqp_activo.sql`
**Tabla / evento:** `BEFORE INSERT ON g1t_pro_mov`

**Qué valida:** que la persona que registra horas de avance esté **activamente** asignada al
proyecto (existe fila en `g1t_pro_eqp` con `pro_per_car_est_reg_cod = 'A'` para esa combinación
proyecto+persona+cargo). La FK `fk_promov_eqp` solo exige que la fila de equipo exista, no que
siga activa.

**Por qué existe:** impide que alguien retirado del equipo siga acumulando horas de trabajo en el
proyecto — coherencia entre "quién está en el equipo hoy" y "quién puede reportar horas hoy".

**Se dispara indirectamente desde:**
- `sp_proyecto_avance_registrar` (`db/functions/50_sp_proyecto_avance_registrar.sql:54-61`), que
  también valida lo mismo antes del `INSERT` para dar un error más claro.
- Invocada desde Java en `src/main/java/edu/unsa/eps/gestproy/dao/ProyectoAvanceDao.java:66`.

### 2.3 `trg_promov_autonumera_sec_etp`

**Archivo:** `db/triggers/03_trg_promov_autonumera_sec_etp.sql`
**Tabla / evento:** `BEFORE INSERT ON g1t_pro_mov`

**Qué hace:** si `NEW.sec_etp` llega `NULL` o `0`, calcula automáticamente
`MAX(sec_etp)+1` para la combinación (proyecto, persona, cargo, etapa) y lo asigna antes de que se
evalúen el `NOT NULL` y la clave primaria compuesta. Así la aplicación no necesita calcular manualmente
el último componente de la PK.

**Por qué existe:** `sec_etp` es "el N-ésimo registro de horas de esta persona en esta etapa de
este proyecto" — un contador por combinación, no un ID global. Delegarlo al trigger evita
condiciones de carrera si se calculara en Java (dos requests concurrentes verían el mismo `MAX`)
y mantiene esa lógica fuera de la capa de aplicación.

**Se dispara desde:**
- `sp_proyecto_avance_registrar` (`db/functions/50_sp_proyecto_avance_registrar.sql:76-82`) inserta
  con `sec_etp = NULL` explícito (línea 81) y luego usa `RETURNING sec_etp INTO v_sec_etp` para
  recuperar el valor que el trigger calculó, y lo retorna a Java.
- Ese valor de retorno llega a `src/main/java/edu/unsa/eps/gestproy/dao/ProyectoAvanceDao.java` (el
  `SELECT sp_proyecto_avance_registrar(...)` de la línea 66 retorna el `sec_etp` asignado).

### 2.4 `trg_procab_valida_fechas`

**Archivo:** `db/triggers/04_trg_procab_valida_fechas.sql`
**Tabla / evento:** `BEFORE INSERT OR UPDATE ON g1t_pro_cab`

**Qué valida** (solo cuando ambos extremos no son `NULL`):
1. `pro_fec_con <= pro_fec_pac` (fecha de contrato no posterior a la pactada)
2. `pro_fec_ini <= pro_fec_ent` (inicio no posterior a la entrega)
3. `pro_fec_ini <= pro_fec_cer` (inicio no posterior al cierre)

**Por qué existe:** es la única garantía de coherencia temporal de la cabecera del proyecto; sin
ella sería posible, por ejemplo, guardar un proyecto "entregado antes de haber empezado".

**Se dispara indirectamente desde:**
- `sp_proyecto_crear` (`db/functions/30_sp_proyecto_crear.sql:48` — el comentario dice
  textualmente "la coherencia fec_con <= fec_pac la valida trg_procab_valida_fechas").
- `sp_proyecto_editar` (`db/functions/31_sp_proyecto_editar.sql:56` — mismo patrón).
- `sp_proyecto_cambiar_estado` (`db/functions/32_sp_proyecto_cambiar_estado.sql:60-68`), que además
  **autocompleta** `pro_fec_ini`/`pro_fec_ent`/`pro_fec_cer` con `CURRENT_DATE` al hacer las
  transiciones de estado correspondientes — el trigger valida el resultado de esa actualización.
- Invocadas desde Java en `src/main/java/edu/unsa/eps/gestproy/dao/ProyectoDao.java:63` (crear), `:73` (editar) y `:86` (cambiar estado).

---

## 3. Funciones y procedimientos almacenados (`db/functions/`)

Convención de nombres del proyecto (ver `db/README.md`): `sp_` = función de **escritura** (INSERT/UPDATE con
reglas de negocio), `fn_` = función de **lectura/cálculo** (sin efectos secundarios, `LANGUAGE sql
STABLE`). Todas están escritas como `FUNCTION` (no `PROCEDURE`) porque cada llamada es una sola
transacción y se invocan directo con `SELECT sp_x(...)` desde `JdbcTemplate`. Cualquier validación
fallida hace `RAISE EXCEPTION` con mensaje en español, que Spring recibe como
`DataAccessException` y muestra tal cual al usuario.

### Grupo 1x — Mantenimiento de catálogos referenciales

#### 3.1 `sp_ref_grupoa_mant`

**Archivo:** `db/functions/10_sp_ref_grupoa_mant.sql`

**Qué hace:** una sola función parametrizada por `p_tabla` que hace el mantenimiento
(`ADICIONAR`/`MODIFICAR`/`ELIMINAR`/`INACTIVAR`/`REACTIVAR`) de las **6 tablas** referenciales que
comparten la misma forma `cod`/`des`/`est_reg`: `gzz_est_reg`, `gzz_tip_cli`, `gzz_est_cli`,
`gzz_est_pro`, `gzz_car_per`, `gzz_car_pro`. Usa ramas `IF/ELSIF` explícitas por tabla (no `EXECUTE`
dinámico) para que el SQL de cada tabla quede literal y visible, sin riesgo de inyección por
nombre de tabla.

**Por qué existe:** evitar duplicar 6 funciones casi idénticas; el nombre de tabla siempre llega
desde el enum Java `ReferencialTabla` (nunca texto libre del usuario), así que es seguro pasarlo
como parámetro.

**Dónde se usa:** `src/main/java/edu/unsa/eps/gestproy/dao/ReferencialDao.java:76` — rama `default`
del `switch` en el método `mantener(t, operacion, r)`: `SELECT sp_ref_grupoa_mant(?, ?, ?, ?)` con
`t.getTabla()` como primer argumento. Se activa para cualquier `ReferencialTabla` que no sea
`TIP_PRO`/`LIN_PRO`/`ETP_PRO`.

**Flujo completo:** `ReferencialController` (`POST /referenciales/{tabla}`, `.../eliminar`,
`.../inactivar`, `.../reactivar`) → `ReferencialDao.mantener()` → `sp_ref_grupoa_mant`.

#### 3.2–3.4 `sp_gzz_tip_pro_mant` / `sp_gzz_lin_pro_mant` / `sp_gzz_etp_pro_mant`

**Archivo:** `db/functions/11_sp_ref_grupob_mant.sql`

**Qué hacen:** mismo patrón que `sp_ref_grupoa_mant`, pero una función **por tabla** porque cada
una del "Grupo B" tiene una columna extra distinta:
- `sp_gzz_tip_pro_mant` → `gzz_tip_pro`, columna extra `tip_pro_tam CHAR(1)` (debe ser `P`, `M` o `G`).
- `sp_gzz_lin_pro_mant` → `gzz_lin_pro`, columnas extra `lin_pro_nom VARCHAR(60)` + `lin_pro_tam CHAR(1)`.
- `sp_gzz_etp_pro_mant` → `gzz_etp_pro`, columna extra `etp_tie_est NUMERIC(5,2)` (debe ser `> 0`
  horas: esta es la tabla que alimenta `horas_estimadas` en `v_proyecto_avance`, §1.3).

**Por qué existen separadas:** la columna adicional difiere en nombre y tipo entre las tres, así
que no calzan en el patrón genérico de `sp_ref_grupoa_mant`.

**Dónde se usan:** `src/main/java/edu/unsa/eps/gestproy/dao/ReferencialDao.java` — método
`mantener()`, ramas `case TIP_PRO` (línea 67), `case LIN_PRO` (línea 70), `case ETP_PRO` (línea 73)
del `switch (t)`.

**Flujo completo:** mismo que 3.1 — `ReferencialController` → `ReferencialDao.mantener()`, pero
enrutado a la función específica según el enum `ReferencialTabla` recibido.

### Grupo 2x — Mantenimiento de maestras

#### 3.5 `sp_cliente_mant`

**Archivo:** `db/functions/20_sp_cliente_mant.sql`

**Qué hace:** mantenimiento completo de `g1m_clientes`. Además del CRUD lógico estándar, valida
que `tip_cli_cod` y `est_cli_cod` referencien catálogos **activos** (la FK por sí sola no valida
estado) y que `fec_ing <= fec_ces` cuando ambas están presentes.

**Dónde se usa:**
- `src/main/java/edu/unsa/eps/gestproy/dao/ClienteDao.java:57` — método `mantener(operacion, c)`
  (ADICIONAR/MODIFICAR con todos los campos).
- `src/main/java/edu/unsa/eps/gestproy/dao/ClienteDao.java:65` — método `cambiarEstado(cod,
  operacion)` (ELIMINAR/INACTIVAR/REACTIVAR, sobrecarga de 2 argumentos de la misma función SQL).

**Flujo completo:** `ClienteController` (`POST /clientes`, `POST /clientes/{cod}`, `.../eliminar`,
`.../inactivar`, `.../reactivar`) → `ClienteDao.mantener()`/`cambiarEstado()` → `sp_cliente_mant`.

#### 3.6 `sp_personal_mant`

**Archivo:** `db/functions/21_sp_personal_mant.sql`

**Qué hace:** mantenimiento de `g1m_personal`. Reglas propias: `car_cod` debe referenciar un cargo
de personal (`gzz_car_per`) activo, `cos_hor > 0`, `fec_ing` obligatoria y no futura.

**Dónde se usa:**
- `src/main/java/edu/unsa/eps/gestproy/dao/PersonalDao.java:51` — método `mantener(operacion, p)`.
- `src/main/java/edu/unsa/eps/gestproy/dao/PersonalDao.java:58` — método `cambiarEstado(cod,
  operacion)`.

**Flujo completo:** `PersonalController` (`POST /personal`, `POST /personal/{cod}`, `.../eliminar`,
`.../inactivar`, `.../reactivar`) → `PersonalDao` → `sp_personal_mant`.

#### 3.7 `sp_per_car_mant`

**Archivo:** `db/functions/22_sp_per_car_mant.sql`

**Qué hace:** mantiene `g1c_per_car` — **qué cargos de proyecto puede ejercer cada persona**
(la tabla de autorizaciones que luego validan los triggers 2.1/2.2). Solo soporta
`ADICIONAR`/`ELIMINAR`/`INACTIVAR`/`REACTIVAR` (no `MODIFICAR`: la fila no tiene más datos que el
estado). Particularidad: si al `ADICIONAR` la combinación (persona, cargo) ya existe pero inactiva,
la **reactiva** con `UPDATE` en vez de fallar por PK duplicada; solo es error si ya existe activa.

**Por qué existe:** es la tabla que responde "¿puede Fulano ser Jefe de Proyecto?" — la autorización
vive separada de la asignación real a un proyecto concreto (`g1t_pro_eqp`), de modo que una persona
puede estar habilitada para un cargo sin estar actualmente trabajando en ningún proyecto con él.

**Dónde se usa:** `src/main/java/edu/unsa/eps/gestproy/dao/PersonalDao.java:82` — método
`mantenerCargo(operacion, perCod, carProCod)`.

**Flujo completo:** `PersonalController` (`POST /personal/{cod}/cargos`, `.../cargos/{carProCod}/inactivar`,
`.../reactivar`, `.../eliminar`) → `PersonalDao.mantenerCargo()` → `sp_per_car_mant`.

### Grupo 3x-4x — Ciclo de vida del proyecto y su equipo

#### 3.8 `sp_proyecto_crear`

**Archivo:** `db/functions/30_sp_proyecto_crear.sql`

**Qué hace:** crea la cabecera de un proyecto nuevo en `g1t_pro_cab`. Calcula
`pro_sec = MAX(pro_sec)+1` **por combinación (cliente, tipo)** — por eso `pro_sec` no es un
`SERIAL` global, sino "proyecto N de este tipo para este cliente". Valida que cliente y tipo de
proyecto existan y estén activos, fija el estado inicial `'01'` (Planificado) y calcula
`pro_uti_pre = monto - costo - gasto` si hay monto presupuestado. Retorna el `pro_sec` generado
(la app lo necesita para redirigir a la pantalla del proyecto recién creado).

**Dónde se usa:** `src/main/java/edu/unsa/eps/gestproy/dao/ProyectoDao.java:63`.

**Flujo completo:** `ProyectoController` (`POST /proyectos`) → `ProyectoDao` → `sp_proyecto_crear`
→ dispara `trg_procab_valida_fechas` (§2.4).

#### 3.9 `sp_proyecto_editar`

**Archivo:** `db/functions/31_sp_proyecto_editar.sql`

**Qué hace:** actualiza fechas y montos de un proyecto **no cerrado** (rechaza si `pro_est_cod =
'04'` o si el proyecto está inactivo/eliminado). Recalcula `pro_uti_pre` y `pro_uti_rea` a partir
de monto−costo−gasto; la utilidad nunca se ingresa a mano, siempre se deriva.

**Dónde se usa:** `src/main/java/edu/unsa/eps/gestproy/dao/ProyectoDao.java:73`.

**Flujo completo:** `ProyectoController` (`POST /proyectos/{cli}/{tip}/{sec}`) → `ProyectoDao` →
`sp_proyecto_editar` → dispara `trg_procab_valida_fechas`.

#### 3.10 `sp_proyecto_cambiar_estado`

**Archivo:** `db/functions/32_sp_proyecto_cambiar_estado.sql`

**Qué hace:** aplica una **matriz de transiciones de estado** explícita para el proyecto:
```
01 Planificado → 02 En Ejecución | 05 Suspendido
02 En Ejecución → 03 Entregado    | 05 Suspendido
05 Suspendido   → 02 En Ejecución
03 Entregado    → 04 Cerrado
```
Cualquier transición fuera de esta matriz lanza `RAISE EXCEPTION`. Además, según el destino,
autocompleta fechas si están `NULL` (`02` fija `pro_fec_ini`, `03` fija `pro_fec_ent`, `04` fija
`pro_fec_cer`), y al llegar a `04` (Cerrado) propaga `cli_fec_ult_pro_cer` a `g1m_clientes`.

**Por qué existe:** el ciclo de vida del proyecto es la regla de negocio más sensible del sistema;
codificarla como matriz en una sola función evita que un estado inválido llegue a persistirse
nunca, sin importar qué controlador la invoque.

**Dónde se usa:** `src/main/java/edu/unsa/eps/gestproy/dao/ProyectoDao.java:86`.

**Flujo completo:** `ProyectoController` (`POST /proyectos/{cli}/{tip}/{sec}/estado`) →
`ProyectoDao` → `sp_proyecto_cambiar_estado` → dispara `trg_procab_valida_fechas` sobre el
`UPDATE` resultante.

#### 3.11 `sp_proyecto_equipo_asignar`

**Archivo:** `db/functions/40_sp_proyecto_equipo_asignar.sql`

**Qué hace:** asigna una persona con un cargo al equipo de un proyecto. Valida, en este orden:
proyecto existente/activo/no cerrado, autorización (persona, cargo) activa en `g1c_per_car`
(la misma regla que impone el trigger 2.1, pero validada antes para dar un mensaje más específico),
y si la fila de equipo ya existía inactiva la **reactiva** en vez de fallar por PK duplicada.

**Dónde se usa:** `src/main/java/edu/unsa/eps/gestproy/dao/ProyectoEquipoDao.java:52`.

**Flujo completo:** `ProyectoController` (`POST /proyectos/{cli}/{tip}/{sec}/equipo`) →
`ProyectoEquipoDao.asignar()` → `sp_proyecto_equipo_asignar` → dispara `trg_proeqp_valida_percar_activo`.

#### 3.12 `sp_proyecto_equipo_quitar` / `sp_proyecto_equipo_reactivar`

**Archivo:** `db/functions/41_sp_proyecto_equipo_quitar_reactivar.sql`

**Qué hacen:** baja lógica (`quitar` → `est_reg = 'I'`) y reincorporación (`reactivar` → `'A'`) de
un miembro del equipo, conservando su historial de movimientos (`g1t_pro_mov` no se toca).

**Dónde se usan:**
- `src/main/java/edu/unsa/eps/gestproy/dao/ProyectoEquipoDao.java:58` (`quitar`).
- `src/main/java/edu/unsa/eps/gestproy/dao/ProyectoEquipoDao.java:64` (`reactivar` — dispara
  `trg_proeqp_valida_percar_activo`, que aquí es la **única** validación de que la autorización
  siga activa).

**Flujo completo:** `ProyectoController` (`POST .../equipo/{perCod}/{carProCod}/quitar` y
`.../reactivar`) → `ProyectoEquipoDao` → las funciones correspondientes.

#### 3.13 `fn_personal_disponible_proyecto`

**Archivo:** `db/functions/42_fn_personal_disponible_proyecto.sql`

**Qué hace:** función de **lectura** (`LANGUAGE sql STABLE`, retorna `TABLE`) que devuelve las
combinaciones (persona, cargo) elegibles para asignar a un proyecto específico: autorización
activa en `g1c_per_car`, persona activa, y que **no** esté ya asignada activamente a ese proyecto
con ese cargo (las asignaciones retiradas sí se ofrecen, porque `sp_proyecto_equipo_asignar` las
reactivaría en vez de duplicarlas).

**Por qué existe:** alimenta directamente el `<select>` del formulario "Asignar miembro" — sin
ella, ese filtro (3 condiciones cruzando 3 tablas) se repetiría en Java cada vez que se necesite.

**Dónde se usa:** `src/main/java/edu/unsa/eps/gestproy/dao/ProyectoEquipoDao.java:41` —
`SELECT * FROM fn_personal_disponible_proyecto(?, ?::smallint, ?::smallint)`.

**Flujo completo:** `ProyectoController` (`GET /proyectos/{cli}/{tip}/{sec}/equipo`, para poblar el
formulario) → `ProyectoEquipoDao.disponibles()` → `fn_personal_disponible_proyecto`.

### Grupo 5x — Registro de avance

#### 3.14 `sp_proyecto_avance_registrar`

**Archivo:** `db/functions/50_sp_proyecto_avance_registrar.sql`

**Qué hace:** registra un movimiento de horas/minutos trabajados por un miembro del equipo en una
etapa (`g1t_pro_mov`). Valida: proyecto activo y no cerrado, etapa activa en `gzz_etp_pro`, persona
activamente asignada al proyecto con ese cargo (misma regla que el trigger 2.2, validada antes para
dar mensaje claro), fecha no futura, horas `0-23`, minutos `0-59`, y que el tiempo trabajado no sea
cero. Inserta con `sec_etp = NULL` explícito para que el trigger `trg_promov_autonumera_sec_etp`
(§2.3) lo calcule, y usa `RETURNING sec_etp` para devolver a Java el valor generado.

**Dónde se usa:** `src/main/java/edu/unsa/eps/gestproy/dao/ProyectoAvanceDao.java:66`.

**Flujo completo:** `ProyectoAvanceController` (`POST /proyectos/{cli}/{tip}/{sec}/avance`) →
`ProyectoAvanceDao.registrar()` → `sp_proyecto_avance_registrar` → dispara
`trg_promov_valida_eqp_activo` y `trg_promov_autonumera_sec_etp`.

#### 3.15 `fn_proyecto_pct_avance`

**Archivo:** `db/functions/51_fn_proyecto_pct_avance.sql`

**Qué hace:** función de lectura que calcula
`% avance = (horas trabajadas en movimientos activos del proyecto) / (suma de etp_tie_est de TODAS
las etapas activas del catálogo gzz_etp_pro) × 100`, redondeado a 2 decimales. Puede superar 100%
si se trabajó más de lo estimado; si no hay etapas activas, retorna 0 (usa `NULLIF` para evitar
división por cero).

**Decisión de modelado documentada en el propio archivo:** `gzz_etp_pro` es el catálogo **único**
de etapas y aplica a todo proyecto por igual — no existe una tabla de "plan de etapas por
proyecto". Es una simplificación consciente, no un bug.

**Dónde se usa:** no se llama directamente desde Java — se invoca **dentro de la vista**
`v_proyecto_avance` (`db/views/v_proyecto_avance.sql:22`), que sí se consulta desde
`ProyectoAvanceDao.java:24` (ver §1.3). Es decir: `ProyectoAvanceDao` → `v_proyecto_avance` →
`fn_proyecto_pct_avance` (una función usada por una vista, ambas alcanzadas desde el mismo DAO).

### Grupo 6x — Autenticación

#### 3.16 `fn_usuario_autenticar`

**Archivo:** `db/functions/60_fn_usuario_autenticar.sql`

**Qué hace:** verifica login + contraseña contra el hash almacenado en `g1s_usuario`, usando
`pgcrypto`: `crypt(p_pass, usu_pass_hash)` vuelve a hashear la contraseña recibida con la misma sal
ya codificada dentro de `usu_pass_hash` (así funciona `crypt`/Blowfish) y compara el resultado.
Retorna `TRUE` solo si el usuario existe, está activo (`usu_est_reg_cod = 'A'`) y el hash coincide.
Es función de lectura pura (`LANGUAGE sql STABLE`): nunca escribe.

**Por qué existe:** la contraseña en texto plano nunca sale de PostgreSQL hacia Java sin
comparar — la validación del hash ocurre íntegramente dentro de la BD; Java solo recibe un booleano.

**Dónde se usa:** `src/main/java/edu/unsa/eps/gestproy/dao/UsuarioDao.java:18` —
`SELECT fn_usuario_autenticar(?, ?)`. También referenciada en el Javadoc de
`src/main/java/edu/unsa/eps/gestproy/web/AuthController.java:18`.

**Flujo completo:** `AuthController` (`POST /login`, línea 42: `if (!usuarioDao.autenticar(login,
password))`) → `UsuarioDao.autenticar()` → `fn_usuario_autenticar` → si es `true`, se emite el JWT
en cookie (login por cookie, no por sesión de servidor).

---

## 4. Tabla resumen de trazabilidad

| Objeto SQL | Tipo | Archivo `db/` | DAO Java (archivo:línea) | Endpoint/Controller |
|---|---|---|---|---|
| `v_proyecto_resumen` | Vista | `views/v_proyecto_resumen.sql` | `ProyectoDao.java:15` | `ProyectoController` (listado/detalle) |
| `v_proyecto_equipo` | Vista | `views/v_proyecto_equipo.sql` | `ProyectoEquipoDao.java:24` | `ProyectoController` (`/equipo`) |
| `v_proyecto_avance` | Vista | `views/v_proyecto_avance.sql` | `ProyectoAvanceDao.java:24` | `ProyectoAvanceController` |
| `trg_proeqp_valida_percar_activo` | Trigger | `triggers/01_...sql` | (indirecto vía §3.11/3.12) | `ProyectoEquipoDao.java:52,64` |
| `trg_promov_valida_eqp_activo` | Trigger | `triggers/02_...sql` | (indirecto vía §3.14) | `ProyectoAvanceDao.java:66` |
| `trg_promov_autonumera_sec_etp` | Trigger | `triggers/03_...sql` | (indirecto vía §3.14) | `ProyectoAvanceDao.java:66` |
| `trg_procab_valida_fechas` | Trigger | `triggers/04_...sql` | (indirecto vía §3.8/3.9/3.10) | `ProyectoDao.java:63,73,86` |
| `sp_ref_grupoa_mant` | SP escritura | `functions/10_...sql` | `ReferencialDao.java:76` | `ReferencialController` |
| `sp_gzz_tip_pro_mant` | SP escritura | `functions/11_...sql` | `ReferencialDao.java:67` | `ReferencialController` |
| `sp_gzz_lin_pro_mant` | SP escritura | `functions/11_...sql` | `ReferencialDao.java:70` | `ReferencialController` |
| `sp_gzz_etp_pro_mant` | SP escritura | `functions/11_...sql` | `ReferencialDao.java:73` | `ReferencialController` |
| `sp_cliente_mant` | SP escritura | `functions/20_...sql` | `ClienteDao.java:57,65` | `ClienteController` |
| `sp_personal_mant` | SP escritura | `functions/21_...sql` | `PersonalDao.java:51,58` | `PersonalController` |
| `sp_per_car_mant` | SP escritura | `functions/22_...sql` | `PersonalDao.java:82` | `PersonalController` (`/cargos`) |
| `sp_proyecto_crear` | SP escritura | `functions/30_...sql` | `ProyectoDao.java:63` | `ProyectoController` (crear) |
| `sp_proyecto_editar` | SP escritura | `functions/31_...sql` | `ProyectoDao.java:73` | `ProyectoController` (editar) |
| `sp_proyecto_cambiar_estado` | SP escritura | `functions/32_...sql` | `ProyectoDao.java:86` | `ProyectoController` (`/estado`) |
| `sp_proyecto_equipo_asignar` | SP escritura | `functions/40_...sql` | `ProyectoEquipoDao.java:52` | `ProyectoController` (`/equipo`) |
| `sp_proyecto_equipo_quitar` | SP escritura | `functions/41_...sql` | `ProyectoEquipoDao.java:58` | `ProyectoController` (`/quitar`) |
| `sp_proyecto_equipo_reactivar` | SP escritura | `functions/41_...sql` | `ProyectoEquipoDao.java:64` | `ProyectoController` (`/reactivar`) |
| `fn_personal_disponible_proyecto` | SP lectura | `functions/42_...sql` | `ProyectoEquipoDao.java:41` | `ProyectoController` (`/equipo`, form) |
| `sp_proyecto_avance_registrar` | SP escritura | `functions/50_...sql` | `ProyectoAvanceDao.java:66` | `ProyectoAvanceController` |
| `fn_proyecto_pct_avance` | SP lectura | `functions/51_...sql` | (vía `v_proyecto_avance`) | `ProyectoAvanceController` |
| `fn_usuario_autenticar` | SP lectura | `functions/60_...sql` | `UsuarioDao.java:18` | `AuthController` (`/login`) |

**Cobertura:** las 3 vistas, los 4 triggers y las 17 funciones/procedimientos de `db/functions/`
(14 archivos) tienen al menos un punto de uso verificado en el código Java — no hay objetos huérfanos.

## 5. Cómo verificar esto por tu cuenta

Cualquier afirmación de este documento se puede reproducir con dos búsquedas:

```powershell
# Nombre del objeto -> dónde se invoca desde Java
Select-String -Path src\main\java\edu\unsa\eps\gestproy\**\*.java -Pattern "nombre_del_objeto"

# Confirmar que el objeto SQL existe con esa definición
Get-Content db\functions\NN_archivo.sql   # o db\views\..., db\triggers\...
```

También existe `db/tests/smoke_tests.sql` (ver `db/README.md`), que ejercita en una transacción
con `ROLLBACK` final las 3 vistas, los 4 triggers y las funciones de negocio — es la forma más
directa de comprobar que el comportamiento descrito aquí sigue siendo cierto.
