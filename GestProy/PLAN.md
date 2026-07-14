# PLAN.md — Gestión de Proyectos Empresariales
## Proyecto Final: Curso de Base de Datos (UNSA, Ingeniería de Sistemas)

---

## 1. Contexto y Objetivos

### 1.1 Descripción del Proyecto
Sistema web de gestión de proyectos empresarial construido sobre el esquema relacional `gestionProyectos` ya modelado y validado. Comprende 15 tablas distribuidas en capas: referenciales (9), maestras (2), relación (1) y transaccionales (3).

### 1.2 Requisitos Institucionales del Docente
- **Lenguaje obligatorio**: Java
- **Motor de BD obligatorio**: PostgreSQL 16
- **Prohibición absoluta**: No se permite usar ORM (JPA/Hibernate, QueryDSL)
- **Paradigma SQL**: Toda la lógica de negocio en funciones/procedimientos/triggers PL/pgSQL versionados (no en Java)
- **Invocación**: Spring JdbcTemplate ejecuta vía SELECT (FUNCTION) o CALL (PROCEDURE)
- **Compilación**: JDK 24 disponible en máquina, compilar con release 21 (compatible Maven)

### 1.3 Stack Elegido
| Componente | Versión |
|-----------|---------|
| Spring Boot | 3.x |
| Maven | 3.9.15 |
| PostgreSQL | 16 |
| JDK | Compilar con `release: 21` |
| Plantillas Web | Thymeleaf (server-side) |
| CSS | Bootstrap 5 (CDN) |

### 1.4 Patrón de Mantenimiento del Docente
Basado en el ejemplo `TipPro.java`, se implementan 8 comandos en el formulario:

| Comando | Operación | GET (Mostrar) | POST (Confirmar) | Campos Editables | Est. Reg Final |
|---------|-----------|:----:|:----:|------|------|
| **Adicionar** | INSERT | form vacío | guardar | Cod, Des, Extras | A (Activo) |
| **Modificar** | UPDATE datos | form + valores | guardar | Solo Des, Extras | (sin cambio) |
| **Eliminar** | Soft DELETE | form llenado | confirmar | NINGUNO | * (Eliminado) |
| **Inactivar** | Soft DESACTIVAR | form llenado | confirmar | NINGUNO | I (Inactivo) |
| **Reactivar** | Soft ACTIVAR | form llenado | confirmar | NINGUNO | A (Activo) |
| **Actualizar** | LEER BD (refresh) | — | form lista | NINGUNO | — |
| **Cancelar** | ABORTAR operación | — | form vacío | NINGUNO | — |
| **Salir** | EXIT | — | cierra | NINGUNO | — |

**Traducción a Web**: 
- `GET /entidad/new` → form vacío (ADICIONAR)
- `GET /entidad/{id}` → form llenado (MODIFICAR/ELIMINAR/INACTIVAR/REACTIVAR)
- `POST /entidad` → INSERT o UPDATE en BD (operación + est_reg según botón)
- `GET /entidad` → listado (Actualizar)

---

## 2. Modelo de Datos

### 2.1 Descripción General de las 15 Tablas

#### **GRUPO A: Referenciales de Estructura Idéntica** (Cod/Des/EstReg)
Todas tienen la forma: `PK: *_Cod`, `DESC: *_Des`, `ESTATUS: *_EstReg` (FK → gzz_est_reg).

| Tabla MySQL | Tabla PostgreSQL | Descripción |
|-----------|---------|----------|
| `GZZ_EST_REG` | `gzz_est_reg` | Base de catálogos (PK=est_reg_cod). Valores: A (Activo), I (Inactivo), * (Eliminado). **Sin FK a sí misma**, bootstrap manual. |
| `GZZ_TIP_CLI` | `gzz_tip_cli` | Tipos de cliente (empresa, ONG, gobierno, etc.) |
| `GZZ_EST_CLI` | `gzz_est_cli` | Estados del cliente (activo, suspendido, clausurado) |
| `GZZ_EST_PRO` | `gzz_est_pro` | Estados del proyecto (planificación, ejecución, cierre, suspensión) |
| `GZZ_CAR_PER` | `gzz_car_per` | Cargos personales (ingeniero, analista, técnico, jefe proyecto) |
| `GZZ_CAR_PRO` | `gzz_car_pro` | Cargos de proyecto (líder técnico, desarrollador, QA, architect) |

#### **GRUPO B: Referenciales con Columnas Extra**
Las 3 tablas de Grupo B tienen columna adicional plus configuración específica:

| Tabla MySQL | Tabla PostgreSQL | Columna Extra | Tipo | Descripción |
|-----------|---------|--------|------|--------|
| `GZZ_TIP_PRO` | `gzz_tip_pro` | `TipProTam` | `char(1)` | Tamaño proyecto (P=pequeño, M=mediano, G=grande) |
| `GZZ_LIN_PRO` | `gzz_lin_pro` | `LinProNom`, `LinProTam` | `varchar(60)`, `char(1)` | Nombre línea + tamaño |
| `GZZ_ETP_PRO` | `gzz_etp_pro` | `EtpTieEst` | `numeric(5,2)` | Horas estimadas de la etapa (ej: 40.50) |

#### **Maestras: Tablas de Datos Permanentes**

