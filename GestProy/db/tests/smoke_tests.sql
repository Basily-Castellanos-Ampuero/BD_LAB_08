-- ============================================================
--  GestProy — Tests de humo de la base de datos
--
--  Verifica que el esquema, las funciones, los triggers y las
--  vistas se comporten según las reglas de negocio del curso.
--
--  TODO el script corre dentro de una transacción que termina
--  en ROLLBACK: puede ejecutarse contra la BD de desarrollo sin
--  dejar rastro. Usa códigos 9xxx para no chocar con datos
--  reales ni con el seed.
--
--  Cada aserción fallida lanza RAISE EXCEPTION 'TEST FALLIDO...'
--  y, con ON_ERROR_STOP, psql termina con código distinto de 0.
--  Ejecutar con: db\scripts\run-tests.ps1  (o psql -f este archivo)
--
--  Requiere: esquema + triggers + funciones + vistas aplicados,
--  el seed de referenciales (catálogos GZZ_*) y el seed de la
--  cuenta admin (seed/04_seed_usuario.sql) cargados.
-- ============================================================
\set ON_ERROR_STOP on

BEGIN;

-- ------------------------------------------------------------
-- T00. Precondiciones: catálogos mínimos del seed
-- ------------------------------------------------------------
DO $t$
BEGIN
  IF (SELECT COUNT(*) FROM gzz_est_reg WHERE est_reg_cod IN ('A','I','*')) <> 3 THEN
    RAISE EXCEPTION 'TEST FALLIDO T00: faltan los estados de registro A/I/* (aplicar seed/01)';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM gzz_etp_pro WHERE etp_est_reg = 'A') THEN
    RAISE EXCEPTION 'TEST FALLIDO T00: no hay etapas activas en gzz_etp_pro (aplicar seed/01)';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM gzz_tip_cli WHERE tip_cli_cod = 'EP' AND tip_cli_est_reg = 'A') THEN
    RAISE EXCEPTION 'TEST FALLIDO T00: falta el tipo de cliente EP del seed';
  END IF;
  RAISE NOTICE 'OK T00: precondiciones del seed presentes';
END;
$t$;

-- ------------------------------------------------------------
-- T01. Referenciales Grupo A: ciclo completo de mantenimiento
-- ------------------------------------------------------------
DO $t$
DECLARE
  v_est CHAR(1);
BEGIN
  PERFORM sp_ref_grupoa_mant('gzz_tip_cli', 'ADICIONAR', 'ZT', 'Tipo de prueba');
  SELECT tip_cli_est_reg INTO v_est FROM gzz_tip_cli WHERE tip_cli_cod = 'ZT';
  IF v_est IS DISTINCT FROM 'A' THEN
    RAISE EXCEPTION 'TEST FALLIDO T01: ADICIONAR no dejó el registro en estado A (quedó %)', v_est;
  END IF;

  -- ADICIONAR duplicado debe fallar
  BEGIN
    PERFORM sp_ref_grupoa_mant('gzz_tip_cli', 'ADICIONAR', 'ZT', 'Duplicado');
    RAISE EXCEPTION 'TEST FALLIDO T01: ADICIONAR duplicado no lanzó error';
  EXCEPTION WHEN OTHERS THEN
    IF SQLERRM LIKE 'TEST FALLIDO%' THEN RAISE; END IF;
  END;

  PERFORM sp_ref_grupoa_mant('gzz_tip_cli', 'MODIFICAR', 'ZT', 'Descripción cambiada');
  IF (SELECT tip_cli_des FROM gzz_tip_cli WHERE tip_cli_cod = 'ZT') <> 'Descripción cambiada' THEN
    RAISE EXCEPTION 'TEST FALLIDO T01: MODIFICAR no actualizó la descripción';
  END IF;

  PERFORM sp_ref_grupoa_mant('gzz_tip_cli', 'INACTIVAR', 'ZT', NULL);
  PERFORM sp_ref_grupoa_mant('gzz_tip_cli', 'REACTIVAR', 'ZT', NULL);
  PERFORM sp_ref_grupoa_mant('gzz_tip_cli', 'ELIMINAR',  'ZT', NULL);
  SELECT tip_cli_est_reg INTO v_est FROM gzz_tip_cli WHERE tip_cli_cod = 'ZT';
  IF v_est IS DISTINCT FROM '*' THEN
    RAISE EXCEPTION 'TEST FALLIDO T01: ELIMINAR no dejó estado * (quedó %)', v_est;
  END IF;

  -- MODIFICAR un código inexistente debe fallar
  BEGIN
    PERFORM sp_ref_grupoa_mant('gzz_tip_cli', 'MODIFICAR', 'ZX', 'No existe');
    RAISE EXCEPTION 'TEST FALLIDO T01: MODIFICAR inexistente no lanzó error';
  EXCEPTION WHEN OTHERS THEN
    IF SQLERRM LIKE 'TEST FALLIDO%' THEN RAISE; END IF;
  END;

  RAISE NOTICE 'OK T01: mantenimiento Grupo A (adicionar/modificar/inactivar/reactivar/eliminar)';
