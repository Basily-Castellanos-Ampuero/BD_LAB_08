-- ============================================================
--  GestProy — Triggers
--  trg_proeqp_valida_percar_activo (BEFORE INSERT OR UPDATE en g1t_pro_eqp)
--
--  La FK fk_proeqp_percar solo garantiza que la combinación
--  (persona, cargo) EXISTA en g1c_per_car; este trigger exige
--  además que esté ACTIVA ('A') cuando se asigna o reactiva un
--  miembro del equipo. No aplica al quitar/inactivar (est_reg
--  destino 'I' o '*'), para poder retirar a alguien aunque su
--  autorización ya no esté vigente.
-- ============================================================
BEGIN;

CREATE OR REPLACE FUNCTION trgfn_proeqp_valida_percar_activo()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
  IF NEW.pro_per_car_est_reg_cod = 'A' THEN
    IF NOT EXISTS (
      SELECT 1
      FROM g1c_per_car pc
      WHERE pc.per_cod = NEW.per_cod
        AND pc.car_pro_cod = NEW.car_pro_cod
        AND pc.per_car_pro_est_reg_cod = 'A'
    ) THEN
      RAISE EXCEPTION 'La persona % no tiene autorizado (activo) el cargo de proyecto %',
        NEW.per_cod, NEW.car_pro_cod;
    END IF;
  END IF;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_proeqp_valida_percar_activo ON g1t_pro_eqp;
CREATE TRIGGER trg_proeqp_valida_percar_activo
BEFORE INSERT OR UPDATE ON g1t_pro_eqp
FOR EACH ROW
EXECUTE FUNCTION trgfn_proeqp_valida_percar_activo();

COMMIT;
