-- ============================================================
--  GestProy — Vistas
--  v_proyecto_equipo : equipo asignado a cada proyecto con
--  nombre de la persona y descripción del cargo.
-- ============================================================
BEGIN;

CREATE OR REPLACE VIEW v_proyecto_equipo AS
SELECT
  e.pro_cli_cod,
  e.pro_tip_cod,
  e.pro_sec,
  e.per_cod,
  p.per_nom,
  e.car_pro_cod,
  c.car_pro_des,
  p.per_cos_hor,
  e.pro_per_car_est_reg_cod,
  CASE e.pro_per_car_est_reg_cod
    WHEN 'A' THEN 'Activo'
    WHEN 'I' THEN 'Retirado'
    ELSE 'Eliminado'
  END AS estado_descripcion
FROM g1t_pro_eqp e
JOIN g1m_personal p ON p.per_cod = e.per_cod
JOIN gzz_car_pro  c ON c.car_pro_cod = e.car_pro_cod;

COMMIT;