END;
$t$;

-- ------------------------------------------------------------
-- T02. Referenciales Grupo B: validación de columna extra
-- ------------------------------------------------------------
DO $t$
BEGIN
  -- tiempo estimado <= 0 debe fallar
  BEGIN
    PERFORM sp_gzz_etp_pro_mant('ADICIONAR', 901::smallint, 'Etapa inválida', -5);
    RAISE EXCEPTION 'TEST FALLIDO T02: etapa con tiempo estimado negativo no lanzó error';
  EXCEPTION WHEN OTHERS THEN
    IF SQLERRM LIKE 'TEST FALLIDO%' THEN RAISE; END IF;
  END;

  -- tamaño fuera de P/M/G debe fallar
  BEGIN
    PERFORM sp_gzz_tip_pro_mant('ADICIONAR', 901::smallint, 'Tipo inválido', 'X');
    RAISE EXCEPTION 'TEST FALLIDO T02: tamaño X (no P/M/G) no lanzó error';
  EXCEPTION WHEN OTHERS THEN
    IF SQLERRM LIKE 'TEST FALLIDO%' THEN RAISE; END IF;
  END;

  PERFORM sp_gzz_etp_pro_mant('ADICIONAR', 901::smallint, 'Etapa de prueba', 12.50);
  IF (SELECT etp_tie_est FROM gzz_etp_pro WHERE etp_cod = 901) <> 12.50 THEN
    RAISE EXCEPTION 'TEST FALLIDO T02: no se guardó el tiempo estimado de la etapa';
  END IF;
  -- se elimina para no alterar el denominador del % de avance
  PERFORM sp_gzz_etp_pro_mant('ELIMINAR', 901::smallint, NULL, NULL);

  RAISE NOTICE 'OK T02: mantenimiento Grupo B y validaciones de columna extra';
END;
$t$;

-- ------------------------------------------------------------
-- T03. Clientes: FKs activas y coherencia de fechas
-- ------------------------------------------------------------
DO $t$
BEGIN
  PERFORM sp_cliente_mant('ADICIONAR', 9001, 'Cliente Prueba Test', 'EP', 'A',
                          DATE '2025-01-01', NULL, NULL);
  IF (SELECT cli_est_reg_cod FROM g1m_clientes WHERE cli_cod = 9001) <> 'A' THEN
    RAISE EXCEPTION 'TEST FALLIDO T03: el cliente nuevo no quedó activo';
  END IF;

  -- tipo de cliente inexistente debe fallar
  BEGIN
    PERFORM sp_cliente_mant('ADICIONAR', 9002, 'Cliente inválido', 'XX', 'A', NULL, NULL, NULL);
    RAISE EXCEPTION 'TEST FALLIDO T03: tipo de cliente inexistente no lanzó error';
  EXCEPTION WHEN OTHERS THEN
    IF SQLERRM LIKE 'TEST FALLIDO%' THEN RAISE; END IF;
  END;

  -- fecha de ingreso posterior al cese debe fallar
  BEGIN
    PERFORM sp_cliente_mant('ADICIONAR', 9002, 'Cliente inválido', 'EP', 'A',
                            DATE '2025-12-01', DATE '2025-01-01', NULL);
    RAISE EXCEPTION 'TEST FALLIDO T03: ingreso > cese no lanzó error';
  EXCEPTION WHEN OTHERS THEN
    IF SQLERRM LIKE 'TEST FALLIDO%' THEN RAISE; END IF;
  END;

  RAISE NOTICE 'OK T03: mantenimiento de clientes y sus validaciones';
