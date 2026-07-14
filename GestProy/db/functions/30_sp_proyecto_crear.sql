-- ============================================================
--  GestProy — Funciones de negocio
--  sp_proyecto_crear : crea la cabecera de un proyecto
--
--  - Calcula pro_sec = MAX(pro_sec)+1 para (cliente, tipo):
--    es la secuencia "proyecto N de este tipo para este cliente"
--    implícita en la PK compuesta (por eso no se usa SERIAL).
--  - Estado inicial: '01' (Planificado).
--  - Calcula la utilidad presupuestada si hay monto/costo/gasto.
--  - Retorna el pro_sec generado.
-- ============================================================
BEGIN;

CREATE OR REPLACE FUNCTION sp_proyecto_crear(
  p_cli_cod INTEGER,
  p_tip_cod SMALLINT,
  p_fec_con DATE    DEFAULT NULL,
  p_fec_pac DATE    DEFAULT NULL,
  p_mon_pre NUMERIC DEFAULT NULL,
  p_cos_pre NUMERIC DEFAULT NULL,
  p_gas_pre NUMERIC DEFAULT NULL
)
RETURNS SMALLINT
LANGUAGE plpgsql
AS $$
DECLARE
  v_sec     SMALLINT;
  v_uti_pre NUMERIC(10,2);
BEGIN
  IF NOT EXISTS (SELECT 1 FROM g1m_clientes
                 WHERE cli_cod = p_cli_cod AND cli_est_reg_cod = 'A') THEN
    RAISE EXCEPTION 'El cliente % no existe o no está activo', p_cli_cod;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM gzz_tip_pro
                 WHERE tip_pro_cod = p_tip_cod AND tip_pro_est_reg = 'A') THEN
    RAISE EXCEPTION 'El tipo de proyecto % no existe o no está activo', p_tip_cod;
  END IF;

  SELECT COALESCE(MAX(pro_sec), 0) + 1
    INTO v_sec
  FROM g1t_pro_cab
  WHERE pro_cli_cod = p_cli_cod AND pro_tip_cod = p_tip_cod;

  IF p_mon_pre IS NOT NULL THEN
    v_uti_pre := p_mon_pre - COALESCE(p_cos_pre, 0) - COALESCE(p_gas_pre, 0);
  END IF;

  -- la coherencia fec_con <= fec_pac la valida trg_procab_valida_fechas
  INSERT INTO g1t_pro_cab
    (pro_cli_cod, pro_tip_cod, pro_sec, pro_fec_con, pro_fec_pac,
     pro_mon_pre, pro_cos_pre, pro_gas_pre, pro_uti_pre,
     pro_est_cod, pro_est_reg_cod)
  VALUES
    (p_cli_cod, p_tip_cod, v_sec, p_fec_con, p_fec_pac,
     p_mon_pre, p_cos_pre, p_gas_pre, v_uti_pre,
     '01', 'A');

  RETURN v_sec;
END;
$$;

COMMIT;
