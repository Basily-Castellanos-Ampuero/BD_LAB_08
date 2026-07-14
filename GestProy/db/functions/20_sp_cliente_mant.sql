-- ============================================================
--  GestProy — Funciones de mantenimiento
--  sp_cliente_mant : mantenimiento de g1m_clientes
--  Valida que las FK a catálogos apunten a registros ACTIVOS
--  (la FK sola no valida el estado) y la coherencia de fechas.
-- ============================================================
BEGIN;

CREATE OR REPLACE FUNCTION sp_cliente_mant(
  p_operacion       TEXT,
  p_cod             INTEGER,
  p_nom             VARCHAR  DEFAULT NULL,
  p_tip_cod         CHAR(2)  DEFAULT NULL,
  p_est_cod         CHAR(1)  DEFAULT NULL,
  p_fec_ing         DATE     DEFAULT NULL,
  p_fec_ces         DATE     DEFAULT NULL,
  p_fec_ult_pro_cer DATE     DEFAULT NULL
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
    RAISE EXCEPTION 'El código del cliente no puede estar vacío';
  END IF;

  IF p_operacion IN ('ADICIONAR','MODIFICAR') THEN
    IF p_nom IS NULL OR btrim(p_nom) = '' THEN
      RAISE EXCEPTION 'El nombre del cliente no puede estar vacío';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM gzz_tip_cli
                   WHERE tip_cli_cod = p_tip_cod AND tip_cli_est_reg = 'A') THEN
      RAISE EXCEPTION 'El tipo de cliente % no existe o no está activo', p_tip_cod;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM gzz_est_cli
                   WHERE est_cli_cod = p_est_cod AND est_cli_est_reg = 'A') THEN
      RAISE EXCEPTION 'El estado de cliente % no existe o no está activo', p_est_cod;
    END IF;
    IF p_fec_ing IS NOT NULL AND p_fec_ces IS NOT NULL AND p_fec_ing > p_fec_ces THEN
      RAISE EXCEPTION 'La fecha de ingreso no puede ser posterior a la fecha de cese';
    END IF;
  END IF;

  v_est_destino := CASE p_operacion
                     WHEN 'ELIMINAR'  THEN '*'
                     WHEN 'INACTIVAR' THEN 'I'
                     WHEN 'REACTIVAR' THEN 'A'
                     ELSE NULL
                   END;

  SELECT EXISTS (SELECT 1 FROM g1m_clientes WHERE cli_cod = p_cod) INTO v_existe;

  IF p_operacion = 'ADICIONAR' THEN
    IF v_existe THEN
      RAISE EXCEPTION 'Ya existe un cliente con el código %', p_cod;
    END IF;
    INSERT INTO g1m_clientes
      (cli_cod, cli_nom, cli_tip_cod, cli_fec_ing, cli_fec_ces,
       cli_fec_ult_pro_cer, cli_est_cod, cli_est_reg_cod)
    VALUES
      (p_cod, p_nom, p_tip_cod, p_fec_ing, p_fec_ces,
       p_fec_ult_pro_cer, p_est_cod, 'A');
  ELSIF NOT v_existe THEN
    RAISE EXCEPTION 'No existe un cliente con el código %', p_cod;
  ELSIF p_operacion = 'MODIFICAR' THEN
    UPDATE g1m_clientes
    SET cli_nom             = p_nom,
        cli_tip_cod         = p_tip_cod,
        cli_est_cod         = p_est_cod,
        cli_fec_ing         = p_fec_ing,
        cli_fec_ces         = p_fec_ces,
        cli_fec_ult_pro_cer = p_fec_ult_pro_cer
    WHERE cli_cod = p_cod;
  ELSE
    UPDATE g1m_clientes SET cli_est_reg_cod = v_est_destino WHERE cli_cod = p_cod;
  END IF;
END;
$$;

COMMIT;
