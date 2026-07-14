-- ============================================================
--  GestProy — Vistas
--  v_proyecto_avance : avance de cada proyecto.
--    horas_estimadas  = suma de etp_tie_est de las etapas
--                       ACTIVAS del catálogo global gzz_etp_pro
--    horas_trabajadas = suma de horas+minutos de los
--                       movimientos activos del proyecto
--    pct_avance       = fn_proyecto_pct_avance(...)
--  Es la vista central del flujo de negocio "avance por etapas".
-- ============================================================
BEGIN;

CREATE OR REPLACE VIEW v_proyecto_avance AS
SELECT
  p.pro_cli_cod,
  p.pro_tip_cod,
  p.pro_sec,
  (SELECT COALESCE(SUM(e.etp_tie_est), 0)
   FROM gzz_etp_pro e
   WHERE e.etp_est_reg = 'A')::numeric(8,2) AS horas_estimadas,
  COALESCE(SUM(m.hor_tra_etp + m.min_tra_etp / 60.0), 0)::numeric(8,2) AS horas_trabajadas,
  fn_proyecto_pct_avance(p.pro_cli_cod, p.pro_tip_cod, p.pro_sec) AS pct_avance
FROM g1t_pro_cab p
LEFT JOIN g1t_pro_mov m
  ON m.pro_cli_cod = p.pro_cli_cod
 AND m.pro_tip_cod = p.pro_tip_cod
 AND m.pro_sec     = p.pro_sec
 AND m.est_reg_cod = 'A'
GROUP BY p.pro_cli_cod, p.pro_tip_cod, p.pro_sec;

COMMIT;
