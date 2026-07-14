-- ============================================================
--  GestProy — Funciones de lectura
--  fn_personal_disponible_proyecto : combinaciones
--  (persona, cargo) que pueden asignarse al equipo de un
--  proyecto. Alimenta el <select> del formulario "Asignar".
--
--  Criterio: autorización activa en g1c_per_car, persona activa,
--  y que NO esté ya asignada ACTIVA a ese proyecto con ese cargo
--  (las asignaciones retiradas sí se ofrecen: al asignarlas de
--  nuevo, sp_proyecto_equipo_asignar las reactiva).
-- ============================================================
BEGIN;

CREATE OR REPLACE FUNCTION fn_personal_disponible_proyecto(
  p_cli_cod INTEGER,
  p_tip_cod SMALLINT,
  p_sec     SMALLINT
)
RETURNS TABLE (
  per_cod     INTEGER,
  per_nom     VARCHAR,
  car_pro_cod SMALLINT,
  car_pro_des VARCHAR
)
LANGUAGE sql
STABLE
AS $$
  SELECT pc.per_cod,
         p.per_nom,
         pc.car_pro_cod,
         c.car_pro_des
  FROM g1c_per_car pc
  JOIN g1m_personal p ON p.per_cod = pc.per_cod
  JOIN gzz_car_pro  c ON c.car_pro_cod = pc.car_pro_cod
  WHERE pc.per_car_pro_est_reg_cod = 'A'
    AND p.per_est_reg_cod = 'A'
    AND NOT EXISTS (
      SELECT 1
      FROM g1t_pro_eqp e
      WHERE e.pro_cli_cod = p_cli_cod
        AND e.pro_tip_cod = p_tip_cod
        AND e.pro_sec     = p_sec
        AND e.per_cod     = pc.per_cod
        AND e.car_pro_cod = pc.car_pro_cod
        AND e.pro_per_car_est_reg_cod = 'A'
    )
  ORDER BY p.per_nom, c.car_pro_des;
$$;

COMMIT;
