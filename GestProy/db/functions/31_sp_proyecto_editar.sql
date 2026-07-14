-- ============================================================
--  GestProy — Funciones de negocio
--  sp_proyecto_editar : actualiza fechas y montos de la
--  cabecera de un proyecto NO cerrado.
--  Recalcula la utilidad presupuestada y real a partir de
--  monto - costo - gasto (la utilidad nunca se ingresa a mano).
-- ============================================================
BEGIN;

CREATE OR REPLACE FUNCTION sp_proyecto_editar(
  p_cli_cod INTEGER,
  p_tip_cod SMALLINT,
  p_sec     SMALLINT,
  p_fec_con DATE    DEFAULT NULL,
  p_fec_pac DATE    DEFAULT NULL,
  p_fec_ini DATE    DEFAULT NULL,
  p_fec_ent DATE    DEFAULT NULL,
  p_mon_pre NUMERIC DEFAULT NULL,
  p_mon_rea NUMERIC DEFAULT NULL,
  p_cos_pre NUMERIC DEFAULT NULL,
  p_cos_rea NUMERIC DEFAULT NULL,
  p_gas_pre NUMERIC DEFAULT NULL,
  p_gas_rea NUMERIC DEFAULT NULL
)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
  v_est_cod     CHAR(2);
  v_est_reg     CHAR(1);
  v_uti_pre     NUMERIC(10,2);
  v_uti_rea     NUMERIC(10,2);
BEGIN
  SELECT pro_est_cod, pro_est_reg_cod INTO v_est_cod, v_est_reg
  FROM g1t_pro_cab
  WHERE pro_cli_cod = p_cli_cod AND pro_tip_cod = p_tip_cod AND pro_sec = p_sec;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'No existe el proyecto (%, %, %)', p_cli_cod, p_tip_cod, p_sec;
  END IF;
  IF v_est_reg <> 'A' THEN
    RAISE EXCEPTION 'El proyecto (%, %, %) no está activo', p_cli_cod, p_tip_cod, p_sec;
  END IF;
  IF v_est_cod = '04' THEN
    RAISE EXCEPTION 'El proyecto (%, %, %) está cerrado y no puede editarse',
      p_cli_cod, p_tip_cod, p_sec;
  END IF;

  IF p_mon_pre IS NOT NULL THEN
    v_uti_pre := p_mon_pre - COALESCE(p_cos_pre, 0) - COALESCE(p_gas_pre, 0);
  END IF;
  IF p_mon_rea IS NOT NULL THEN
    v_uti_rea := p_mon_rea - COALESCE(p_cos_rea, 0) - COALESCE(p_gas_rea, 0);
  END IF;

  -- la coherencia de fechas la valida trg_procab_valida_fechas
  UPDATE g1t_pro_cab
  SET pro_fec_con = p_fec_con,
      pro_fec_pac = p_fec_pac,
      pro_fec_ini = p_fec_ini,
      pro_fec_ent = p_fec_ent,
      pro_mon_pre = p_mon_pre,
      pro_mon_rea = p_mon_rea,
      pro_cos_pre = p_cos_pre,
      pro_cos_rea = p_cos_rea,
      pro_gas_pre = p_gas_pre,
      pro_gas_rea = p_gas_rea,
      pro_uti_pre = v_uti_pre,
      pro_uti_rea = v_uti_rea
  WHERE pro_cli_cod = p_cli_cod AND pro_tip_cod = p_tip_cod AND pro_sec = p_sec;
END;
$$;

COMMIT;
