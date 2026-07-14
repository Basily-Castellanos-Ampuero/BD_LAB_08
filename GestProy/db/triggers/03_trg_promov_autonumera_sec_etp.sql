-- ============================================================
--  GestProy — Triggers
--  trg_promov_autonumera_sec_etp (BEFORE INSERT en g1t_pro_mov)
--
--  Calcula automáticamente sec_etp = MAX(sec_etp)+1 para la
--  combinación (proyecto, persona, cargo, etapa) cuando el
--  INSERT no lo provee (NULL o 0). Así la capa de aplicación no
--  necesita calcular el último componente de la PK compuesta.
--  Al ser BEFORE trigger, rellena NEW.sec_etp antes de que se
--  evalúen el NOT NULL y la PK.
-- ============================================================
BEGIN;

CREATE OR REPLACE FUNCTION trgfn_promov_autonumera_sec_etp()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
  IF NEW.sec_etp IS NULL OR NEW.sec_etp = 0 THEN
    SELECT COALESCE(MAX(m.sec_etp), 0) + 1
      INTO NEW.sec_etp
    FROM g1t_pro_mov m
    WHERE m.pro_cli_cod = NEW.pro_cli_cod
      AND m.pro_tip_cod = NEW.pro_tip_cod
      AND m.pro_sec     = NEW.pro_sec
      AND m.per_cod     = NEW.per_cod
      AND m.car_pro_cod = NEW.car_pro_cod
      AND m.etp_cod     = NEW.etp_cod;
  END IF;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_promov_autonumera_sec_etp ON g1t_pro_mov;
CREATE TRIGGER trg_promov_autonumera_sec_etp
BEFORE INSERT ON g1t_pro_mov
FOR EACH ROW
EXECUTE FUNCTION trgfn_promov_autonumera_sec_etp();

COMMIT;
