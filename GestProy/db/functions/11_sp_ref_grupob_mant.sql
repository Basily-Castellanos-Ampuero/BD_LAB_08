-- ============================================================
--  GestProy — Funciones de mantenimiento
--  Grupo B: referenciales con columna(s) extra. Una función
--  específica por tabla porque la columna adicional difiere en
--  nombre y tipo:
--    sp_gzz_tip_pro_mant : gzz_tip_pro (+ tam char(1))
--    sp_gzz_lin_pro_mant : gzz_lin_pro (+ nom varchar(60), tam char(1))
--    sp_gzz_etp_pro_mant : gzz_etp_pro (+ tie_est numeric(5,2))
--  sp_gzz_tip_pro_mant replica en PostgreSQL el comportamiento
--  del ejemplo TipPro.java del docente.
-- ============================================================
BEGIN;

-- ---- gzz_tip_pro ---------------------------------------------------------
CREATE OR REPLACE FUNCTION sp_gzz_tip_pro_mant(
  p_operacion TEXT,
  p_cod       SMALLINT,
  p_des       VARCHAR,
  p_tam       CHAR
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
    RAISE EXCEPTION 'El código no puede estar vacío';
  END IF;
  IF p_operacion IN ('ADICIONAR','MODIFICAR') THEN
    IF p_des IS NULL OR btrim(p_des) = '' THEN
      RAISE EXCEPTION 'La descripción no puede estar vacía';
    END IF;
    IF p_tam IS NULL OR p_tam NOT IN ('P','M','G') THEN
      RAISE EXCEPTION 'El tamaño debe ser P, M o G';
    END IF;
  END IF;

  v_est_destino := CASE p_operacion
                     WHEN 'ELIMINAR'  THEN '*'
                     WHEN 'INACTIVAR' THEN 'I'
                     WHEN 'REACTIVAR' THEN 'A'
                     ELSE NULL
                   END;

  SELECT EXISTS (SELECT 1 FROM gzz_tip_pro WHERE tip_pro_cod = p_cod) INTO v_existe;

  IF p_operacion = 'ADICIONAR' THEN
    IF v_existe THEN RAISE EXCEPTION 'Ya existe un tipo de proyecto con el código %', p_cod; END IF;
    INSERT INTO gzz_tip_pro (tip_pro_cod, tip_pro_des, tip_pro_tam, tip_pro_est_reg)
    VALUES (p_cod, p_des, p_tam, 'A');
  ELSIF NOT v_existe THEN
    RAISE EXCEPTION 'No existe un tipo de proyecto con el código %', p_cod;
  ELSIF p_operacion = 'MODIFICAR' THEN
    UPDATE gzz_tip_pro SET tip_pro_des = p_des, tip_pro_tam = p_tam
    WHERE tip_pro_cod = p_cod;
  ELSE
    UPDATE gzz_tip_pro SET tip_pro_est_reg = v_est_destino WHERE tip_pro_cod = p_cod;
  END IF;
END;
$$;

-- ---- gzz_lin_pro ---------------------------------------------------------
CREATE OR REPLACE FUNCTION sp_gzz_lin_pro_mant(
  p_operacion TEXT,
  p_cod       SMALLINT,
  p_nom       VARCHAR,
  p_tam       CHAR
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
    RAISE EXCEPTION 'El código no puede estar vacío';
  END IF;
  IF p_operacion IN ('ADICIONAR','MODIFICAR') THEN
    IF p_nom IS NULL OR btrim(p_nom) = '' THEN
      RAISE EXCEPTION 'El nombre no puede estar vacío';
    END IF;
    IF p_tam IS NULL OR p_tam NOT IN ('P','M','G') THEN
      RAISE EXCEPTION 'El tamaño debe ser P, M o G';
    END IF;
  END IF;

  v_est_destino := CASE p_operacion
                     WHEN 'ELIMINAR'  THEN '*'
                     WHEN 'INACTIVAR' THEN 'I'
                     WHEN 'REACTIVAR' THEN 'A'
                     ELSE NULL
                   END;

  SELECT EXISTS (SELECT 1 FROM gzz_lin_pro WHERE lin_pro_cod = p_cod) INTO v_existe;

  IF p_operacion = 'ADICIONAR' THEN
    IF v_existe THEN RAISE EXCEPTION 'Ya existe una línea de proyecto con el código %', p_cod; END IF;
    INSERT INTO gzz_lin_pro (lin_pro_cod, lin_pro_nom, lin_pro_tam, lin_pro_est_reg_cod)
    VALUES (p_cod, p_nom, p_tam, 'A');
  ELSIF NOT v_existe THEN
    RAISE EXCEPTION 'No existe una línea de proyecto con el código %', p_cod;
  ELSIF p_operacion = 'MODIFICAR' THEN
    UPDATE gzz_lin_pro SET lin_pro_nom = p_nom, lin_pro_tam = p_tam
    WHERE lin_pro_cod = p_cod;
  ELSE
    UPDATE gzz_lin_pro SET lin_pro_est_reg_cod = v_est_destino WHERE lin_pro_cod = p_cod;
  END IF;
END;
$$;

-- ---- gzz_etp_pro ---------------------------------------------------------
CREATE OR REPLACE FUNCTION sp_gzz_etp_pro_mant(
  p_operacion TEXT,
  p_cod       SMALLINT,
  p_des       VARCHAR,
  p_tie_est   NUMERIC
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
    RAISE EXCEPTION 'El código no puede estar vacío';
  END IF;
  IF p_operacion IN ('ADICIONAR','MODIFICAR') THEN
    IF p_des IS NULL OR btrim(p_des) = '' THEN
      RAISE EXCEPTION 'La descripción no puede estar vacía';
    END IF;
    IF p_tie_est IS NULL OR p_tie_est <= 0 THEN
      RAISE EXCEPTION 'El tiempo estimado de la etapa debe ser mayor a 0 horas';
    END IF;
  END IF;

  v_est_destino := CASE p_operacion
                     WHEN 'ELIMINAR'  THEN '*'
                     WHEN 'INACTIVAR' THEN 'I'
                     WHEN 'REACTIVAR' THEN 'A'
                     ELSE NULL
                   END;

  SELECT EXISTS (SELECT 1 FROM gzz_etp_pro WHERE etp_cod = p_cod) INTO v_existe;

  IF p_operacion = 'ADICIONAR' THEN
    IF v_existe THEN RAISE EXCEPTION 'Ya existe una etapa de proyecto con el código %', p_cod; END IF;
    INSERT INTO gzz_etp_pro (etp_cod, etp_des, etp_tie_est, etp_est_reg)
    VALUES (p_cod, p_des, p_tie_est, 'A');
  ELSIF NOT v_existe THEN
    RAISE EXCEPTION 'No existe una etapa de proyecto con el código %', p_cod;
  ELSIF p_operacion = 'MODIFICAR' THEN
    UPDATE gzz_etp_pro SET etp_des = p_des, etp_tie_est = p_tie_est
    WHERE etp_cod = p_cod;
  ELSE
    UPDATE gzz_etp_pro SET etp_est_reg = v_est_destino WHERE etp_cod = p_cod;
  END IF;
END;
$$;

COMMIT;
