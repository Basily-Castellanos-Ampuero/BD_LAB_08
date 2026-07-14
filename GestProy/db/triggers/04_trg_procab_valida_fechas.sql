-- ============================================================
--  GestProy — Triggers
--  trg_procab_valida_fechas (BEFORE INSERT OR UPDATE en g1t_pro_cab)
--
--  Coherencia de fechas de la cabecera de proyecto:
--    - fecha de contrato <= fecha pactada
--    - fecha de inicio   <= fecha de entrega
--    - fecha de inicio   <= fecha de cierre
--  Solo se validan cuando ambos extremos no son NULL.
-- ============================================================
BEGIN;

CREATE OR REPLACE FUNCTION trgfn_procab_valida_fechas()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
  IF NEW.pro_fec_con IS NOT NULL AND NEW.pro_fec_pac IS NOT NULL
     AND NEW.pro_fec_con > NEW.pro_fec_pac THEN
    RAISE EXCEPTION 'La fecha de contrato (%) no puede ser posterior a la fecha pactada (%)',
      NEW.pro_fec_con, NEW.pro_fec_pac;
  END IF;

  IF NEW.pro_fec_ini IS NOT NULL AND NEW.pro_fec_ent IS NOT NULL
     AND NEW.pro_fec_ini > NEW.pro_fec_ent THEN
    RAISE EXCEPTION 'La fecha de inicio (%) no puede ser posterior a la fecha de entrega (%)',
      NEW.pro_fec_ini, NEW.pro_fec_ent;
  END IF;

  IF NEW.pro_fec_ini IS NOT NULL AND NEW.pro_fec_cer IS NOT NULL
     AND NEW.pro_fec_ini > NEW.pro_fec_cer THEN
    RAISE EXCEPTION 'La fecha de inicio (%) no puede ser posterior a la fecha de cierre (%)',
      NEW.pro_fec_ini, NEW.pro_fec_cer;
  END IF;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_procab_valida_fechas ON g1t_pro_cab;
CREATE TRIGGER trg_procab_valida_fechas
BEFORE INSERT OR UPDATE ON g1t_pro_cab
FOR EACH ROW
EXECUTE FUNCTION trgfn_procab_valida_fechas();

COMMIT;
