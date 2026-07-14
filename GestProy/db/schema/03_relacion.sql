-- ============================================================
--  GestProy — Esquema PostgreSQL
--  03_relacion.sql : tabla de relación (G1C_*)
--  g1c_per_car define qué cargos de proyecto puede ejercer
--  cada persona (autorización persona ↔ cargo).
--  Requiere: 01_referenciales.sql, 02_maestras.sql
-- ============================================================
BEGIN;

DROP TABLE IF EXISTS g1c_per_car CASCADE;
CREATE TABLE g1c_per_car (
  per_cod                  INTEGER  NOT NULL,
  car_pro_cod              SMALLINT NOT NULL,
  per_car_pro_est_reg_cod  CHAR(1)  NOT NULL,
  PRIMARY KEY (per_cod, car_pro_cod),
  CONSTRAINT fk_percar_per FOREIGN KEY (per_cod)
    REFERENCES g1m_personal (per_cod),
  CONSTRAINT fk_percar_carpro FOREIGN KEY (car_pro_cod)
    REFERENCES gzz_car_pro (car_pro_cod),
  CONSTRAINT fk_percar_estreg FOREIGN KEY (per_car_pro_est_reg_cod)
    REFERENCES gzz_est_reg (est_reg_cod)
);

COMMIT;
