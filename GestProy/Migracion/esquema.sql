-- =====================================================================
-- Migración MySQL -> PostgreSQL
-- Base de datos origen: proyectos
-- Esquema generado a partir de esquema.sql (mysqldump)
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- Tabla base (sin dependencias) - debe ir primera
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS "gzz_est_reg" CASCADE;
CREATE TABLE "gzz_est_reg" (
  "EstRegCod" char(1) NOT NULL,
  "EstRegDes" varchar(40) NOT NULL,
  "EstRegEstReg" char(1) NOT NULL,
  PRIMARY KEY ("EstRegCod")
);

-- ---------------------------------------------------------------------
-- Tablas catálogo que solo dependen de gzz_est_reg
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS "gzz_car_per" CASCADE;
CREATE TABLE "gzz_car_per" (
  "CarPerCod" smallint NOT NULL,
  "CarPerDes" varchar(40) NOT NULL,
  "CarPerEstReg" char(1) NOT NULL,
  PRIMARY KEY ("CarPerCod"),
  CONSTRAINT "fk_carper_estreg" FOREIGN KEY ("CarPerEstReg") REFERENCES "gzz_est_reg" ("EstRegCod")
);

DROP TABLE IF EXISTS "gzz_car_pro" CASCADE;
CREATE TABLE "gzz_car_pro" (
  "CarProCod" smallint NOT NULL,
  "CarProDes" varchar(40) NOT NULL,
  "CarProEstReg" char(1) NOT NULL,
  PRIMARY KEY ("CarProCod"),
  CONSTRAINT "fk_carpro_estreg" FOREIGN KEY ("CarProEstReg") REFERENCES "gzz_est_reg" ("EstRegCod")
);

DROP TABLE IF EXISTS "gzz_est_cli" CASCADE;
CREATE TABLE "gzz_est_cli" (
  "EstCliCod" char(1) NOT NULL,
  "EstCliDes" varchar(40) NOT NULL,
  "EstCliEstReg" char(1) NOT NULL,
  PRIMARY KEY ("EstCliCod"),
  CONSTRAINT "fk_estcli_estreg" FOREIGN KEY ("EstCliEstReg") REFERENCES "gzz_est_reg" ("EstRegCod")
);

DROP TABLE IF EXISTS "gzz_est_pro" CASCADE;
CREATE TABLE "gzz_est_pro" (
  "EstProCod" char(2) NOT NULL,
  "EstProDes" varchar(40) NOT NULL,
  "EstProEstReg" char(1) NOT NULL,
  PRIMARY KEY ("EstProCod"),
  CONSTRAINT "fk_estpro_estreg" FOREIGN KEY ("EstProEstReg") REFERENCES "gzz_est_reg" ("EstRegCod")
);

DROP TABLE IF EXISTS "gzz_etp_pro" CASCADE;
CREATE TABLE "gzz_etp_pro" (
  "EtpCod" smallint NOT NULL,
  "EtpDes" varchar(40) NOT NULL,
  "EtpTieEst" numeric(5,2) NOT NULL,
  "EtpEstReg" char(1) NOT NULL,
  PRIMARY KEY ("EtpCod"),
  CONSTRAINT "fk_etppro_estreg" FOREIGN KEY ("EtpEstReg") REFERENCES "gzz_est_reg" ("EstRegCod")
);

DROP TABLE IF EXISTS "gzz_lin_pro" CASCADE;
CREATE TABLE "gzz_lin_pro" (
  "LinProCod" smallint NOT NULL,
  "LinProNom" varchar(60) NOT NULL,
  "LinProTam" char(1) NOT NULL,
  "LinProEstRegCod" char(1) NOT NULL,
  PRIMARY KEY ("LinProCod"),
  CONSTRAINT "fk_linpro_estreg" FOREIGN KEY ("LinProEstRegCod") REFERENCES "gzz_est_reg" ("EstRegCod")
);

DROP TABLE IF EXISTS "gzz_tip_cli" CASCADE;
CREATE TABLE "gzz_tip_cli" (
  "TipCliCod" char(2) NOT NULL,
  "TipCliDes" varchar(40) NOT NULL,
  "TipCliEstReg" char(1) NOT NULL,
  PRIMARY KEY ("TipCliCod"),
  CONSTRAINT "fk_tipcli_estreg" FOREIGN KEY ("TipCliEstReg") REFERENCES "gzz_est_reg" ("EstRegCod")
);

DROP TABLE IF EXISTS "gzz_tip_pro" CASCADE;
CREATE TABLE "gzz_tip_pro" (
  "TipProCod" smallint NOT NULL,
  "TipProDes" varchar(40) NOT NULL,
  "TipProTam" char(1) NOT NULL,
  "TipProEstReg" char(1) NOT NULL,
  PRIMARY KEY ("TipProCod"),
  CONSTRAINT "fk_tippro_estreg" FOREIGN KEY ("TipProEstReg") REFERENCES "gzz_est_reg" ("EstRegCod")
);

