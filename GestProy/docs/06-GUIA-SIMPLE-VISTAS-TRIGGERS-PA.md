# Guía simple: vistas, triggers y procedimientos (PA)

Este documento explica, en palabras simples, **qué hace** cada vista, trigger y procedimiento almacenado (PA) de la carpeta `db/`, **por qué existe**, y **en qué página de la aplicación** se usa. Para el detalle técnico con líneas de código exactas, ver [`05-VISTAS-TRIGGERS-PROCEDIMIENTOS.md`](05-VISTAS-TRIGGERS-PROCEDIMIENTOS.md).

## Antes de empezar: ¿qué es cada cosa?

- **Vista (view)** — una "foto" ya armada de varias tablas combinadas y con cálculos hechos. En vez de que Java pida datos sueltos y los combine, le pide a la vista el resultado ya listo. Se usa con `SELECT`, igual que si fuera una tabla normal.
- **Trigger** — un "vigilante" que la base de datos activa sola, automáticamente, cada vez que se inserta o modifica una fila en cierta tabla. Nadie en el código Java lo llama; actúa solo, en silencio, dentro de la base de datos.
- **PA (procedimiento almacenado / función)** — una "receta" completa que vive en la base de datos: recibe datos, valida que tengan sentido, y hace la operación (crear, modificar, eliminar). Java solo le pasa los datos y espera el resultado; toda la lógica de negocio (las reglas de qué es válido y qué no) está adentro.

La razón de fondo para las tres cosas es la misma: **las reglas del negocio viven en la base de datos, no en Java**. Así, sin importar por dónde entren los datos, las reglas siempre se cumplen.

---

## 1. Vistas

### `v_proyecto_resumen`
- **Qué hace:** junta los datos del proyecto con el nombre del cliente, la descripción del tipo de proyecto y la descripción del estado, para no tener que hacer esas combinaciones en Java.
- **Por qué existe:** para no repetir en cada consulta el mismo `JOIN` entre proyecto, cliente, tipo y estado.
- **Dónde se usa:** en la **lista de proyectos** (`/proyectos`) y en el **detalle de un proyecto** (`/proyectos/{cliente}/{tipo}/{sec}`) — cada vez que se ve el nombre del cliente o la descripción del estado junto a un proyecto, viene de esta vista.

### `v_proyecto_equipo`
- **Qué hace:** junta cada miembro asignado a un proyecto con su nombre, su cargo en el proyecto y su costo por hora.
- **Por qué existe:** para mostrar de forma legible quién trabaja en el proyecto sin tener que cruzar manualmente la tabla de asignaciones con la de personas y la de cargos.
- **Dónde se usa:** en la pestaña **"Equipo"** de un proyecto (`/proyectos/{c}/{t}/{s}/equipo`) — la tabla que muestra "Persona — Cargo — Costo/hora — Estado" viene de esta vista.

### `v_proyecto_avance`
- **Qué hace:** calcula cuántas horas se estimaron para el proyecto, cuántas horas ya se trabajaron realmente, y qué porcentaje de avance representa eso.
- **Por qué existe:** para que la barra de progreso del proyecto no se calcule "a mano" en Java, sino que salga de un solo lugar confiable en la base de datos.
- **Dónde se usa:** en la pestaña **"Avance"** de un proyecto (`/proyectos/{c}/{t}/{s}/avance`) — el panel de arriba con la barra de progreso y el porcentaje viene de esta vista, y se recalcula cada vez que se abre esa pantalla.

---

## 2. Triggers

### `trg_proeqp_valida_percar_activo`
- **Qué hace:** cuando se asigna (o reactiva) a alguien en el equipo de un proyecto, revisa que esa persona tenga ese cargo autorizado y activo. Si no, rechaza la operación.
- **Por qué existe:** para que nunca se pueda poner a alguien en un proyecto con un cargo que no tiene autorizado — es una regla que debe cumplirse siempre, la pida quien la pida.
- **Dónde se activa:** en la pestaña **"Equipo"** de un proyecto, al usar el formulario **"Asignar personal"** o el botón **"Reincorporar"**.

