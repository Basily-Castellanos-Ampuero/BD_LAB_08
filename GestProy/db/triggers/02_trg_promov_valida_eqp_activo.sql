-- ============================================================
--  GestProy — Triggers
--  trg_promov_valida_eqp_activo (BEFORE INSERT en g1t_pro_mov)
--
--  Impide registrar horas de avance para una persona que ya no
--  está activamente asignada al proyecto (retirada del equipo,
--  est_reg 'I' o '*'). La FK fk_promov_eqp solo valida que la
--  fila de equipo exista, no su estado.
-- ============================================================
BEGIN;

CREATE OR REPLACE FUNCTION trgfn_promov_valida_eqp_activo()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM g1t_pro_eqp e
    WHERE e.pro_cli_cod = NEW.pro_cli_cod
      AND e.pro_tip_cod = NEW.pro_tip_cod
      AND e.pro_sec     = NEW.pro_sec
      AND e.per_cod     = NEW.per_cod
      AND e.car_pro_cod = NEW.car_pro_cod
      AND e.pro_per_car_est_reg_cod = 'A'
  ) THEN
    RAISE EXCEPTION 'La persona % no está activamente asignada al proyecto (%,%,%) con el cargo %',
      NEW.per_cod, NEW.pro_cli_cod, NEW.pro_tip_cod, NEW.pro_sec, NEW.car_pro_cod;
  END IF;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_promov_valida_eqp_activo ON g1t_pro_mov;
CREATE TRIGGER trg_promov_valida_eqp_activo
BEFORE INSERT ON g1t_pro_mov
FOR EACH ROW
EXECUTE FUNCTION trgfn_promov_valida_eqp_activo();

COMMIT;