**g1m_clientes** (PK: cli_cod INT)
- `cli_cod`: INT, PK (usuario ingresa)
- `cli_nom`: VARCHAR(60), nombre/razón social
- `cli_tip_cod`: CHAR(2), FK → gzz_tip_cli
- `cli_fec_ing`: DATE, fecha ingreso (nullable)
- `cli_fec_ces`: DATE, fecha cese (nullable)
- `cli_fec_ult_pro_cer`: DATE, fecha último proyecto cerrado (nullable)
- `cli_est_cod`: CHAR(1), FK → gzz_est_cli
- `cli_est_reg_cod`: CHAR(1), FK → gzz_est_reg (A/I/*)

**g1m_personal** (PK: per_cod INT)
- `per_cod`: INT, PK (usuario ingresa)
- `per_nom`: VARCHAR(60), nombre
- `per_car_cod`: SMALLINT, FK → gzz_car_per (cargo principal)
- `per_cos_hor`: DECIMAL(10,2), costo/hora (> 0 obligatorio)
- `per_fec_ing`: DATE, fecha ingreso
- `per_est_reg_cod`: CHAR(1), FK → gzz_est_reg

#### **Relación: Autorización de Cargos**

**g1c_per_car** (PK compuesta: per_cod INT + car_pro_cod SMALLINT)
- `per_cod`: INT, FK → g1m_personal
- `car_pro_cod`: SMALLINT, FK → gzz_car_pro
- `per_car_pro_est_reg_cod`: CHAR(1), FK → gzz_est_reg
- **Semántica**: Indica qué cargos de proyecto puede ejercer cada persona (ej: Juan puede ser Líder Técnico o Desarrollador)

#### **Transaccionales: Datos del Proyecto**

**g1t_pro_cab** (PK compuesta: pro_cli_cod INT + pro_tip_cod SMALLINT + pro_sec SMALLINT)
- Cabecera del proyecto
- **Fechas**:
  - `pro_fec_con`: DATE, fecha de contrato
  - `pro_fec_pac`: DATE, fecha pactada
  - `pro_fec_ini`: DATE, fecha inicio real
  - `pro_fec_ent`: DATE, fecha entrega real
  - `pro_fec_cer`: DATE, fecha cierre
- **Montos** (presupuestado/real):
  - `pro_mon_pre`, `pro_mon_rea`: DECIMAL(10,2), ingresos
  - `pro_cos_pre`, `pro_cos_rea`: DECIMAL(10,2), costos
  - `pro_gas_pre`, `pro_gas_rea`: DECIMAL(10,2), gastos
  - `pro_uti_pre`, `pro_uti_rea`: DECIMAL(10,2), utilidad (calculada)
- `pro_est_cod`: CHAR(2), FK → gzz_est_pro (estado proyecto)
- `pro_est_reg_cod`: CHAR(1), FK → gzz_est_reg

**g1t_pro_eqp** (PK compuesta: 5 columnas)
- `pro_cli_cod`, `pro_tip_cod`, `pro_sec`: FK → g1t_pro_cab
- `per_cod`: INT, FK → g1m_personal
- `car_pro_cod`: SMALLINT, cargoAsignado en proyecto
- `pro_per_car_est_reg_cod`: CHAR(1), FK → gzz_est_reg (A=asignado, I=retirado, *=eliminado)
- **Semántica**: Qué personas (en qué cargos) están asignadas a este proyecto

**g1t_pro_mov** (PK compuesta: 7 columnas)
- `pro_cli_cod`, `pro_tip_cod`, `pro_sec`: FK → g1t_pro_cab
- `per_cod`, `car_pro_cod`: FK → g1t_pro_eqp
- `etp_cod`: SMALLINT, FK → gzz_etp_pro (etapa del proyecto)
- `sec_etp`: SMALLINT, secuencia dentro de etapa (auto-incrementada por trigger)
- `fec_reg_etp`: DATE, fecha registro
- `hor_tra_etp`: SMALLINT, horas trabajadas (0-23)
- `min_tra_etp`: SMALLINT, minutos trabajados (0-59)
- `est_reg_cod`: CHAR(1), FK → gzz_est_reg
- **Semántica**: Registro de horas/minutos trabajados por persona en cada etapa del proyecto

### 2.2 Mapeo de Nombres MySQL → PostgreSQL

Convención de migración aplicada:
- **Identidad de tabla**: `GZZ_TIP_PRO` → `gzz_tip_pro` (minúsculas, snake_case)
- **Identidad de columna**: `TipProCod` → `tip_pro_cod` (snake_case minúsculas)
- **Quitar backticks**: `\`GZZ_EST_REG\`` → `gzz_est_reg` (sin comillas)

#### Tabla Completa de Equivalencias

| Tabla | MySQL | PostgreSQL | Columnas MySQL → PostgreSQL |
|-------|-------|-----------|-----|
| **Est. Registro** | `GZZ_EST_REG` | `gzz_est_reg` | `EstRegCod` → `est_reg_cod`, `EstRegDes` → `est_reg_des`, `EstRegEstReg` → `est_reg_est_reg` |
| **Tipo Cliente** | `GZZ_TIP_CLI` | `gzz_tip_cli` | `TipCliCod` → `tip_cli_cod`, `TipCliDes` → `tip_cli_des`, `TipCliEstReg` → `tip_cli_est_reg` |
| **Est. Cliente** | `GZZ_EST_CLI` | `gzz_est_cli` | `EstCliCod` → `est_cli_cod`, `EstCliDes` → `est_cli_des`, `EstCliEstReg` → `est_cli_est_reg` |
| **Est. Proyecto** | `GZZ_EST_PRO` | `gzz_est_pro` | `EstProCod` → `est_pro_cod`, `EstProDes` → `est_pro_des`, `EstProEstReg` → `est_pro_est_reg` |
| **Cargo Personal** | `GZZ_CAR_PER` | `gzz_car_per` | `CarPerCod` → `car_per_cod`, `CarPerDes` → `car_per_des`, `CarPerEstReg` → `car_per_est_reg` |
| **Cargo Proyecto** | `GZZ_CAR_PRO` | `gzz_car_pro` | `CarProCod` → `car_pro_cod`, `CarProDes` → `car_pro_des`, `CarProEstReg` → `car_pro_est_reg` |
| **Tipo Proyecto** | `GZZ_TIP_PRO` | `gzz_tip_pro` | `TipProCod` → `tip_pro_cod`, `TipProDes` → `tip_pro_des`, `TipProTam` → `tip_pro_tam`, `TipProEstReg` → `tip_pro_est_reg` |
| **Línea Proyecto** | `GZZ_LIN_PRO` | `gzz_lin_pro` | `LinProCod` → `lin_pro_cod`, `LinProNom` → `lin_pro_nom`, `LinProTam` → `lin_pro_tam`, `LinProEstRegCod` → `lin_pro_est_reg_cod` |
| **Etapa Proyecto** | `GZZ_ETP_PRO` | `gzz_etp_pro` | `EtpCod` → `etp_cod`, `EtpDes` → `etp_des`, `EtpTieEst` → `etp_tie_est`, `EtpEstReg` → `etp_est_reg` |
| **Clientes** | `G1M_CLIENTES` | `g1m_clientes` | `CliCod` → `cli_cod`, `CliNom` → `cli_nom`, `CliTipCod` → `cli_tip_cod`, `CliFecIng` → `cli_fec_ing`, `CliFecCes` → `cli_fec_ces`, `CliFecUltProCer` → `cli_fec_ult_pro_cer`, `CliEstCod` → `cli_est_cod`, `CliEstRegCod` → `cli_est_reg_cod` |
| **Personal** | `G1M_PERSONAL` | `g1m_personal` | `PerCod` → `per_cod`, `PerNom` → `per_nom`, `PerCarCod` → `per_car_cod`, `PerCosHor` → `per_cos_hor`, `PerFecIng` → `per_fec_ing`, `PerEstReg` → `per_est_reg_cod` |
| **Cargos x Personal** | `G1C_PER_CAR` | `g1c_per_car` | `PerCod` → `per_cod`, `CarProCod` → `car_pro_cod`, `PerCarProEstReg` → `per_car_pro_est_reg_cod` |
| **Proyecto Cabecera** | `G1T_PRO_CAB` | `g1t_pro_cab` | `ProCliCod` → `pro_cli_cod`, `ProTipCod` → `pro_tip_cod`, `ProSec` → `pro_sec`, `ProFecCon` → `pro_fec_con`, `ProFecPac` → `pro_fec_pac`, `ProFecIni` → `pro_fec_ini`, `ProFecEnt` → `pro_fec_ent`, `ProFecCer` → `pro_fec_cer`, `ProMonPre` → `pro_mon_pre`, `ProMonRea` → `pro_mon_rea`, `ProCosPre` → `pro_cos_pre`, `ProCosRea` → `pro_cos_rea`, `ProGasPre` → `pro_gas_pre`, `ProGasRea` → `pro_gas_rea`, `ProUtiPre` → `pro_uti_pre`, `ProUtiRea` → `pro_uti_rea`, `ProEstCod` → `pro_est_cod`, `ProEstRegCod` → `pro_est_reg_cod` |
| **Proyecto Equipo** | `G1T_PRO_EQP` | `g1t_pro_eqp` | `ProCliCod` → `pro_cli_cod`, `ProTipCod` → `pro_tip_cod`, `ProSec` → `pro_sec`, `PerCod` → `per_cod`, `CarProCod` → `car_pro_cod`, `ProPerCarEstRegCod` → `pro_per_car_est_reg_cod` |
| **Proyecto Movimientos** | `G1T_PRO_MOV` | `g1t_pro_mov` | `ProCliCod` → `pro_cli_cod`, `ProTipCod` → `pro_tip_cod`, `ProSec` → `pro_sec`, `PerCod` → `per_cod`, `CarProCod` → `car_pro_cod`, `EtpCod` → `etp_cod`, `SecEtp` → `sec_etp`, `FecRegEtp` → `fec_reg_etp`, `HorTraEtp` → `hor_tra_etp`, `MinTraEtp` → `min_tra_etp`, `EstRegCod` → `est_reg_cod` |

---

## 3. Estructura de Carpetas (Árbol Maven)

```
GestProy/
├── README.md                           # Overview y quick start
├── PLAN.md                             # Este documento
├── pom.xml                             # POM Maven (Spring Boot 3, PostgreSQL, Thymeleaf)
├── db.properties.example               # Plantilla credenciales (gitignored real: db.properties)
│
├── db/                                 # Versionamiento SQL
│   ├── README.md                       # Orden de ejecución + mapeo de nombres
│   ├── schema/
│   │   ├── 01_referenciales.sql       # Grupo A + B tablas referenciales
│   │   ├── 02_maestras.sql            # g1m_clientes, g1m_personal
│   │   ├── 03_relacion.sql            # g1c_per_car
│   │   ├── 04_transaccionales.sql     # g1t_pro_cab, g1t_pro_eqp, g1t_pro_mov
│   │   └── 05_indices.sql             # CREATE INDEX explícitos (Postgres no auto-indexa FKs)
│   ├── triggers/
│   │   ├── 01_trg_proeqp_valida_percar_activo.sql
│   │   ├── 02_trg_promov_valida_eqp_activo.sql
│   │   ├── 03_trg_promov_autonumera_sec_etp.sql
│   │   └── 04_trg_procab_valida_fechas.sql
│   ├── functions/
│   │   ├── 10_sp_ref_grupoa_mant.sql  # Función unificada Grupo A
│   │   ├── 11_sp_ref_grupob_mant.sql  # Función unificada Grupo B
│   │   ├── 20_sp_cliente_mant.sql     # Mantenimiento clientes
│   │   ├── 21_sp_personal_mant.sql    # Mantenimiento personal
│   │   ├── 22_sp_per_car_mant.sql     # Mantenimiento cargos por persona
│   │   ├── 30_sp_proyecto_crear.sql   # Crear proyecto (genera pro_sec automático)
│   │   ├── 31_sp_proyecto_editar.sql  # Editar proyecto no cerrado
│   │   ├── 32_sp_proyecto_cambiar_estado.sql # Transiciones de estado
│   │   ├── 40_sp_proyecto_equipo_asignar.sql # Asignar persona a proyecto
│   │   ├── 41_sp_proyecto_equipo_quitar.sql  # Quitar/reactivar miembro
│   │   ├── 42_fn_personal_disponible_proyecto.sql # RETURNS TABLE personas sin asignar
│   │   ├── 50_sp_proyecto_avance_registrar.sql # Registrar horas trabajadas
│   │   └── 51_fn_proyecto_pct_avance.sql      # Calcula % avance por proyecto
│   ├── views/
│   │   ├── v_proyecto_resumen.sql     # Proyecto + cliente + tipo + estado (listado)
│   │   ├── v_proyecto_equipo.sql      # Equipo del proyecto con nombres/cargos
│   │   └── v_proyecto_avance.sql      # Horas est. vs. trabajadas + pct
│   ├── seed/
│   │   ├── 01_seed_referenciales.sql  # Catálogos (est_reg, tipos, cargos)
│   │   ├── 02_seed_maestras.sql       # Clientes y personal de prueba
│   │   └── 03_seed_transaccional.sql  # Proyectos/equipo/movimientos de demo
│   └── scripts/
│       └── apply-all.ps1              # Script PowerShell: crea BD, ejecuta schema+triggers+functions+views+seed
│
└── src/main/
    ├── java/edu/unsa/eps/gestproy/
    │   ├── config/
    │   │   ├── JdbcConfig.java        # JdbcTemplate bean
    │   │   └── WebConfig.java         # Configuración web (CORS, etc.)
    │   ├── model/
    │   │   ├── Referencial.java       # Clase genérica (tabla, código, descripción, estatus)
    │   │   ├── referencial/
    │   │   │   ├── TipoCli.java
    │   │   │   ├── EstadoCli.java
    │   │   │   ├── EstadoPro.java
    │   │   │   ├── CargoPer.java
    │   │   │   ├── CargoPro.java
    │   │   │   ├── TipoPro.java
    │   │   │   ├── LineaPro.java
    │   │   │   └── EtapaPro.java
    │   │   ├── Cliente.java
    │   │   ├── Persona.java
    │   │   ├── CargoPersona.java       # g1c_per_car
    │   │   ├── Proyecto.java           # g1t_pro_cab
    │   │   ├── ProyectoEquipo.java     # g1t_pro_eqp
    │   │   ├── ProyectoMovimiento.java # g1t_pro_mov
    │   │   ├── ProyectoResumen.java    # Proyección v_proyecto_resumen
    │   │   ├── ProyectoAvance.java     # Proyección v_proyecto_avance
    │   │   └── ResultadoOperacion.java # DTO respuesta función SQL
    │   ├── dao/
    │   │   ├── ReferencialDAO.java     # CRUD genérico referenciales (5 operaciones)
    │   │   ├── ClienteDAO.java        # CRUD clientes
    │   │   ├── PersonaDAO.java        # CRUD personal
    │   │   ├── CargoPersonaDAO.java   # CRUD g1c_per_car
    │   │   ├── ProyectoDAO.java       # CRUD proyecto cabecera
    │   │   ├── ProyectoEquipoDAO.java # CRUD equipo
    │   │   ├── ProyectoMovimientoDAO.java # CRUD movimientos
    │   │   └── EstadisticasDAO.java   # SELECT vistas (proyectos, equipo, avance)
    │   ├── service/
    │   │   ├── ReferencialService.java    # Orquestación referencias
    │   │   ├── ClienteService.java        # Orquestación clientes
    │   │   ├── PersonaService.java        # Orquestación personal
    │   │   ├── ProyectoService.java       # Orquestación proyectos (creación, edición, equipo, avance)
    │   │   └── EstadisticasService.java   # Lectura de vistas
    │   ├── web/
    │   │   ├── controller/
    │   │   │   ├── HomeController.java
    │   │   │   ├── ReferencialController.java (genérico 9 tablas)
    │   │   │   ├── ClienteController.java
    │   │   │   ├── PersonaController.java
    │   │   │   ├── ProyectoController.java
    │   │   │   └── RestErrorController.java
    │   │   └── dto/
    │   │       ├── ReferencialDTO.java
    │   │       ├── ClienteDTO.java
    │   │       ├── PersonaDTO.java
    │   │       ├── ProyectoDTO.java
    │   │       └── ProyectoEquipoDTO.java
    │   └── exception/
    │       ├── OperacionException.java
    │       ├── ValidacionException.java
    │       └── GlobalExceptionHandler.java
    │
    └── resources/
        ├── application.properties         # Config app + import db.properties
        ├── application-dev.properties     # Perfil dev (logs DEBUG)
        ├── application-prod.properties    # Perfil prod
        ├── static/
        │   ├── css/
        │   │   └── estilos.css           # Estilos propios + Bootstrap overrides
        │   ├── js/
        │   │   └── validaciones.js       # Validaciones cliente (submit, campos)
        │   └── img/
        │       └── logo.png
        └── templates/
            ├── layout/
            │   └── base.html             # Master layout + fragments
            ├── fragments/
            │   ├── header.html           # Nav bar
            │   ├── footer.html
            │   ├── nav.html              # Menú lateral
            │   ├── estado-badge.html     # Badge A/I/* colores
            │   ├── mensajes.html         # Flash alerts (success/error/warning)
            │   └── paginacion.html       # Controles paginación
            ├── index.html                # Home
            ├── referenciales/
            │   ├── list.html             # Listado genérico (9 tablas)
            │   └── form.html             # Formulario genérico (create/edit)
            ├── clientes/
            │   ├── list.html             # Listado clientes
            │   ├── form.html             # Crear/editar cliente
            │   └── detalle.html          # Detalle cliente
            ├── personal/
            │   ├── list.html             # Listado personal
            │   ├── form.html             # Crear/editar persona
            │   ├── cargos-list.html      # Cargos autorizados de persona
            │   └── cargos-form.html      # Agregar cargo a persona
            └── proyectos/
                ├── list.html             # Listado proyectos
                ├── form.html             # Crear/editar proyecto
                ├── detalle.html          # Detalle proyecto
                ├── equipo-list.html      # Equipo asignado
                ├── equipo-form.html      # Asignar persona
                ├── avance-list.html      # Registro de horas (movimientos)
                └── avance-form.html      # Registrar horas en etapa
```

---

## 4. Reglas de Conversión MySQL → PostgreSQL

### 4.1 Cambios Estructurales

| Aspecto | MySQL | PostgreSQL |
|--------|-------|-----------|
| **Identificadores** | Backticks `GZZ_TIP_PRO` | Sin comillas, minúsculas `gzz_tip_pro` |
| **Snake Case** | CamelCase `TipProCod` | snake_case `tip_pro_cod` |
| **Tipos Numéricos** | SMALLINT, INT | SMALLINT, INT (compatibles) |
| **Decimales** | DECIMAL(10,2) | numeric(10,2) (compatible) |
| **Caracteres** | CHAR(1), VARCHAR(60) | char(1), varchar(60) (compatibles) |
| **Fechas** | DATE | DATE (compatible) |
| **Secuencias** | SERIAL (auto) | SERIAL (no usado); secuencias manuales en SP |
| **Pragma MySQL** | SET @OLD_..., SQL_MODE, ... | Eliminadas (Postgres no las usa) |

### 4.2 Cambios de DDL

**MySQL**:
```sql
DROP TABLE IF EXISTS `GZZ_TIP_PRO`;
CREATE TABLE IF NOT EXISTS `GZZ_TIP_PRO` (
  `TipProCod` SMALLINT NOT NULL,
  ...
  PRIMARY KEY (`TipProCod`),
  CONSTRAINT `fk_tippro_estreg` FOREIGN KEY (`TipProEstReg`) 
    REFERENCES `GZZ_EST_REG` (`EstRegCod`)
);
```

**PostgreSQL** (cada archivo envuelto en BEGIN...COMMIT):
```sql
BEGIN;
DROP TABLE IF EXISTS gzz_tip_pro CASCADE;
CREATE TABLE gzz_tip_pro (
  tip_pro_cod SMALLINT NOT NULL,
  ...
  PRIMARY KEY (tip_pro_cod),
  CONSTRAINT fk_tippro_estreg FOREIGN KEY (tip_pro_est_reg)
    REFERENCES gzz_est_reg(est_reg_cod)
);
COMMIT;
```

### 4.3 Cambios en Seeds

**MySQL**:
```sql
INSERT IGNORE INTO GZZ_EST_REG (...) VALUES ...;
```

**PostgreSQL**:
```sql
INSERT INTO gzz_est_reg (...) VALUES ...
ON CONFLICT (est_reg_cod) DO NOTHING;
```

### 4.4 Índices Explícitos

PostgreSQL **NO indexa automáticamente las Foreign Keys**, a diferencia de MySQL.

**05_indices.sql** crea índices explícitos para todas las FK:
```sql
CREATE INDEX idx_gzz_tip_cli_est_reg ON gzz_tip_cli(tip_cli_est_reg);
CREATE INDEX idx_g1m_clientes_cli_tip ON g1m_clientes(cli_tip_cod);
CREATE INDEX idx_g1t_pro_cab_pro_est ON g1t_pro_cab(pro_est_cod);
-- etc.
```

---

## 5. Diseño SQL Detallado

### 5.1 Convenciones de Nombres

- **Funciones de Mantenimiento** (INSERT/UPDATE/DELETE): `sp_*` (ejemplo: `sp_cliente_mant`)
  - Invocadas: `SELECT sp_cliente_mant(...)`
  - Retornan: `RECORD` con (p_cod, p_mensaje) o `TEXT` (mensaje)
- **Funciones de Lectura** (SELECT): `fn_*` (ejemplo: `fn_personal_disponible_proyecto`)
  - Invocadas: `SELECT * FROM fn_personal_disponible_proyecto(...)`
  - Retornan: `SETOF RECORD` o `TABLE` proyectado
- **Triggers**: `trg_*` (ejemplo: `trg_proeqp_valida_percar_activo`)
- **Vistas**: `v_*` (ejemplo: `v_proyecto_resumen`)

**Todas las funciones usan FUNCTION (no PROCEDURE)** porque Spring JdbcTemplate ejecuta con SELECT y espera un resultado (`executeQuery`, no `execute`).

### 5.2 Funciones de Referenciales Grupo A

**Contrato de errores (aplica a TODAS las funciones sp_/fn_)**: las funciones NO devuelven tuplas `(exitoso, mensaje)` ni capturan errores con `EXCEPTION WHEN OTHERS`. Toda validación fallida lanza `RAISE EXCEPTION 'mensaje claro en español'`, que Spring recibe como `DataAccessException` y el `GlobalExceptionHandler` convierte en mensaje flash (ver sección 6.5). Las funciones de mantenimiento retornan `void`; las que generan valores (ej. `sp_proyecto_crear`) retornan esos valores.

**Firma** (UNA sola función, ramas `IF/ELSIF` con SQL literal por tabla — NO sub-funciones, NO `EXECUTE format()` dinámico):
```sql
CREATE OR REPLACE FUNCTION sp_ref_grupoa_mant(
  p_tabla TEXT,        -- 'gzz_est_reg' | 'gzz_tip_cli' | 'gzz_est_cli' | 'gzz_est_pro' | 'gzz_car_per' | 'gzz_car_pro'
  p_operacion TEXT,    -- 'ADICIONAR' | 'MODIFICAR' | 'ELIMINAR' | 'INACTIVAR' | 'REACTIVAR'
  p_cod VARCHAR,       -- código (char(1) o char(2) según tabla)
  p_des VARCHAR        -- descripción
)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
  v_est_reg CHAR(1);   -- estado destino según operación: NULL(mantiene)/'*'/'I'/'A'
BEGIN
  -- 1. Resolver operación → estado destino / tipo de sentencia
  -- 2. Ramas explícitas por tabla, con el INSERT/UPDATE literal de cada una:
  IF p_tabla = 'gzz_tip_cli' THEN
    IF p_operacion = 'ADICIONAR' THEN
      -- valida duplicado y luego:
      INSERT INTO gzz_tip_cli (tip_cli_cod, tip_cli_des, tip_cli_est_reg) VALUES (p_cod, p_des, 'A');
    ELSIF p_operacion = 'MODIFICAR' THEN
      UPDATE gzz_tip_cli SET tip_cli_des = p_des WHERE tip_cli_cod = p_cod;
    ELSE  -- ELIMINAR / INACTIVAR / REACTIVAR
      UPDATE gzz_tip_cli SET tip_cli_est_reg = v_est_reg WHERE tip_cli_cod = p_cod;
    END IF;
  ELSIF p_tabla = 'gzz_est_cli' THEN
    -- ... mismo patrón con SQL literal de gzz_est_cli ...
  -- ... una rama ELSIF por cada una de las 6 tablas del Grupo A ...
  ELSE
    RAISE EXCEPTION 'Tabla no reconocida: %', p_tabla;
  END IF;
  -- Si el código no existe en MODIFICAR/ELIMINAR/INACTIVAR/REACTIVAR → RAISE EXCEPTION
  -- Si el código ya existe en ADICIONAR → RAISE EXCEPTION 'Ya existe un registro con el código %'
END;
$$;
```

**Operaciones por rama**:

1. **ADICIONAR**: INSERT nuevo registro
   - Validar que p_cod no exista
   - Validar p_des no esté vacío
   - INSERT con est_reg_cod = 'A' (Activo)
   - RAISE EXCEPTION si duplicate key

2. **MODIFICAR**: UPDATE descripción
   - Validar que p_cod exista
   - UPDATE p_des donde *_cod = p_cod
   - No cambiar est_reg_cod (se mantiene)

3. **ELIMINAR**: Soft delete → est_reg_cod = '*'
   - UPDATE est_reg_cod = '*' donde *_cod = p_cod
   - Validar que registro exista

4. **INACTIVAR**: Soft desactivar → est_reg_cod = 'I'
   - UPDATE est_reg_cod = 'I' donde *_cod = p_cod

5. **REACTIVAR**: Soft activar → est_reg_cod = 'A'
   - UPDATE est_reg_cod = 'A' donde *_cod = p_cod

### 5.3 Funciones de Referenciales Grupo B

Tres funciones: `sp_gzz_tip_pro_mant`, `sp_gzz_lin_pro_mant`, `sp_gzz_etp_pro_mant`

**Contrato identical al Grupo A** + columna extra en cada tabla:
- `sp_gzz_tip_pro_mant(p_operacion, p_cod, p_des, p_tam)` → actualiza `tip_pro_tam`
- `sp_gzz_lin_pro_mant(p_operacion, p_cod, p_nom, p_tam)` → actualiza `lin_pro_nom`, `lin_pro_tam`
- `sp_gzz_etp_pro_mant(p_operacion, p_cod, p_des, p_tie_est)` → actualiza `etp_tie_est`

### 5.4 Funciones de Maestras

#### **sp_cliente_mant**(p_operacion, p_cod, p_nom, p_tip_cod, p_est_cod, p_fec_ing, p_fec_ces, ...)
- **ADICIONAR**: 
  - Validar p_cod no exista
  - Validar p_tip_cod sea FK activa a gzz_tip_cli
  - Validar p_est_cod sea FK activa a gzz_est_cli
  - INSERT con cli_est_reg_cod = 'A'
- **MODIFICAR**: UPDATE campos (excepto cli_cod)
- **ELIMINAR/INACTIVAR/REACTIVAR**: Cambiar cli_est_reg_cod a '*/I/A'

**Retorna**: void; errores vía RAISE EXCEPTION

#### **sp_personal_mant**(p_operacion, p_cod, p_nom, p_car_cod, p_cos_hor, p_fec_ing)
- **Validaciones**:
  - p_cos_hor > 0 obligatorio
  - p_car_cod debe ser FK activa a gzz_car_per
  - p_fec_ing <= HOY
- **ADICIONAR**: Insertar con per_est_reg_cod = 'A'
- **Otras operaciones**: análogas a clientes

**Retorna**: void; errores vía RAISE EXCEPTION

#### **sp_per_car_mant**(p_operacion, p_per_cod, p_car_pro_cod)
- **Validaciones**:
  - p_per_cod debe ser FK activa a g1m_personal
  - p_car_pro_cod debe ser FK activa a gzz_car_pro
- **ADICIONAR**: 
  - Si PK (per_cod, car_pro_cod) NO existe → INSERT con per_car_pro_est_reg_cod = 'A'
  - Si PK existe CON est_reg = 'I' o '*' → UPDATE a 'A' (reactivar, no error)
  - Si PK existe CON est_reg = 'A' → RAISE EXCEPTION (ya activa)
- **MODIFICAR/ELIMINAR/INACTIVAR/REACTIVAR**: cambiar estado PK existente

**Retorna**: void; errores vía RAISE EXCEPTION

### 5.5 Funciones de Proyectos

#### **sp_proyecto_crear**(p_cli_cod, p_tip_cod, p_fec_con, p_fec_pac, p_est_cod, ...)
- **Validaciones**:
  - p_cli_cod FK activa a g1m_clientes
  - p_tip_cod FK activa a gzz_tip_pro
  - p_est_cod FK activa a gzz_est_pro
  - Fechas coherentes (si no NULL): fec_con <= fec_pac
- **Cálculo automático**:
  - `pro_sec` = MAX(pro_sec) + 1 para (pro_cli_cod = p_cli_cod, pro_tip_cod = p_tip_cod)
  - Si no hay registros previos para ese cliente+tipo → pro_sec = 1
- **INSERT** cabecera con pro_est_reg_cod = 'A'
- **RETORNA PK completa**: (pro_cli_cod, pro_tip_cod, pro_sec)

#### **sp_proyecto_editar**(p_cli_cod, p_tip_cod, p_sec, p_fec_ini, p_fec_ent, p_mon_pre, ...)
- **Validación**: proyecto debe existir y no estar cerrado (pro_est_cod ≠ 'Cierre')
- **UPDATE** cabecera (excepto PK y pro_est_cod)
- **RETORNA**: void; errores vía RAISE EXCEPTION

#### **sp_proyecto_cambiar_estado**(p_cli_cod, p_tip_cod, p_sec, p_nuevo_est_cod)
- **Validaciones**:
  - Proyecto debe existir
  - Validar transición de estado es válida según matriz (ej: Planificación→Ejecución OK, Cierre→Planificación NO)
  - Si p_nuevo_est_cod = 'Cierre' → requiere haber completado etapas (opcional en v1)
- **UPDATE** pro_est_cod
- **RETORNA**: void; errores vía RAISE EXCEPTION

#### **sp_proyecto_equipo_asignar**(p_cli_cod, p_tip_cod, p_sec, p_per_cod, p_car_pro_cod)
- **Validaciones**:
  - Proyecto debe existir y no estar cerrado
  - g1c_per_car (p_per_cod, p_car_pro_cod) debe existir CON est_reg = 'A'
  - Combinación (pro_cli_cod, pro_tip_cod, pro_sec, per_cod, car_pro_cod) no debe existir activa
- **Lógica**:
  - Si PK NO existe → INSERT con pro_per_car_est_reg_cod = 'A'
  - Si PK existe CON est_reg = 'I' o '*' → UPDATE a 'A' (reactivar)
  - Si PK existe CON est_reg = 'A' → RAISE EXCEPTION (ya asignado)
- **RETORNA**: void; errores vía RAISE EXCEPTION

#### **sp_proyecto_equipo_quitar**(p_cli_cod, p_tip_cod, p_sec, p_per_cod, p_car_pro_cod)
- **Validación**: tupla debe existir con est_reg = 'A'
- **UPDATE** pro_per_car_est_reg_cod = 'I' (quitar, no eliminar)
- **RETORNA**: void; errores vía RAISE EXCEPTION

#### **sp_proyecto_equipo_reactivar**(p_cli_cod, p_tip_cod, p_sec, p_per_cod, p_car_pro_cod)
- **Validación**: tupla debe existir con est_reg = 'I'
- **UPDATE** pro_per_car_est_reg_cod = 'A'
- **RETORNA**: void; errores vía RAISE EXCEPTION

#### **fn_personal_disponible_proyecto**(p_cli_cod, p_tip_cod, p_sec)
- **Qué retorna**: Tabla de (per_cod, per_nom, car_pro_cod, car_pro_des) personas que:
  - Tienen g1c_per_car.est_reg = 'A'
  - NO están en g1t_pro_eqp con pro_per_car_est_reg_cod = 'A' para este proyecto
- **Uso**: Llena <select> de "Agregar personal" en formulario equipo
- **RETURNS TABLE** (per_cod, per_nom, car_pro_cod, car_pro_des)

#### **sp_proyecto_avance_registrar**(p_cli_cod, p_tip_cod, p_sec, p_per_cod, p_car_pro_cod, p_etp_cod, p_hor_tra, p_min_tra, p_fec_reg)
- **Validaciones**:
  - Persona+cargo debe estar en g1t_pro_eqp con est_reg = 'A'
  - Etapa debe existir y estar activa
  - p_hor_tra ∈ [0,23], p_min_tra ∈ [0,59]
  - p_fec_reg <= HOY
- **INSERT** movimiento
  - sec_etp se calcula automáticamente por trigger (MAX+1)
- **RETORNA**: smallint (sec_etp asignada); errores vía RAISE EXCEPTION

#### **fn_proyecto_pct_avance**(p_cli_cod, p_tip_cod, p_sec)
- **Cálculo**:
  ```
  pct = (COALESCE(SUM(hor_tra_etp + min_tra_etp/60), 0) / 
         NULLIF(SUM(etp_tie_est WHERE est_reg='A'), 0) * 100)
  ```
- **Retorna**: NUMERIC(5,2), valor entre 0 y 999.99
- **Casos especiales**:
  - Sin movimientos registrados → 0
  - Sin etapas activas → 0 (o NULL si se elige)
  - Avance > 100 es posible (persona trabajó más que estimado) → mostrar en rojo en UI

### 5.6 Triggers

#### **01_trg_proeqp_valida_percar_activo** (BEFORE INSERT OR UPDATE en g1t_pro_eqp)
```sql
CREATE OR REPLACE FUNCTION trg_proeqp_valida_percar_activo()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  -- Valida que g1c_per_car (NEW.per_cod, NEW.car_pro_cod) exista CON est_reg = 'A'
  IF NOT EXISTS (
    SELECT 1 FROM g1c_per_car
    WHERE per_cod = NEW.per_cod
      AND car_pro_cod = NEW.car_pro_cod
      AND per_car_pro_est_reg_cod = 'A'
  ) THEN
    RAISE EXCEPTION 'El personal no tiene el cargo de proyecto autorizado y activo';
  END IF;
  RETURN NEW;
END;
$$;

CREATE TRIGGER trg_proeqp_valida_percar_activo
BEFORE INSERT OR UPDATE ON g1t_pro_eqp
FOR EACH ROW
EXECUTE FUNCTION trg_proeqp_valida_percar_activo();
```

**Propósito**: La FK sola no valida el estado (est_reg) de g1c_per_car, solo que exista. Este trigger garantiza que el cargo está activo.

#### **02_trg_promov_valida_eqp_activo** (BEFORE INSERT en g1t_pro_mov)
```sql
CREATE OR REPLACE FUNCTION trg_promov_valida_eqp_activo()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  -- Valida que g1t_pro_eqp (pro_cli_cod, pro_tip_cod, pro_sec, per_cod, car_pro_cod)
  -- exista CON pro_per_car_est_reg_cod = 'A'
  IF NOT EXISTS (
    SELECT 1 FROM g1t_pro_eqp
    WHERE pro_cli_cod = NEW.pro_cli_cod
      AND pro_tip_cod = NEW.pro_tip_cod
      AND pro_sec = NEW.pro_sec
      AND per_cod = NEW.per_cod
      AND car_pro_cod = NEW.car_pro_cod
      AND pro_per_car_est_reg_cod = 'A'
  ) THEN
    RAISE EXCEPTION 'El personal no está activamente asignado a este proyecto';
  END IF;
  RETURN NEW;
END;
$$;

CREATE TRIGGER trg_promov_valida_eqp_activo
BEFORE INSERT ON g1t_pro_mov
FOR EACH ROW
EXECUTE FUNCTION trg_promov_valida_eqp_activo();
```

**Propósito**: Impide registrar horas de personal retirado del proyecto (est_reg='I').

#### **03_trg_promov_autonumera_sec_etp** (BEFORE INSERT en g1t_pro_mov)
```sql
CREATE OR REPLACE FUNCTION trg_promov_autonumera_sec_etp()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  -- Calcula sec_etp = MAX(sec_etp) + 1 para esta combinación
  -- (pro_cli_cod, pro_tip_cod, pro_sec, per_cod, car_pro_cod, etp_cod)
  SELECT COALESCE(MAX(sec_etp), 0) + 1
  INTO NEW.sec_etp
  FROM g1t_pro_mov
  WHERE pro_cli_cod = NEW.pro_cli_cod
    AND pro_tip_cod = NEW.pro_tip_cod
    AND pro_sec = NEW.pro_sec
    AND per_cod = NEW.per_cod
    AND car_pro_cod = NEW.car_pro_cod
    AND etp_cod = NEW.etp_cod;
  RETURN NEW;
END;
$$;

CREATE TRIGGER trg_promov_autonumera_sec_etp
BEFORE INSERT ON g1t_pro_mov
FOR EACH ROW
EXECUTE FUNCTION trg_promov_autonumera_sec_etp();
```

**Propósito**: No requiere input de usuario; se calcula automáticamente.

#### **04_trg_procab_valida_fechas** (BEFORE INSERT OR UPDATE en g1t_pro_cab)
```sql
CREATE OR REPLACE FUNCTION trg_procab_valida_fechas()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  -- Valida coherencia de fechas
  IF NEW.pro_fec_ini IS NOT NULL AND NEW.pro_fec_ent IS NOT NULL THEN
    IF NEW.pro_fec_ini > NEW.pro_fec_ent THEN
      RAISE EXCEPTION 'Fecha inicio no puede ser posterior a fecha entrega';
    END IF;
  END IF;
  
  IF NEW.pro_fec_con IS NOT NULL AND NEW.pro_fec_pac IS NOT NULL THEN
    IF NEW.pro_fec_con > NEW.pro_fec_pac THEN
      RAISE EXCEPTION 'Fecha contrato no puede ser posterior a fecha pactada';
    END IF;
  END IF;
  
  RETURN NEW;
END;
$$;

CREATE TRIGGER trg_procab_valida_fechas
BEFORE INSERT OR UPDATE ON g1t_pro_cab
FOR EACH ROW
EXECUTE FUNCTION trg_procab_valida_fechas();
```

**Propósito**: Garantiza integridad de lógica de fechas en cabecera.

### 5.7 Vistas SQL

#### **v_proyecto_resumen**
```sql
CREATE OR REPLACE VIEW v_proyecto_resumen AS
SELECT
  p.pro_cli_cod,
  p.pro_tip_cod,
  p.pro_sec,
  c.cli_nom AS cliente_nombre,
  t.tip_pro_des AS tipo_descripcion,
  e.est_pro_des AS estado_descripcion,
  p.pro_fec_ini,
  p.pro_fec_ent,
  p.pro_fec_cer,
  p.pro_mon_pre,
  p.pro_mon_rea,
  p.pro_est_cod,
  p.pro_est_reg_cod
FROM g1t_pro_cab p
JOIN g1m_clientes c ON p.pro_cli_cod = c.cli_cod
JOIN gzz_tip_pro t ON p.pro_tip_cod = t.tip_pro_cod
JOIN gzz_est_pro e ON p.pro_est_cod = e.est_pro_cod
WHERE p.pro_est_reg_cod = 'A';
```

**Uso**: Listado de proyectos en vista principal (tabla con cliente, tipo, estado, fechas).

#### **v_proyecto_equipo**
```sql
CREATE OR REPLACE VIEW v_proyecto_equipo AS
SELECT
  e.pro_cli_cod,
  e.pro_tip_cod,
  e.pro_sec,
  e.per_cod,
  p.per_nom,
  e.car_pro_cod,
  c.car_pro_des,
  e.pro_per_car_est_reg_cod,
  CASE WHEN e.pro_per_car_est_reg_cod = 'A' THEN 'Activo'
       WHEN e.pro_per_car_est_reg_cod = 'I' THEN 'Retirado'
       ELSE 'Eliminado' END AS estado_equipo
FROM g1t_pro_eqp e
JOIN g1m_personal p ON e.per_cod = p.per_cod
JOIN gzz_car_pro c ON e.car_pro_cod = c.car_pro_cod;
```

**Uso**: Detalle de equipo asignado a proyecto (nombres humanos en lugar de códigos).

#### **v_proyecto_avance**
```sql
CREATE OR REPLACE VIEW v_proyecto_avance AS
SELECT
  p.pro_cli_cod,
  p.pro_tip_cod,
  p.pro_sec,
  COALESCE(SUM(m.hor_tra_etp + m.min_tra_etp::numeric/60), 0)::numeric(8,2) AS horas_trabajadas,
  (SELECT COALESCE(SUM(etp_tie_est), 0)
   FROM gzz_etp_pro e
   WHERE e.etp_est_reg = 'A')::numeric(8,2) AS horas_estimadas,
  fn_proyecto_pct_avance(p.pro_cli_cod, p.pro_tip_cod, p.pro_sec) AS pct_avance
FROM g1t_pro_cab p
LEFT JOIN g1t_pro_mov m ON (
  m.pro_cli_cod = p.pro_cli_cod
  AND m.pro_tip_cod = p.pro_tip_cod
  AND m.pro_sec = p.pro_sec
  AND m.est_reg_cod = 'A'
)
GROUP BY p.pro_cli_cod, p.pro_tip_cod, p.pro_sec;
```

**Uso**: Dashboard de avance (horas trabajadas vs estimadas, porcentaje, barra de progreso).

---

## 6. Diseño de la Aplicación Spring Boot

### 6.1 Arquitectura de Capas

```
HTTP Request (GET/POST)
         ↓
[Controller] → valida entrada, traduce DTO → modelo
         ↓
[Service] → orquestación, validaciones biz logic, excepciones
         ↓
[DAO] → JdbcTemplate → ejecuta función/procedimiento SQL
         ↓
[PL/pgSQL] → manipula tablas, triggers, validaciones BD
         ↓
[ResultSet / RECORD] → retorna a DAO
         ↓
[DTO] → JSON en respuesta HTTP / Thymeleaf modelo
```

### 6.2 Configuración Maven (pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.0</version> <!-- Spring Boot 3.x LTS -->
  </parent>
  
  <groupId>edu.unsa.eps</groupId>
  <artifactId>gestproy</artifactId>
  <version>1.0.0</version>
  <packaging>jar</packaging>
  
  <properties>
    <maven.compiler.release>21</maven.compiler.release>
  </properties>
  
  <dependencies>
    <!-- Spring Web + Thymeleaf -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
    
    <!-- JDBC (SIN JPA) -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>
    
    <!-- Validación -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    
    <!-- PostgreSQL Driver -->
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <version>42.7.4</version>
      <scope>runtime</scope>
    </dependency>
    
    <!-- Lombok (opcional, para @Getter/@Setter) -->
    <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <optional>true</optional>
    </dependency>
    
    <!-- Testing -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>
  
  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

### 6.3 application.properties

```properties
# Server
server.port=8080
server.servlet.context-path=/

# Datasource (credenciales desde db.properties gitignored)
spring.config.import=optional:file:./db.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/gestion_proyectos
spring.datasource.username=postgres
spring.datasource.password=
spring.datasource.driver-class-name=org.postgresql.Driver

# Thymeleaf
spring.thymeleaf.cache=false
spring.thymeleaf.encoding=UTF-8
spring.thymeleaf.mode=HTML
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html

# Logging
logging.level.root=INFO
logging.level.edu.unsa.eps.gestproy=DEBUG

# Internacionalization
spring.messages.basename=messages
spring.messages.encoding=UTF-8
```

### 6.4 Tabla de Rutas HTTP

| Método | Ruta | Descripción | Controlador | Acción |
|--------|------|-------------|-------------|--------|
| GET | `/` | Home | HomeController | Página principal |
| **REFERENCIALES (9 tablas genéricas)** |
| GET | `/referenciales/{tabla}` | Listado tabla referencial | ReferencialController | lista |
| GET | `/referenciales/{tabla}/new` | Formulario adicionar | ReferencialController | formulario (vacío) |
| GET | `/referenciales/{tabla}/{cod}` | Formulario editar | ReferencialController | formulario (llenado) |
| POST | `/referenciales/{tabla}` | Guardar (crear/editar) | ReferencialController | guardar |
| POST | `/referenciales/{tabla}/{cod}/eliminar` | Eliminar | ReferencialController | eliminar |
| POST | `/referenciales/{tabla}/{cod}/inactivar` | Inactivar | ReferencialController | inactivar |
| POST | `/referenciales/{tabla}/{cod}/reactivar` | Reactivar | ReferencialController | reactivar |
| **CLIENTES** |
| GET | `/clientes` | Listado | ClienteController | lista |
| GET | `/clientes/new` | Formulario adicionar | ClienteController | formulario (vacío) |
| GET | `/clientes/{cod}` | Formulario editar | ClienteController | formulario (llenado) |
| POST | `/clientes` | Guardar | ClienteController | guardar |
| POST | `/clientes/{cod}/eliminar` | Eliminar | ClienteController | eliminar |
| POST | `/clientes/{cod}/inactivar` | Inactivar | ClienteController | inactivar |
| POST | `/clientes/{cod}/reactivar` | Reactivar | ClienteController | reactivar |
| **PERSONAL** |
| GET | `/personal` | Listado | PersonaController | lista |
| GET | `/personal/new` | Formulario adicionar | PersonaController | formulario (vacío) |
| GET | `/personal/{cod}` | Formulario editar | PersonaController | formulario (llenado) |
| POST | `/personal` | Guardar | PersonaController | guardar |
| POST | `/personal/{cod}/eliminar` | Eliminar | PersonaController | eliminar |
| POST | `/personal/{cod}/inactivar` | Inactivar | PersonaController | inactivar |
| POST | `/personal/{cod}/reactivar` | Reactivar | PersonaController | reactivar |
| **CARGOS por PERSONA** |
| GET | `/personal/{perCod}/cargos` | Listado cargos | PersonaController | cargos_lista |
| GET | `/personal/{perCod}/cargos/new` | Formulario agregar | PersonaController | cargos_formulario |
| POST | `/personal/{perCod}/cargos` | Agregar cargo | PersonaController | cargos_guardar |
| POST | `/personal/{perCod}/cargos/{carProCod}/inactivar` | Inactivar cargo | PersonaController | cargos_inactivar |
| POST | `/personal/{perCod}/cargos/{carProCod}/reactivar` | Reactivar cargo | PersonaController | cargos_reactivar |
| **PROYECTOS** |
| GET | `/proyectos` | Listado | ProyectoController | lista |
| GET | `/proyectos/new` | Formulario crear | ProyectoController | formulario (vacío) |
| GET | `/proyectos/{cliCod}/{tipCod}/{sec}` | Formulario editar | ProyectoController | formulario (llenado) |
| POST | `/proyectos` | Crear/editar proyecto | ProyectoController | guardar |
| POST | `/proyectos/{cliCod}/{tipCod}/{sec}/estado` | Cambiar estado | ProyectoController | cambiar_estado |
| GET | `/proyectos/{cliCod}/{tipCod}/{sec}/detalle` | Detalle proyecto | ProyectoController | detalle |
| **EQUIPO de PROYECTO** |
| GET | `/proyectos/{cliCod}/{tipCod}/{sec}/equipo` | Listado equipo | ProyectoController | equipo_lista |
| GET | `/proyectos/{cliCod}/{tipCod}/{sec}/equipo/new` | Formulario asignar | ProyectoController | equipo_formulario |
| POST | `/proyectos/{cliCod}/{tipCod}/{sec}/equipo` | Asignar | ProyectoController | equipo_asignar |
| POST | `/proyectos/{cliCod}/{tipCod}/{sec}/equipo/{perCod}/{carProCod}/quitar` | Quitar | ProyectoController | equipo_quitar |
| POST | `/proyectos/{cliCod}/{tipCod}/{sec}/equipo/{perCod}/{carProCod}/reactivar` | Reactivar | ProyectoController | equipo_reactivar |
| **AVANCE de PROYECTO** |
| GET | `/proyectos/{cliCod}/{tipCod}/{sec}/avance` | Listado movimientos | ProyectoController | avance_lista |
| GET | `/proyectos/{cliCod}/{tipCod}/{sec}/avance/new` | Formulario registrar | ProyectoController | avance_formulario |
| POST | `/proyectos/{cliCod}/{tipCod}/{sec}/avance` | Registrar horas | ProyectoController | avance_registrar |

### 6.5 Manejo de Errores

**GlobalExceptionHandler** (@ControllerAdvice):
- Captura DataAccessException (RAISE EXCEPTION de PL/pgSQL)
- Convierte mensaje SQL en mensaje legible (español)
- Retorna Model + atributo `error` para Thymeleaf
- Redirige a listado o formulario con flash message

Ejemplo:
```java
@ExceptionHandler(DataAccessException.class)
public String handleDataAccessException(DataAccessException ex, RedirectAttributes redirectAttrs) {
  String mensaje = ex.getMessage();
  if (mensaje.contains("El personal no tiene el cargo de proyecto autorizado")) {
    redirectAttrs.addFlashAttribute("error", "El personal no está autorizado para ese cargo");
  } else if (mensaje.contains("Ya existe")) {
    redirectAttrs.addFlashAttribute("error", "El código ya existe");
  } else {
    redirectAttrs.addFlashAttribute("error", "Error en operación: " + mensaje);
  }
  return "redirect:/...";
}
```

### 6.6 Enum ReferencialTabla

Metadatos de las 9 tablas referenciales para uso genérico:

```java
public enum ReferencialTabla {
  // slug URL, tabla SQL, etiqueta singular/plural, columnas extra
  EST_REG("est_reg", "gzz_est_reg", "Estado de Registro", "Estados de Registro"),
  TIP_CLI("tip_cli", "gzz_tip_cli", "Tipo de Cliente", "Tipos de Cliente"),
  EST_CLI("est_cli", "gzz_est_cli", "Estado de Cliente", "Estados de Cliente"),
  EST_PRO("est_pro", "gzz_est_pro", "Estado de Proyecto", "Estados de Proyecto"),
  CAR_PER("car_per", "gzz_car_per", "Cargo Personal", "Cargos Personales"),
  CAR_PRO("car_pro", "gzz_car_pro", "Cargo de Proyecto", "Cargos de Proyecto"),
  TIP_PRO("tip_pro", "gzz_tip_pro", "Tipo de Proyecto", "Tipos de Proyecto"),   // extra: tam char(1)
  LIN_PRO("lin_pro", "gzz_lin_pro", "Línea de Proyecto", "Líneas de Proyecto"), // extra: nom varchar(60) + tam char(1)
  ETP_PRO("etp_pro", "gzz_etp_pro", "Etapa de Proyecto", "Etapas de Proyecto"); // extra: tie_est numeric(5,2)
  // Cada constante conoce: slug, nombre de tabla, prefijo de columnas, función SQL a invocar
  // y la definición de sus columnas extra (nombre, tipo, etiqueta) para la vista genérica.
}
```
**Nota**: son 9 tablas (incluye `gzz_est_reg`). Las 6 primeras usan `sp_ref_grupoa_mant`; las 3 últimas (Grupo B) su función específica.

### 6.7 Componentes Thymeleaf

#### **layout/base.html** (Master Layout)
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title th:text="${titulo} + ' - Gestión de Proyectos'">...</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
  <link th:href="@{/css/estilos.css}" rel="stylesheet">
</head>
<body>
  <nav class="navbar navbar-expand-lg navbar-dark bg-dark">
    <!-- Navbar fragment -->
  </nav>
  <div class="container-fluid mt-3">
    <div th:replace="fragments/mensajes :: alertas"></div>
    <main th:block="${content}"></main>
  </div>
  <footer>...</footer>
</body>
</html>
```

#### **fragments/estado-badge.html**
```html
<th:block th:fragment="badge(valor)">
  <span th:if="${valor == 'A'}" class="badge bg-success">Activo</span>
  <span th:if="${valor == 'I'}" class="badge bg-warning">Inactivo</span>
  <span th:if="${valor == '*'}" class="badge bg-danger">Eliminado</span>
</th:block>
```

#### **referenciales/list.html** (Listado genérico)
Tabla con columnas: Código, Descripción, (Columna Extra si aplica), Estado, Acciones
- Botones: Editar, Eliminar, Inactivar, Reactivar (según estado actual)
- Enlace "Adicionar" al top

#### **referenciales/form.html** (Formulario genérico)
Campos de entrada según tabla (código, descripción, columna extra si aplica)
- GET vacío (Adicionar) vs GET llenado (Modificar/Eliminar/etc)
- Botones: Actualizar, Cancelar
- Validaciones JavaScript en cliente (código no vacío, descripción no vacío)

#### **proyectos/detalle.html**
Muestra:
- Resumen proyecto (cliente, tipo, estado, fechas, montos)
- Sección Equipo (tabla v_proyecto_equipo)
- Sección Avance (barra de progreso % con horas trabajadas vs estimadas)
- Botones: Editar, Cambiar Estado, Quitar Persona, Registrar Horas

#### **proyectos/avance-list.html**
Tabla con histórico movimientos:
- Persona, Cargo, Etapa, Fecha, Horas, Minutos, Estado
- Gráfico de barra de progreso general del proyecto (% avance)

---

## 7. Fases de Implementación

### Fase 0: Prerrequisitos
- Máquina con PostgreSQL 16 instalado y ejecutándose
- JDK 24 disponible en PATH
- Maven 3.9.15 en PATH
- Visual Studio Code o IDE + extensiones Java

**Criterio de Done**: `psql --version` y `java -version` y `mvn -version` responden correctamente.

### Fase 1: Estructura y Conexión BD
**Tareas**:
1. Crear carpeta GestProy/ en trBD
2. Copiar estructura de carpetas (db/, src/)
3. Crear BD PostgreSQL `gestion_proyectos` vacía
4. Crear `db.properties` con credenciales (gitignored)
5. Crear plantilla `db.properties.example` versionada
6. Spring Boot `pom.xml` básico + `application.properties`
7. Crear endpoint de prueba: GET `/` → "OK, conexión activa"

**Criterio de Done**: `mvn spring-boot:run` arranca sin errores; GET `http://localhost:8080/` retorna HTML "OK".

### Fase 2: Schema SQL
**Tareas**:
1. Escribir `db/schema/01_referenciales.sql` (9 tablas Grupo A+B)
2. Escribir `db/schema/02_maestras.sql` (g1m_clientes, g1m_personal)
3. Escribir `db/schema/03_relacion.sql` (g1c_per_car)
4. Escribir `db/schema/04_transaccionales.sql` (g1t_pro_cab, g1t_pro_eqp, g1t_pro_mov)
5. Escribir `db/schema/05_indices.sql` (índices FK explícitos)
6. Crear `db/scripts/apply-all.ps1` que ejecuta todos los .sql
7. Ejecutar apply-all.ps1 contra BD nueva
8. Verificar con `\dt` (lista tablas) y `\d+ g1t_pro_cab` (describe tabla)

**Criterio de Done**: 15 tablas creadas; `\dt` lista todas; `\d+` muestra FKs correctamente.

### Fase 3: Funciones Referenciales + Seeds
**Tareas**:
1. Escribir `db/functions/10_sp_ref_grupoa_mant.sql` (una función para las 6 tablas Grupo A)
2. Escribir `db/functions/11_sp_ref_grupob_mant.sql` (Grupo B: tip_pro, lin_pro, etp_pro)
3. Escribir `db/seed/01_seed_referenciales.sql` (INSERT gzz_est_reg, gzz_tip_pro, gzz_car_per, etc.)
4. Probar en psql:
   ```sql
   SELECT sp_ref_grupoa_mant('gzz_tip_cli', 'ADICIONAR', 'EN', 'Empresa Nacional');
   SELECT * FROM gzz_tip_cli;
   ```

**Criterio de Done**: Función sin sintaxis SQL; seed inserta datos sin errores; SELECT retorna datos.

### Fase 4: Andamiaje Spring Boot
**Tareas**:
1. Crear clase `Referencial` (modelo base)
2. Crear `ReferencialDAO` con JdbcTemplate
3. Crear `ReferencialService`
4. Crear `ReferencialController` (GET /referenciales/{tabla}, POST guardar)
5. Crear `ReferencialRepository` → `RowMapper` genérico
6. Crear vista `referenciales/list.html` (tabla HTML)
7. Crear vista `referenciales/form.html` (formulario)
8. Endpoint GET `/referenciales/tip_pro` debe listar tipos proyecto

**Criterio de Done**: GET `/referenciales/tip_pro` muestra tabla con datos de BD; GET `/referenciales/tip_pro/new` muestra formulario vacío.

### Fase 5: Módulo Referenciales Completo (5 Operaciones)
**Tareas**:
1. Implementar ADICIONAR (POST `/referenciales/tip_pro` → INSERT)
2. Implementar MODIFICAR (GET formula llenado, POST update)
3. Implementar ELIMINAR (soft delete est_reg='*')
4. Implementar INACTIVAR (est_reg='I')
5. Implementar REACTIVAR (est_reg='A')
6. GlobalExceptionHandler captura excepciones PL/pgSQL
7. Flash messages (éxito/error) en Thymeleaf

**Criterio de Done**: Ciclo completo CRUD en navegador para una tabla referencial (ej: TipoPro); validaciones cliente y BD; mensajes de error legibles.

### Fase 6: Maestras (Clientes, Personal, Cargos x Persona)
**Tareas**:
1. Escribir `db/functions/20_sp_cliente_mant.sql`
2. Escribir `db/functions/21_sp_personal_mant.sql`
3. Escribir `db/functions/22_sp_per_car_mant.sql` (con lógica reactivación en duplicado)
4. Crear modelos Java: `Cliente`, `Persona`, `CargoPersona`
5. Crear DAOs y Services para cada maestro
6. Controllers `/clientes`, `/personal`, `/personal/{cod}/cargos`
7. Vistas: `clientes/{list, form}`, `personal/{list, form, cargos-list, cargos-form}`
8. Prueba: Crear cliente, crear persona, asignar cargos a persona

**Criterio de Done**: CRUD completo clientes, personal y cargos×persona; validaciones (FK activas, costo/hora > 0); asignación de cargos sin duplicados.

### Fase 7: Proyectos + Equipo + Triggers
**Tareas**:
1. Escribir triggers `db/triggers/01-04_*.sql`
2. Escribir `db/functions/30_sp_proyecto_crear.sql` (cálculo pro_sec)
3. Escribir `db/functions/31_sp_proyecto_editar.sql`
4. Escribir `db/functions/32_sp_proyecto_cambiar_estado.sql`
5. Escribir `db/functions/40_sp_proyecto_equipo_asignar.sql` (con reactivación)
6. Escribir `db/functions/41_sp_proyecto_equipo_quitar.sql` + `reactivar`
7. Escribir `db/functions/42_fn_personal_disponible_proyecto.sql` (RETURNS TABLE)
8. Crear modelos: `Proyecto`, `ProyectoEquipo`
9. Crear DAOs, Services, Controllers, Vistas
10. Implementar formulario equipo con <select> dinámico de `fn_personal_disponible_proyecto`
11. Casos de error:
    - Asignar persona sin cargo autorizado → error esperado (trigger + FK)
    - Asignar persona ya activa → error
    - Quitar y reactivar → funciona

**Criterio de Done**: Crear proyecto → asignar equipo → ver error si persona no tiene cargo → asignar persona correcta → duplicar asignación (error) → quitar → reactivar. Todas las operaciones visibles en navegador.

### Fase 8: Avance de Proyecto
**Tareas**:
1. Escribir `db/functions/50_sp_proyecto_avance_registrar.sql`
2. Escribir `db/functions/51_fn_proyecto_pct_avance.sql`
3. Escribir `db/views/v_proyecto_avance.sql`
4. Crear modelo `ProyectoMovimiento`, `ProyectoAvance`
5. Crear DAO, Service, Controller para avance
6. Vistas: `proyectos/avance-list.html`, `proyectos/avance-form.html`
7. Implementar barra de progreso (CSS Bootstrap progress-bar)
8. Mostrar % avance en v_proyecto_resumen (listado)
9. Probar: Registrar horas en varias etapas → verificar % en vista → barra roja si <100%, verde si >=100%

**Criterio de Done**: Registrar horas en proyecto → % avance actualizado en tiempo real; barra visual; casos >100% (en rojo).

### Fase 9: Seed Transaccional + Prueba End-to-End
**Tareas**:
1. Escribir `db/seed/03_seed_transaccional.sql` con datos de prueba completos:
   - 3-4 proyectos de muestra
   - Equipo asignado
   - Algunos movimientos registrados
2. Ejecutar apply-all.ps1 con todos los seeds
3. Verificar datos en pgAdmin o psql
4. Prueba en navegador de flujo completo:
   - Listar proyectos → ver % avance
   - Editar cliente
   - Cambiar estado proyecto (Planificación→Ejecución→Cierre)
   - Agregar miembro equipo
   - Registrar horas
   - Inactivar referencial → verificar que no se puede usar en nuevos registros
5. Captura de pantalla de vistas principales

**Criterio de Done**: Navegador muestra datos de prueba; todas las operaciones responden correctamente; sin errores SQL o Java en logs.

### Fase 10: Revisión y Extensiones Futuras
**Tareas**:
1. Documentar en README.md: cómo compilar, ejecutar, conexión BD
2. Revisar código de estilo Java (nombres, estructura)
3. Verificar que db.properties.example está versionado (no db.properties real)
4. Anotar en PLAN.md extensiones futuras (NO ALCANCE v1):
   - Dashboard con gráficos utilidad financiera
   - Login y autenticación de usuarios
   - Reportes PDF
   - API REST (además de Thymeleaf web)
   - Notificaciones por email

**Criterio de Done**: README completo; db.properties en .gitignore; código limpio; comentarios en funciones SQL complejas; documento de extensiones futuras.

---

## 8. Verificación y Criterios de Aceptación

### 8.1 Verificación de Base de Datos

- **Ejecución de apply-all.ps1**:
  ```powershell
  # Debe ejecutarse sin errores contra BD nueva
  # apply-all.ps1 crea gestion_proyectos, ejecuta schema, triggers, functions, views, seeds
  ```
  - `psql -h localhost -U postgres -d gestion_proyectos -c "\dt"` → 15 tablas
  - `psql -h localhost -U postgres -d gestion_proyectos -c "\df sp_*"` → funciones listadas

- **Ejecución de Seeds**:
  ```sql
  SELECT COUNT(*) FROM gzz_est_reg;  -- 3 (A, I, *)
  SELECT COUNT(*) FROM g1m_clientes;  -- N > 0
  SELECT COUNT(*) FROM g1m_personal;  -- N > 0
  ```

### 8.2 Verificación de Funciones SQL (en psql)

**Referenciales**:
```sql
SELECT sp_ref_grupoa_mant('gzz_tip_cli', 'ADICIONAR', 'EU', 'Empresa Europea');
SELECT * FROM gzz_tip_cli WHERE tip_cli_cod = 'EU';
```

**Clientes**:
```sql
SELECT sp_cliente_mant('ADICIONAR', 1001, 'Acme Corp', 'EN', 'A', ...);
SELECT * FROM g1m_clientes WHERE cli_cod = 1001;
```

**Proyectos**:
```sql
SELECT * FROM sp_proyecto_crear(1001, 1, CURRENT_DATE, ...);
-- Retorna pro_sec calculado automáticamente
```

### 8.3 Verificación de Aplicación Spring Boot

- **Arranque limpio**:
  ```bash
  mvn clean spring-boot:run
  ```
  Debe mostrar:
  ```
  ...INFO : Tomcat started on port 8080
  ...INFO : Started GestProyApplication in X.XXX seconds
  ```

- **Acceso a endpoints**:
  - GET `http://localhost:8080/` → Home HTML
  - GET `http://localhost:8080/referenciales/tip_pro` → Tabla de tipos proyecto
  - GET `http://localhost:8080/clientes` → Tabla de clientes

### 8.4 Pruebas Funcionales en Navegador

#### Ciclo Referencial (Tipo Proyecto)
1. GET `/referenciales/tip_pro` → listado
2. Click "Adicionar" → GET `/referenciales/tip_pro/new` → form vacío
3. Ingresa código "15", descripción "Consultoría", tamaño "G" → POST `/referenciales/tip_pro`
4. Verificar "Consultoría" en listado
5. Click "Editar" → GET `/referenciales/tip_pro/15` → form llenado
6. Cambiar descripción a "Consultoría Premium" → POST actualizar
7. Verificar cambio en listado
8. Click "Inactivar" → POST → estado "I" en listado
9. Click "Reactivar" → POST → estado "A"
10. Click "Eliminar" → POST → estado "*" en listado

**Validaciones esperadas**:
- Campo código obligatorio (GET nueva propuesta con código vacío → POST rechaza)
- Código duplicado (intentar agregar "15" dos veces) → error "Ya existe"
- Campos se limpian después de cancel

#### Ciclo Cliente
1. GET `/clientes` → listado
2. Adicionar cliente (código 2001, nombre "ABC Ltda", tipo "EN", estado "A")
3. Modificar nombre a "ABC Ltda."
4. Listar → verificar cambio
5. Intentar eliminar → verificar soft delete

#### Ciclo Personal + Cargos
1. GET `/personal` → listado
2. Adicionar persona (código 5001, nombre "Juan García", cargo "Ingeniero", costo 50.00)
3. GET `/personal/5001/cargos` → sin cargos aún
4. Click "Agregar Cargo" → GET `/personal/5001/cargos/new` → <select> solo cargos activos
5. Seleccionar "Desarrollador" → POST → persona puede ejercer cargo
6. Intentar agregar "Desarrollador" de nuevo → error (ya existe)
7. Inactivar cargo → GET `/personal/5001/cargos` → estado "I"
8. Reactivar → estado "A"

#### Ciclo Proyecto + Equipo + Avance
1. GET `/proyectos` → listado (si hay seeds)
2. Adicionar proyecto (cliente "ABC Ltda", tipo "Desarrollo", fecha inicio, fecha entrega)
3. pro_sec debe calcularse automático = 1
4. GET `/proyectos/2001/1/1/equipo` → sin equipo aún
5. Click "Asignar" → GET form
   - <select> personas = personalDisponible(proyecto)
   - Juan García aparece (tiene cargos autorizados, no asignado aún)
6. Seleccionar Juan + cargo "Desarrollador" → POST asignar
7. Verificar en equipo
8. Intentar asignar Juan/"Desarrollador" otra vez → error (ya asignado)
9. GET `/proyectos/2001/1/1/avance` → sin movimientos
10. Click "Registrar Horas" → GET form
    - <select> etapas = etapas activas
    - <select> equipos = equipo del proyecto (solo Juan)
11. Seleccionar Etapa "Análisis", Juan, 8 horas, 30 minutos → POST registrar
12. Verificar en listado movimientos
13. Ver barra de progreso % avance (ej: si Análisis = 16 horas estimadas, 8.5 trabajadas → 53%)

**Validaciones esperadas**:
- Asignar persona sin cargo autorizado en proyecto → error (trigger + FK)
- Registrar horas de persona retirada (est_reg='I') → error (trigger)
- Horas inválidas (>23, <0, minutos >59) → error cliente + servidor
- Fechas incoherentes en proyecto → error (trigger)

### 8.5 Verificación de Control de Versiones

- **db.properties**: En .gitignore, NO aparece en `git status` después de crear
- **db.properties.example**: Versionado (sin credenciales reales)
- **Commits**: Uno por fase, mensaje descriptivo
- Ejemplo:
  ```
  - Fase 2: Schema SQL (15 tablas + índices)
  - Fase 3: Funciones referenciales + triggers + seeds
  - Fase 4-5: Módulo referenciales genérico
  - etc.
  ```

### 8.6 Checklist Final

- [ ] BD PostgreSQL `gestion_proyectos` crea + popula correctamente con apply-all.ps1
- [ ] 15 tablas existen (\dt muestra todas)
- [ ] Funciones compiladas sin errores (SELECT sp_* retorna resultados)
- [ ] Triggers activos (INSERT en g1t_pro_eqp valida g1c_per_car estado)
- [ ] Views creadas (v_proyecto_resumen, v_proyecto_equipo, v_proyecto_avance)
- [ ] Spring Boot arranca sin errores
- [ ] JdbcTemplate conecta y ejecuta consultas
- [ ] GET `/` funciona → home HTML
- [ ] CRUD referenciales completo (5 operaciones × 9 tablas)
- [ ] CRUD clientes, personal, cargos×personal
- [ ] CRUD proyectos: crear, editar, cambiar estado
- [ ] Asignar/quitar/reactivar equipo en proyecto
- [ ] Registrar horas, calcular % avance, mostrar barra
- [ ] Manejo de errores: excepciones SQL → mensajes legibles en HTML
- [ ] Flash messages muestran éxito/error
- [ ] db.properties en .gitignore, ejemplo versionado
- [ ] README.md explica setup y ejecución
- [ ] PLAN.md actualizado con decisiones finales

---

## 9. Extensiones Futuras (NO Alcance v1)

1. **Dashboard Financiero**: Gráficos de utilidad presupuestada vs real por cliente/tipo proyecto
2. **Módulo de Login**: Autenticación con Spring Security + roles (admin, PM, dev, viewer)
3. **Reportes PDF**: Generar reportes de proyectos, equipo, avance con iText/JasperReports
4. **API REST**: Endpoints JSON para consumo desde mobile/terceros (además de Thymeleaf)
5. **Notificaciones Email**: Recordatorios de fecha entrega, cambios de estado
6. **Auditoría**: Tabla de logs (quién, qué, cuándo, dónde)
7. **Backlog de Tareas**: Subtareas dentro de etapas de proyecto
8. **Control de Cambios**: Historial de cambios (antes/después) en campos críticos
9. **Internacionalización i18n**: Soporte español/inglés en UI
10. **Mobile App**: Aplicación Android/iOS para registro de horas en terreno

---

## 10. Referencias Normativas

- **Especificación de Proyecto**: UNSA, Ingeniería de Sistemas, Curso BD
- **Patrón de Referencia**: TipPro.java (docente)
- **Base de Datos Original**: sqllabbd.sql (MySQL)
- **Datos de Prueba**: datos_prueba.sql
- **Framework**: Spring Boot 3.x LTS
- **ORM**: Prohibido (JPA, Hibernate)
- **Motor SQL**: PostgreSQL 16, no MySQL

---

## 11. Contactos y Gestión de Cambios

**Responsable**: Estudiante (Vasily)
**Fecha de Creación**: 2026-07-07
**Última Actualización**: 2026-07-07
**Estado**: Aprobado (diseño listo para implementación)

**Cambios Permitidos**: Ajustes de naming interno (Java, SQL) dentro de arquitectura aprobada.
**Cambios Prohibidos**: Cambios a stack (Java→C#, PostgreSQL→MySQL), eliminar capas, usar ORM.

---

**Fin del documento PLAN.md**
