# Guía simple: la carpeta `db/`

Este documento explica, en palabras simples, **qué es y para qué sirve** cada carpeta y cada archivo dentro de `db/`. Es el lugar donde vive **toda la base de datos** del proyecto: no solo las tablas, sino también las reglas de negocio (ver [`05-VISTAS-TRIGGERS-PROCEDIMIENTOS.md`](05-VISTAS-TRIGGERS-PROCEDIMIENTOS.md) y [`06-GUIA-SIMPLE-VISTAS-TRIGGERS-PA.md`](06-GUIA-SIMPLE-VISTAS-TRIGGERS-PA.md) para el detalle de esas reglas).

## Idea general: ¿por qué está dividida en tantas carpetas?

Cada carpeta representa una "capa" distinta de la base de datos, y **el orden en que se aplican importa**: primero tiene que existir la tabla antes de poder ponerle un trigger, y tiene que existir el trigger antes de poder probarlo. Por eso la carpeta está organizada así:

```
schema/    → 1º: crea las tablas (vacías)
triggers/  → 2º: pone los "vigilantes" sobre esas tablas
functions/ → 3º: pone las "recetas" que escriben en esas tablas
views/     → 4º: arma las "fotos combinadas" de lectura
seed/      → 5º: llena las tablas con datos de ejemplo
tests/     → (aparte) prueba que todo lo anterior funcione bien
scripts/   → (aparte) automatiza aplicar todo esto de un tirón
```

Cada archivo dentro de cada carpeta además tiene un **número al inicio del nombre** (`01_`, `02_`, `10_`, `20_`...) que indica el orden exacto en que debe ejecutarse dentro de esa carpeta — igual que capítulos de un libro.

---

## `db/README.md`

**Qué es:** el manual de instrucciones de toda la carpeta `db/`.
**Por qué existe:** para que cualquiera (tú en seis meses, un compañero nuevo) sepa en qué orden aplicar los scripts, qué convenciones de nombres se usan, y cómo se prueban los cambios, sin tener que adivinarlo leyendo 30 archivos sueltos.

---

## `db/schema/` — las tablas (la "estructura" vacía)

Son los planos de la base de datos: qué tablas existen, qué columnas tiene cada una, y cómo se relacionan entre sí. Aquí no hay ninguna regla de negocio todavía, solo la forma de los datos.

- **`01_referenciales.sql`** — crea los **catálogos** (`gzz_*`): tipos de cliente, estados de cliente, tipos de proyecto, líneas de proyecto, estados de proyecto, cargos de personal, cargos de proyecto, etapas de proyecto, y el catálogo maestro `gzz_est_reg` (los valores `A`/`I`/`*` que usa todo el sistema para "activo/inactivo/eliminado"). Va primero porque **todo lo demás depende de estos catálogos** (por ejemplo, un cliente necesita que ya exista su tipo de cliente).
- **`02_maestras.sql`** — crea las tablas **principales** (`g1m_*`): `g1m_clientes` y `g1m_personal`. Van después de los catálogos porque cada cliente y cada persona necesitan un catálogo válido (tipo de cliente, cargo de personal).
- **`03_relacion.sql`** — crea `g1c_per_car`, la tabla que dice **qué cargos de proyecto puede ejercer cada persona** (ej. "Lucía puede ser Desarrolladora"). Es una tabla intermedia entre personal y catálogo de cargos, por eso va después de ambas.
- **`04_transaccionales.sql`** — crea las tablas del **día a día del negocio** (`g1t_*`): `g1t_pro_cab` (cabecera del proyecto), `g1t_pro_eqp` (equipo asignado a un proyecto) y `g1t_pro_mov` (horas trabajadas registradas). Van al final porque dependen de todo lo anterior (cliente, tipo de proyecto, persona, autorización de cargo).
- **`05_indices.sql`** — crea **índices** (atajos de búsqueda) sobre las columnas que se usan para relacionar tablas. Existe porque, a diferencia de MySQL, PostgreSQL **no crea automáticamente** un índice en cada columna que referencia a otra tabla — hay que pedirlo explícitamente, o las consultas se vuelven lentas a medida que crecen los datos.
- **`06_usuario.sql`** — crea `g1s_usuario`, la tabla con la **única cuenta de administrador** (`admi`) que puede iniciar sesión. Guarda solo un *hash* de la contraseña (nunca la contraseña real), calculado con la extensión `pgcrypto` que este mismo archivo activa. Va al final porque es una pieza independiente del resto (login), no del negocio de proyectos.

