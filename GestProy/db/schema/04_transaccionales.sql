-- ============================================================
--  GestProy — Esquema PostgreSQL
--  04_transaccionales.sql : tablas transaccionales (G1T_*)
--  Jerarquía: g1t_pro_cab (cabecera de proyecto)
--             → g1t_pro_eqp (equipo asignado)
--             → g1t_pro_mov (movimientos de avance por etapa)
--  Requiere: 01, 02, 03
-- ============================================================
BEGIN;

DROP TABLE IF EXISTS g1t_pro_cab CASCADE;
CREATE TABLE g1t_pro_cab (
  pro_cli_cod      INTEGER       NOT NULL,
  pro_tip_cod      SMALLINT      NOT NULL,
  -- secuencia de proyecto por (cliente, tipo); la calcula sp_proyecto_crear
  pro_sec          SMALLINT      NOT NULL,
  pro_fec_con      DATE          NULL DEFAULT NULL,
  pro_fec_pac      DATE          NULL DEFAULT NULL,
  pro_fec_ini      DATE          NULL DEFAULT NULL,
  pro_fec_ent      DATE          NULL DEFAULT NULL,
  pro_fec_cer      DATE          NULL DEFAULT NULL,
  pro_mon_pre      NUMERIC(10,2) NULL DEFAULT NULL,
  pro_mon_rea      NUMERIC(10,2) NULL DEFAULT NULL,
  pro_cos_pre      NUMERIC(10,2) NULL DEFAULT NULL,
  pro_cos_rea      NUMERIC(10,2) NULL DEFAULT NULL,
  pro_gas_pre      NUMERIC(10,2) NULL DEFAULT NULL,
  pro_gas_rea      NUMERIC(10,2) NULL DEFAULT NULL,
  pro_uti_pre      NUMERIC(10,2) NULL DEFAULT NULL,
  pro_uti_rea      NUMERIC(10,2) NULL DEFAULT NULL,
  pro_est_cod      CHAR(2)       NOT NULL,
  pro_est_reg_cod  CHAR(1)       NOT NULL,
  PRIMARY KEY (pro_cli_cod, pro_tip_cod, pro_sec),
  CONSTRAINT fk_procab_cli FOREIGN KEY (pro_cli_cod)
    REFERENCES g1m_clientes (cli_cod),
  CONSTRAINT fk_procab_tippro FOREIGN KEY (pro_tip_cod)
    REFERENCES gzz_tip_pro (tip_pro_cod),
  CONSTRAINT fk_procab_estpro FOREIGN KEY (pro_est_cod)
    REFERENCES gzz_est_pro (est_pro_cod),
  CONSTRAINT fk_procab_estreg FOREIGN KEY (pro_est_reg_cod)
    REFERENCES gzz_est_reg (est_reg_cod)
);

DROP TABLE IF EXISTS g1t_pro_eqp CASCADE;
CREATE TABLE g1t_pro_eqp (
  pro_cli_cod              INTEGER  NOT NULL,
  pro_tip_cod              SMALLINT NOT NULL,
  pro_sec                  SMALLINT NOT NULL,
  per_cod                  INTEGER  NOT NULL,
  car_pro_cod              SMALLINT NOT NULL,
  pro_per_car_est_reg_cod  CHAR(1)  NOT NULL,
  PRIMARY KEY (pro_cli_cod, pro_tip_cod, pro_sec, per_cod, car_pro_cod),
  CONSTRAINT fk_proeqp_cab FOREIGN KEY (pro_cli_cod, pro_tip_cod, pro_sec)
    REFERENCES g1t_pro_cab (pro_cli_cod, pro_tip_cod, pro_sec),
  -- la FK garantiza que la combinación persona+cargo exista en g1c_per_car;
  -- que esté ACTIVA lo valida el trigger trg_proeqp_valida_percar_activo
  CONSTRAINT fk_proeqp_percar FOREIGN KEY (per_cod, car_pro_cod)
    REFERENCES g1c_per_car (per_cod, car_pro_cod),
  CONSTRAINT fk_proeqp_estreg FOREIGN KEY (pro_per_car_est_reg_cod)
    REFERENCES gzz_est_reg (est_reg_cod)
);

DROP TABLE IF EXISTS g1t_pro_mov CASCADE;
CREATE TABLE g1t_pro_mov (
  pro_cli_cod  INTEGER  NOT NULL,
  pro_tip_cod  SMALLINT NOT NULL,
  pro_sec      SMALLINT NOT NULL,
  per_cod      INTEGER  NOT NULL,
  car_pro_cod  SMALLINT NOT NULL,
  etp_cod      SMALLINT NOT NULL,
  -- secuencia del movimiento dentro de la etapa; la calcula el trigger
  -- trg_promov_autonumera_sec_etp
  sec_etp      SMALLINT NOT NULL,
  fec_reg_etp  DATE     NULL DEFAULT NULL,
  hor_tra_etp  SMALLINT NULL DEFAULT NULL,
  min_tra_etp  SMALLINT NULL DEFAULT NULL,
  est_reg_cod  CHAR(1)  NOT NULL,
  PRIMARY KEY (pro_cli_cod, pro_tip_cod, pro_sec, per_cod, car_pro_cod, etp_cod, sec_etp),
  CONSTRAINT fk_promov_eqp FOREIGN KEY (pro_cli_cod, pro_tip_cod, pro_sec, per_cod, car_pro_cod)
    REFERENCES g1t_pro_eqp (pro_cli_cod, pro_tip_cod, pro_sec, per_cod, car_pro_cod),
  CONSTRAINT fk_promov_etppro FOREIGN KEY (etp_cod)
    REFERENCES gzz_etp_pro (etp_cod),
  CONSTRAINT fk_promov_estreg FOREIGN KEY (est_reg_cod)
    REFERENCES gzz_est_reg (est_reg_cod)
);

COMMIT;