END;
$t$;

-- ------------------------------------------------------------
-- T04. Personal: costo/hora > 0 y fecha de ingreso no futura
-- ------------------------------------------------------------
DO $t$
BEGIN
  BEGIN
    PERFORM sp_personal_mant('ADICIONAR', 9101, 'Persona Inválida', 1::smallint, 0, CURRENT_DATE);
    RAISE EXCEPTION 'TEST FALLIDO T04: costo/hora 0 no lanzó error';
  EXCEPTION WHEN OTHERS THEN
    IF SQLERRM LIKE 'TEST FALLIDO%' THEN RAISE; END IF;
  END;

  BEGIN
    PERFORM sp_personal_mant('ADICIONAR', 9101, 'Persona Inválida', 1::smallint, 50,
                             CURRENT_DATE + 1);
    RAISE EXCEPTION 'TEST FALLIDO T04: fecha de ingreso futura no lanzó error';
  EXCEPTION WHEN OTHERS THEN
    IF SQLERRM LIKE 'TEST FALLIDO%' THEN RAISE; END IF;
  END;

  PERFORM sp_personal_mant('ADICIONAR', 9101, 'Persona Prueba Uno', 1::smallint, 80.00,
                           DATE '2024-01-15');
  PERFORM sp_personal_mant('ADICIONAR', 9102, 'Persona Prueba Dos', 3::smallint, 55.00,
                           DATE '2024-06-01');
  RAISE NOTICE 'OK T04: mantenimiento de personal y sus validaciones';
END;
$t$;

-- ------------------------------------------------------------
-- T05. Autorizaciones (g1c_per_car): duplicado y reactivación
-- ------------------------------------------------------------
DO $t$
BEGIN
  PERFORM sp_per_car_mant('ADICIONAR', 9101, 1::smallint);   -- Líder de Proyecto

  -- adicionar la misma autorización activa debe fallar
  BEGIN
    PERFORM sp_per_car_mant('ADICIONAR', 9101, 1::smallint);
    RAISE EXCEPTION 'TEST FALLIDO T05: autorización duplicada activa no lanzó error';
  EXCEPTION WHEN OTHERS THEN
    IF SQLERRM LIKE 'TEST FALLIDO%' THEN RAISE; END IF;
  END;

  -- inactivar y re-ADICIONAR debe REACTIVAR (no fallar por PK duplicada)
  PERFORM sp_per_car_mant('INACTIVAR', 9101, 1::smallint);
  PERFORM sp_per_car_mant('ADICIONAR', 9101, 1::smallint);
  IF (SELECT per_car_pro_est_reg_cod FROM g1c_per_car
      WHERE per_cod = 9101 AND car_pro_cod = 1) <> 'A' THEN
    RAISE EXCEPTION 'TEST FALLIDO T05: re-ADICIONAR no reactivó la autorización';
  END IF;

  RAISE NOTICE 'OK T05: autorizaciones de cargo (duplicado y reactivación)';
END;
$t$;

-- ------------------------------------------------------------
-- T06–T13. Flujo completo de proyecto: creación, estados,
-- equipo, avance y vistas (un solo bloque para compartir pro_sec)
-- ------------------------------------------------------------
DO $t$
DECLARE
  v_sec1     SMALLINT;   -- proyecto de trabajo (equipo/avance)
  v_sec2     SMALLINT;   -- proyecto que se cierra
  v_num      NUMERIC;
  v_est_hor  NUMERIC;
  v_fec      DATE;
  v_txt      TEXT;
  v_cnt      INTEGER;
