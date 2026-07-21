# Plan de Migración — MySQL a PostgreSQL

## Migración de base de datos MySQL a PostgreSQL

Se migró la base de datos **"proyectos"** (15 tablas) desde MySQL 8.0 hacia PostgreSQL, usando exportación/importación manual con `mysqldump` y pgAdmin.

---

### 1. Exportación desde MySQL

Se generaron dos archivos con `mysqldump`, separando estructura y datos:

**Esquema:**
```bash
mysqldump -u root -p --no-data proyectos > esquema.sql
```

**Datos:**
```bash
mysqldump -u root -p --no-create-info --complete-insert --skip-triggers proyectos > datos.sql
```

Se usó `--complete-insert` para que cada `INSERT` incluya el nombre de las columnas explícitamente, facilitando el mapeo hacia PostgreSQL.

---

### 2. Conversión de encoding

Los archivos exportados desde Windows quedaron en UTF-16LE, incompatible con psql/pgAdmin. Se convirtieron a UTF-8 con `iconv`.

---

### 3. Adaptación de la estructura (DDL)

Se ajustó la sintaxis de MySQL a PostgreSQL:

| MySQL | PostgreSQL |
|---|---|
| Comillas invertidas (`` ` ``) | Comillas dobles (`"`) |
| `int` | `integer` |
| `decimal(p,s)` | `numeric(p,s)` |

Además, se eliminaron cláusulas propias de MySQL (`ENGINE`, `CHARSET`, `COLLATE`), que no existen en PostgreSQL.

---

### 4. Reordenamiento por dependencias

MySQL desactiva la validación de *foreign keys* durante la carga; PostgreSQL no. Se determinó el orden de dependencia entre las tablas (catálogos primero, tablas transaccionales después) y se reescribieron los `CREATE TABLE` e `INSERT` respetando ese orden.

---

### 5. Ejecución en PostgreSQL (pgAdmin – Query Tool)

- Creación de la base de datos vacía.
- Ejecución del script de esquema (crea tablas, llaves primarias, foráneas e índices).
- Ejecución del script de datos (carga los registros respetando el orden de dependencias).

---

### 6. Contenerización del entorno destino

Una vez validada la migración en PostgreSQL local, se definió un `docker-compose.yml` que levanta un contenedor con la imagen oficial de PostgreSQL 16. Los scripts de esquema y datos generados en los pasos anteriores se organizaron en una carpeta `db/scripts` y se automatizó su aplicación mediante un script (`apply-all`), de forma que al iniciar el contenedor la base de datos migrada queda lista sin intervención manual.

---

### 7. Validación

Se comparó el conteo de filas por tabla entre origen y destino, confirmando coincidencia exacta en las 15 tablas (**149 registros migrados en total**), sin errores de integridad referencial.
