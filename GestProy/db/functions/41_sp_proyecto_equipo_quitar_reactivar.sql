-- ============================================================
--  GestProy — Funciones de negocio
--  sp_proyecto_equipo_quitar    : baja lógica de un miembro
--                                 del equipo (est_reg 'I')
--  sp_proyecto_equipo_reactivar : reincorpora a un miembro
--                                 retirado (est_reg 'A')
--  El historial de movimientos del miembro se conserva.
-- ============================================================
BEGIN;

CREATE OR REPLACE FUNCTION sp_proyecto_equipo_quitar(
  p_cli_cod     INTEGER,
  p_tip_cod     SMALLINT,
  p_sec         SMALLINT,
  p_per_cod     INTEGER,
  p_car_pro_cod SMALLINT
)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
  v_est_equipo CHAR(1);
BEGIN
  SELECT pro_per_car_est_reg_cod INTO v_est_equipo
  FROM g1t_pro_eqp
  WHERE pro_cli_cod = p_cli_cod AND pro_tip_cod = p_tip_cod AND pro_sec = p_sec
    AND per_cod = p_per_cod AND car_pro_cod = p_car_pro_cod;

  IF v_est_equipo IS NULL THEN
    RAISE EXCEPTION 'La persona % (cargo %) no forma parte del equipo de este proyecto',
      p_per_cod, p_car_pro_cod;
  END IF;
  IF v_est_equipo <> 'A' THEN
    RAISE EXCEPTION 'La persona % (cargo %) ya está retirada del equipo',
      p_per_cod, p_car_pro_cod;
  END IF;

  UPDATE g1t_pro_eqp SET pro_per_car_est_reg_cod = 'I'
  WHERE pro_cli_cod = p_cli_cod AND pro_tip_cod = p_tip_cod AND pro_sec = p_sec
    AND per_cod = p_per_cod AND car_pro_cod = p_car_pro_cod;
END;
$$;

CREATE OR REPLACE FUNCTION sp_proyecto_equipo_reactivar(
  p_cli_cod     INTEGER,
  p_tip_cod     SMALLINT,
  p_sec         SMALLINT,
  p_per_cod     INTEGER,
  p_car_pro_cod SMALLINT
)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
  v_est_equipo CHAR(1);
BEGIN
  SELECT pro_per_car_est_reg_cod INTO v_est_equipo
  FROM g1t_pro_eqp
  WHERE pro_cli_cod = p_cli_cod AND pro_tip_cod = p_tip_cod AND pro_sec = p_sec
    AND per_cod = p_per_cod AND car_pro_cod = p_car_pro_cod;

  IF v_est_equipo IS NULL THEN
    RAISE EXCEPTION 'La persona % (cargo %) no forma parte del equipo de este proyecto',
      p_per_cod, p_car_pro_cod;
  END IF;
  IF v_est_equipo = 'A' THEN
    RAISE EXCEPTION 'La persona % (cargo %) ya está activa en el equipo',
      p_per_cod, p_car_pro_cod;
  END IF;

  -- el trigger trg_proeqp_valida_percar_activo verifica que la
  -- autorización (persona, cargo) siga activa en g1c_per_car
  UPDATE g1t_pro_eqp SET pro_per_car_est_reg_cod = 'A'
  WHERE pro_cli_cod = p_cli_cod AND pro_tip_cod = p_tip_cod AND pro_sec = p_sec
    AND per_cod = p_per_cod AND car_pro_cod = p_car_pro_cod;
END;
$$;

COMMIT;
