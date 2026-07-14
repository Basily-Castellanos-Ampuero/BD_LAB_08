# GestProy — Sistema de Gestión de Proyectos

Proyecto final del curso de **Base de Datos** (UNSA, Escuela de Ingeniería de
Sistemas). Sistema web de gestión de proyectos empresariales sobre el esquema
`gestionProyectos` (15 tablas), migrado de MySQL a **PostgreSQL 16**.

**Principio central del curso**: toda la lógica de datos vive en la base de
datos como scripts SQL versionados — DDL, **triggers**, **funciones/procedimientos
PL/pgSQL** y **vistas** (carpeta [`db/`](db/README.md)). La aplicación Java
(Spring Boot + Thymeleaf) los **invoca** vía `JdbcTemplate`, **sin ORM**.

## Puesta en marcha con Docker (recomendado para otra máquina)

Es la forma más simple de correr GestProy en cualquier equipo: **no** hace
falta instalar Java, Maven ni PostgreSQL, solo Docker. `docker compose`
construye la app, levanta PostgreSQL 16 y aplica todos los scripts SQL
(esquema + triggers + funciones + vistas + datos semilla + cuenta admin)
automáticamente en el primer arranque.

**Requisito**: Docker Desktop (o Docker Engine) con Compose v2.

```bash
cd GestProy
docker compose up --build
```

