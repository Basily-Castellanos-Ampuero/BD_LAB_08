# GestProy — Flujos del Sistema

> Recorrido paso a paso de cada caso de uso: qué clic hace el usuario, qué
> ruta HTTP se dispara, qué método Java se ejecuta en cada capa, qué función
> SQL corre en PostgreSQL, qué triggers pueden dispararse y qué ve el
> usuario al final (éxito o el mensaje de error). Complementa a
> [`01-METODOS-Y-LOGICA.md`](01-METODOS-Y-LOGICA.md), que explica cada
> pieza por separado; aquí se explica cómo se **encadenan**.

---

## Índice de flujos

1. [Mantenimiento genérico de un catálogo](#1-mantenimiento-genérico-de-un-catálogo)
2. [Alta de un cliente](#2-alta-de-un-cliente)
3. [Alta de personal y autorización de cargos](#3-alta-de-personal-y-autorización-de-cargos)
4. [Creación de un proyecto](#4-creación-de-un-proyecto)
5. [Edición de un proyecto](#5-edición-de-un-proyecto)
6. [Cambio de estado de un proyecto](#6-cambio-de-estado-de-un-proyecto)
7. [Asignación de equipo a un proyecto](#7-asignación-de-equipo-a-un-proyecto)
8. [Quitar / reactivar un miembro del equipo](#8-quitar--reactivar-un-miembro-del-equipo)
9. [Registro de avance (horas trabajadas)](#9-registro-de-avance-horas-trabajadas)
10. [Consulta de listados y vistas](#10-consulta-de-listados-y-vistas)
11. [Manejo de errores de punta a punta](#11-manejo-de-errores-de-punta-a-punta)
12. [Arranque de la aplicación](#12-arranque-de-la-aplicación)
13. [Instalación / reinstalación de la base de datos](#13-instalación--reinstalación-de-la-base-de-datos)
14. [Ejecución de los tests de humo](#14-ejecución-de-los-tests-de-humo)
15. [Inicio de sesión, modo solo vista y bloqueo de escrituras](#15-inicio-de-sesión-modo-solo-vista-y-bloqueo-de-escrituras)

---

## 1. Mantenimiento genérico de un catálogo

Aplica a las 9 tablas `gzz_*` (tipos de cliente, estados, cargos, etapas,
etc.) a través de un único controlador genérico. Ejemplo con "Tipos de Cliente".

**Adicionar:**
1. Usuario hace clic en *Catálogos → Tipos de Cliente* → `GET /referenciales/tip_cli`.
   `ReferencialController.listar()` resuelve `tip_cli` a `ReferencialTabla.TIP_CLI`
   con `@ModelAttribute("tabla")`; si el slug no existiera, se respondería `404`.
2. `ReferencialService.listar(tabla)` → `ReferencialDao.listar(tabla)` arma un
   `SELECT` con los nombres de columna que trae el enum y renderiza `referenciales/list.html`.
3. Clic en *Adicionar* → `GET /referenciales/tip_cli/nuevo` → formulario vacío.
4. Usuario llena código y descripción, envía → `POST /referenciales/tip_cli`.
5. `ReferencialController.adicionar()` llama
   `ejecutar(() -> service.adicionar(tabla, registro), "...", ra)`.
6. `ReferencialService.adicionar` → `ReferencialDao.mantener(tabla, "ADICIONAR", r)`
   → como `TIP_CLI` es Grupo A, invoca `SELECT sp_ref_grupoa_mant('gzz_tip_cli', 'ADICIONAR', ?, ?)`.
7. En PL/pgSQL: valida código no vacío, descripción no vacía, y que el
   código no exista ya. Si todo pasa, `INSERT ... est_reg='A'`.
8. Si algo falla, `RAISE EXCEPTION` → llega a Java como `DataAccessException`
   → `ejecutar()` lo captura, guarda el mensaje como flash `error` y redirige
   de vuelta al formulario (`/nuevo`). Si todo sale bien, flash `exito` y
   redirige al listado.

**Modificar / Eliminar / Inactivar / Reactivar**: mismo esqueleto — GET
`/{cod}/editar` llena el formulario (404 si no existe), POST confirma; los
cambios de estado (`Eliminar/Inactivar/Reactivar`) son botones del listado
que hacen `POST /{cod}/{accion}` directamente, sin pasar por un formulario.

Para las 3 tablas del Grupo B (`tip_pro`, `lin_pro`, `etp_pro`) el flujo es
idéntico, pero `ReferencialDao.mantener` enruta a la función específica
(`sp_gzz_tip_pro_mant`, etc.) porque tienen una columna extra (`tam` o `tie_est`).

---

## 2. Alta de un cliente

1. `GET /clientes/nuevo` → `ClienteController.nuevo()` carga en el modelo
   los tipos de cliente y estados de cliente **activos** (para los `<select>`)
   y muestra `clientes/form.html`.
2. Usuario llena nombre, tipo, estado, fecha de ingreso → `POST /clientes`.
3. `ClienteController.adicionar()` → `ejecutar(() -> service.adicionar(cliente), ...)`.
4. `ClienteService.adicionar` → `ClienteDao.mantener("ADICIONAR", cliente)`
   → `SELECT sp_cliente_mant('ADICIONAR', cod, nom, tip_cod, est_cod, fec_ing, fec_ces, fec_ult_pro_cer)`.
5. En PL/pgSQL (`sp_cliente_mant`): valida nombre no vacío, `tip_cod` activo
   en `gzz_tip_cli`, `est_cod` activo en `gzz_est_cli`, y
   `fec_ing <= fec_ces` (si ambas se completaron). Si el código ya existe, error.
6. Éxito → `INSERT` con `cli_est_reg_cod = 'A'` → flash de éxito → `redirect:/clientes`.
7. Error típico que puede ver el usuario: *"El tipo de cliente XX no existe
   o no está activo"* — ocurre si el `<select>` se generó con datos ya
   caducados (poco probable en uso normal, posible si dos pestañas están
   abiertas y en una se inactivó el catálogo).

---

## 3. Alta de personal y autorización de cargos

1. `GET /personal/nuevo` → formulario con cargos de personal activos.
2. `POST /personal` → `sp_personal_mant('ADICIONAR', cod, nom, car_cod, cos_hor, fec_ing)`.
   Valida costo/hora > 0 y fecha de ingreso no futura.
3. Una vez creada la persona, para que pueda ser miembro de un **equipo de
   proyecto** con un cargo específico, hay que autorizarla:
   `GET /personal/{cod}/cargos` → lista sus autorizaciones actuales +
   `<select>` de cargos de proyecto activos.
4. `POST /personal/{cod}/cargos` con `carProCod` → `PersonalController.adicionarCargo()`
   → `PersonalService.adicionarCargo` → `PersonalDao.mantenerCargo("ADICIONAR", ...)`
   → `sp_per_car_mant('ADICIONAR', per_cod, car_pro_cod)`.
5. En PL/pgSQL: valida persona activa y cargo de proyecto activo. Si la
   autorización no existía → `INSERT`. Si existía inactiva/eliminada → se
   **reactiva** con `UPDATE` (no falla por PK duplicada). Si ya estaba
   activa → error *"ya tiene autorizado el cargo de proyecto"*.
6. Esta autorización es la que después consulta el trigger
   `trg_proeqp_valida_percar_activo` cuando se intenta asignar esta persona
   a un equipo de proyecto (flujo 7) — sin este paso previo, la asignación
   siempre fallará.

---

## 4. Creación de un proyecto

1. `GET /proyectos/nuevo` → `ProyectoController.nuevo()` carga clientes
   activos y tipos de proyecto activos.
2. Usuario elige cliente y tipo, completa fechas/montos → `POST /proyectos`.
3. `ProyectoController.crear()` llama `ProyectoService.crear(proyecto)` →
   `ProyectoDao.crear(p)` → `SELECT sp_proyecto_crear(cli_cod, tip_cod::smallint,
   fec_con, fec_pac, mon_pre, cos_pre, gas_pre)`.
4. En PL/pgSQL:
   - Valida cliente activo y tipo de proyecto activo.
   - Calcula `pro_sec = MAX(pro_sec)+1` para ese cliente+tipo (empieza en 1).
   - Calcula `pro_uti_pre = monto - costo - gasto`.
   - `INSERT` con `pro_est_cod='01'` (Planificado), `pro_est_reg_cod='A'`.
   - Antes de que el `INSERT` se confirme, el trigger `trg_procab_valida_fechas`
     revisa que `fec_con <= fec_pac` — si no, aborta con `RAISE EXCEPTION`
     y **nada** se inserta (todo el `INSERT` es una sola operación atómica).
5. La función retorna el `pro_sec` generado; el controlador redirige a
   `GET /proyectos/{cliCod}/{tipCod}/{sec}` (el detalle del proyecto recién
   creado) con un flash indicando la secuencia asignada.
6. **Nota importante para quien nunca vio PK compuestas**: un mismo cliente
   puede tener varios proyectos del mismo tipo — cada uno con su propio
   `pro_sec` correlativo (1, 2, 3...). La URL siempre necesita los tres
   valores (`cliCod/tipCod/sec`) para identificar un proyecto exacto.

---

## 5. Edición de un proyecto

1. `GET /proyectos/{cli}/{tip}/{sec}/editar` → 404 si el proyecto no existe.
2. `POST /proyectos/{cli}/{tip}/{sec}` → `ProyectoDao.editar(p)` →
   `SELECT sp_proyecto_editar(cli, tip, sec, fec_con, fec_pac, fec_ini, fec_ent,
   mon_pre, mon_rea, cos_pre, cos_rea, gas_pre, gas_rea)`.
3. En PL/pgSQL: busca el proyecto; si `pro_est_reg_cod <> 'A'` o
   `pro_est_cod = '04'` (Cerrado), aborta con error — **un proyecto cerrado
   es inmutable** salvo por la propia transición de estado.
4. Si pasa, recalcula `pro_uti_pre`/`pro_uti_rea` y hace `UPDATE`. El
   trigger de fechas vuelve a correr sobre los nuevos valores.
5. Éxito → redirige al detalle; error → redirige de vuelta al formulario de
   edición con el mensaje flash.

---

## 6. Cambio de estado de un proyecto

Este es el flujo con más efectos secundarios encadenados del sistema.

1. Desde el detalle del proyecto (`GET /proyectos/{cli}/{tip}/{sec}`), el
   usuario elige un nuevo estado del `<select>` de estados activos y envía
   → `POST /proyectos/{cli}/{tip}/{sec}/estado` con `nuevoEstado`.
2. `ProyectoController.cambiarEstado()` → `ProyectoService.cambiarEstado`
   → `ProyectoDao.cambiarEstado` → `SELECT sp_proyecto_cambiar_estado(cli, tip, sec, nuevoEstado)`.
3. En PL/pgSQL:
   - Verifica que el proyecto exista, esté activo, y que el nuevo estado no
     sea igual al actual.
   - Verifica la **matriz de transiciones** (ver tabla en
     `01-METODOS-Y-LOGICA.md` §A.5) — por ejemplo, no se puede pasar de
     `01 Planificado` directo a `03 Entregado`; hay que pasar por `02`.
   - Si la transición es válida, hace `UPDATE pro_est_cod` y, según el
     destino, autocompleta `pro_fec_ini`/`pro_fec_ent`/`pro_fec_cer` **solo
     si esos campos aún eran `NULL`** (no pisa una fecha ya cargada a mano).
   - Si el destino es `04` (Cerrado), **además** actualiza
     `g1m_clientes.cli_fec_ult_pro_cer` del cliente dueño — este es un
     efecto en cascada sobre una tabla distinta a la que se está editando,
     que solo ocurre en esta función.
4. Éxito → flash "Estado del proyecto actualizado" y recarga el detalle
   (ahí se ven las fechas recién autocompletadas). Error típico: *"Transición
   de estado no permitida: 01 -> 03"*.

---

## 7. Asignación de equipo a un proyecto

1. `GET /proyectos/{cli}/{tip}/{sec}/equipo` → `ProyectoController.equipo()`
   carga el equipo actual (`ProyectoService.equipo` → `v_proyecto_equipo`) y
   las personas **disponibles** (`ProyectoService.disponibles` →
   `fn_personal_disponible_proyecto`, que ya excluye a quien está activo).
2. El `<select>` de "disponibles" usa como `value` la clave compuesta
   `"perCod|carProCod"` (ver `PersonalDisponible.clave()`).
3. `POST /proyectos/{cli}/{tip}/{sec}/equipo` con `asignacion=perCod|carProCod`.
4. `ProyectoController.asignar()` parte el string por `|` y llama
   `ProyectoService.asignarEquipo(cli, tip, sec, perCod, carProCod)` →
   `ProyectoEquipoDao.asignar` → `SELECT sp_proyecto_equipo_asignar(...)`.
5. En PL/pgSQL:
   - El proyecto debe existir, estar activo y **no** estar cerrado.
   - La autorización `(perCod, carProCod)` debe existir y estar **activa**
     en `g1c_per_car` (si no, error — recordar el flujo 3).
   - Si la fila de equipo no existía → `INSERT`. Si existía retirada → se
     reactiva. Si ya estaba activa → error "ya está asignada".
6. El trigger `trg_proeqp_valida_percar_activo` corre como segunda barrera
   sobre el mismo `INSERT`/`UPDATE` — en la práctica nunca debería
   dispararse (la función ya validó lo mismo), pero protege contra
   cualquier otro camino de escritura que se agregue en el futuro sin pasar
   por esta función.
7. Éxito → recarga `/equipo` con la persona ya en la lista de miembros
   activos y removida de "disponibles" (porque `fn_personal_disponible_proyecto`
   ya no la ofrece).

---

## 8. Quitar / reactivar un miembro del equipo

1. Desde la lista de equipo, botón "Quitar" → `POST
   /proyectos/{cli}/{tip}/{sec}/equipo/{perCod}/{carProCod}/quitar` →
   `sp_proyecto_equipo_quitar(...)`.
2. En PL/pgSQL: exige que la fila exista y esté `'A'`; la pasa a `'I'`. **No
   se borra nada** — el historial de horas que esa persona ya registró en
   `g1t_pro_mov` sigue intacto y visible.
3. Efecto inmediato: si ahora alguien intenta registrar horas para esa
   persona en ese proyecto (flujo 9), el trigger
   `trg_promov_valida_eqp_activo` lo rechazará, porque exige
   `pro_per_car_est_reg_cod = 'A'`.
4. Botón "Reactivar" → `POST .../reactivar` → `sp_proyecto_equipo_reactivar(...)`.
   Exige que la fila exista y **no** esté `'A'`; la pasa a `'A'`. En este
   punto el trigger `trg_proeqp_valida_percar_activo` vuelve a comprobar que
   la autorización de cargo en `g1c_per_car` siga activa — si mientras
   tanto se inactivó esa autorización, la reactivación fallará hasta que se
   reautorice (flujo 3).

---

## 9. Registro de avance (horas trabajadas)

1. `GET /proyectos/{cli}/{tip}/{sec}/avance` → `ProyectoAvanceController.avance()`
   carga: el resumen (`v_proyecto_avance`, para la barra de progreso), el
   historial de movimientos, el equipo **activo** del proyecto (para el
   `<select>` de "quién trabajó"), y las etapas **activas** (para el
   `<select>` de "en qué etapa").
2. Usuario elige miembro (`perCod|carProCod`), etapa, fecha, horas y
   minutos → `POST /proyectos/{cli}/{tip}/{sec}/avance`.
3. `ProyectoAvanceController.registrar()` parte `miembro` por `|` y llama
   `ProyectoAvanceService.registrar(...)` → `ProyectoAvanceDao.registrar`
   → `SELECT sp_proyecto_avance_registrar(cli, tip, sec, perCod, carProCod,
   etpCod, fecReg, horTra, minTra)`.
4. En PL/pgSQL (`sp_proyecto_avance_registrar`):
   - Proyecto debe existir, activo, no cerrado.
   - Etapa debe existir y estar activa.
   - La persona+cargo deben estar **activamente** asignados al proyecto
     (mismo criterio que valida después el trigger
     `trg_promov_valida_eqp_activo`, como segunda barrera).
   - Fecha no futura; horas en `[0,23]`; minutos en `[0,59]`; el par no
     puede ser `(0,0)`.
   - `INSERT` con `sec_etp = NULL` explícito.
5. **Antes** de que el `INSERT` se confirme, el trigger
   `trg_promov_autonumera_sec_etp` calcula `sec_etp = MAX(sec_etp)+1` para
   esa combinación exacta (proyecto, persona, cargo, etapa) y lo asigna a
   `NEW.sec_etp` — así el `INSERT` original (que traía `NULL`) termina
   escribiendo con la secuencia correcta.
6. La función retorna el `sec_etp` asignado; el controlador redirige de
   vuelta a `/avance`, donde ahora se recalculan `horas_trabajadas` y
   `pct_avance` (leídos de `v_proyecto_avance`, que a su vez usa
   `fn_proyecto_pct_avance`).
7. Si el % de avance supera 100 (se trabajó más de lo estimado),
   `ProyectoAvance.excedido()` devuelve `true` y la plantilla lo resalta
   visualmente (ver `templates/proyectos/avance.html`).

---

## 10. Consulta de listados y vistas

Todas las pantallas de solo-lectura siguen el mismo patrón, sin pasar por
ninguna función `sp_*` (esas son solo para escritura):

| Pantalla | Ruta | Fuente de datos |
|---|---|---|
| Listado de proyectos | `GET /proyectos` | `v_proyecto_resumen` |
| Detalle de proyecto | `GET /proyectos/{cli}/{tip}/{sec}` | `v_proyecto_resumen` (WHERE PK) |
| Equipo de un proyecto | `GET /proyectos/{cli}/{tip}/{sec}/equipo` | `v_proyecto_equipo` + `fn_personal_disponible_proyecto` |
| Avance de un proyecto | `GET /proyectos/{cli}/{tip}/{sec}/avance` | `v_proyecto_avance` + tabla `g1t_pro_mov` con JOIN |
| Listado de clientes / personal | `GET /clientes`, `GET /personal` | Tabla base + JOIN a su catálogo |
| Catálogos | `GET /referenciales/{tabla}` | Tabla `gzz_*` correspondiente |
| Página de inicio | `GET /` | 3 `COUNT(*)` directos sobre `g1t_pro_cab`/`g1m_clientes`/`g1m_personal` (WHERE activo) |

Ninguna de estas rutas modifica datos: son controladores que solo leen y
pasan el resultado a la plantilla Thymeleaf correspondiente.

---

## 11. Manejo de errores de punta a punta

Ejemplo concreto: usuario intenta cerrar un proyecto que está `Planificado`
(transición `01 → 04`, no permitida).

1. `POST /proyectos/9001/1/3/estado` con `nuevoEstado=04`.
2. `sp_proyecto_cambiar_estado` evalúa la matriz de transiciones, no
   encuentra `(01, 04)` permitida, ejecuta:
   `RAISE EXCEPTION 'Transición de estado no permitida: % -> %', '01', '04';`
3. PostgreSQL propaga esto como un error SQL genérico (`SQLSTATE P0001`).
   El driver JDBC lo envuelve en un `SQLException` cuyo mensaje es algo
   como `"ERROR: Transición de estado no permitida: 01 -> 04\n Where: ..."`
4. `JdbcTemplate.queryForObject(...)` lo relanza como
   `org.springframework.dao.DataAccessException` (una subclase específica,
   normalmente `UncategorizedSQLException`).
5. En el controlador, `MantenimientoControllerBase.ejecutar(...)` había
   envuelto la llamada en un `try/catch (DataAccessException |
   ReglaNegocioException ex)`. Captura el error, llama
   `ErroresBd.extraerMensaje(ex)`:
   - Baja hasta la causa más específica (`getMostSpecificCause()`).
   - Toma solo la primera línea del mensaje (descarta el `Where: ...`).
   - Quita el prefijo `"ERROR: "`.
   - Resultado limpio: `"Transición de estado no permitida: 01 -> 04"`.
6. Ese texto se guarda como `RedirectAttributes.addFlashAttribute("error", ...)`
   y el controlador redirige de vuelta al detalle del proyecto.
7. `fragments/mensajes.html` (incluido en el layout) detecta el atributo
   flash `error` y lo muestra como una alerta Bootstrap roja en la
   siguiente página que se renderiza — el usuario nunca ve una pantalla de
   error genérica ni un stack trace.
8. Si el error hubiera ocurrido en un **GET** (por ejemplo, una consulta
   rota) en vez de en un POST de mantenimiento, no habría pasado por
   `ejecutar(...)` — en ese caso lo atrapa `GlobalExceptionHandler`
   (`@ControllerAdvice` global) y renderiza la plantilla `error.html` con el
   mismo mensaje limpio.

---

## 12. Arranque de la aplicación

1. `mvn spring-boot:run` compila y ejecuta `GestProyApplication.main()`.
2. Spring Boot lee `application.properties`, que a su vez importa
   `./db.properties` (`spring.config.import=optional:file:./db.properties`)
   — ahí están la URL, usuario y contraseña reales de la BD. Este archivo
   **no existe en el repositorio** (está en `.gitignore`); cada quien lo
   crea copiando `db.properties.example`.
3. `spring-boot-starter-jdbc` autoconfigura un `DataSource` y un
   `JdbcTemplate` a partir de esas propiedades — no hay una clase de
   configuración manual (`config/` está vacío a propósito: todo lo maneja
   el autoconfigure de Spring Boot).
4. Se registran todos los `@Repository` (DAOs), `@Service` y `@Controller`
   por escaneo de componentes (paquete raíz `edu.unsa.eps.gestproy`).
5. `HomeController` responde en `/` contando filas activas de tres tablas —
   si la conexión a PostgreSQL falla, esta ruta ya falla de inmediato,
   sirviendo como primera comprobación de que la BD está accesible.

---

## 13. Instalación / reinstalación de la base de datos

`db/scripts/apply-all.ps1` ejecuta, en este orden estricto, cada script
`.sql` de `db/` contra la base de datos indicada (por defecto
`gestion_proyectos`):

```
schema/01..05  →  triggers/01..04  →  functions/10..51  →  views/*  →  seed/01..03
```

- **Por qué este orden**: cada capa depende de la anterior — las tablas
  deben existir antes que los triggers (se adjuntan a una tabla concreta),
  los triggers antes que las funciones que insertan en esas tablas (para
  que las reglas ya estén activas incluso al cargar el seed), las funciones
  antes que las vistas (`v_proyecto_avance` llama a `fn_proyecto_pct_avance`),
  y todo lo anterior antes que los datos semilla.
- **Advertencia importante**: los scripts de `schema/` hacen
  `DROP TABLE ... CASCADE` antes de recrear cada tabla. Re-ejecutar
  `apply-all.ps1` sobre una base de datos con datos reales **borra todo**
  y la deja solo con el seed. Está pensado para instalar desde cero, no
  para "actualizar" una base ya en uso.
- El seed es **idempotente** (`ON CONFLICT ... DO NOTHING`): se puede
  re-ejecutar sin duplicar filas, siempre que las tablas ya existan con los
  mismos datos previos.

---

## 14. Ejecución de los tests de humo

`db/scripts/run-tests.ps1` ejecuta `db/tests/smoke_tests.sql` contra la BD
indicada:

1. El script abre una transacción (`BEGIN`).
2. Corre 15 grupos de pruebas (`DO $t$ ... $t$`) que ejercitan cada función,
   trigger y vista descritos en este documento (incluida la autenticación,
   `fn_usuario_autenticar`), usando códigos de prueba `9xxx` para no chocar
   con datos reales.
3. Cada aserción que no se cumple dispara su propio
   `RAISE EXCEPTION 'TEST FALLIDO Txx: ...'`, lo que interrumpe el script
   inmediatamente (gracias a `\set ON_ERROR_STOP on`).
4. Al final, pase lo que pase, el script hace `ROLLBACK` — así que **nunca**
   modifica los datos reales de la base, sin importar cuántas veces se
   ejecute ni contra qué entorno.
5. `psql` retorna código de salida `0` si todo pasó, o distinto de `0` si
   algún test falló — el mensaje `TEST FALLIDO Txx: ...` en la salida indica
   exactamente qué regla se rompió.

Ver el detalle de qué verifica cada test en el propio archivo
`db/tests/smoke_tests.sql` (está comentado test por test).

---

## 15. Inicio de sesión, modo solo vista y bloqueo de escrituras

### 15.1 Navegación sin sesión (modo solo vista)

1. Cualquier visitante entra a `http://localhost:8080/` sin ninguna cookie
   de sesión. `JwtAuthFilter` no encuentra la cookie `gp_access` (ni
   `gp_refresh`), así que deja `request.setAttribute("gestproy.admin",
   false)` y continúa la cadena sin bloquear nada — este filtro nunca
   rechaza una petición, solo *identifica*.
2. `GlobalModelAttributes` lee ese atributo y agrega `admin=false` al
   modelo de la vista que se esté renderizando.
3. El visitante puede navegar libremente por listados, detalle de
   proyecto, equipo, avance, catálogos — todo responde `200` porque
   ninguna de esas rutas es POST ni termina en `/nuevo`/`/editar`.
4. En cada plantilla, los botones de Adicionar/Modificar/Eliminar/Asignar/
   Registrar/Cambiar estado están envueltos en `th:if="${admin}"` — con
   `admin=false` simplemente no se renderizan. El visitante ve la
   aplicación "completa" pero sin ningún control de escritura visible.

### 15.2 Intento de escritura sin sesión (bloqueo real, no solo visual)

Aunque los botones estén ocultos, nada impide que alguien arme el `POST` a
mano (`curl`, Postman, o simplemente escribiendo la URL `/clientes/nuevo`
en la barra de direcciones). Este es el camino que realmente protege los datos:

1. `GET /clientes/nuevo` (o cualquier `POST` a cualquier ruta) llega antes
   que nada a `AutorizacionInterceptor.preHandle(...)` — corre dentro del
   ciclo de Spring MVC, **después** de `JwtAuthFilter` (que ya dejó
   `admin=false` en el request) pero **antes** de que el controlador se
   ejecute.
2. La ruta no es `/login` ni `/logout`, `admin` es `false`, y la ruta
   termina en `/nuevo` (o el método es `POST`) → se lanza
   `NoAutorizadoException("Debes iniciar sesión como administrador...")`.
3. Spring MVC captura esa excepción con el `@ExceptionHandler` de
   `GlobalExceptionHandler` anotado `@ResponseStatus(HttpStatus.FORBIDDEN)`
   → responde `403` y renderiza `error.html` con el mensaje.
4. El controlador (`ClienteController.nuevo()` en este ejemplo) **nunca
   llega a ejecutarse** — ni siquiera se consultó la base de datos.

### 15.3 Iniciar sesión

1. El visitante hace clic en "Iniciar sesión" (botón visible en el header
   cuando `admin=false`, ver `fragments/nav.html`) → `GET /login` →
   `AuthController.formulario()` → plantilla `login.html`.
2. Completa usuario (`admi`) y contraseña, envía → `POST /login`
   (`AuthController.iniciarSesion(...)`) — esta ruta está explícitamente
   exceptuada en `AutorizacionInterceptor`, así que llega siempre al
   controlador sin importar si hay sesión o no.
3. `UsuarioDao.autenticar("admi", password)` ejecuta
   `SELECT fn_usuario_autenticar('admi', ?)`. Si la BD devuelve `false`
   (contraseña incorrecta, login inexistente, o cuenta inactivada — la
   función no distingue estos casos a propósito, ver
   `01-METODOS-Y-LOGICA.md` §A.6), el controlador guarda un flash `error`
   y redirige de vuelta a `/login` **sin** escribir ninguna cookie.
4. Si la BD devuelve `true`: `JwtService` genera un access token (expira en
   15 min) y un refresh token (expira en 7 días); `CookieUtil` los escribe
   como `Set-Cookie: gp_access=...; HttpOnly; SameSite=Lax` y
   `Set-Cookie: gp_refresh=...; HttpOnly; SameSite=Lax` en la respuesta del
   redirect a `/`.
5. Desde ese momento, cada petición del navegador incluye automáticamente
   ambas cookies (el navegador las maneja solo, JavaScript no interviene ni
   puede leerlas). `JwtAuthFilter` las valida en cada petición y deja
   `admin=true`: los botones de escritura aparecen y
   `AutorizacionInterceptor` deja pasar los `POST`.

### 15.4 Renovación silenciosa del access token

1. Pasados 15 minutos, `gp_access` expira. La siguiente petición llega con
   una cookie `gp_access` cuya firma es válida pero cuya fecha de
   expiración ya pasó → `JwtService.validarYObtenerLogin(..., "access")`
   devuelve `null`.
2. `JwtAuthFilter` no se da por vencido: intenta con `gp_refresh`. Si esa
   cookie sigue vigente (hasta 7 días), la petición se trata igual como
   autenticada, **y** se genera un access token nuevo que se adjunta a la
   respuesta actual vía `Set-Cookie`.
3. El usuario no nota nada: la página se renderiza normalmente, con
   `admin=true`, y a partir de esa respuesta el navegador ya tiene un
   `gp_access` fresco por otros 15 minutos. Esto es exactamente el patrón
   de Django SimpleJWT en modo cookie: el refresh token nunca se envía "a
   mano" ni se expone a JavaScript, el servidor lo usa por su cuenta para
   mantener la sesión viva.
4. Si también `gp_refresh` expiró (más de 7 días sin actividad), ambas
   validaciones fallan, `admin` vuelve a `false`, y el usuario tiene que
   iniciar sesión de nuevo — no hay ningún registro en la BD que "recuerde"
   la sesión: todo el estado vive en las dos cookies.

### 15.5 Cerrar sesión

1. Con sesión activa, el botón "Cerrar sesión" del header hace
   `POST /logout` → `AuthController.cerrarSesion(...)` (también exceptuado
   del interceptor).
2. `CookieUtil.borrarCookiesSesion(...)` reescribe ambas cookies con
   `Max-Age=0`, lo que le indica al navegador que las descarte de inmediato.
3. Redirige a `/`; la siguiente petición ya no trae `gp_access`/`gp_refresh`
   válidas, así que `admin` vuelve a `false` y la aplicación vuelve al modo
   solo vista.