-- ---------------------------------------------------------------------
-- Nivel 2: dependen de las tablas catálogo anteriores
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS "g1m_personal" CASCADE;
CREATE TABLE "g1m_personal" (
  "PerCod" integer NOT NULL,
  "PerNom" varchar(60) NOT NULL,
  "PerCarCod" smallint NOT NULL,
  "PerCosHor" numeric(10,2) NOT NULL,
  "PerFecIng" date NOT NULL,
  "PerEstReg" char(1) NOT NULL,
  PRIMARY KEY ("PerCod"),
  CONSTRAINT "fk_per_carper" FOREIGN KEY ("PerCarCod") REFERENCES "gzz_car_per" ("CarPerCod"),
  CONSTRAINT "fk_per_estreg" FOREIGN KEY ("PerEstReg") REFERENCES "gzz_est_reg" ("EstRegCod")
);

DROP TABLE IF EXISTS "g1m_clientes" CASCADE;
CREATE TABLE "g1m_clientes" (
  "CliCod" integer NOT NULL,
  "CliNom" varchar(60) NOT NULL,
  "CliTipCod" char(2) NOT NULL,
  "CliFecIng" date DEFAULT NULL,
  "CliFecCes" date DEFAULT NULL,
  "CliFecUltProCer" date DEFAULT NULL,
  "CliEstCod" char(1) NOT NULL,
  "CliEstRegCod" char(1) NOT NULL,
  PRIMARY KEY ("CliCod"),
  CONSTRAINT "fk_cli_estcli" FOREIGN KEY ("CliEstCod") REFERENCES "gzz_est_cli" ("EstCliCod"),
  CONSTRAINT "fk_cli_estreg" FOREIGN KEY ("CliEstRegCod") REFERENCES "gzz_est_reg" ("EstRegCod"),
  CONSTRAINT "fk_cli_tipcli" FOREIGN KEY ("CliTipCod") REFERENCES "gzz_tip_cli" ("TipCliCod")
);

-- ---------------------------------------------------------------------
-- Nivel 3
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS "g1c_per_car" CASCADE;
CREATE TABLE "g1c_per_car" (
  "PerCod" integer NOT NULL,
  "CarProCod" smallint NOT NULL,
  "PerCarProEstReg" char(1) NOT NULL,
  PRIMARY KEY ("PerCod", "CarProCod"),
  CONSTRAINT "fk_percar_carpro" FOREIGN KEY ("CarProCod") REFERENCES "gzz_car_pro" ("CarProCod"),
  CONSTRAINT "fk_percar_estreg" FOREIGN KEY ("PerCarProEstReg") REFERENCES "gzz_est_reg" ("EstRegCod"),
  CONSTRAINT "fk_percar_per" FOREIGN KEY ("PerCod") REFERENCES "g1m_personal" ("PerCod")
);

DROP TABLE IF EXISTS "g1t_pro_cab" CASCADE;
CREATE TABLE "g1t_pro_cab" (
  "ProCliCod" integer NOT NULL,
  "ProTipCod" smallint NOT NULL,
  "ProSec" smallint NOT NULL,
  "ProFecCon" date DEFAULT NULL,
  "ProFecPac" date DEFAULT NULL,
  "ProFecIni" date DEFAULT NULL,
  "ProFecEnt" date DEFAULT NULL,
  "ProFecCer" date DEFAULT NULL,
  "ProMonPre" numeric(10,2) DEFAULT NULL,
  "ProMonRea" numeric(10,2) DEFAULT NULL,
  "ProCosPre" numeric(10,2) DEFAULT NULL,
  "ProCosRea" numeric(10,2) DEFAULT NULL,
  "ProGasPre" numeric(10,2) DEFAULT NULL,
  "ProGasRea" numeric(10,2) DEFAULT NULL,
  "ProUtiPre" numeric(10,2) DEFAULT NULL,
  "ProUtiRea" numeric(10,2) DEFAULT NULL,
  "ProEstCod" char(2) NOT NULL,
  "ProEstRegCod" char(1) NOT NULL,
  PRIMARY KEY ("ProCliCod", "ProTipCod", "ProSec"),
  CONSTRAINT "fk_procab_cli" FOREIGN KEY ("ProCliCod") REFERENCES "g1m_clientes" ("CliCod"),
  CONSTRAINT "fk_procab_estpro" FOREIGN KEY ("ProEstCod") REFERENCES "gzz_est_pro" ("EstProCod"),
  CONSTRAINT "fk_procab_estreg" FOREIGN KEY ("ProEstRegCod") REFERENCES "gzz_est_reg" ("EstRegCod"),
  CONSTRAINT "fk_procab_tippro" FOREIGN KEY ("ProTipCod") REFERENCES "gzz_tip_pro" ("TipProCod")
);

