-- ============================================================
--  GestProy — Funciones de mantenimiento
--  sp_personal_mant : mantenimiento de g1m_personal
--  Reglas de negocio: costo/hora > 0, cargo de personal activo,
--  fecha de ingreso no futura.
-- ============================================================
BEGIN;

CREATE OR REPLACE FUNCTION sp_personal_mant(
  p_operacion TEXT,
  p_cod       INTEGER,
  p_nom       VARCHAR  DEFAULT NULL,
  p_car_cod   SMALLINT DEFAULT NULL,
  p_cos_hor   NUMERIC  DEFAULT NULL,
  p_fec_ing   DATE     DEFAULT NULL
)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
  v_est_destino CHAR(1);
  v_existe      BOOLEAN;
BEGIN
  IF p_operacion NOT IN ('ADICIONAR','MODIFICAR','ELIMINAR','INACTIVAR','REACTIVAR') THEN
    RAISE EXCEPTION 'Operación no reconocida: %', p_operacion;
  END IF;
  IF p_cod IS NULL THEN
    RAISE EXCEPTION 'El código del personal no puede estar vacío';
  END IF;

  IF p_operacion IN ('ADICIONAR','MODIFICAR') THEN
    IF p_nom IS NULL OR btrim(p_nom) = '' THEN
      RAISE EXCEPTION 'El nombre del personal no puede estar vacío';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM gzz_car_per
                   WHERE car_per_cod = p_car_cod AND car_per_est_reg = 'A') THEN
      RAISE EXCEPTION 'El cargo de personal % no existe o no está activo', p_car_cod;
    END IF;
    IF p_cos_hor IS NULL OR p_cos_hor <= 0 THEN
      RAISE EXCEPTION 'El costo por hora debe ser mayor a 0';
    END IF;
    IF p_fec_ing IS NULL THEN
      RAISE EXCEPTION 'La fecha de ingreso es obligatoria';
    END IF;
    IF p_fec_ing > CURRENT_DATE THEN
      RAISE EXCEPTION 'La fecha de ingreso no puede ser futura';
    END IF;
  END IF;

  v_est_destino := CASE p_operacion
                     WHEN 'ELIMINAR'  THEN '*'
                     WHEN 'INACTIVAR' THEN 'I'
                     WHEN 'REACTIVAR' THEN 'A'
                     ELSE NULL
                   END;

  SELECT EXISTS (SELECT 1 FROM g1m_personal WHERE per_cod = p_cod) INTO v_existe;

  IF p_operacion = 'ADICIONAR' THEN
    IF v_existe THEN
      RAISE EXCEPTION 'Ya existe personal con el código %', p_cod;
    END IF;
    INSERT INTO g1m_personal
      (per_cod, per_nom, per_car_cod, per_cos_hor, per_fec_ing, per_est_reg_cod)
    VALUES
      (p_cod, p_nom, p_car_cod, p_cos_hor, p_fec_ing, 'A');
  ELSIF NOT v_existe THEN
    RAISE EXCEPTION 'No existe personal con el código %', p_cod;
  ELSIF p_operacion = 'MODIFICAR' THEN
    UPDATE g1m_personal
    SET per_nom     = p_nom,
        per_car_cod = p_car_cod,
        per_cos_hor = p_cos_hor,
        per_fec_ing = p_fec_ing
    WHERE per_cod = p_cod;
  ELSE
    UPDATE g1m_personal SET per_est_reg_cod = v_est_destino WHERE per_cod = p_cod;
  END IF;
END;
$$;

COMMIT;
