-- ============================================================
--  GestProy — Funciones de mantenimiento
--  sp_ref_grupoa_mant : mantenimiento de las 6 tablas
--  referenciales del Grupo A (forma idéntica Cod/Des/EstReg):
--    gzz_est_reg, gzz_tip_cli, gzz_est_cli,
--    gzz_est_pro, gzz_car_per, gzz_car_pro
--
--  Una sola función con ramas IF/ELSIF explícitas por tabla:
--  el SQL de cada tabla queda literal y visible (sin EXECUTE
--  dinámico, sin riesgo de inyección por nombre de tabla).
--
--  p_operacion (mismo vocabulario que el patrón del docente):
--    ADICIONAR  -> INSERT con est_reg 'A'
--    MODIFICAR  -> UPDATE de la descripción
--    ELIMINAR   -> UPDATE est_reg '*'  (eliminación lógica)
--    INACTIVAR  -> UPDATE est_reg 'I'
--    REACTIVAR  -> UPDATE est_reg 'A'
--
--  Errores: siempre vía RAISE EXCEPTION (la aplicación los
--  recibe como DataAccessException y los muestra al usuario).
-- ============================================================
BEGIN;

CREATE OR REPLACE FUNCTION sp_ref_grupoa_mant(
  p_tabla     TEXT,
  p_operacion TEXT,
  p_cod       VARCHAR,
  p_des       VARCHAR
)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
  v_est_destino CHAR(1);  -- estado destino para ELIMINAR/INACTIVAR/REACTIVAR
  v_existe      BOOLEAN;
