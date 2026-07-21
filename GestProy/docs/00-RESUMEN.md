# GestProy — Resumen General

> Documento breve de entrada. Para profundizar, ver el índice al final y la
> carpeta [`docs/`](.).

## 1. Qué es

**GestProy** es un sistema web de gestión de proyectos empresariales,
desarrollado como proyecto final del curso de **Base de Datos** (UNSA,
Escuela de Ingeniería de Sistemas). Administra clientes, personal, proyectos
y el avance de trabajo por etapas, sobre un esquema relacional de 15 tablas
(`gestionProyectos`).

**Principio central del curso** (y del diseño de todo el proyecto): **toda
la lógica de negocio vive en la base de datos**, como scripts SQL
versionados (funciones, triggers, vistas). La aplicación Java es una capa
delgada que **invoca** esa lógica; nunca la reimplementa ni usa un ORM.

## 2. Stack tecnológico

| Componente | Tecnología | Rol |
|---|---|---|
| Base de datos | PostgreSQL 16 | Dueña de todos los datos y de toda la lógica de negocio |
| Backend | Java 21 + Spring Boot 3.4.1 | Capa web; sin JPA/Hibernate |
| Acceso a datos | Spring `JdbcTemplate` | Invoca funciones SQL con `SELECT sp_x(...)`, sin ORM |
| Vistas | Thymeleaf (server-side) + Bootstrap 5 (CDN) | Renderizado HTML |
| Build | Maven 3.9+ | Compilación y empaquetado (`spring-boot-maven-plugin`) |
| Driver BD | `org.postgresql:postgresql` | Conexión JDBC a PostgreSQL |

| Autenticación | JJWT (`io.jsonwebtoken`) | Firma/valida los JWT de la única cuenta admin, transportados en cookies `HttpOnly` |

Hay una sola cuenta con permiso de escritura (`admi`); el resto del sitio es
de solo lectura sin sesión. No hay registro de usuarios, roles, ni
protección CSRF: es un proyecto académico de un solo administrador, no un
producto multiusuario expuesto a Internet.

Backend
- Java 21 + Spring Boot 3.4.1
- spring-boot-starter-web (MVC/REST)
- spring-boot-starter-thymeleaf (motor de plantillas server-side)
- spring-boot-starter-jdbc con JdbcTemplate — sin JPA/ORM, ya que el curso exige SQL explícito (funciones, triggers, vistas)
- spring-boot-starter-validation
- JWT (io.jsonwebtoken / jjwt 0.12.6) para autenticación de la cuenta admin vía cookies HttpOnly
- Maven como gestor de build (pom.xml, spring-boot-maven-plugin)

Base de datos
- PostgreSQL 16
- Uso intensivo de SQL avanzado: funciones/procedimientos almacenados (sp_*, fn_*), triggers, vistas (v_proyecto_resumen, etc.), esquema con seeds — consistente con un curso de Base de Datos
- pgcrypto para verificación de contraseñas en BD

Frontend
- Thymeleaf (HTML server-rendered) + CSS plano (static/css/estilos.css), sin framework JS visible

Infraestructura
- Docker / Docker Compose (contenedores para app + BD, con healthcheck e inicialización automática de scripts SQL)

## 3. Arquitectura en capas

```
Navegador (Bootstrap 5)
      │  HTML / formularios (+ cookies HttpOnly de sesión)
Thymeleaf (templates/)
      │
JwtAuthFilter (security/)            ← identifica si la petición trae la sesión admin
AutorizacionInterceptor (security/)  ← bloquea con 403 cualquier escritura sin esa sesión
      │
Spring MVC — Controllers (web/)      ← rutas GET (mostrar) / POST (confirmar)
      │
Services (service/)                  ← orquestación fina, delega todo a los DAO
      │
DAO (dao/)                           ← JdbcTemplate, SIN ORM
      │
Funciones / triggers / vistas PL/pgSQL (db/)   ← TODA la lógica de negocio
      │
PostgreSQL 16
```

Los `Service` son casi transparentes: no contienen reglas de negocio, solo
delegan a los DAO. Las reglas viven exclusivamente en PL/pgSQL y se
comunican como errores mediante `RAISE EXCEPTION`, que Spring recibe como
`DataAccessException` y el `GlobalExceptionHandler`/`MantenimientoControllerBase`
convierten en mensajes flash para el usuario. La capa `security/` es la
única excepción a "sin lógica en Java": autenticar una sesión no es una
regla de negocio de datos, así que vive en Java, no en PL/pgSQL (la
verificación de la contraseña en sí, en cambio, sí vive en la BD — ver
sección 9).

## 4. Modelo de datos (15 tablas de negocio + 1 de acceso)

| Grupo | Tablas | Descripción |
|---|---|---|
| **Referenciales Grupo A** (6) | `gzz_est_reg`, `gzz_tip_cli`, `gzz_est_cli`, `gzz_est_pro`, `gzz_car_per`, `gzz_car_pro` | Catálogos con forma idéntica Cod/Des/EstReg |
| **Referenciales Grupo B** (3) | `gzz_tip_pro` (+tam), `gzz_lin_pro` (+nom,+tam), `gzz_etp_pro` (+tie_est) | Catálogos con columna(s) extra |
| **Maestras** (2) | `g1m_clientes`, `g1m_personal` | Entidades de negocio permanentes |
| **Relación** (1) | `g1c_per_car` | Qué cargos de proyecto puede ejercer cada persona |
| **Transaccionales** (3) | `g1t_pro_cab`, `g1t_pro_eqp`, `g1t_pro_mov` | Cabecera de proyecto → equipo asignado → horas trabajadas por etapa |
| **Acceso** (1) | `g1s_usuario` | La única cuenta admin (login + hash de contraseña); no forma parte del modelo de negocio del curso |

