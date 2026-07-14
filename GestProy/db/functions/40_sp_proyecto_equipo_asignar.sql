-- ============================================================
--  GestProy — Funciones de negocio
--  sp_proyecto_equipo_asignar : asigna una persona (con un
--  cargo de proyecto) al equipo de un proyecto.
--
--  Validaciones (además del trigger trg_proeqp_valida_percar_activo,
--  que actúa como red de seguridad a nivel de tabla, aquí se
--  valida primero para dar mensajes de error más específicos):
--    - proyecto existente, activo y no cerrado
--    - autorización (persona, cargo) activa en g1c_per_car
--    - si la fila de equipo ya existe inactiva -> se REACTIVA
--    - si ya existe activa -> error "ya asignado"
-- ============================================================
BEGIN;

CREATE OR REPLACE FUNCTION sp_proyecto_equipo_asignar(
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
  v_est_pro    CHAR(2);
  v_est_reg    CHAR(1);
  v_est_equipo CHAR(1);
BEGIN
  SELECT pro_est_cod, pro_est_reg_cod INTO v_est_pro, v_est_reg
  FROM g1t_pro_cab
  WHERE pro_cli_cod = p_cli_cod AND pro_tip_cod = p_tip_cod AND pro_sec = p_sec;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'No existe el proyecto (%, %, %)', p_cli_cod, p_tip_cod, p_sec;
  END IF;
  IF v_est_reg <> 'A' THEN
    RAISE EXCEPTION 'El proyecto (%, %, %) no está activo', p_cli_cod, p_tip_cod, p_sec;
  END IF;
  IF v_est_pro = '04' THEN
    RAISE EXCEPTION 'El proyecto (%, %, %) está cerrado: no se puede modificar su equipo',
      p_cli_cod, p_tip_cod, p_sec;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM g1c_per_car
                 WHERE per_cod = p_per_cod
                   AND car_pro_cod = p_car_pro_cod
                   AND per_car_pro_est_reg_cod = 'A') THEN
    RAISE EXCEPTION 'La persona % no tiene autorizado (activo) el cargo de proyecto %',
      p_per_cod, p_car_pro_cod;
  END IF;

  SELECT pro_per_car_est_reg_cod INTO v_est_equipo
  FROM g1t_pro_eqp
  WHERE pro_cli_cod = p_cli_cod AND pro_tip_cod = p_tip_cod AND pro_sec = p_sec
    AND per_cod = p_per_cod AND car_pro_cod = p_car_pro_cod;

  IF v_est_equipo IS NULL THEN
    INSERT INTO g1t_pro_eqp
      (pro_cli_cod, pro_tip_cod, pro_sec, per_cod, car_pro_cod, pro_per_car_est_reg_cod)
    VALUES
      (p_cli_cod, p_tip_cod, p_sec, p_per_cod, p_car_pro_cod, 'A');
  ELSIF v_est_equipo = 'A' THEN
    RAISE EXCEPTION 'La persona % ya está asignada a este proyecto con el cargo %',
      p_per_cod, p_car_pro_cod;
  ELSE
    -- existía retirada/eliminada: se reactiva en vez de fallar por PK duplicada
    UPDATE g1t_pro_eqp SET pro_per_car_est_reg_cod = 'A'
    WHERE pro_cli_cod = p_cli_cod AND pro_tip_cod = p_tip_cod AND pro_sec = p_sec
      AND per_cod = p_per_cod AND car_pro_cod = p_car_pro_cod;
  END IF;
END;
$$;

COMMIT;