Cuando el log muestre `Started GestProyApplication`, abrir
<http://localhost:8080>. La cuenta admin es `admi` / `testpass123` (ver
[Acceso](#acceso)).

- **Credenciales/secretos**: opcionalmente copia `.env.example` → `.env` y
  define `POSTGRES_PASSWORD` y `APP_JWT_SECRET` propios (si no, se usan los
  valores por defecto del `docker-compose.yml`, aptos solo para pruebas
  locales). `.env` está en `.gitignore`.
- **Detener**: `docker compose down` (los datos persisten en el volumen
  `pgdata`). Para empezar de cero y **re-aplicar** los scripts SQL —por
  ejemplo tras editarlos— usa `docker compose down -v`, que borra el
  volumen: el init de la BD solo corre cuando el volumen está vacío.
- **Pruebas de humo** dentro del contenedor de BD (no modifican datos,
  corren en `ROLLBACK`):
  ```bash
  docker compose exec -T db psql -U postgres -d gestion_proyectos -v ON_ERROR_STOP=1 \
    -f /gestproy-db/tests/smoke_tests.sql
  ```

## Puesta en marcha manual (instalación local)

**Requisitos**: JDK 21+ (probado con JDK 24), Maven 3.9+, PostgreSQL 16 con
`psql` en el PATH.

1. **Crear la base de datos** (una sola vez):

   ```
   psql -U postgres -c "CREATE DATABASE gestion_proyectos ENCODING 'UTF8'"
   ```

2. **Aplicar los scripts SQL** (esquema + triggers + funciones + vistas + datos semilla):

   ```powershell
   cd GestProy\db\scripts
   $env:PGPASSWORD = "tu_contrasena"
   .\apply-all.ps1
   ```

3. **Verificar que todo quedó bien** (opcional pero recomendado): corre 15
   grupos de pruebas de humo sobre las reglas de negocio de la BD dentro de
   una transacción con `ROLLBACK`, así que no modifica ningún dato:

   ```powershell
   .\run-tests.ps1
   ```

4. **Configurar credenciales**: copiar `db.properties.example` → `db.properties`
   (en esta carpeta `GestProy/`) y completar la contraseña, y agregar
   `app.jwt.secret` (clave de firma de los JWT de sesión, ver plantilla).
   `db.properties` está en `.gitignore` y nunca se sube al repositorio.

5. **Levantar la aplicación**:

   ```
   cd GestProy
   mvn spring-boot:run
   ```

   Abrir <http://localhost:8080>.

## Acceso

El sitio se navega **sin iniciar sesión**: todo se ve, nada se puede
modificar. Para editar (Adicionar/Modificar/Eliminar/Asignar/Registrar/...)
hay que iniciar sesión con la única cuenta admin desde el botón "Iniciar
sesión" del header:

- **Usuario**: `admi`
- **Contraseña inicial**: `testpass123` (definida en `db/seed/04_seed_usuario.sql`
  como hash; cámbiala antes de cualquier uso real — ver el comentario de ese
  archivo).

## Funcionalidad

| Módulo | Descripción |
|--------|-------------|
| **Catálogos** (9 tablas GZZ_*) | Mantenimiento genérico con el patrón del curso: Adicionar / Modificar / Eliminar (lógico `*`) / Inactivar (`I`) / Reactivar (`A`). Una sola función SQL (`sp_ref_grupoa_mant`) cubre las 6 tablas de forma idéntica; las 3 con columna extra tienen función propia. |
| **Clientes y Personal** (maestras) | Mantenimiento con validaciones de negocio en PL/pgSQL (FKs a catálogos activos, costo/hora > 0, fechas coherentes) y autorización de cargos de proyecto por persona (`g1c_per_car`). |
| **Proyectos** | Creación con secuencia calculada por la BD (`MAX+1` por cliente+tipo), utilidad presupuestada/real calculada automáticamente (monto − costo − gasto), y transiciones de estado validadas por matriz (planificado → ejecución → entregado → cerrado, con suspensión/reanudación). Al cerrar, se actualiza la fecha de último proyecto cerrado del cliente. |
| **Equipo de proyecto** | Asignación de personas con cargo validando la autorización activa (función + trigger), rechazo de duplicados con reactivación automática de asignaciones retiradas, baja/alta lógica conservando el historial. |
| **Avance por etapas** | Registro de horas/minutos trabajados por miembro y etapa (secuencia autonumerada por trigger), con % de avance calculado contra el tiempo estimado del catálogo de etapas (`v_proyecto_avance`) y barra de progreso en la interfaz. |
| **Acceso** (modo solo vista + admin) | Toda la app es visible sin sesión; escribir requiere haber iniciado sesión con la única cuenta `admi` (JWT en cookies `HttpOnly`, ver sección Arquitectura). |

## Arquitectura

```
Navegador ── Thymeleaf (templates/)
                │
   JwtAuthFilter + AutorizacionInterceptor (security/)  ← ¿quién sos? / ¿podés escribir?
                │
        Spring MVC (web/)          ← rutas GET (ver) / POST (confirmar)
                │
         Servicios (service/)
                │
           DAOs (dao/)             ← JdbcTemplate, SIN ORM
                │
   Funciones / triggers / vistas PL/pgSQL (db/)  ← toda la lógica de negocio de datos
                │
          PostgreSQL 16
```

- Los errores de negocio se lanzan con `RAISE EXCEPTION` en PL/pgSQL y se
  muestran al usuario tal cual (capturados como `DataAccessException`).
- Equivalencia con el patrón Swing del laboratorio (`TipPro.java`):
  GET muestra formularios (no muta), el submit POST es el "Actualizar",
  y "Cancelar" es un enlace de vuelta al listado.
- **Autenticación de una sola cuenta**: `JwtAuthFilter` lee la cookie
  `HttpOnly` del access token en cada petición (y la renueva en silencio con
  la cookie de refresh token si expiró, igual que Django SimpleJWT con
  cookies). `AutorizacionInterceptor` bloquea con `403` cualquier POST o
  formulario de alta/edición si esa cookie no identifica a `admi` — la
  contraseña se verifica en la BD (`fn_usuario_autenticar`, hash con
  pgcrypto), nunca en Java. El refresh token no se guarda en ningún lado del
  servidor: es autocontenido (stateless), como el JWT sin blacklist de Django.

## Documentación

| Documento | Para qué sirve |
|---|---|
| [`docs/00-RESUMEN.md`](docs/00-RESUMEN.md) | Panorama general breve: stack, arquitectura, modelo de datos, módulos |
| [`docs/01-METODOS-Y-LOGICA.md`](docs/01-METODOS-Y-LOGICA.md) | Catálogo a profundidad de cada función SQL, trigger, vista y método Java |
| [`docs/02-FLUJOS.md`](docs/02-FLUJOS.md) | Recorrido paso a paso de cada caso de uso, de clic a fila de BD |
| [`docs/03-DOCUMENTACION-GENERAL.md`](docs/03-DOCUMENTACION-GENERAL.md) | Documento maestro: explica el porqué de cada decisión de diseño, pensado para alguien que nunca vio el proyecto |
| [`docs/04-DJANGO-A-SPRING.md`](docs/04-DJANGO-A-SPRING.md) | GestProy explicado por comparación con Django, para quien viene de ese framework |
| [`PLAN.md`](PLAN.md) | Plan de implementación original (modelo de datos, mapeo MySQL→PostgreSQL, diseño SQL, rutas, fases) |
| [`db/README.md`](db/README.md) | Orden de ejecución de scripts SQL y convenciones |
