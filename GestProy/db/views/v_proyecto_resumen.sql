-- ============================================================
--  GestProy — Vistas
--  v_proyecto_resumen : listado de proyectos con nombres
--  legibles (cliente, tipo, estado) en lugar de códigos.
--  No filtra por est_reg: la UI necesita ver también los
--  inactivos/eliminados para poder reactivarlos.
-- ============================================================
BEGIN;

CREATE OR REPLACE VIEW v_proyecto_resumen AS
SELECT
  p.pro_cli_cod,
  p.pro_tip_cod,
  p.pro_sec,
  c.cli_nom       AS cliente_nombre,
  t.tip_pro_des   AS tipo_descripcion,
  p.pro_est_cod,
  e.est_pro_des   AS estado_descripcion,
  p.pro_fec_con,
  p.pro_fec_pac,
  p.pro_fec_ini,
  p.pro_fec_ent,
  p.pro_fec_cer,
  p.pro_mon_pre,
  p.pro_mon_rea,
  p.pro_cos_pre,
  p.pro_cos_rea,
  p.pro_gas_pre,
  p.pro_gas_rea,
  p.pro_uti_pre,
  p.pro_uti_rea,
  p.pro_est_reg_cod
FROM g1t_pro_cab p
JOIN g1m_clientes c ON c.cli_cod = p.pro_cli_cod
JOIN gzz_tip_pro  t ON t.tip_pro_cod = p.pro_tip_cod
JOIN gzz_est_pro  e ON e.est_pro_cod = p.pro_est_cod;

COMMIT;