**Eliminación lógica en todo el esquema**: ninguna fila se borra con
`DELETE`. Cada tabla tiene una columna `*est_reg*` (FK a `gzz_est_reg`) con
valores `'A'` (Activo), `'I'` (Inactivo) o `'*'` (Eliminado).

## 5. Lógica de negocio en la base de datos

| Tipo | Cantidad | Ejemplos |
|---|---|---|
| Vistas | 3 | `v_proyecto_resumen`, `v_proyecto_equipo`, `v_proyecto_avance` |
| Triggers | 4 | Autorización activa al asignar equipo, miembro activo al registrar horas, autonumeración de `sec_etp`, coherencia de fechas |
| Funciones (`sp_`/`fn_`) | 16 | Mantenimiento de catálogos/maestras, ciclo de vida de proyectos, equipo, avance, autenticación |

Todo bajo `CREATE FUNCTION` (no `PROCEDURE`), porque `JdbcTemplate` las
invoca con `SELECT`, no con `CALL`. Toda validación fallida usa
`RAISE EXCEPTION 'mensaje en español'`.

## 6. Módulos funcionales de la aplicación

| Módulo | Qué permite |
|---|---|
| **Catálogos** (9 tablas GZZ_*) | CRUD genérico: Adicionar / Modificar / Eliminar / Inactivar / Reactivar |
| **Clientes** | Alta/edición con validación de FKs activas y coherencia de fechas |
| **Personal** | Alta/edición (costo/hora > 0) + autorización de cargos de proyecto por persona |
| **Proyectos** | Creación (secuencia autogenerada), edición, transición de estados (matriz validada), utilidad calculada automáticamente |
| **Equipo de proyecto** | Asignar/quitar/reactivar personas validando autorización activa |
| **Avance por etapas** | Registro de horas trabajadas (secuencia autonumerada), % de avance vs. tiempo estimado |

## 7. Acceso: modo solo vista + una cuenta admin

Todo el sitio se navega **sin iniciar sesión** (listados, detalle, equipo,
avance — nada se oculta a nivel de lectura). Escribir cualquier cosa
requiere haber iniciado sesión con la única cuenta del sistema:

- **Usuario**: `admi` — no hay registro de cuentas ni roles.
- **Autenticación**: JWT en dos cookies `HttpOnly` (nunca accesibles desde
  JavaScript): un *access token* de vida corta (15 min) que autoriza cada
  escritura, y un *refresh token* de vida larga (7 días) que solo sirve
  para renovar el access token en silencio — igual que Django SimpleJWT en
  modo cookie. El refresh token **no se guarda en ninguna tabla ni en el
  usuario**: es autocontenido (stateless), se valida solo por su firma.
- **Verificación de contraseña en la BD**: `fn_usuario_autenticar` compara
  el hash (pgcrypto/Blowfish) guardado en `g1s_usuario`; Java nunca ve ni
  guarda la contraseña en texto plano.
- **Bloqueo real, no solo cosmético**: `AutorizacionInterceptor` rechaza
  con `403` cualquier `POST` (o formulario de alta/edición) que no traiga
  la sesión admin válida, sin importar si el botón está oculto o no en la
  plantilla — alguien podría forjar la petición directamente y el servidor
  la rechazaría igual.

Ver el detalle completo en `01-METODOS-Y-LOGICA.md` §C y `02-FLUJOS.md` §15.

## 8. Cómo se ejecuta

```powershell
# 1. Crear la BD
psql -U postgres -c "CREATE DATABASE gestion_proyectos ENCODING 'UTF8'"

# 2. Aplicar todos los scripts SQL (esquema + triggers + funciones + vistas + seed)
cd GestProy\db\scripts
$env:PGPASSWORD = "tu_contrasena"
.\apply-all.ps1

# 3. Verificar que todo funciona (no modifica datos, corre en ROLLBACK)
.\run-tests.ps1

# 4. Configurar credenciales de la app: copiar db.properties.example -> db.properties
#    (incluye la contraseña de la BD y app.jwt.secret, la clave de firma de los JWT)

# 5. Levantar la aplicación
cd ..\..
mvn spring-boot:run

# 6. Entrar como admi (usuario "admi", contraseña inicial "testpass123") para poder editar
```

## 9. Índice de toda la documentación

| Documento | Contenido |
|---|---|
| [`README.md`](../README.md) | Puesta en marcha rápida |
| **`docs/00-RESUMEN.md`** (este archivo) | Panorama general breve |
| [`docs/01-METODOS-Y-LOGICA.md`](01-METODOS-Y-LOGICA.md) | Catálogo a profundidad de cada función SQL, trigger, vista y método Java |
| [`docs/02-FLUJOS.md`](02-FLUJOS.md) | Recorrido paso a paso de cada caso de uso, de clic a fila de BD |
| [`docs/03-DOCUMENTACION-GENERAL.md`](03-DOCUMENTACION-GENERAL.md) | Documento maestro: explica el porqué de cada decisión, pensado para alguien sin experiencia previa en el proyecto |
| [`docs/04-DJANGO-A-SPRING.md`](04-DJANGO-A-SPRING.md) | GestProy explicado por comparación con Django, para quien viene de ese framework |
| [`db/README.md`](../db/README.md) | Orden de ejecución de scripts SQL y convenciones |
| [`PLAN.md`](../PLAN.md) | Plan de implementación original (diseño detallado previo a la construcción) |