BEGIN
  -- T06: creación con secuencia correlativa y utilidad calculada
  v_sec1 := sp_proyecto_crear(9001, 1::smallint, DATE '2026-01-10', DATE '2026-12-10',
                              10000, 4000, 1000);
  v_sec2 := sp_proyecto_crear(9001, 1::smallint, DATE '2026-02-01', DATE '2026-11-30',
                              5000, 2000, 500);
  IF v_sec2 <> v_sec1 + 1 THEN
    RAISE EXCEPTION 'TEST FALLIDO T06: la secuencia no es correlativa (% luego %)', v_sec1, v_sec2;
  END IF;
  SELECT pro_uti_pre INTO v_num FROM g1t_pro_cab
  WHERE pro_cli_cod = 9001 AND pro_tip_cod = 1 AND pro_sec = v_sec1;
  IF v_num <> 5000 THEN
    RAISE EXCEPTION 'TEST FALLIDO T06: utilidad presupuestada esperada 5000, obtenida %', v_num;
  END IF;
  RAISE NOTICE 'OK T06: sp_proyecto_crear (secuencia MAX+1 y utilidad = monto-costo-gasto)';

  -- T07: el trigger de fechas rechaza contrato > pactada
  BEGIN
    PERFORM sp_proyecto_crear(9001, 1::smallint, DATE '2026-12-01', DATE '2026-06-01',
                              NULL, NULL, NULL);
    RAISE EXCEPTION 'TEST FALLIDO T07: fecha contrato > pactada no lanzó error';
  EXCEPTION WHEN OTHERS THEN
    IF SQLERRM LIKE 'TEST FALLIDO%' THEN RAISE; END IF;
  END;
  RAISE NOTICE 'OK T07: trg_procab_valida_fechas rechaza contrato > pactada';

  -- T08: matriz de transiciones y efectos del cierre
  BEGIN
    PERFORM sp_proyecto_cambiar_estado(9001, 1::smallint, v_sec2, '03');  -- 01->03 prohibida
    RAISE EXCEPTION 'TEST FALLIDO T08: transición 01->03 no lanzó error';
  EXCEPTION WHEN OTHERS THEN
    IF SQLERRM LIKE 'TEST FALLIDO%' THEN RAISE; END IF;
  END;

  PERFORM sp_proyecto_cambiar_estado(9001, 1::smallint, v_sec2, '02');
  SELECT pro_fec_ini INTO v_fec FROM g1t_pro_cab
  WHERE pro_cli_cod = 9001 AND pro_tip_cod = 1 AND pro_sec = v_sec2;
  IF v_fec IS NULL THEN
    RAISE EXCEPTION 'TEST FALLIDO T08: pasar a En Ejecución no fijó pro_fec_ini';
  END IF;

  PERFORM sp_proyecto_cambiar_estado(9001, 1::smallint, v_sec2, '03');
  PERFORM sp_proyecto_cambiar_estado(9001, 1::smallint, v_sec2, '04');
  SELECT cli_fec_ult_pro_cer INTO v_fec FROM g1m_clientes WHERE cli_cod = 9001;
  IF v_fec IS DISTINCT FROM CURRENT_DATE THEN
    RAISE EXCEPTION 'TEST FALLIDO T08: el cierre no actualizó cli_fec_ult_pro_cer del cliente';
  END IF;

  BEGIN
    PERFORM sp_proyecto_cambiar_estado(9001, 1::smallint, v_sec2, '02');  -- desde cerrado
    RAISE EXCEPTION 'TEST FALLIDO T08: transición desde Cerrado no lanzó error';
  EXCEPTION WHEN OTHERS THEN
    IF SQLERRM LIKE 'TEST FALLIDO%' THEN RAISE; END IF;
  END;
  RAISE NOTICE 'OK T08: matriz de estados 01->02->03->04, fechas automáticas y cierre en cliente';

  -- T09: asignación de equipo (autorización obligatoria, sin duplicados)
  PERFORM sp_proyecto_equipo_asignar(9001, 1::smallint, v_sec1, 9101, 1::smallint);

  BEGIN  -- 9102 no tiene autorización para el cargo 2
    PERFORM sp_proyecto_equipo_asignar(9001, 1::smallint, v_sec1, 9102, 2::smallint);
    RAISE EXCEPTION 'TEST FALLIDO T09: asignar sin autorización no lanzó error';
  EXCEPTION WHEN OTHERS THEN
    IF SQLERRM LIKE 'TEST FALLIDO%' THEN RAISE; END IF;
  END;

  BEGIN  -- ya asignado activo
    PERFORM sp_proyecto_equipo_asignar(9001, 1::smallint, v_sec1, 9101, 1::smallint);
    RAISE EXCEPTION 'TEST FALLIDO T09: asignación duplicada no lanzó error';
  EXCEPTION WHEN OTHERS THEN
    IF SQLERRM LIKE 'TEST FALLIDO%' THEN RAISE; END IF;
  END;

  BEGIN  -- proyecto cerrado no admite cambios de equipo
    PERFORM sp_proyecto_equipo_asignar(9001, 1::smallint, v_sec2, 9101, 1::smallint);
    RAISE EXCEPTION 'TEST FALLIDO T09: asignar a proyecto cerrado no lanzó error';
  EXCEPTION WHEN OTHERS THEN
    IF SQLERRM LIKE 'TEST FALLIDO%' THEN RAISE; END IF;
  END;
  RAISE NOTICE 'OK T09: asignación de equipo (autorización, duplicado, proyecto cerrado)';

  -- T10: registro de avance, autonumeración y rangos de tiempo
  v_num := sp_proyecto_avance_registrar(9001, 1::smallint, v_sec1, 9101, 1::smallint,
                                        1::smallint, CURRENT_DATE, 8::smallint, 0::smallint);
  IF v_num <> 1 THEN
    RAISE EXCEPTION 'TEST FALLIDO T10: primer movimiento debía tener sec_etp=1, obtuvo %', v_num;
  END IF;
  v_num := sp_proyecto_avance_registrar(9001, 1::smallint, v_sec1, 9101, 1::smallint,
                                        1::smallint, CURRENT_DATE, 2::smallint, 30::smallint);
  IF v_num <> 2 THEN
    RAISE EXCEPTION 'TEST FALLIDO T10: el trigger no autonumeró sec_etp=2, obtuvo %', v_num;
  END IF;

  BEGIN  -- horas fuera de rango
    PERFORM sp_proyecto_avance_registrar(9001, 1::smallint, v_sec1, 9101, 1::smallint,
                                         1::smallint, CURRENT_DATE, 24::smallint, 0::smallint);
    RAISE EXCEPTION 'TEST FALLIDO T10: 24 horas no lanzó error';
  EXCEPTION WHEN OTHERS THEN
    IF SQLERRM LIKE 'TEST FALLIDO%' THEN RAISE; END IF;
  END;

  BEGIN  -- tiempo cero
    PERFORM sp_proyecto_avance_registrar(9001, 1::smallint, v_sec1, 9101, 1::smallint,
                                         1::smallint, CURRENT_DATE, 0::smallint, 0::smallint);
    RAISE EXCEPTION 'TEST FALLIDO T10: tiempo 0h0m no lanzó error';
  EXCEPTION WHEN OTHERS THEN
    IF SQLERRM LIKE 'TEST FALLIDO%' THEN RAISE; END IF;
  END;

  BEGIN  -- persona no asignada al proyecto
    PERFORM sp_proyecto_avance_registrar(9001, 1::smallint, v_sec1, 9102, 3::smallint,
                                         1::smallint, CURRENT_DATE, 1::smallint, 0::smallint);
    RAISE EXCEPTION 'TEST FALLIDO T10: horas de persona no asignada no lanzó error';
  EXCEPTION WHEN OTHERS THEN
    IF SQLERRM LIKE 'TEST FALLIDO%' THEN RAISE; END IF;
  END;
  RAISE NOTICE 'OK T10: registro de avance (autonumeración sec_etp y rangos de tiempo)';

  -- T11: baja lógica del equipo bloquea nuevas horas; reactivar las permite
  PERFORM sp_proyecto_equipo_quitar(9001, 1::smallint, v_sec1, 9101, 1::smallint);
  BEGIN
    PERFORM sp_proyecto_avance_registrar(9001, 1::smallint, v_sec1, 9101, 1::smallint,
                                         1::smallint, CURRENT_DATE, 1::smallint, 0::smallint);
    RAISE EXCEPTION 'TEST FALLIDO T11: horas de miembro retirado no lanzó error';
  EXCEPTION WHEN OTHERS THEN
    IF SQLERRM LIKE 'TEST FALLIDO%' THEN RAISE; END IF;
  END;
  PERFORM sp_proyecto_equipo_reactivar(9001, 1::smallint, v_sec1, 9101, 1::smallint);
  RAISE NOTICE 'OK T11: quitar/reactivar miembro del equipo (historial conservado)';

  -- T12: vistas — resumen, equipo y avance con cálculo verificado
  SELECT cliente_nombre INTO v_txt FROM v_proyecto_resumen
  WHERE pro_cli_cod = 9001 AND pro_tip_cod = 1 AND pro_sec = v_sec1;
  IF v_txt IS DISTINCT FROM 'Cliente Prueba Test' THEN
    RAISE EXCEPTION 'TEST FALLIDO T12: v_proyecto_resumen no resolvió el nombre del cliente';
  END IF;

  SELECT COUNT(*) INTO v_cnt FROM v_proyecto_equipo
  WHERE pro_cli_cod = 9001 AND pro_tip_cod = 1 AND pro_sec = v_sec1
    AND per_cod = 9101 AND pro_per_car_est_reg_cod = 'A';
  IF v_cnt <> 1 THEN
    RAISE EXCEPTION 'TEST FALLIDO T12: v_proyecto_equipo no muestra al miembro activo';
  END IF;

  -- horas trabajadas = 8h + 2h30m = 10.50; el % se contrasta contra
  -- el denominador real (etapas activas) para no depender del seed exacto
  SELECT horas_trabajadas, pct_avance, horas_estimadas
    INTO v_num, v_txt, v_est_hor
  FROM v_proyecto_avance
  WHERE pro_cli_cod = 9001 AND pro_tip_cod = 1 AND pro_sec = v_sec1;
  IF v_num <> 10.50 THEN
    RAISE EXCEPTION 'TEST FALLIDO T12: horas trabajadas esperadas 10.50, obtenidas %', v_num;
  END IF;
  IF v_txt::numeric <> ROUND(10.50 / NULLIF(v_est_hor, 0) * 100, 2) THEN
    RAISE EXCEPTION 'TEST FALLIDO T12: pct_avance % no coincide con 10.50/%*100', v_txt, v_est_hor;
  END IF;
  IF v_txt::numeric <> fn_proyecto_pct_avance(9001, 1::smallint, v_sec1) THEN
    RAISE EXCEPTION 'TEST FALLIDO T12: la vista y fn_proyecto_pct_avance no coinciden';
  END IF;
  RAISE NOTICE 'OK T12: vistas v_proyecto_resumen / v_proyecto_equipo / v_proyecto_avance';

  -- T13: el personal ya asignado activo no se ofrece como disponible
  SELECT COUNT(*) INTO v_cnt
  FROM fn_personal_disponible_proyecto(9001, 1::smallint, v_sec1)
  WHERE per_cod = 9101 AND car_pro_cod = 1;
  IF v_cnt <> 0 THEN
    RAISE EXCEPTION 'TEST FALLIDO T13: fn_personal_disponible ofrece a alguien ya asignado';
  END IF;
  RAISE NOTICE 'OK T13: fn_personal_disponible_proyecto excluye asignaciones activas';
