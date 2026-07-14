-- ============================================================
--  GestProy — Esquema PostgreSQL
--  01_referenciales.sql : tablas de catálogo (GZZ_*)
--  Convertido de sqllabbd.sql (MySQL) a PostgreSQL (snake_case).
--  Orden: gzz_est_reg primero (base de la eliminación lógica).
-- ============================================================
BEGIN;

DROP TABLE IF EXISTS gzz_est_reg CASCADE;
CREATE TABLE gzz_est_reg (
  est_reg_cod      CHAR(1)     NOT NULL,
  est_reg_des      VARCHAR(40) NOT NULL,
  -- sin FK a sí misma: bootstrap del propio catálogo de estados (A/I/*)
  est_reg_est_reg  CHAR(1)     NOT NULL,
  PRIMARY KEY (est_reg_cod)
);

DROP TABLE IF EXISTS gzz_tip_cli CASCADE;
CREATE TABLE gzz_tip_cli (
  tip_cli_cod      CHAR(2)     NOT NULL,
  tip_cli_des      VARCHAR(40) NOT NULL,
  tip_cli_est_reg  CHAR(1)     NOT NULL,
  PRIMARY KEY (tip_cli_cod),
  CONSTRAINT fk_tipcli_estreg FOREIGN KEY (tip_cli_est_reg)
    REFERENCES gzz_est_reg (est_reg_cod)
);

DROP TABLE IF EXISTS gzz_est_cli CASCADE;
CREATE TABLE gzz_est_cli (
  est_cli_cod      CHAR(1)     NOT NULL,
  est_cli_des      VARCHAR(40) NOT NULL,
  est_cli_est_reg  CHAR(1)     NOT NULL,
  PRIMARY KEY (est_cli_cod),
  CONSTRAINT fk_estcli_estreg FOREIGN KEY (est_cli_est_reg)
    REFERENCES gzz_est_reg (est_reg_cod)
);

DROP TABLE IF EXISTS gzz_lin_pro CASCADE;
CREATE TABLE gzz_lin_pro (
  lin_pro_cod          SMALLINT    NOT NULL,
  lin_pro_nom          VARCHAR(60) NOT NULL,
  lin_pro_tam          CHAR(1)     NOT NULL,
  lin_pro_est_reg_cod  CHAR(1)     NOT NULL,
  PRIMARY KEY (lin_pro_cod),
  CONSTRAINT fk_linpro_estreg FOREIGN KEY (lin_pro_est_reg_cod)
    REFERENCES gzz_est_reg (est_reg_cod)
);

DROP TABLE IF EXISTS gzz_tip_pro CASCADE;
CREATE TABLE gzz_tip_pro (
  tip_pro_cod      SMALLINT    NOT NULL,
  tip_pro_des      VARCHAR(40) NOT NULL,
  tip_pro_tam      CHAR(1)     NOT NULL,
  tip_pro_est_reg  CHAR(1)     NOT NULL,
  PRIMARY KEY (tip_pro_cod),
  CONSTRAINT fk_tippro_estreg FOREIGN KEY (tip_pro_est_reg)
    REFERENCES gzz_est_reg (est_reg_cod)
);

DROP TABLE IF EXISTS gzz_est_pro CASCADE;
CREATE TABLE gzz_est_pro (
  est_pro_cod      CHAR(2)     NOT NULL,
  est_pro_des      VARCHAR(40) NOT NULL,
  est_pro_est_reg  CHAR(1)     NOT NULL,
  PRIMARY KEY (est_pro_cod),
  CONSTRAINT fk_estpro_estreg FOREIGN KEY (est_pro_est_reg)
    REFERENCES gzz_est_reg (est_reg_cod)
);

DROP TABLE IF EXISTS gzz_car_per CASCADE;
CREATE TABLE gzz_car_per (
  car_per_cod      SMALLINT    NOT NULL,
  car_per_des      VARCHAR(40) NOT NULL,
  car_per_est_reg  CHAR(1)     NOT NULL,
  PRIMARY KEY (car_per_cod),
  CONSTRAINT fk_carper_estreg FOREIGN KEY (car_per_est_reg)
    REFERENCES gzz_est_reg (est_reg_cod)
);

DROP TABLE IF EXISTS gzz_car_pro CASCADE;
CREATE TABLE gzz_car_pro (
  car_pro_cod      SMALLINT    NOT NULL,
  car_pro_des      VARCHAR(40) NOT NULL,
  car_pro_est_reg  CHAR(1)     NOT NULL,
  PRIMARY KEY (car_pro_cod),
  CONSTRAINT fk_carpro_estreg FOREIGN KEY (car_pro_est_reg)
    REFERENCES gzz_est_reg (est_reg_cod)
);

DROP TABLE IF EXISTS gzz_etp_pro CASCADE;
CREATE TABLE gzz_etp_pro (
  etp_cod      SMALLINT     NOT NULL,
  etp_des      VARCHAR(40)  NOT NULL,
  -- horas estimadas de la etapa (base del cálculo de % de avance)
  etp_tie_est  NUMERIC(5,2) NOT NULL,
  etp_est_reg  CHAR(1)      NOT NULL,
  PRIMARY KEY (etp_cod),
  CONSTRAINT fk_etppro_estreg FOREIGN KEY (etp_est_reg)
    REFERENCES gzz_est_reg (est_reg_cod)
);

COMMIT;