---

## `db/triggers/` — los "vigilantes" automáticos

Ver el detalle de qué valida cada uno en `06-GUIA-SIMPLE-VISTAS-TRIGGERS-PA.md`. En resumen, cada archivo aquí define **una regla que se cumple sola**, sin que nadie tenga que acordarse de invocarla:

- **`01_trg_proeqp_valida_percar_activo.sql`** — evita asignar a alguien a un proyecto con un cargo que no tiene autorizado.
- **`02_trg_promov_valida_eqp_activo.sql`** — evita registrar horas de alguien que no está activo en el proyecto.
- **`03_trg_promov_autonumera_sec_etp.sql`** — numera solo los movimientos de horas (1, 2, 3...).
- **`04_trg_procab_valida_fechas.sql`** — evita fechas de proyecto sin sentido (ej. entrega antes que inicio).

**Por qué van en carpeta aparte y antes que las funciones:** aunque las funciones (`sp_*`) ya validan casi todo esto por su cuenta antes de escribir, los triggers son el **respaldo final dentro de la tabla misma** — funcionan incluso si en el futuro alguien escribe en la tabla sin pasar por la función. Van antes que `functions/` porque una función que hace un `INSERT` sobre una tabla con trigger conviene probarla ya con el trigger puesto.

---

## `db/functions/` — las "recetas" que hacen el trabajo (PA)

Ver el detalle de cada una en `06-GUIA-SIMPLE-VISTAS-TRIGGERS-PA.md`. Los números del nombre agrupan las funciones por el **módulo de la app** al que pertenecen — así, si vas a tocar algo de "Personal", sabes que solo tienes que mirar los archivos `2x`:

- **`10_sp_ref_grupoa_mant.sql`** — una única función genérica que mantiene **6 catálogos simples** (tipo cliente, estado cliente, estado proyecto, cargo personal, cargo proyecto, estado registro). Es una sola porque las 6 tablas tienen exactamente la misma forma (código + descripción + estado).
- **`11_sp_ref_grupob_mant.sql`** — 3 funciones separadas para los **3 catálogos que sí tienen una columna extra** (tipo de proyecto y línea de proyecto tienen "tamaño"; etapa de proyecto tiene "horas estimadas"), por lo que no pueden compartir la función genérica de arriba.
- **`20_sp_cliente_mant.sql`** — crea/modifica/inactiva/elimina un **cliente**.
- **`21_sp_personal_mant.sql`** — crea/modifica una **persona** del personal.
- **`22_sp_per_car_mant.sql`** — autoriza/retira el **cargo de proyecto** que puede ejercer una persona.
- **`30_sp_proyecto_crear.sql`** — crea un **proyecto nuevo**, calculando su número de secuencia.
- **`31_sp_proyecto_editar.sql`** — modifica los datos de un proyecto existente.
- **`32_sp_proyecto_cambiar_estado.sql`** — cambia el estado de un proyecto, validando que el cambio tenga sentido (no se puede "cerrar" un proyecto que ni siquiera empezó).
- **`40_sp_proyecto_equipo_asignar.sql`** — asigna una persona al equipo de un proyecto.
- **`41_sp_proyecto_equipo_quitar_reactivar.sql`** — 2 funciones: retirar y reincorporar a alguien del equipo (sin borrar su historial).
- **`42_fn_personal_disponible_proyecto.sql`** — calcula qué personas (con qué cargo) se pueden asignar todavía a un proyecto.
- **`50_sp_proyecto_avance_registrar.sql`** — registra horas trabajadas por una persona en una etapa del proyecto.
- **`51_fn_proyecto_pct_avance.sql`** — calcula el porcentaje de avance de un proyecto.
- **`60_fn_usuario_autenticar.sql`** — verifica usuario y contraseña para el login, sin que la contraseña real salga nunca de la base de datos.