### `trg_promov_valida_eqp_activo`
- **Qué hace:** cuando se registra un movimiento de horas trabajadas, revisa que la persona esté activamente asignada al proyecto con ese mismo cargo.
- **Por qué existe:** para no poder registrar horas de alguien que ya no forma parte (activa) del equipo del proyecto.
- **Dónde se activa:** en la pestaña **"Avance"** de un proyecto, al enviar el formulario **"Registrar horas trabajadas"**.

### `trg_promov_autonumera_sec_etp`
- **Qué hace:** le pone automáticamente un número de secuencia a cada movimiento de horas de una persona en una etapa (1, 2, 3...), sin que Java tenga que calcularlo.
- **Por qué existe:** para evitar que dos registros choquen o se pisen si dos personas registran horas al mismo tiempo; la base de datos decide el número de forma segura y consistente.
- **Dónde se activa:** en la misma pestaña **"Avance"**, cada vez que se registra un nuevo movimiento de horas — es lo que hace que el historial se numere solo (1, 2, 3, 4...).

### `trg_procab_valida_fechas`
- **Qué hace:** revisa que las fechas del proyecto tengan sentido cronológico (por ejemplo, que la fecha de contrato no sea posterior a la fecha pactada de entrega, o que el inicio no sea posterior al cierre).
- **Por qué existe:** para evitar proyectos con fechas absurdas, sin importar si el error viene del formulario web o de cualquier otro origen.
- **Dónde se activa:** al **crear un proyecto** (`/proyectos/nuevo`) o al **modificarlo** (botón "Modificar" en el detalle del proyecto) — cualquier fecha inconsistente hace que el formulario muestre un mensaje de error.

---

## 3. Procedimientos y funciones (PA)

### Página de login

- **`fn_usuario_autenticar`** — Verifica el usuario y la contraseña directamente en la base de datos (la contraseña nunca se compara en Java, solo dentro de PostgreSQL). Se usa en la **pantalla de inicio de sesión** (`/login`) al enviar el formulario.

### Página de Clientes

- **`sp_cliente_mant`** — Crea, modifica, inactiva/reactiva o elimina (lógicamente) un cliente, validando que los datos sean correctos. Se usa en el **formulario de clientes** (`/clientes/nuevo` y `/clientes/{cod}/editar`).

### Página de Personal

- **`sp_personal_mant`** — Crea o modifica los datos de una persona del personal. Se usa en el **formulario de personal** (`/personal/nuevo` y `/personal/{cod}/editar`).
- **`sp_per_car_mant`** — Autoriza, inactiva, reactiva o elimina un cargo de proyecto para una persona (por ejemplo, autorizar a alguien como "Analista"). Se usa en la pantalla **"Cargos de proyecto autorizados"** de una persona (`/personal/{cod}/cargos`).

### Página de Proyectos

- **`sp_proyecto_crear`** — Crea un proyecto nuevo, calculando automáticamente su número de secuencia para ese cliente y tipo. Se usa al **crear un proyecto** (`/proyectos/nuevo`).
- **`sp_proyecto_editar`** — Modifica los datos de un proyecto existente (fechas, montos). Se usa al **modificar un proyecto** (botón "Modificar" en el detalle).
- **`sp_proyecto_cambiar_estado`** — Cambia el estado de un proyecto (planificado → en ejecución → entregado → cerrado, o suspendido), validando que la transición sea válida. Se usa en el panel **"Cambiar estado"** del detalle del proyecto.
- **`sp_proyecto_equipo_asignar`** — Asigna una persona con un cargo al equipo del proyecto. Se usa en la pestaña **"Equipo"**, formulario "Asignar personal".
- **`sp_proyecto_equipo_quitar`** / **`sp_proyecto_equipo_reactivar`** — Retiran o reincorporan a una persona del equipo del proyecto (sin borrar el historial). Se usan en la pestaña **"Equipo"**, botones "Quitar" y "Reincorporar".
- **`fn_personal_disponible_proyecto`** — Calcula qué combinaciones de persona + cargo están disponibles para asignar a un proyecto (personas con cargo activo autorizado que aún no están en el equipo). Se usa en la pestaña **"Equipo"** para llenar la lista desplegable "Persona — Cargo" del formulario de asignación.
- **`sp_proyecto_avance_registrar`** — Registra un movimiento de horas trabajadas de una persona en una etapa del proyecto, validando que todo sea correcto (fecha, horas, persona activa, etc.). Se usa en la pestaña **"Avance"**, formulario "Registrar horas trabajadas".
- **`fn_proyecto_pct_avance`** — Calcula el porcentaje de avance de un proyecto (horas trabajadas ÷ horas estimadas × 100). No se llama directamente desde Java: la usa por dentro la vista `v_proyecto_avance`, así que su resultado aparece en la pestaña **"Avance"** cada vez que se abre esa pantalla.

