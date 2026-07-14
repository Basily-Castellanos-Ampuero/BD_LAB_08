-- ============================================================
--  GestProy — Funciones de negocio
--  sp_proyecto_cambiar_estado : transición de estado del
--  proyecto validada contra una matriz de transiciones.
--
--  Estados (gzz_est_pro, ver seed):
--    01 Planificado   02 En Ejecución   03 Entregado
--    04 Cerrado       05 Suspendido
--
--  Transiciones permitidas:
--    01 -> 02 (iniciar: fija pro_fec_ini si es NULL)
--    01 -> 05, 02 -> 05 (suspender)
--    05 -> 02 (retomar)
--    02 -> 03 (entregar: fija pro_fec_ent si es NULL)
--    03 -> 04 (cerrar: fija pro_fec_cer y actualiza la fecha de
--              último proyecto cerrado del cliente)
-- ============================================================
BEGIN;

CREATE OR REPLACE FUNCTION sp_proyecto_cambiar_estado(
  p_cli_cod       INTEGER,
  p_tip_cod       SMALLINT,
  p_sec           SMALLINT,
  p_nuevo_est_cod CHAR(2)
)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
  v_est_actual CHAR(2);
  v_est_reg    CHAR(1);
BEGIN
  SELECT pro_est_cod, pro_est_reg_cod INTO v_est_actual, v_est_reg
  FROM g1t_pro_cab
  WHERE pro_cli_cod = p_cli_cod AND pro_tip_cod = p_tip_cod AND pro_sec = p_sec;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'No existe el proyecto (%, %, %)', p_cli_cod, p_tip_cod, p_sec;
  END IF;
  IF v_est_reg <> 'A' THEN
    RAISE EXCEPTION 'El proyecto (%, %, %) no está activo', p_cli_cod, p_tip_cod, p_sec;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM gzz_est_pro
                 WHERE est_pro_cod = p_nuevo_est_cod AND est_pro_est_reg = 'A') THEN
    RAISE EXCEPTION 'El estado de proyecto % no existe o no está activo', p_nuevo_est_cod;
  END IF;
  IF v_est_actual = p_nuevo_est_cod THEN
    RAISE EXCEPTION 'El proyecto ya se encuentra en el estado %', p_nuevo_est_cod;
  END IF;

  -- matriz de transiciones permitidas
  IF NOT ( (v_est_actual = '01' AND p_nuevo_est_cod IN ('02','05'))
        OR (v_est_actual = '02' AND p_nuevo_est_cod IN ('03','05'))
        OR (v_est_actual = '05' AND p_nuevo_est_cod = '02')
        OR (v_est_actual = '03' AND p_nuevo_est_cod = '04') ) THEN
    RAISE EXCEPTION 'Transición de estado no permitida: % -> %',
      v_est_actual, p_nuevo_est_cod;
  END IF;

  UPDATE g1t_pro_cab
  SET pro_est_cod = p_nuevo_est_cod,
      pro_fec_ini = CASE WHEN p_nuevo_est_cod = '02' AND pro_fec_ini IS NULL
                         THEN CURRENT_DATE ELSE pro_fec_ini END,
      pro_fec_ent = CASE WHEN p_nuevo_est_cod = '03' AND pro_fec_ent IS NULL
                         THEN CURRENT_DATE ELSE pro_fec_ent END,
      pro_fec_cer = CASE WHEN p_nuevo_est_cod = '04' AND pro_fec_cer IS NULL
                         THEN CURRENT_DATE ELSE pro_fec_cer END
  WHERE pro_cli_cod = p_cli_cod AND pro_tip_cod = p_tip_cod AND pro_sec = p_sec;

  -- al cerrar, se registra en el cliente la fecha de su último proyecto cerrado
  IF p_nuevo_est_cod = '04' THEN
    UPDATE g1m_clientes
    SET cli_fec_ult_pro_cer = (SELECT pro_fec_cer FROM g1t_pro_cab
                               WHERE pro_cli_cod = p_cli_cod
                                 AND pro_tip_cod = p_tip_cod
                                 AND pro_sec = p_sec)
    WHERE cli_cod = p_cli_cod;
  END IF;
END;
$$;

COMMIT;