-- ---------------------------------------------------------------------
-- Nivel 4
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS "g1t_pro_eqp" CASCADE;
CREATE TABLE "g1t_pro_eqp" (
  "ProCliCod" integer NOT NULL,
  "ProTipCod" smallint NOT NULL,
  "ProSec" smallint NOT NULL,
  "PerCod" integer NOT NULL,
  "CarProCod" smallint NOT NULL,
  "ProPerCarEstRegCod" char(1) NOT NULL,
  PRIMARY KEY ("ProCliCod", "ProTipCod", "ProSec", "PerCod", "CarProCod"),
  CONSTRAINT "fk_proeqp_cab" FOREIGN KEY ("ProCliCod", "ProTipCod", "ProSec") REFERENCES "g1t_pro_cab" ("ProCliCod", "ProTipCod", "ProSec"),
  CONSTRAINT "fk_proeqp_estreg" FOREIGN KEY ("ProPerCarEstRegCod") REFERENCES "gzz_est_reg" ("EstRegCod"),
  CONSTRAINT "fk_proeqp_percar" FOREIGN KEY ("PerCod", "CarProCod") REFERENCES "g1c_per_car" ("PerCod", "CarProCod")
);

-- ---------------------------------------------------------------------
-- Nivel 5
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS "g1t_pro_mov" CASCADE;
CREATE TABLE "g1t_pro_mov" (
  "ProCliCod" integer NOT NULL,
  "ProTipCod" smallint NOT NULL,
  "ProSec" smallint NOT NULL,
  "PerCod" integer NOT NULL,
  "CarProCod" smallint NOT NULL,
  "EtpCod" smallint NOT NULL,
  "SecEtp" smallint NOT NULL,
  "FecRegEtp" date DEFAULT NULL,
  "HorTraEtp" smallint DEFAULT NULL,
  "MinTraEtp" smallint DEFAULT NULL,
  "EstRegCod" char(1) NOT NULL,
  PRIMARY KEY ("ProCliCod", "ProTipCod", "ProSec", "PerCod", "CarProCod", "EtpCod", "SecEtp"),
  CONSTRAINT "fk_promov_eqp" FOREIGN KEY ("ProCliCod", "ProTipCod", "ProSec", "PerCod", "CarProCod") REFERENCES "g1t_pro_eqp" ("ProCliCod", "ProTipCod", "ProSec", "PerCod", "CarProCod"),
  CONSTRAINT "fk_promov_estreg" FOREIGN KEY ("EstRegCod") REFERENCES "gzz_est_reg" ("EstRegCod"),
  CONSTRAINT "fk_promov_etppro" FOREIGN KEY ("EtpCod") REFERENCES "gzz_etp_pro" ("EtpCod")
);

-- Índices auxiliares (equivalentes a las KEY no-PK del dump MySQL,
-- en Postgres las columnas FK no crean índice automático como en MySQL)
CREATE INDEX "idx_carper_estreg" ON "gzz_car_per" ("CarPerEstReg");
CREATE INDEX "idx_carpro_estreg" ON "gzz_car_pro" ("CarProEstReg");
CREATE INDEX "idx_estcli_estreg" ON "gzz_est_cli" ("EstCliEstReg");
CREATE INDEX "idx_estpro_estreg" ON "gzz_est_pro" ("EstProEstReg");
CREATE INDEX "idx_etppro_estreg" ON "gzz_etp_pro" ("EtpEstReg");
CREATE INDEX "idx_linpro_estreg" ON "gzz_lin_pro" ("LinProEstRegCod");
CREATE INDEX "idx_tipcli_estreg" ON "gzz_tip_cli" ("TipCliEstReg");
CREATE INDEX "idx_tippro_estreg" ON "gzz_tip_pro" ("TipProEstReg");
CREATE INDEX "idx_per_carper" ON "g1m_personal" ("PerCarCod");
CREATE INDEX "idx_per_estreg" ON "g1m_personal" ("PerEstReg");
CREATE INDEX "idx_cli_tipcli" ON "g1m_clientes" ("CliTipCod");
CREATE INDEX "idx_cli_estcli" ON "g1m_clientes" ("CliEstCod");
CREATE INDEX "idx_cli_estreg" ON "g1m_clientes" ("CliEstRegCod");
CREATE INDEX "idx_percar_carpro" ON "g1c_per_car" ("CarProCod");
CREATE INDEX "idx_percar_estreg" ON "g1c_per_car" ("PerCarProEstReg");
CREATE INDEX "idx_procab_tippro" ON "g1t_pro_cab" ("ProTipCod");
CREATE INDEX "idx_procab_estpro" ON "g1t_pro_cab" ("ProEstCod");
CREATE INDEX "idx_procab_estreg" ON "g1t_pro_cab" ("ProEstRegCod");
CREATE INDEX "idx_proeqp_percar" ON "g1t_pro_eqp" ("PerCod", "CarProCod");
CREATE INDEX "idx_proeqp_estreg" ON "g1t_pro_eqp" ("ProPerCarEstRegCod");
CREATE INDEX "idx_promov_etppro" ON "g1t_pro_mov" ("EtpCod");
CREATE INDEX "idx_promov_estreg" ON "g1t_pro_mov" ("EstRegCod");

COMMIT;
