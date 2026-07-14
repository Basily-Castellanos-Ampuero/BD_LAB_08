-- ============================================================
--  GestProy — Esquema PostgreSQL
--  05_indices.sql : índices explícitos sobre columnas FK
--  PostgreSQL NO indexa automáticamente las foreign keys
--  (MySQL Workbench los generaba inline con INDEX ... VISIBLE).
--  Las columnas que son prefijo de la PK no necesitan índice extra.
-- ============================================================
BEGIN;

-- Referenciales: FK a gzz_est_reg
CREATE INDEX idx_tipcli_estreg  ON gzz_tip_cli (tip_cli_est_reg);
CREATE INDEX idx_estcli_estreg  ON gzz_est_cli (est_cli_est_reg);
CREATE INDEX idx_linpro_estreg  ON gzz_lin_pro (lin_pro_est_reg_cod);
CREATE INDEX idx_tippro_estreg  ON gzz_tip_pro (tip_pro_est_reg);
CREATE INDEX idx_estpro_estreg  ON gzz_est_pro (est_pro_est_reg);
CREATE INDEX idx_carper_estreg  ON gzz_car_per (car_per_est_reg);
CREATE INDEX idx_carpro_estreg  ON gzz_car_pro (car_pro_est_reg);
CREATE INDEX idx_etppro_estreg  ON gzz_etp_pro (etp_est_reg);

-- Maestras
CREATE INDEX idx_cli_tipcli     ON g1m_clientes (cli_tip_cod);
CREATE INDEX idx_cli_estcli     ON g1m_clientes (cli_est_cod);
CREATE INDEX idx_cli_estreg     ON g1m_clientes (cli_est_reg_cod);
CREATE INDEX idx_per_carper     ON g1m_personal (per_car_cod);
CREATE INDEX idx_per_estreg     ON g1m_personal (per_est_reg_cod);

-- Relación
CREATE INDEX idx_percar_carpro  ON g1c_per_car (car_pro_cod);
CREATE INDEX idx_percar_estreg  ON g1c_per_car (per_car_pro_est_reg_cod);

-- Transaccionales
CREATE INDEX idx_procab_tippro  ON g1t_pro_cab (pro_tip_cod);
CREATE INDEX idx_procab_estpro  ON g1t_pro_cab (pro_est_cod);
CREATE INDEX idx_procab_estreg  ON g1t_pro_cab (pro_est_reg_cod);
CREATE INDEX idx_proeqp_percar  ON g1t_pro_eqp (per_cod, car_pro_cod);
CREATE INDEX idx_proeqp_estreg  ON g1t_pro_eqp (pro_per_car_est_reg_cod);
CREATE INDEX idx_promov_etppro  ON g1t_pro_mov (etp_cod);
CREATE INDEX idx_promov_estreg  ON g1t_pro_mov (est_reg_cod);

COMMIT;
