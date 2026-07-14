-- ============================================================
--  GestProy — Funciones de negocio
--  sp_proyecto_avance_registrar : registra un movimiento de
--  horas/minutos trabajados por un miembro del equipo en una
--  etapa del proyecto (g1t_pro_mov).
--
--  sec_etp lo calcula el trigger trg_promov_autonumera_sec_etp;
--  que el miembro esté activo lo garantiza también el trigger
--  trg_promov_valida_eqp_activo (aquí se valida antes para dar
--  un mensaje más claro).
--  Retorna la secuencia (sec_etp) asignada al movimiento.
-- ============================================================
BEGIN;

CREATE OR REPLACE FUNCTION sp_proyecto_avance_registrar(
  p_cli_cod     INTEGER,
  p_tip_cod     SMALLINT,
  p_sec         SMALLINT,
  p_per_cod     INTEGER,
  p_car_pro_cod SMALLINT,
  p_etp_cod     SMALLINT,
  p_fec_reg     DATE,
  p_hor_tra     SMALLINT,
  p_min_tra     SMALLINT
)
RETURNS SMALLINT
LANGUAGE plpgsql
AS $$
DECLARE
  v_est_pro     CHAR(2);
  v_est_reg     CHAR(1);
  v_sec_etp     SMALLINT;
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
    RAISE EXCEPTION 'El proyecto (%, %, %) está cerrado: no se puede registrar avance',
      p_cli_cod, p_tip_cod, p_sec;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM gzz_etp_pro
                 WHERE etp_cod = p_etp_cod AND etp_est_reg = 'A') THEN
    RAISE EXCEPTION 'La etapa % no existe o no está activa', p_etp_cod;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM g1t_pro_eqp
                 WHERE pro_cli_cod = p_cli_cod AND pro_tip_cod = p_tip_cod
                   AND pro_sec = p_sec AND per_cod = p_per_cod
                   AND car_pro_cod = p_car_pro_cod
                   AND pro_per_car_est_reg_cod = 'A') THEN
    RAISE EXCEPTION 'La persona % (cargo %) no está activamente asignada a este proyecto',
      p_per_cod, p_car_pro_cod;
  END IF;

  IF p_fec_reg IS NULL OR p_fec_reg > CURRENT_DATE THEN
    RAISE EXCEPTION 'La fecha de registro es obligatoria y no puede ser futura';
  END IF;
  IF p_hor_tra IS NULL OR p_hor_tra < 0 OR p_hor_tra > 23 THEN
    RAISE EXCEPTION 'Las horas trabajadas deben estar entre 0 y 23';
  END IF;
  IF p_min_tra IS NULL OR p_min_tra < 0 OR p_min_tra > 59 THEN
    RAISE EXCEPTION 'Los minutos trabajados deben estar entre 0 y 59';
  END IF;
  IF p_hor_tra = 0 AND p_min_tra = 0 THEN
    RAISE EXCEPTION 'El tiempo trabajado no puede ser cero';
  END IF;

  INSERT INTO g1t_pro_mov
    (pro_cli_cod, pro_tip_cod, pro_sec, per_cod, car_pro_cod,
     etp_cod, sec_etp, fec_reg_etp, hor_tra_etp, min_tra_etp, est_reg_cod)
  VALUES
    (p_cli_cod, p_tip_cod, p_sec, p_per_cod, p_car_pro_cod,
     p_etp_cod, NULL, p_fec_reg, p_hor_tra, p_min_tra, 'A')
  RETURNING sec_etp INTO v_sec_etp;

  RETURN v_sec_etp;
END;
$$;

COMMIT;