BEGIN
  -- ---- Validaciones generales -------------------------------------------
  IF p_operacion NOT IN ('ADICIONAR','MODIFICAR','ELIMINAR','INACTIVAR','REACTIVAR') THEN
    RAISE EXCEPTION 'Operación no reconocida: %', p_operacion;
  END IF;
  IF p_cod IS NULL OR btrim(p_cod) = '' THEN
    RAISE EXCEPTION 'El código no puede estar vacío';
  END IF;
  IF p_operacion IN ('ADICIONAR','MODIFICAR') AND (p_des IS NULL OR btrim(p_des) = '') THEN
    RAISE EXCEPTION 'La descripción no puede estar vacía';
  END IF;

  v_est_destino := CASE p_operacion
                     WHEN 'ELIMINAR'  THEN '*'
                     WHEN 'INACTIVAR' THEN 'I'
                     WHEN 'REACTIVAR' THEN 'A'
                     ELSE NULL
                   END;

  -- ---- gzz_est_reg -------------------------------------------------------
  IF p_tabla = 'gzz_est_reg' THEN
    SELECT EXISTS (SELECT 1 FROM gzz_est_reg WHERE est_reg_cod = p_cod) INTO v_existe;
    IF p_operacion = 'ADICIONAR' THEN
      IF v_existe THEN RAISE EXCEPTION 'Ya existe un estado de registro con el código %', p_cod; END IF;
      INSERT INTO gzz_est_reg (est_reg_cod, est_reg_des, est_reg_est_reg)
      VALUES (p_cod, p_des, 'A');
    ELSIF NOT v_existe THEN
      RAISE EXCEPTION 'No existe un estado de registro con el código %', p_cod;
    ELSIF p_operacion = 'MODIFICAR' THEN
      UPDATE gzz_est_reg SET est_reg_des = p_des WHERE est_reg_cod = p_cod;
    ELSE
      UPDATE gzz_est_reg SET est_reg_est_reg = v_est_destino WHERE est_reg_cod = p_cod;
    END IF;

  -- ---- gzz_tip_cli -------------------------------------------------------
  ELSIF p_tabla = 'gzz_tip_cli' THEN
    SELECT EXISTS (SELECT 1 FROM gzz_tip_cli WHERE tip_cli_cod = p_cod) INTO v_existe;
    IF p_operacion = 'ADICIONAR' THEN
      IF v_existe THEN RAISE EXCEPTION 'Ya existe un tipo de cliente con el código %', p_cod; END IF;
      INSERT INTO gzz_tip_cli (tip_cli_cod, tip_cli_des, tip_cli_est_reg)
      VALUES (p_cod, p_des, 'A');
    ELSIF NOT v_existe THEN
      RAISE EXCEPTION 'No existe un tipo de cliente con el código %', p_cod;
    ELSIF p_operacion = 'MODIFICAR' THEN
      UPDATE gzz_tip_cli SET tip_cli_des = p_des WHERE tip_cli_cod = p_cod;
    ELSE
      UPDATE gzz_tip_cli SET tip_cli_est_reg = v_est_destino WHERE tip_cli_cod = p_cod;
    END IF;

  -- ---- gzz_est_cli -------------------------------------------------------
  ELSIF p_tabla = 'gzz_est_cli' THEN
    SELECT EXISTS (SELECT 1 FROM gzz_est_cli WHERE est_cli_cod = p_cod) INTO v_existe;
    IF p_operacion = 'ADICIONAR' THEN
      IF v_existe THEN RAISE EXCEPTION 'Ya existe un estado de cliente con el código %', p_cod; END IF;
      INSERT INTO gzz_est_cli (est_cli_cod, est_cli_des, est_cli_est_reg)
      VALUES (p_cod, p_des, 'A');
    ELSIF NOT v_existe THEN
      RAISE EXCEPTION 'No existe un estado de cliente con el código %', p_cod;
    ELSIF p_operacion = 'MODIFICAR' THEN
      UPDATE gzz_est_cli SET est_cli_des = p_des WHERE est_cli_cod = p_cod;
    ELSE
      UPDATE gzz_est_cli SET est_cli_est_reg = v_est_destino WHERE est_cli_cod = p_cod;
    END IF;

  -- ---- gzz_est_pro -------------------------------------------------------
  ELSIF p_tabla = 'gzz_est_pro' THEN
    SELECT EXISTS (SELECT 1 FROM gzz_est_pro WHERE est_pro_cod = p_cod) INTO v_existe;
    IF p_operacion = 'ADICIONAR' THEN
      IF v_existe THEN RAISE EXCEPTION 'Ya existe un estado de proyecto con el código %', p_cod; END IF;
      INSERT INTO gzz_est_pro (est_pro_cod, est_pro_des, est_pro_est_reg)
      VALUES (p_cod, p_des, 'A');
    ELSIF NOT v_existe THEN
      RAISE EXCEPTION 'No existe un estado de proyecto con el código %', p_cod;
    ELSIF p_operacion = 'MODIFICAR' THEN
      UPDATE gzz_est_pro SET est_pro_des = p_des WHERE est_pro_cod = p_cod;
    ELSE
      UPDATE gzz_est_pro SET est_pro_est_reg = v_est_destino WHERE est_pro_cod = p_cod;
    END IF;

  -- ---- gzz_car_per -------------------------------------------------------
  ELSIF p_tabla = 'gzz_car_per' THEN
    SELECT EXISTS (SELECT 1 FROM gzz_car_per WHERE car_per_cod = p_cod::smallint) INTO v_existe;
    IF p_operacion = 'ADICIONAR' THEN
      IF v_existe THEN RAISE EXCEPTION 'Ya existe un cargo de personal con el código %', p_cod; END IF;
      INSERT INTO gzz_car_per (car_per_cod, car_per_des, car_per_est_reg)
      VALUES (p_cod::smallint, p_des, 'A');
    ELSIF NOT v_existe THEN
      RAISE EXCEPTION 'No existe un cargo de personal con el código %', p_cod;
    ELSIF p_operacion = 'MODIFICAR' THEN
      UPDATE gzz_car_per SET car_per_des = p_des WHERE car_per_cod = p_cod::smallint;
    ELSE
      UPDATE gzz_car_per SET car_per_est_reg = v_est_destino WHERE car_per_cod = p_cod::smallint;
    END IF;

  -- ---- gzz_car_pro -------------------------------------------------------
  ELSIF p_tabla = 'gzz_car_pro' THEN
    SELECT EXISTS (SELECT 1 FROM gzz_car_pro WHERE car_pro_cod = p_cod::smallint) INTO v_existe;
    IF p_operacion = 'ADICIONAR' THEN
      IF v_existe THEN RAISE EXCEPTION 'Ya existe un cargo de proyecto con el código %', p_cod; END IF;
      INSERT INTO gzz_car_pro (car_pro_cod, car_pro_des, car_pro_est_reg)
      VALUES (p_cod::smallint, p_des, 'A');
    ELSIF NOT v_existe THEN
      RAISE EXCEPTION 'No existe un cargo de proyecto con el código %', p_cod;
    ELSIF p_operacion = 'MODIFICAR' THEN
      UPDATE gzz_car_pro SET car_pro_des = p_des WHERE car_pro_cod = p_cod::smallint;
    ELSE
      UPDATE gzz_car_pro SET car_pro_est_reg = v_est_destino WHERE car_pro_cod = p_cod::smallint;
    END IF;

  ELSE
    RAISE EXCEPTION 'Tabla no reconocida para el Grupo A: %', p_tabla;
  END IF;
END;
$$;

COMMIT;