**Por qué existen como funciones y no como código Java:** para que la regla de negocio (qué es válido, qué no) viva en **un solo lugar** — la base de datos — sin importar si en el futuro otra aplicación distinta a este proyecto Java también necesita escribir esos mismos datos; nunca podría saltarse la regla.

---

## `db/views/` — las "fotos ya armadas" de lectura

Ver detalle en `06-GUIA-SIMPLE-VISTAS-TRIGGERS-PA.md`.

- **`v_proyecto_resumen.sql`** — proyecto + nombre de cliente + descripción de tipo + descripción de estado, todo junto.
- **`v_proyecto_equipo.sql`** — miembros de un proyecto + su nombre + su cargo + su costo por hora.
- **`v_proyecto_avance.sql`** — horas estimadas, horas trabajadas y porcentaje de avance de un proyecto.

**Por qué van después de `functions/`:** `v_proyecto_avance` usa por dentro la función `fn_proyecto_pct_avance`, así que esa función tiene que existir antes de poder crear la vista que la usa.

---

## `db/seed/` — datos de ejemplo para poder probar la app

Sin esto, al levantar la base de datos por primera vez todas las tablas estarían vacías y la aplicación no tendría nada que mostrar.

- **`01_seed_referenciales.sql`** — llena los catálogos (`gzz_*`) con valores reales de ejemplo: tipos de cliente, tipos de proyecto, cargos, etapas con sus horas estimadas, etc.
- **`02_seed_maestras.sql`** — crea 5 clientes y 6 personas de ejemplo, más las autorizaciones de qué cargo puede ejercer cada persona.
- **`03_seed_transaccional.sql`** — crea 2 proyectos de ejemplo, les asigna equipo, y registra varios movimientos de horas — esto es lo que permite ver la barra de "Avance" con datos reales sin tener que cargarlos a mano desde la interfaz.
- **`04_seed_usuario.sql`** — crea la cuenta de administrador (`admi`) con una contraseña de referencia para desarrollo (**hay que cambiarla antes de usar el sistema en serio**).

**Por qué va al final de todo (después de `views/`):** porque para insertar, por ejemplo, un movimiento de horas de ejemplo, ya tienen que existir los triggers que lo validan y le ponen la secuencia — si el seed se aplicara antes que los triggers, los datos de ejemplo quedarían mal numerados o inconsistentes.

---

## `db/tests/smoke_tests.sql`

**Qué es:** un script que hace una batería de comprobaciones ("¿el trigger de fechas realmente rechaza una fecha inválida?", "¿la función de cambiar estado realmente respeta la secuencia planificado→ejecución→...?") contra la base de datos real.
**Por qué existe:** para poder verificar, después de cualquier cambio en `schema/`, `triggers/`, `functions/` o `views/`, que nada se rompió — sin tener que probar manualmente cada caso desde la interfaz web. Todo el script corre dentro de una transacción que termina en `ROLLBACK`, así que **no deja ningún dato de prueba en la base de datos**, se puede correr todas las veces que haga falta.

---

## `db/scripts/` — automatización para aplicar y probar todo

- **`apply-all.ps1`** — aplica, en el orden correcto, los 31 archivos `.sql` de `schema/`, `triggers/`, `functions/`, `views/` y `seed/` de un tirón, para no tener que ejecutar cada archivo a mano uno por uno (y sin arriesgarse a aplicarlos en el orden equivocado).
- **`run-tests.ps1`** — ejecuta `tests/smoke_tests.sql` contra la base de datos y avisa con un mensaje claro si algo falló.

**Por qué existen:** para que inicializar o verificar la base de datos sea **un solo comando**, en vez de recordar de memoria el orden y el nombre exacto de 30+ archivos.
