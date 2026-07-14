-- ============================================================
--  GestProy — Esquema PostgreSQL
--  02_maestras.sql : tablas maestras (G1M_*)
--  Requiere: 01_referenciales.sql
-- ============================================================
BEGIN;

DROP TABLE IF EXISTS g1m_clientes CASCADE;
CREATE TABLE g1m_clientes (
  cli_cod              INTEGER     NOT NULL,
  cli_nom              VARCHAR(60) NOT NULL,
  cli_tip_cod          CHAR(2)     NOT NULL,
  cli_fec_ing          DATE        NULL DEFAULT NULL,
  cli_fec_ces          DATE        NULL DEFAULT NULL,
  cli_fec_ult_pro_cer  DATE        NULL DEFAULT NULL,
  cli_est_cod          CHAR(1)     NOT NULL,
  cli_est_reg_cod      CHAR(1)     NOT NULL,
  PRIMARY KEY (cli_cod),
  CONSTRAINT fk_cli_tipcli FOREIGN KEY (cli_tip_cod)
    REFERENCES gzz_tip_cli (tip_cli_cod),
  CONSTRAINT fk_cli_estcli FOREIGN KEY (cli_est_cod)
    REFERENCES gzz_est_cli (est_cli_cod),
  CONSTRAINT fk_cli_estreg FOREIGN KEY (cli_est_reg_cod)
    REFERENCES gzz_est_reg (est_reg_cod)
);

DROP TABLE IF EXISTS g1m_personal CASCADE;
CREATE TABLE g1m_personal (
  per_cod          INTEGER       NOT NULL,
  per_nom          VARCHAR(60)   NOT NULL,
  per_car_cod      SMALLINT      NOT NULL,
  -- costo por hora del recurso (debe ser > 0, validado en sp_personal_mant)
  per_cos_hor      NUMERIC(10,2) NOT NULL,
  per_fec_ing      DATE          NOT NULL,
  per_est_reg_cod  CHAR(1)       NOT NULL,
  PRIMARY KEY (per_cod),
  CONSTRAINT fk_per_carper FOREIGN KEY (per_car_cod)
    REFERENCES gzz_car_per (car_per_cod),
  CONSTRAINT fk_per_estreg FOREIGN KEY (per_est_reg_cod)
    REFERENCES gzz_est_reg (est_reg_cod)
);

COMMIT;
