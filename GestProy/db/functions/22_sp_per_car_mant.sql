-- ============================================================
--  GestProy — Funciones de mantenimiento
--  sp_per_car_mant : mantenimiento de g1c_per_car
--  (qué cargos de proyecto puede ejercer cada persona)
--
--  Particularidad del ADICIONAR sobre PK compuesta: si la fila
--  (persona, cargo) ya existe pero está inactiva/eliminada, se
--  REACTIVA con UPDATE en vez de fallar por PK duplicada; solo
--  es error si ya existe activa.
-- ============================================================
BEGIN;

CREATE OR REPLACE FUNCTION sp_per_car_mant(
  p_operacion   TEXT,
  p_per_cod     INTEGER,
  p_car_pro_cod SMALLINT
)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
  v_est_destino CHAR(1);
  v_est_actual  CHAR(1);
BEGIN
  IF p_operacion NOT IN ('ADICIONAR','ELIMINAR','INACTIVAR','REACTIVAR') THEN
    RAISE EXCEPTION 'Operación no reconocida para cargos por persona: %', p_operacion;
  END IF;
  IF p_per_cod IS NULL OR p_car_pro_cod IS NULL THEN
    RAISE EXCEPTION 'La persona y el cargo de proyecto son obligatorios';
  END IF;

  SELECT per_car_pro_est_reg_cod INTO v_est_actual
  FROM g1c_per_car
  WHERE per_cod = p_per_cod AND car_pro_cod = p_car_pro_cod;

  IF p_operacion = 'ADICIONAR' THEN
    IF NOT EXISTS (SELECT 1 FROM g1m_personal
                   WHERE per_cod = p_per_cod AND per_est_reg_cod = 'A') THEN
      RAISE EXCEPTION 'El personal % no existe o no está activo', p_per_cod;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM gzz_car_pro
                   WHERE car_pro_cod = p_car_pro_cod AND car_pro_est_reg = 'A') THEN
      RAISE EXCEPTION 'El cargo de proyecto % no existe o no está activo', p_car_pro_cod;
    END IF;

    IF v_est_actual IS NULL THEN
      INSERT INTO g1c_per_car (per_cod, car_pro_cod, per_car_pro_est_reg_cod)
      VALUES (p_per_cod, p_car_pro_cod, 'A');
    ELSIF v_est_actual = 'A' THEN
      RAISE EXCEPTION 'La persona % ya tiene autorizado el cargo de proyecto %',
        p_per_cod, p_car_pro_cod;
    ELSE
      -- existía inactiva o eliminada: se reactiva
      UPDATE g1c_per_car SET per_car_pro_est_reg_cod = 'A'
      WHERE per_cod = p_per_cod AND car_pro_cod = p_car_pro_cod;
    END IF;
    RETURN;
  END IF;

  -- ELIMINAR / INACTIVAR / REACTIVAR sobre fila existente
  IF v_est_actual IS NULL THEN
    RAISE EXCEPTION 'No existe la autorización de cargo (persona %, cargo %)',
      p_per_cod, p_car_pro_cod;
  END IF;

  v_est_destino := CASE p_operacion
                     WHEN 'ELIMINAR'  THEN '*'
                     WHEN 'INACTIVAR' THEN 'I'
                     WHEN 'REACTIVAR' THEN 'A'
                   END;

  UPDATE g1c_per_car SET per_car_pro_est_reg_cod = v_est_destino
  WHERE per_cod = p_per_cod AND car_pro_cod = p_car_pro_cod;
END;
$$;

COMMIT;