### Página de Catálogos (menú "Catálogos" del navbar)

Estas tablas son listas de valores fijos que usa el resto del sistema (tipos, estados, cargos, etapas). Todas comparten el mismo tipo de formulario de mantenimiento, pero según la tabla usan una función distinta:

- **`sp_ref_grupoa_mant`** — Mantiene las tablas más simples: Tipos de Cliente, Estados de Cliente, Estados de Proyecto, Cargos de Personal, Cargos de Proyecto, Estados de Registro. Se usa en `/referenciales/tip_cli`, `/est_cli`, `/est_pro`, `/car_per`, `/car_pro`, `/est_reg`.
- **`sp_gzz_tip_pro_mant`** — Mantiene los Tipos de Proyecto. Se usa en `/referenciales/tip_pro`.
- **`sp_gzz_lin_pro_mant`** — Mantiene las Líneas de Proyecto. Se usa en `/referenciales/lin_pro`.
- **`sp_gzz_etp_pro_mant`** — Mantiene las Etapas de Proyecto, que además llevan una cantidad de horas estimadas (usada por `v_proyecto_avance` para calcular el % de avance). Se usa en `/referenciales/etp_pro`.

*(Estas cuatro funciones existen separadas porque cada tabla referencial tiene columnas ligeramente distintas — por ejemplo, "Etapas de Proyecto" tiene una columna extra de horas estimadas que las demás no tienen — así que cada una necesita su propia validación.)*

---

## Resumen: qué se usa en cada página

| Página de la app | Vista(s) | Trigger(s) que se pueden disparar | PA que se ejecuta |
|---|---|---|---|
| `/login` | — | — | `fn_usuario_autenticar` |
| `/clientes` (formulario) | — | — | `sp_cliente_mant` |
| `/personal` (formulario) | — | — | `sp_personal_mant` |
| `/personal/{cod}/cargos` | — | — | `sp_per_car_mant` |
| `/proyectos` (lista) | `v_proyecto_resumen` | — | — |
| `/proyectos/{c}/{t}/{s}` (detalle) | `v_proyecto_resumen` | — | `sp_proyecto_cambiar_estado` |
| `/proyectos/nuevo` y `.../editar` | — | `trg_procab_valida_fechas` | `sp_proyecto_crear` / `sp_proyecto_editar` |
| `/proyectos/{c}/{t}/{s}/equipo` | `v_proyecto_equipo` | `trg_proeqp_valida_percar_activo` | `sp_proyecto_equipo_asignar`, `sp_proyecto_equipo_quitar`, `sp_proyecto_equipo_reactivar`, `fn_personal_disponible_proyecto` |
| `/proyectos/{c}/{t}/{s}/avance` | `v_proyecto_avance` (usa `fn_proyecto_pct_avance` por dentro) | `trg_promov_valida_eqp_activo`, `trg_promov_autonumera_sec_etp` | `sp_proyecto_avance_registrar` |
| `/referenciales/*` | — | — | `sp_ref_grupoa_mant`, `sp_gzz_tip_pro_mant`, `sp_gzz_lin_pro_mant`, `sp_gzz_etp_pro_mant` |

> Nota: autorizar un cargo en `/personal/{cod}/cargos` (`sp_per_car_mant`) es lo que luego permite pasar la validación del trigger `trg_proeqp_valida_percar_activo` cuando esa misma persona se asigna a un proyecto desde la pestaña "Equipo" — son pantallas distintas, pero una regla depende de la otra.