END;
$t$;

-- ------------------------------------------------------------
-- T14. Autenticación de la cuenta admin (fn_usuario_autenticar)
-- ------------------------------------------------------------
DO $t$
BEGIN
  IF NOT fn_usuario_autenticar('admi', 'testpass123') THEN
    RAISE EXCEPTION 'TEST FALLIDO T14: la contraseña correcta de admi no autenticó';
  END IF;
  IF fn_usuario_autenticar('admi', 'clave_incorrecta') THEN
    RAISE EXCEPTION 'TEST FALLIDO T14: una contraseña incorrecta autenticó';
  END IF;
  IF fn_usuario_autenticar('usuario_inexistente', 'lo_que_sea') THEN
    RAISE EXCEPTION 'TEST FALLIDO T14: un login inexistente autenticó';
  END IF;

  -- un usuario inactivo no debe poder autenticarse aunque la contraseña sea correcta
  UPDATE g1s_usuario SET usu_est_reg_cod = 'I' WHERE usu_login = 'admi';
  IF fn_usuario_autenticar('admi', 'testpass123') THEN
    RAISE EXCEPTION 'TEST FALLIDO T14: un usuario inactivo pudo autenticarse';
  END IF;
  UPDATE g1s_usuario SET usu_est_reg_cod = 'A' WHERE usu_login = 'admi';

  RAISE NOTICE 'OK T14: fn_usuario_autenticar (correcta, incorrecta, inexistente, inactiva)';
END;
$t$;

-- Nada de lo anterior se persiste: la BD queda exactamente como estaba.
ROLLBACK;

\echo ''
\echo '>>> TODOS LOS TESTS PASARON (los datos de prueba fueron revertidos con ROLLBACK) <<<'
