# GestProy — Documentación General (Guía Completa)

> **Para quién es este documento**: para alguien que nunca vio este
> proyecto — ni siquiera tiene por qué conocer bien Spring Boot o
> PL/pgSQL — y necesita entenderlo de punta a punta: qué es, por qué está
> construido así, cómo se instala, cómo se usa, y qué hacer cuando algo
> falla. Es intencionalmente el documento más largo y explicativo de la
> carpeta `docs/`; si tienes prisa, empieza por
> [`00-RESUMEN.md`](00-RESUMEN.md).
>
> Los otros documentos de esta carpeta responden preguntas puntuales:
> - [`01-METODOS-Y-LOGICA.md`](01-METODOS-Y-LOGICA.md): "¿qué hace exactamente esta función/método?"
> - [`02-FLUJOS.md`](02-FLUJOS.md): "¿qué pasa paso a paso cuando hago clic en X?"
>
> Este documento responde una tercera pregunta, la más difícil de
> contestar leyendo solo el código: **"¿por qué está hecho así?"**

---

## Índice

1. [Contexto y propósito del proyecto](#1-contexto-y-propósito-del-proyecto)
2. [La decisión de diseño más importante: lógica en la BD](#2-la-decisión-de-diseño-más-importante-lógica-en-la-bd)
3. [Stack tecnológico explicado pieza por pieza](#3-stack-tecnológico-explicado-pieza-por-pieza)
4. [El modelo de datos, explicado](#4-el-modelo-de-datos-explicado)
5. [Arquitectura en capas, explicada capa por capa](#5-arquitectura-en-capas-explicada-capa-por-capa)
6. [Convenciones de nomenclatura](#6-convenciones-de-nomenclatura)
7. [Recorrido por cada módulo de negocio](#7-recorrido-por-cada-módulo-de-negocio)
8. [Manejo de errores explicado de punta a punta](#8-manejo-de-errores-explicado-de-punta-a-punta)
9. [Instalación paso a paso](#9-instalación-paso-a-paso)
10. [Cómo verificar que todo funciona](#10-cómo-verificar-que-todo-funciona)
11. [Seguridad: qué tiene y qué le falta, y por qué](#11-seguridad-qué-tiene-y-qué-le-falta-y-por-qué)
12. [Huecos de lógica de negocio conocidos](#12-huecos-de-lógica-de-negocio-conocidos)
13. [Preguntas frecuentes / troubleshooting](#13-preguntas-frecuentes--troubleshooting)
14. [Glosario](#14-glosario)
15. [Mapa completo de archivos](#15-mapa-completo-de-archivos)

---

## 1. Contexto y propósito del proyecto

GestProy es el proyecto final del curso de **Base de Datos** de la Escuela
de Ingeniería de Sistemas de la UNSA (Universidad Nacional de San Agustín).
No es un producto comercial ni está pensado para exponerse a Internet: es
un ejercicio académico cuyo objetivo **no es "hacer una app bonita"**, sino
demostrar dominio de PL/pgSQL — funciones, triggers y vistas — como
mecanismo *primario* para implementar reglas de negocio, en contraste con
el enfoque más común hoy en día de poner toda la lógica en el backend
(Java/Python/Node) y tratar a la base de datos como un simple almacén de
filas.

El dominio elegido — gestión de proyectos empresariales — es deliberadamente
simple en su *interfaz* (formularios CRUD, listados, un dashboard) para que
toda la complejidad real esté concentrada donde el curso la evalúa: el
esquema relacional y su lógica en SQL.

**De dónde viene el esquema**: el modelo de 15 tablas (`gestionProyectos`)
proviene de un laboratorio anterior en MySQL (`sqllabbd.sql`, en la raíz
del repositorio superior a `GestProy/`). Este proyecto lo **migró** a
PostgreSQL 16 y construyó sobre él toda la capa de funciones/triggers/vistas
que el laboratorio original no tenía. El documento [`PLAN.md`](../PLAN.md)
en la raíz de `GestProy/` es el plan de diseño que se escribió *antes* de
construir el código — útil para entender la intención original, aunque
algunos detalles (nombres exactos de columnas, si un JS de validación
existe) cambiaron durante la implementación; **este documento describe el
estado real del código**, no el plan.

---

## 2. La decisión de diseño más importante: lógica en la BD

Si solo te llevas una idea de este documento, que sea esta:

> **En GestProy, la base de datos no es un lugar donde se guardan datos —
> es donde vive el negocio.** Java es una fachada web sobre esa lógica, no
> al revés.

### 2.1 ¿Qué significa esto en la práctica?

Cuando alguien "elimina" un cliente, la regla de que solo se puede si no
tiene proyectos activos (bueno, en este proyecto en particular esa regla
**no** está implementada — ver §12 — pero conceptualmente así funcionaría)
no se escribiría como un `if` en `ClienteService.java`. Se escribiría
dentro de la función PL/pgSQL `sp_cliente_mant`, como una comprobación
`SELECT` seguida de un `RAISE EXCEPTION` si la condición no se cumple.

Como consecuencia:
- **Los `Service` de Java están casi vacíos.** Ábrelos y verás que cada
  método tiene una sola línea: delega al DAO. Eso no es pereza del
  programador — es el diseño funcionando como se pretendía. Si encuentras
  un `Service` con un `if` de negocio dentro, algo se está desviando del
  principio del curso.
- **Los DAO nunca arman `INSERT`/`UPDATE`/`DELETE` a mano** para
  operaciones de negocio. Siempre llaman a una función:
  `jdbc.queryForObject("SELECT sp_algo(?, ?, ...)", ...)`. Las únicas
  sentencias `SELECT` directas sobre tablas/vistas son de **solo lectura**.
- **Los errores de negocio son errores de PostgreSQL.** Cuando una regla
  falla, la función ejecuta `RAISE EXCEPTION 'mensaje en español'`.
  PostgreSQL aborta la transacción, el driver JDBC convierte eso en una
  excepción Java, y la aplicación se limita a **mostrar ese mensaje tal
  cual** al usuario (ver §8 para el detalle completo del recorrido).

### 2.2 ¿Por qué no usar un ORM (Hibernate/JPA)?

Porque un ORM está diseñado exactamente para lo contrario: abstraer al
programador de escribir SQL y modelar el negocio como objetos Java con sus
propios métodos y validaciones. Eso es válido y muy común en la industria,
pero anularía el objetivo del curso, que es evaluar SQL avanzado
(funciones, triggers, vistas). Por eso el `pom.xml` incluye
`spring-boot-starter-jdbc` (que trae `JdbcTemplate`, un envoltorio delgado
sobre JDBC) y **deliberadamente no** incluye `spring-boot-starter-data-jpa`.

### 2.3 Ventajas y desventajas reales de este enfoque

No es gratis — vale la pena entender el trade-off en vez de asumir que
"lógica en la BD" es automáticamente mejor o peor:

**Ventajas:**
- La regla de negocio vive en **un solo lugar** (la función SQL), sin
  importar desde qué aplicación se invoque (hoy es Spring Boot; podría ser
  otra app en otro lenguaje y la regla seguiría cumpliéndose).
- Las transacciones son atómicas por construcción: todo lo que pasa dentro
  de una función `plpgsql` es una sola unidad — o se aplica todo o no se
  aplica nada, sin necesidad de coordinar manualmente un `@Transactional`
  en Java.
- Los triggers actúan como **red de seguridad** incluso si alguien escribe
  directamente con SQL sin pasar por las funciones (ver `01_trg_proeqp_...`
  como ejemplo: protege la tabla, no solo la función).

**Desventajas (razón por la que la industria mayormente no trabaja así):**
- La lógica de negocio queda "escondida" del lenguaje de aplicación —
  alguien que solo lee el código Java (sin mirar `db/`) cree que la app "no
  valida nada", cuando en realidad valida todo, solo que en otro lugar.
- PL/pgSQL es más difícil de testear con las herramientas usuales de
  testing de Java (JUnit, Mockito); por eso este proyecto tiene su propia
  suite de tests en SQL puro (`db/tests/smoke_tests.sql`, ver §10).
- Migrar de motor de base de datos (por ejemplo, de PostgreSQL a MySQL) es
  mucho más costoso: toda la lógica habría que reescribirla en el dialecto
  del nuevo motor, no solo copiar el esquema.
- Depurar un error requiere leer mensajes de PL/pgSQL, no solo un stack
  trace de Java — de ahí que `ErroresBd.java` exista específicamente para
  "traducir" ese mensaje a algo legible (ver §8).

---

## 3. Stack tecnológico explicado pieza por pieza

| Pieza | Versión | Por qué esta y no otra |
|---|---|---|
| **PostgreSQL** | 16 | Requisito del curso; motor con soporte robusto de PL/pgSQL, `RETURNS TABLE`, y funciones `STABLE`/`VOLATILE` que este proyecto usa activamente |
| **Java** | 21 (compilado con `release: 21`, probado con JDK 24) | LTS reciente; el proyecto usa `record` (Java 16+) para los modelos de solo lectura |
| **Spring Boot** | 3.4.1 | Framework estándar de la industria Java; trae autoconfiguración de `DataSource`/`JdbcTemplate` sin escribir una clase `@Configuration` manual |
| **Spring JDBC (`JdbcTemplate`)** | (vía `spring-boot-starter-jdbc`) | El "ORM" que **no** es un ORM: ejecuta SQL literal con parámetros bind, mapea filas a objetos con un `RowMapper` explícito escrito a mano — sin generar SQL automáticamente, sin caché de sesión, sin *lazy loading* |
| **Thymeleaf** | (vía `spring-boot-starter-thymeleaf`) | Motor de plantillas *server-side*: el HTML final se genera en el servidor y se envía completo al navegador (no hay una SPA de React/Vue) |
| **Bootstrap 5** | CDN | Solo CSS/componentes visuales (navbar, alerts, badges, barra de progreso); no hay build de frontend (sin npm/webpack) |
| **Maven** | 3.9+ | Gestiona dependencias y compila; `spring-boot-maven-plugin` empaqueta un JAR ejecutable |
| **Driver JDBC PostgreSQL** | `org.postgresql:postgresql` (scope `runtime`) | Traduce las llamadas de `JdbcTemplate` al protocolo de red de PostgreSQL |
| **JJWT** (`io.jsonwebtoken`) | 0.12.6 | Firma y valida los JWT de la cuenta admin (access/refresh). Librería enfocada solo en JWT, sin traer todo el aparato de Spring Security |
| **pgcrypto** (extensión PostgreSQL) | incluida en PostgreSQL 16 | Hash de la contraseña de `admi` (Blowfish); la verificación ocurre en la BD, no en Java |

**Por qué no hay una SPA (React/Angular/Vue)**: el curso evalúa la base de
datos, no el frontend. Un servidor que solo renderiza HTML con Thymeleaf
minimiza el código de "relleno" y deja el foco en `db/`.

**Por qué JJWT + un filtro/interceptor propios y no `spring-boot-starter-security`**:
Spring Security está diseñado para resolver un problema mucho más amplio
(múltiples proveedores de autenticación, jerarquías de roles, ACLs por
recurso...) que no existe aquí — el sistema entero tiene **una** cuenta y
**dos** estados posibles (puede escribir / no puede escribir). Traer todo
ese framework habría significado configurar y entender una cantidad de
maquinaria (`SecurityFilterChain`, `UserDetailsService`,
`AuthenticationManager`...) desproporcionada al problema real, además de
oscurecer justamente la parte que el curso quiere mostrar (la lógica en la
BD). Un filtro (`JwtAuthFilter`) y un interceptor
(`AutorizacionInterceptor`) de ~40 líneas cada uno resuelven el mismo
requisito con código que cualquiera puede leer de punta a punta en un
minuto. Ver §7.6 para el detalle completo de esta decisión.

---

## 4. El modelo de datos, explicado

### 4.1 Por qué 15 tablas y en estos 4 grupos

El esquema separa las tablas según su **naturaleza de cambio**, un criterio
de diseño común en sistemas de gestión:

1. **Referenciales / catálogos (9 tablas, prefijo `gzz_`)**: listas de
   valores que casi no cambian (tipos de cliente, estados, cargos). Se
   modelan aparte para no repetir texto libre ("Empresa Privada") en cada
   fila de `g1m_clientes` — en su lugar, se guarda un código corto
   (`'EP'`) que apunta a la fila del catálogo. Esto es **normalización**:
   evita inconsistencias (que alguien escriba "Emp. Privada" en una fila y
   "Empresa Privada" en otra) y permite traducir la descripción en un solo
   lugar si cambia.
   - **Grupo A** (6 tablas): forma idéntica `cod/des/est_reg`. Al ser
     idénticas, se cubren con **una sola función** (`sp_ref_grupoa_mant`)
     en vez de 6 funciones casi iguales — menos código que mantener.
   - **Grupo B** (3 tablas): tienen una columna adicional propia
     (`gzz_tip_pro.tip_pro_tam`, `gzz_lin_pro.lin_pro_nom`/`lin_pro_tam`,
     `gzz_etp_pro.etp_tie_est`) que rompe la forma idéntica, así que cada
     una necesita su propia función de mantenimiento.

2. **Maestras (2 tablas, prefijo `g1m_`)**: entidades de negocio con
   identidad propia y datos que sí les pertenecen (un cliente tiene nombre,
   fecha de ingreso; una persona tiene costo/hora). No son solo un código +
   descripción, por eso no entran en el patrón de catálogo.

3. **Relación (1 tabla, `g1c_per_car`)**: una tabla puente clásica de
   modelado relacional — resuelve una relación **muchos a muchos** entre
   personas y cargos de proyecto ("Juan puede ser Líder de Proyecto o
   Analista; María solo puede ser Desarrolladora"). Sin esta tabla, esa
   información no cabría en ninguna de las dos tablas maestras sin
   duplicar filas.

4. **Transaccionales (3 tablas, prefijo `g1t_`)**: los datos que realmente
   cambian todo el tiempo durante el uso del sistema, organizados en una
   **jerarquía de tres niveles**:
   ```
   g1t_pro_cab  (cabecera: UN proyecto)
        └── g1t_pro_eqp  (equipo: QUIÉNES trabajan en ese proyecto)
                └── g1t_pro_mov  (movimientos: CUÁNTAS horas trabajó cada quien, por etapa)
   ```
   Esta jerarquía existe porque las horas trabajadas (`g1t_pro_mov`) no
   tienen sentido sin saber quién las registró y con qué cargo
   (`g1t_pro_eqp`), y el equipo no tiene sentido sin saber a qué proyecto
   pertenece (`g1t_pro_cab`).

Existe una tabla adicional, `g1s_usuario` (prefijo `g1s_`, de "seguridad"),
que **no** pertenece a ninguno de estos 4 grupos ni al modelo de negocio
del curso: guarda la única cuenta admin del panel web (login + hash de
contraseña). Se mantiene deliberadamente fuera de la numeración de las 15
tablas porque conceptualmente es infraestructura de acceso, no un dato de
"gestión de proyectos" — ver §7.6 para el porqué completo del diseño de
acceso.

### 4.2 Por qué claves primarias compuestas (y no un simple `id SERIAL`)

`g1t_pro_cab` usa `(pro_cli_cod, pro_tip_cod, pro_sec)` como clave primaria
en vez de un `id` autoincremental de una sola columna. Esto refleja
literalmente cómo el negocio identifica un proyecto: "el proyecto número 2
de tipo Desarrollo de Software para el cliente Corporación Andina" — el
`pro_sec` no es un ID global, es **secuencial dentro de cada combinación
cliente+tipo** (lo calcula `sp_proyecto_crear`, ver
`01-METODOS-Y-LOGICA.md`). Un `id SERIAL` habría sido más simple de
programar, pero habría perdido ese significado de negocio en la clave
misma. Las tablas hijas (`g1t_pro_eqp`, `g1t_pro_mov`) heredan esa PK
compuesta y le agregan sus propias columnas de identidad (persona, cargo,
etapa, secuencia de movimiento).

### 4.3 Por qué eliminación lógica (nunca `DELETE FROM`)

Ninguna operación del sistema borra una fila físicamente. En su lugar,
cada tabla tiene una columna `*est_reg*` que es una FK hacia
`gzz_est_reg`, con tres valores posibles:

| Valor | Significado | Cuándo se usa |
|---|---|---|
| `'A'` | Activo | Estado normal de una fila en uso |
| `'I'` | Inactivo | Temporalmente fuera de uso, se puede reactivar |
| `'*'` | Eliminado | Baja lógica "definitiva" (pero la fila sigue en la tabla) |

**Por qué esto es mejor que `DELETE`** en un sistema de gestión: el
historial importa. Si un proyecto tiene registradas 40 horas de trabajo de
una persona, y esa persona luego "se elimina" del sistema, un `DELETE`
tradicional forzaría a elegir entre (a) borrar también sus 40 horas
históricas (perdiendo información real de lo que pasó) o (b) dejar
registros huérfanos apuntando a una persona que ya no existe (rompiendo la
integridad referencial). La eliminación lógica evita el dilema: la persona
queda marcada `'*'`, deja de aparecer en los `<select>` de "personal
activo" y no se le puede asignar a nada nuevo, pero sus 40 horas
históricas y su fila completa siguen ahí, consultables.

### 4.4 Vocabulario de operación (`ADICIONAR`/`MODIFICAR`/`ELIMINAR`/`INACTIVAR`/`REACTIVAR`)

Estas cinco palabras (en mayúsculas, en español) son literalmente los
valores de texto que viajan como parámetro `p_operacion` a casi todas las
funciones de mantenimiento. Provienen del patrón de programación que el
docente enseñó con un ejemplo Swing de escritorio (`TipPro.java`, mencionado
en el `PLAN.md`), donde un formulario tenía botones con esos exactos
nombres. GestProy reprodujo el mismo vocabulario para que la lógica de
negocio (qué significa cada botón) sea consistente entre el ejemplo del
curso y este proyecto, aunque la interfaz ya no sea un formulario Swing
sino una página web.

---

## 5. Arquitectura en capas, explicada capa por capa

```
Navegador (HTML + Bootstrap 5, sin JS de framework)
      │  formularios GET/POST (+ cookies HttpOnly de sesión, si hay login)
Thymeleaf (src/main/resources/templates/)
      │  renderiza HTML a partir del "Model" que le da el controller
JwtAuthFilter + AutorizacionInterceptor (security/*.java)
      │  ¿quién sos? (filtro, toda petición) / ¿podés escribir? (interceptor, solo escrituras)
Spring MVC — Controllers (web/*.java)
      │  traduce rutas HTTP a llamadas de Service; traduce errores a mensajes flash
Services (service/*.java)
      │  envoltorios delgados; NO tienen reglas de negocio
DAO (dao/*.java)
      │  arma el SQL; lecturas = SELECT directo; escrituras = SELECT de función
JdbcTemplate (Spring, autoconfigurado)
      │  ejecuta el SQL vía JDBC, mapea filas con RowMapper
PostgreSQL 16
      │  funciones PL/pgSQL + triggers + vistas + tablas (db/)
      │  AQUÍ vive toda la lógica de negocio real
```

### 5.1 Por qué esta separación y no menos capas

Podría parecer sobre-ingeniería tener Controller → Service → DAO cuando el
Service casi no hace nada. La razón de mantenerlo así, aun siendo delgado:

- **Consistencia con el resto del ecosistema Spring**: es el patrón
  estándar que cualquier desarrollador Java reconoce de inmediato
  (Controller = HTTP, Service = casos de uso, DAO = acceso a datos). Aunque
  hoy el Service esté vacío, si mañana se necesitara lógica que **sí**
  debe vivir en Java (por ejemplo, enviar un correo tras cerrar un
  proyecto, algo que PL/pgSQL no puede hacer), ya hay un lugar natural
  donde ponerla sin reestructurar nada.
- **El Controller nunca debería hablar con JDBC directamente**: mezclar el
  manejo de rutas HTTP con SQL crudo dificultaría mucho las pruebas y la
  lectura del código.

### 5.2 Qué hace exactamente cada capa (con ejemplos)

- **Controller**: entiende de HTTP (verbos GET/POST, parámetros de ruta,
  `RedirectAttributes` para mensajes flash). No sabe nada de SQL. Ejemplo:
  `ClienteController.adicionar(...)` no sabe que existe
  `sp_cliente_mant` — solo sabe que existe `ClienteService.adicionar(cliente)`.
- **Service**: entiende de "casos de uso" del dominio (adicionar un
  cliente, cambiar el estado de un proyecto). No sabe de HTTP ni de SQL
  literal. Ejemplo: `ClienteService.adicionar(c)` es literalmente
  `dao.mantener("ADICIONAR", c)` — una traducción de nombre, nada más.
- **DAO**: entiende de SQL y de cómo mapear un `ResultSet` a un objeto Java
  (`RowMapper`). No sabe nada de HTTP ni de reglas de negocio — si intenta
  adicionar un cliente con un tipo inválido, el DAO no lo detecta: se lo
  manda tal cual a PostgreSQL y deja que la función `sp_cliente_mant`
  decida si es válido.
- **PL/pgSQL**: entiende de reglas de negocio. No sabe nada de HTTP, ni de
  Java, ni de cómo se ve un formulario — solo sabe: "estos son los datos
  que me llegaron, ¿cumplen las reglas? Si no, `RAISE EXCEPTION`."

---

## 6. Convenciones de nomenclatura

Aprender estas convenciones una vez ahorra tener que releer cada archivo
para saber qué tipo de objeto SQL es:

| Prefijo/sufijo | Tipo de objeto | Ejemplo |
|---|---|---|
| `sp_` | Función de **escritura** (mantenimiento) | `sp_cliente_mant` |
| `fn_` | Función de **lectura** (cálculo/consulta) | `fn_proyecto_pct_avance` |
| `trgfn_` | Función asociada a un trigger | `trgfn_procab_valida_fechas` |
| `trg_` | El trigger en sí (objeto que se adjunta a la tabla) | `trg_procab_valida_fechas` |
| `v_` | Vista | `v_proyecto_resumen` |
| `gzz_` | Tabla referencial/catálogo | `gzz_tip_cli` |
| `g1m_` | Tabla maestra | `g1m_clientes` |
| `g1c_` | Tabla de relación (cruce) | `g1c_per_car` |
| `g1t_` | Tabla transaccional | `g1t_pro_mov` |

Dentro del nombre de columna, los sufijos también son consistentes:
`*_cod` (código, clave corta), `*_des`/`*_nom` (descripción/nombre legible),
`*_est_reg` o `*_est_reg_cod` (estado de registro A/I/*), `*_fec_*` (fecha).

En Java, la convención de nombres de método es paralela y deliberada:
`listar` (todas las filas), `buscar` (una fila por clave), `mantener`
(escritura genérica con el vocabulario ADICIONAR/MODIFICAR/...),
`cambiarEstado` (atajo específico para ELIMINAR/INACTIVAR/REACTIVAR, que
no necesitan los demás campos del formulario).

---

## 7. Recorrido por cada módulo de negocio

Esta sección explica el **porqué** de cada módulo; el **cómo paso a paso**
está en [`02-FLUJOS.md`](02-FLUJOS.md) y el **detalle técnico** de cada
función en [`01-METODOS-Y-LOGICA.md`](01-METODOS-Y-LOGICA.md).

### 7.1 Catálogos (9 tablas GZZ_*)

Por qué existen como módulo genérico: en vez de escribir 9 controladores,
9 servicios y 9 DAOs casi idénticos (uno por catálogo), el proyecto
resolvió el problema con **un enum** (`ReferencialTabla`) que describe las
diferencias entre tablas como *datos*, no como *código repetido*. Esto es
el mismo principio de "no te repitas" (DRY) aplicado dos veces: una vez en
SQL (`sp_ref_grupoa_mant` cubre 6 tablas) y otra vez en Java
(`ReferencialController`/`ReferencialDao` cubren las 9).

### 7.2 Clientes y Personal (maestras)

Por qué tienen validaciones más ricas que un catálogo simple: representan
entidades reales del negocio con reglas propias — un cliente no puede
tener una fecha de ingreso posterior a su fecha de cese (no tendría
sentido cronológico); una persona no puede tener un costo por hora de cero
o negativo (rompería cualquier cálculo de costo real de proyecto más
adelante). Estas reglas están en `sp_cliente_mant`/`sp_personal_mant`
porque son reglas de **datos**, no de presentación — deben cumplirse sin
importar si el dato llega desde el formulario web, desde un script de
carga masiva, o desde cualquier otra vía futura.

`g1c_per_car` (autorización de cargos) modela una realidad de negocio
específica: no cualquier persona puede ejercer cualquier rol en un
proyecto. Antes de poder asignar a "Ana" como "Líder de Proyecto" en algún
proyecto, primero alguien debe autorizar explícitamente que Ana puede
ejercer ese cargo (`sp_per_car_mant`). Esto separa "¿quién existe en la
empresa?" (tabla `g1m_personal`) de "¿qué puede hacer cada quién?" (tabla
`g1c_per_car`), permitiendo que la misma persona esté autorizada para
varios cargos distintos (María puede ser Líder de Proyecto **y** Analista).

### 7.3 Proyectos (ciclo de vida completo)

El módulo más complejo del sistema, con tres piezas encadenadas:

**a) Creación y secuencia.** Cubierto en detalle en §4.2. La razón de negocio
detrás de la secuencia por cliente+tipo (en vez de un ID global) es que así
lo pide el negocio: "este es el tercer proyecto de Migración de Datos que
hacemos para este cliente" es información útil por sí misma, no un detalle
técnico de la base de datos.

**b) Máquina de estados.** El proyecto pasa por 5 estados posibles
(Planificado, En Ejecución, Entregado, Cerrado, Suspendido) y **no todas
las transiciones tienen sentido de negocio** — no se puede "entregar" un
proyecto que nunca empezó a ejecutarse, ni "reabrir" uno ya cerrado. La
función `sp_proyecto_cambiar_estado` codifica esa matriz explícitamente
(ver tabla completa en `01-METODOS-Y-LOGICA.md` §A.5) en vez de dejar que
cualquier valor de estado se pueda asignar libremente — esto es lo que en
diseño de software se llama una **máquina de estados finita**, implementada
aquí directamente en SQL en vez de en una librería de Java.

**c) Utilidad calculada, nunca ingresada a mano.** `pro_uti_pre` y
`pro_uti_rea` (utilidad presupuestada/real) siempre se calculan como
`monto - costo - gasto` dentro de las funciones de creación/edición. Nunca
hay un campo de formulario para "utilidad" — esto evita que alguien
ingrese manualmente un número que no cuadre con los demás valores
(inconsistencia de datos), forzando que ese número **siempre** sea
matemáticamente coherente con el resto.

### 7.4 Equipo de proyecto

Modela quién trabaja en cada proyecto y con qué cargo, con dos reglas de
negocio superpuestas: (1) la persona debe tener autorización activa para
ese cargo (`g1c_per_car`, ver §7.2) y (2) no puede haber duplicados activos
(no tiene sentido que "Juan - Desarrollador" aparezca dos veces como activo
en el mismo proyecto). La reactivación en vez de re-inserción (ver
`sp_proyecto_equipo_asignar` en `01-METODOS-Y-LOGICA.md`) preserva la
misma fila de PK en vez de crear una nueva cada vez que alguien entra y
sale del equipo — esto importa porque las horas trabajadas
(`g1t_pro_mov`) apuntan a esa fila exacta vía FK; si se creara una fila
nueva cada vez, el historial de horas quedaría fragmentado entre varias
"versiones" de la misma asignación persona-cargo-proyecto.

### 7.5 Avance por etapas

El módulo de registro de horas responde a una necesidad de negocio típica
de consultoría/desarrollo de software: saber cuánto se ha avanzado
respecto a lo planeado. La decisión de diseño más importante aquí (y la
más fácil de malinterpretar leyendo el código sin este contexto) es que
**`gzz_etp_pro` es un catálogo único y global de etapas** (Análisis,
Diseño, Desarrollo, Pruebas, Implantación, Capacitación), no un plan
particular por proyecto. Esto significa que **todos** los proyectos se
miden contra el mismo total de horas estimadas (200 horas en el seed
actual) — una simplificación consciente que facilita mucho el cálculo
(`fn_proyecto_pct_avance`, ver `01-METODOS-Y-LOGICA.md`) al costo de no
poder modelar, por ejemplo, un proyecto pequeño que solo necesite 3 de las
6 etapas. Si en el futuro se quisiera un plan de etapas por proyecto,
haría falta una tabla intermedia nueva (algo como
`g1t_pro_etp_plan`) — hoy no existe.

### 7.6 Acceso: modo solo vista y la cuenta admin única

Este módulo es distinto a los cinco anteriores en un aspecto importante:
**no gestiona una entidad del dominio de negocio del curso** (proyectos,
clientes, etapas...) sino el acceso a la aplicación en sí. Por eso su
lógica no vive en PL/pgSQL como todo lo demás, sino en una capa Java
dedicada (`security/`) — decidir "¿quién sos?" no es una regla de negocio
de datos.

**Por qué "modo solo vista" en vez de bloquear todo detrás de un login**:
el objetivo del curso es mostrar el modelo de datos y su lógica de
negocio; obligar a iniciar sesión para ver un listado no aporta nada a esa
evaluación y sí le resta a la posibilidad de que cualquiera (un
compañero, el docente) entre a mirar el sistema sin necesitar credenciales.
Solo las operaciones que *cambian* datos requieren la cuenta admin.

**Por qué una sola cuenta y no un sistema de usuarios/roles**: el
alcance pedido es "que se pueda modificar cualquier cosa, o que no se
pueda modificar nada" — dos estados, no una matriz de permisos por
módulo. Modelar roles, permisos por tabla, o múltiples cuentas habría sido
una abstracción sin ningún caso de uso real detrás (recordar el principio
del proyecto: no construir para requisitos hipotéticos). Si en el futuro
hiciera falta más de un administrador, la tabla `g1s_usuario` ya está
preparada para tener más de una fila — lo que faltaría es una función
`sp_usuario_mant` (no existe) y una UI de gestión de cuentas.

**Por qué JWT en cookies `HttpOnly` y no un token en el header
`Authorization`**: el patrón "token en el header" requiere que JavaScript
lo adjunte a mano en cada petición (`fetch`/`XMLHttpRequest` con
`Authorization: Bearer ...`). GestProy es una aplicación *server-rendered*
clásica: los formularios Thymeleaf hacen `POST` normales del navegador, sin
una sola línea de JavaScript de por medio. Una cookie es el único
mecanismo que el navegador adjunta solo, sin que la aplicación tenga que
reescribirse a base de `fetch`. Que la cookie sea `HttpOnly` es lo que la
protege de un ataque XSS (un script inyectado no puede leerla ni robarla);
`SameSite=Lax` mitiga que otro sitio la reutilice en un `POST` forjado.

**Por qué el refresh token no se guarda en ningún lado del servidor**: es
el mismo comportamiento por defecto de `django-rest-framework-simplejwt`
sin la app de *blacklist* instalada — el refresh token es autocontenido
(firmado, con su propia fecha de expiración) y su sola posesión (en la
cookie correcta) basta para que sea válido. La ventaja es simplicidad: no
hace falta una tabla ni una consulta a la BD para validar un refresh
token, todo el trabajo lo hace la verificación criptográfica de la firma.
La desventaja (aceptada conscientemente, ver §12) es que no hay forma de
"revocar" un refresh token antes de que expire por sí solo — si alguien
robara la cookie, sería válida hasta el final de sus 7 días.

**Por qué el filtro (`JwtAuthFilter`) y el interceptor
(`AutorizacionInterceptor`) son dos clases separadas y no una sola**:
separan dos preguntas distintas a propósito. El filtro resuelve
*autenticación* ("¿quién hizo esta petición?") y corre para toda petición,
sin excepciones, porque hasta las páginas de solo lectura necesitan saber
si deben mostrar los botones de edición. El interceptor resuelve
*autorización* ("¿puede esta petición hacer lo que pide?") y solo actúa
sobre las rutas de escritura. Esta separación es el mismo principio detrás
de `AuthenticationMiddleware` vs. `permission_classes`/decoradores de
autorización en Django: identificar al usuario y decidir si puede hacer
algo son responsabilidades distintas, aunque en GestProy —con una sola
cuenta— la diferencia entre "autenticado" y "autorizado" colapse a lo
mismo.

---

## 8. Manejo de errores explicado de punta a punta

Esta es, junto con la eliminación lógica, la pieza de diseño que más se
repite en todo el proyecto y vale la pena entender una sola vez a fondo.

### 8.1 El contrato: `RAISE EXCEPTION` con mensaje en español

Cada función PL/pgSQL de mantenimiento sigue el mismo contrato: si una
validación falla, ejecuta

```sql
RAISE EXCEPTION 'Mensaje claro en español, con % para interpolar valores', valor;
```

y nada más — no hay un patrón de "retornar `(exitoso, mensaje)`" ni
`EXCEPTION WHEN OTHERS THEN` para capturar el error dentro de la misma
función. La función **deja que el error se propague** hacia quien la
llamó. Esta decisión es deliberada: simplifica cada función (no hay que
escribir un `IF NOT exitoso THEN RETURN` en cada rama) y aprovecha que
PostgreSQL ya aborta automáticamente toda la transacción cuando una
excepción no se captura — no hace falta un `ROLLBACK` manual en ningún lado.

### 8.2 Cómo llega ese error hasta el navegador

```
PL/pgSQL: RAISE EXCEPTION 'texto...'
      │
PostgreSQL aborta la transacción, reporta un error SQL (SQLSTATE P0001)
      │
Driver JDBC de PostgreSQL: lo envuelve en un java.sql.SQLException
      │  (el mensaje trae un prefijo "ERROR: " y líneas extra "Where: ...")
Spring JdbcTemplate: relanza como org.springframework.dao.DataAccessException
      │
MantenimientoControllerBase.ejecutar(...): captura DataAccessException | ReglaNegocioException
      │
ErroresBd.extraerMensaje(ex): limpia el mensaje (primera línea, sin "ERROR: ")
      │
RedirectAttributes.addFlashAttribute("error", mensajeLimpio)
      │
Redirect a la página anterior (formulario o listado)
      │
fragments/mensajes.html: detecta el atributo flash "error" y lo pinta como alerta roja
```

### 8.3 Por qué existe `ErroresBd` como clase separada

Sin esta clase, el mensaje que llegaría al usuario se vería así (texto
real que devuelve el driver JDBC):

```
ERROR: El costo por hora debe ser mayor a 0
  Where: función PL/pgSQL sp_personal_mant(text,integer,character varying,smallint,numeric,date) línea 32 en RAISE
```

`ErroresBd.extraerMensaje` se queda solo con la primera línea y le quita el
prefijo, dejando exactamente: `"El costo por hora debe ser mayor a 0"` —
el mensaje que el docente/desarrollador de PL/pgSQL escribió a propósito
para el usuario final, sin el ruido técnico del "Where".

### 8.4 Dos rutas de captura, un mismo resultado

Hay dos lugares donde se captura el mismo tipo de excepción, y es
importante no confundirlos:

1. **`MantenimientoControllerBase.ejecutar(...)`**: usado explícitamente
   por cada controlador de mantenimiento alrededor de sus operaciones de
   escritura (POST). Es la ruta "esperada" — el usuario ve el mensaje como
   una alerta en la misma pantalla donde estaba, sin perder el contexto.
2. **`GlobalExceptionHandler`** (`@ControllerAdvice`): red de seguridad
   *global* que atrapa cualquier `DataAccessException`/`ReglaNegocioException`
   que se haya escapado sin pasar por `ejecutar(...)` — típicamente en un
   GET que hace una consulta y algo sale mal (por ejemplo, la BD se cae a
   mitad de una consulta de listado). En ese caso se muestra la plantilla
   genérica `error.html`, una experiencia menos pulida pero que evita un
   error 500 crudo del servidor.

---

## 9. Instalación paso a paso

### 9.1 Requisitos previos

- JDK 21 o superior (el proyecto se probó también con JDK 24).
- Maven 3.9+.
- PostgreSQL 16 con el comando `psql` disponible en el `PATH`.

### 9.2 Pasos

```powershell
# 1. Crear la base de datos vacía (una sola vez)
psql -U postgres -c "CREATE DATABASE gestion_proyectos ENCODING 'UTF8'"

# 2. Aplicar TODOS los scripts SQL en el orden correcto
cd GestProy\db\scripts
$env:PGPASSWORD = "tu_contrasena"
.\apply-all.ps1

# 3. (Recomendado) Verificar que todas las reglas de negocio funcionan
.\run-tests.ps1

# 4. Configurar las credenciales de la aplicación
#    (desde GestProy/, no desde db/scripts/)
cd ..\..
copy db.properties.example db.properties
#    Editar db.properties y completar la contraseña real

# 5. Levantar la aplicación
mvn spring-boot:run
```

Abrir <http://localhost:8080> — debería verse el dashboard con los
contadores de clientes/personal/proyectos activos (si `apply-all.ps1`
cargó el seed, estos contadores no serán cero).

### 9.3 Qué hace cada script internamente (por si algo falla)

`apply-all.ps1` simplemente ejecuta, uno por uno y en orden estricto, cada
archivo `.sql` de la carpeta `db/` con `psql -v ON_ERROR_STOP=1` — es
decir, **se detiene en el primer error** en vez de seguir aplicando
scripts sobre un esquema a medio construir. El orden (`schema → triggers →
functions → views → seed`) no es arbitrario: cada capa depende
físicamente de la anterior (ver razón detallada en
[`02-FLUJOS.md`](02-FLUJOS.md) §13).

**Advertencia que vale la pena repetir**: los scripts de `schema/` hacen
`DROP TABLE ... CASCADE`. Ejecutar `apply-all.ps1` sobre una base de datos
que ya tiene datos reales de uso **los borra todos**. Este script es para
instalar desde cero (o para reiniciar completamente un entorno de
pruebas), no para "actualizar" una base en producción.

---

## 10. Cómo verificar que todo funciona

Además de usar la aplicación web manualmente, existe una suite de pruebas
SQL automatizadas: `db/tests/smoke_tests.sql`, ejecutable con
`db/scripts/run-tests.ps1`.

### 10.1 Qué verifica

Quince grupos de pruebas que ejercitan, con datos de prueba dedicados
(códigos `9xxx`, que nunca chocan con el seed real):
- El ciclo completo Adicionar/Modificar/Inactivar/Reactivar/Eliminar de
  catálogos Grupo A y B, incluyendo que los duplicados y los códigos
  inexistentes efectivamente fallen.
- Las validaciones de clientes y personal (FKs activas, fechas coherentes,
  costo/hora positivo).
- Las autorizaciones de cargo, incluyendo el caso de reactivación en vez de
  duplicado.
- El ciclo de vida completo de un proyecto: creación con secuencia
  correlativa, el trigger de fechas, la matriz completa de transiciones de
  estado, y el efecto en cascada sobre `cli_fec_ult_pro_cer` al cerrar.
- La asignación de equipo (con y sin autorización, proyecto cerrado).
- El registro de avance (autonumeración de `sec_etp`, rangos válidos,
  persona no asignada).
- Que las 3 vistas devuelvan exactamente los valores esperados, verificando
  la aritmética del % de avance con un cálculo independiente.
- La autenticación (`fn_usuario_autenticar`): contraseña correcta,
  incorrecta, login inexistente y cuenta inactivada.

### 10.2 Por qué es seguro correrlo contra la base de datos real

Todo el script corre dentro de `BEGIN; ... ROLLBACK;`. Sin importar cuántas
filas se inserten o modifiquen durante los tests, al final **nada** se
persiste — la base de datos queda exactamente como estaba antes de correr
el script. Esto se puede (y se recomienda) verificar contando filas con
código `9xxx` antes y después: siempre da cero.

### 10.3 Cómo interpretar el resultado

- Si todo pasa, la última línea impresa es:
  `>>> TODOS LOS TESTS PASARON (los datos de prueba fueron revertidos con ROLLBACK) <<<`
- Si algo falla, `psql` se detiene en la primera línea
  `TEST FALLIDO Txx: <explicación de qué regla se violó>` y termina con
  código de salida distinto de cero — útil para integrarlo en un pipeline
  de CI si en algún momento se quisiera automatizar.

---

## 11. Seguridad: qué tiene y qué le falta, y por qué

Este proyecto es un ejercicio académico con **una sola cuenta
administradora**, no un sistema multiusuario expuesto a Internet. Aun así,
vale la pena saber exactamente qué protecciones existen y cuáles no, para
no asumir garantías que el código no ofrece.

### 11.1 Lo que sí está bien resuelto

- **Sin inyección SQL**: toda escritura pasa por funciones invocadas con
  parámetros bind (`?`), nunca con concatenación de valores de usuario. El
  único lugar donde se concatena SQL (`ReferencialDao`, para el `SELECT` de
  listado genérico) concatena **nombres de columna/tabla que vienen del
  enum `ReferencialTabla`**, nunca de un valor que el usuario escribió —
  por diseño, no es posible pedir "listar la tabla X" con X arbitrario
  desde el navegador; el slug de la URL se valida contra el enum
  (`ReferencialTabla.porSlug`) y si no coincide con ninguna constante,
  responde `404` antes de tocar la base de datos.
- **Sin `EXECUTE format()` dinámico en SQL**: `sp_ref_grupoa_mant` podría
  haberse escrito con SQL dinámico (`EXECUTE format('UPDATE %I SET...', p_tabla)`)
  para no repetir 6 ramas casi idénticas — deliberadamente no se hizo así,
  precisamente para eliminar cualquier superficie de inyección por nombre
  de tabla, a costa de más código repetido.
- **Credenciales y claves fuera del control de versiones**: `db.properties`
  (contraseña de la BD **y** `app.jwt.secret`, la clave de firma de los
  JWT) está en `.gitignore`; el repositorio solo trae
  `db.properties.example` como plantilla, sin secretos reales.
- **Contraseña nunca en texto plano**: `g1s_usuario.usu_pass_hash` guarda
  un hash Blowfish (pgcrypto); ni la BD ni el código Java conservan la
  contraseña real en ningún punto después del login (`AuthController` la
  recibe del formulario y la pasa directo a `fn_usuario_autenticar`, sin
  guardarla en ninguna variable de más vida que esa petición).
- **Escrituras bloqueadas del lado del servidor, no solo ocultas en la
  UI**: `AutorizacionInterceptor` rechaza con `403` cualquier `POST` (o
  formulario de alta/edición) sin la sesión admin — esconder los botones
  en Thymeleaf es una ayuda de UX, no la protección real. Ver §7.6 y
  `02-FLUJOS.md` §15.2 para el detalle de este mecanismo.
- **Cookies de sesión bien configuradas**: `HttpOnly` (JavaScript no puede
  leerlas, mitiga robo por XSS) y `SameSite=Lax` (mitiga que otro sitio las
  reutilice en un `POST` forjado). El refresh token, al no persistirse en
  el servidor, tampoco puede filtrarse por una fuga de la base de datos.

### 11.2 Lo que falta (omisiones conscientes para un proyecto académico)

- **Sin protección CSRF explícita**: al no usar Spring Security, ningún
  formulario incluye un token CSRF anti-falsificación. Se mitiga
  parcialmente con `SameSite=Lax` (la mayoría de navegadores modernos no
  envían la cookie en un `POST` disparado desde otro origen), pero no es
  una protección tan robusta como un token CSRF dedicado.
- **Sin límite de intentos de login**: `AuthController.iniciarSesion` no
  tiene *rate limiting* ni bloqueo tras N intentos fallidos — nada impide
  probar contraseñas por fuerza bruta contra `/login` salvo la propia
  demora de la red. Para un solo usuario académico el riesgo es bajo, pero
  sería la primera mejora a agregar antes de cualquier uso real.
- **El refresh token no se puede revocar antes de que expire**: al ser
  stateless (ver §7.6), si esa cookie se filtrara, seguiría siendo válida
  hasta agotar sus 7 días — no hay una tabla de tokens revocados
  (*blacklist*) contra la que consultar, a diferencia de lo que ofrece
  `django-rest-framework-simplejwt` con su app opcional de blacklist.
- **Conexión con un usuario amplio de PostgreSQL**: `db.properties.example`
  sugiere el usuario `postgres` (superusuario). En un entorno real
  convendría un rol dedicado con permisos acotados a `EXECUTE` sobre las
  funciones y `SELECT` sobre las vistas, sin acceso directo de escritura a
  las tablas — reforzaría a nivel de permisos de BD la misma regla que hoy
  solo se sostiene "por convención" en el código Java (que nunca escribe
  con `INSERT`/`UPDATE` directo).
- **`app.jwt.cookie-secure=false` por defecto**: correcto para desarrollo
  local sobre `http://localhost`, pero **debe** cambiarse a `true` si la
  aplicación alguna vez corre sobre HTTPS — si no, las cookies viajarían
  también por conexiones sin cifrar.

Si este proyecto se fuera a usar más allá del entorno académico, agregar
límite de intentos de login y un token CSRF serían las siguientes mejoras,
en ese orden.

---

## 12. Huecos de lógica de negocio conocidos

Identificados en una auditoría previa del proyecto y documentados aquí para
que quien continúe el desarrollo sepa exactamente dónde no confiar
ciegamente en que "si compila y no da error, está bien":

1. **Montos negativos aceptados**: `sp_proyecto_editar`/`sp_proyecto_crear`
   no validan que `monto`/`costo`/`gasto` sean `>= 0`.
2. **Eliminar/inactivar personal no lo retira de los equipos activos**: una
   persona dada de baja lógicamente puede seguir figurando activa en
   `g1t_pro_eqp` y seguir registrando horas — los triggers solo verifican
   el estado de la fila de equipo, no el de la persona en sí.
3. **Eliminar un cliente con proyectos activos está permitido**: no hay
   ninguna validación cruzada que lo impida.
4. **Se puede inactivar/eliminar los códigos base de `gzz_est_reg`
   (`'A'`/`'I'`/`'*'`)**, lo que corrompería el mecanismo de eliminación
   lógica de todo el esquema si ocurriera por error.
5. **Sin `CHECK` constraints como defensa en profundidad**: las
   validaciones de rango (horas 0-23, minutos 0-59, tamaño P/M/G) solo
   viven en las funciones PL/pgSQL — un `INSERT` directo sobre la tabla
   (sin pasar por la función) las evade.
6. **El trigger de fechas no cubre todos los pares posibles**: valida
   `fec_con<=fec_pac` e `fec_ini<=fec_ent`/`fec_ini<=fec_cer`, pero no
   `fec_ent<=fec_cer` ni `fec_con<=fec_ini`.
7. **Condición de carrera en los cálculos `MAX+1`**: tanto `pro_sec`
   (creación de proyecto) como `sec_etp` (movimientos) se calculan sin
   bloqueo explícito; dos escrituras concurrentes sobre la misma
   combinación podrían colisionar en la clave primaria (produce un error
   visible al usuario, no corrupción silenciosa de datos).

Ninguno de estos puntos rompe el uso normal de la aplicación (probado con
la suite de tests, que cubre el camino feliz y varios caminos de error
intencionales) — son casos límite que valdría la pena cerrar si el
proyecto creciera más allá del entorno de un solo usuario en clase.

---

## 13. Preguntas frecuentes / troubleshooting

**"La app arranca pero `/` da un error de conexión a la base de datos."**
Revisa que `db.properties` exista (copiado desde `db.properties.example`)
y que la contraseña sea correcta, y que PostgreSQL esté corriendo en el
puerto 5432 (`pg_isready -h localhost -p 5432`).

**"`apply-all.ps1` falla a mitad de camino."**
Con `ON_ERROR_STOP=1`, el script se detiene en el primer script `.sql` que
falle e imprime cuál fue. Revisa que la base de datos exista y esté vacía
o en un estado consistente con el orden de scripts — si ya tenía tablas de
una corrida anterior a medio aplicar, puede ayudar recrear la base desde
cero (`DROP DATABASE` + `CREATE DATABASE`).

**"Veo un mensaje de error en español al usar la app, ¿es un bug?"**
No necesariamente — es el comportamiento esperado. Ese mensaje viene
directo de un `RAISE EXCEPTION` en PL/pgSQL, o sea, de una regla de negocio
que se violó a propósito (ver §8). Antes de asumir que es un bug, verifica
si el escenario que intentaste realmente debería estar permitido según las
reglas descritas en §7 y en `01-METODOS-Y-LOGICA.md`.

**"¿Cómo pruebo un cambio en una función SQL sin arriesgar los datos reales?"**
Corre `db/scripts/run-tests.ps1` contra la base de datos real después de
aplicar tu cambio — como corre en una transacción con `ROLLBACK`, no deja
rastro. Si tu cambio agrega una regla nueva, agrégale también su propio
test en `db/tests/smoke_tests.sql` siguiendo el patrón `DO $t$ ... $t$` de
los tests existentes.

**"¿Por qué un `POST` me redirige de vuelta al mismo formulario en vez de
mostrar un error en la misma página sin recargar?"**
Es el patrón **Post-Redirect-Get**: evita que si el usuario recarga la
página (F5) después de un submit, el navegador reenvíe el mismo `POST` por
accidente (lo que duplicaría la operación). El mensaje de error/éxito viaja
como *flash attribute* (`RedirectAttributes`), que sobrevive exactamente un
redirect y se muestra en la página de destino.

---

## 14. Glosario

| Término/abreviatura | Significado |
|---|---|
| `cod` | Código (clave corta de una entidad) |
| `des` | Descripción |
| `nom` | Nombre |
| `est` | Estado (de negocio, ej. estado de un proyecto) |
| `est_reg` / `est_reg_cod` | Estado de **registro**: `A` activo, `I` inactivo, `*` eliminado (eliminación lógica) |
| `fec` | Fecha |
| `pro` | Proyecto |
| `cli` | Cliente |
| `per` | Persona / Personal |
| `car` | Cargo |
| `tip` | Tipo |
| `pre` / `rea` | Presupuestado / Real (en montos: `pro_mon_pre` = monto presupuestado, `pro_mon_rea` = monto real) |
| `sec` | Secuencia (número correlativo dentro de un contexto, ej. `pro_sec`) |
| `etp` | Etapa |
| `eqp` | Equipo |
| `mov` | Movimiento (registro de horas trabajadas) |
| `PK compuesta` | Clave primaria formada por varias columnas juntas (ej. `pro_cli_cod + pro_tip_cod + pro_sec`) |
| `FK` | Clave foránea (referencia a otra tabla) |
| `Trigger` | Código SQL que se ejecuta automáticamente antes/después de un `INSERT`/`UPDATE`/`DELETE` sobre una tabla |
| `RAISE EXCEPTION` | Instrucción PL/pgSQL que aborta la operación actual con un mensaje de error |
| `DataAccessException` | Excepción de Spring que envuelve cualquier error venido de la base de datos |
| `Flash attribute` | Dato que sobrevive exactamente un redirect HTTP (usado para mensajes de éxito/error tras un POST) |
| `RowMapper` | Objeto de `JdbcTemplate` que convierte una fila de resultado SQL en un objeto Java |
| `ORM` | *Object-Relational Mapping*: técnica (Hibernate/JPA) para modelar tablas como clases Java automáticamente — **no se usa en este proyecto** |
| `STABLE` (PL/pgSQL) | Marca una función de solo lectura cuyo resultado no cambia dentro de la misma sentencia SQL — permite optimizaciones internas de PostgreSQL |
| `plpgsql` vs `sql` | Los dos lenguajes usados para las funciones: `plpgsql` permite `IF`/`DECLARE`/control de flujo; `sql` es solo una consulta pura (usado en las funciones de lectura más simples) |
| `usu` | Usuario (prefijo de columnas de `g1s_usuario`, ej. `usu_login`) |
| `JWT` | *JSON Web Token*: token firmado que codifica datos (aquí, el login) de forma verificable sin consultar una BD — ver §7.6 |
| `Access token` | JWT de vida corta (15 min) que autoriza cada operación de escritura |
| `Refresh token` | JWT de vida larga (7 días) que solo sirve para renovar el access token sin pedir credenciales de nuevo; no se guarda en el servidor (*stateless*) |
| `HttpOnly` (cookie) | Atributo que impide que JavaScript lea la cookie — mitiga robo de sesión por XSS |
| `SameSite` (cookie) | Atributo que restringe cuándo el navegador envía la cookie en peticiones de otro sitio — mitiga CSRF |
| `pgcrypto` | Extensión de PostgreSQL usada para el hash de contraseñas (`crypt`/`gen_salt('bf')`, algoritmo Blowfish) |
| `Silent refresh` (renovación silenciosa) | Emitir un access token nuevo automáticamente (usando el refresh token) sin que el usuario note que el anterior expiró |
| `HandlerInterceptor` (Spring MVC) | Punto de extensión que corre antes/después de un controller; a diferencia de un `Filter`, sus excepciones sí llegan a `@ExceptionHandler` |
| `OncePerRequestFilter` (Spring) | Filtro de servlet que garantiza ejecutarse una sola vez por petición, incluso con *forwards* internos |

---

## 15. Mapa completo de archivos

```
GestProy/
├── README.md                     # Puesta en marcha rápida
├── PLAN.md                       # Plan de diseño original (pre-implementación)
├── pom.xml                       # Dependencias y build Maven
├── db.properties.example         # Plantilla de credenciales (sin secretos)
├── db.properties                 # Credenciales reales (gitignored, no existe en el repo)
│
├── docs/                         # Esta carpeta de documentación
│   ├── 00-RESUMEN.md
│   ├── 01-METODOS-Y-LOGICA.md
│   ├── 02-FLUJOS.md
│   └── 03-DOCUMENTACION-GENERAL.md   (este archivo)
│
├── db/                           # TODA la lógica de negocio, en SQL versionado
│   ├── README.md                 # Orden de ejecución y convenciones
│   ├── schema/                   # DDL: las 15 tablas de negocio + g1s_usuario + índices
│   ├── triggers/                 # 4 triggers de validación/automatización
│   ├── functions/                # 16 funciones PL/pgSQL (mantenimiento, negocio, autenticación)
│   ├── views/                    # 3 vistas de lectura
│   ├── seed/                     # Datos de ejemplo (idempotentes) + cuenta admin
│   ├── tests/                    # smoke_tests.sql: pruebas automatizadas (con ROLLBACK)
│   └── scripts/                  # apply-all.ps1, run-tests.ps1
│
└── src/main/
    ├── java/edu/unsa/eps/gestproy/
    │   ├── GestProyApplication.java   # Punto de entrada Spring Boot
    │   ├── model/                     # POJOs / records (sin lógica)
    │   │   └── referencial/           # ReferencialTabla (enum) + RegistroReferencial
    │   ├── dao/                       # Acceso a datos vía JdbcTemplate
    │   ├── service/                   # Orquestación delgada (sin reglas de negocio)
    │   ├── web/                       # Controllers Spring MVC (incluye AuthController, GlobalModelAttributes)
    │   ├── security/                  # JwtService, JwtAuthFilter, AutorizacionInterceptor, CookieUtil
    │   ├── exception/                 # Manejo de errores (ErroresBd, GlobalExceptionHandler, NoAutorizadoException, ...)
    │   └── config/                    # WebConfig (registra el interceptor); resto autoconfigurado por Spring Boot
    │
    └── resources/
        ├── application.properties     # Config de la app (importa db.properties, config de JWT)
        ├── static/css/estilos.css     # Estilos propios sobre Bootstrap 5
        └── templates/                 # Vistas Thymeleaf
            ├── index.html, login.html
            ├── fragments/             # head, nav (con botón login/logout), mensajes (flash), estado-badge
            ├── clientes/, personal/, proyectos/, referenciales/
            └── error.html
```
