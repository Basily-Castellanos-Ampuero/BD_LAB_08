-- ============================================================
--  GestProy — Funciones de lectura
--  fn_proyecto_pct_avance : porcentaje de avance de un proyecto.
--
--  % = horas trabajadas (movimientos activos del proyecto)
--      / suma del tiempo estimado de las etapas ACTIVAS del
--        catálogo global gzz_etp_pro
--      * 100
--
--  Decisión de modelado (confirmada): gzz_etp_pro es el catálogo
--  único de etapas y aplica a todo proyecto; no existe una tabla
--  de plan de etapas por proyecto. Puede superar 100 si se
--  trabajó más de lo estimado. Sin etapas activas retorna 0.
-- ============================================================
BEGIN;

CREATE OR REPLACE FUNCTION fn_proyecto_pct_avance(
  p_cli_cod INTEGER,
  p_tip_cod SMALLINT,
  p_sec     SMALLINT
)
RETURNS NUMERIC
LANGUAGE sql
STABLE
AS $$
  SELECT ROUND(
    COALESCE(
      (SELECT SUM(m.hor_tra_etp + m.min_tra_etp / 60.0)
       FROM g1t_pro_mov m
       WHERE m.pro_cli_cod = p_cli_cod
         AND m.pro_tip_cod = p_tip_cod
         AND m.pro_sec     = p_sec
         AND m.est_reg_cod = 'A')
      / NULLIF((SELECT SUM(e.etp_tie_est)
                FROM gzz_etp_pro e
                WHERE e.etp_est_reg = 'A'), 0)
      * 100
    , 0)
  , 2);
$$;

COMMIT;
