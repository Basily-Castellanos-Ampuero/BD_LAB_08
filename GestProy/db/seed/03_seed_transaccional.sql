-- ============================================================
--  GestProy — Datos semilla
--  03_seed_transaccional.sql : proyectos de ejemplo con equipo
--  y movimientos de avance, para probar v_proyecto_avance de
--  punta a punta sin pasar por la interfaz.
--  Requiere: 01 y 02 de seed, triggers y funciones aplicados.
--  Los sec_etp van explícitos para que ON CONFLICT DO NOTHING
--  mantenga el script idempotente (si fueran NULL, el trigger
--  autonumeraría y cada re-ejecución duplicaría movimientos).
-- ============================================================
BEGIN;

-- Proyecto 1: Corporación Andina (1001), Desarrollo de Software (1), sec 1 — en ejecución
INSERT INTO g1t_pro_cab
  (pro_cli_cod, pro_tip_cod, pro_sec, pro_fec_con, pro_fec_pac, pro_fec_ini,
   pro_mon_pre, pro_cos_pre, pro_gas_pre, pro_uti_pre, pro_est_cod, pro_est_reg_cod)
VALUES
  (1001, 1, 1, DATE '2026-04-01', DATE '2026-10-30', DATE '2026-04-15',
   85000.00, 52000.00, 8000.00, 25000.00, '02', 'A')
ON CONFLICT (pro_cli_cod, pro_tip_cod, pro_sec) DO NOTHING;

-- Proyecto 2: Municipalidad de Arequipa (1002), Implementación de Sistemas (3), sec 1 — planificado
INSERT INTO g1t_pro_cab
  (pro_cli_cod, pro_tip_cod, pro_sec, pro_fec_con, pro_fec_pac,
   pro_mon_pre, pro_cos_pre, pro_gas_pre, pro_uti_pre, pro_est_cod, pro_est_reg_cod)
VALUES
  (1002, 3, 1, DATE '2026-06-10', DATE '2026-12-15',
   120000.00, 78000.00, 12000.00, 30000.00, '01', 'A')
ON CONFLICT (pro_cli_cod, pro_tip_cod, pro_sec) DO NOTHING;

-- Equipo del proyecto 1 (los triggers exigen autorización activa en g1c_per_car)
INSERT INTO g1t_pro_eqp
  (pro_cli_cod, pro_tip_cod, pro_sec, per_cod, car_pro_cod, pro_per_car_est_reg_cod)
VALUES
  (1001, 1, 1, 2001, 1, 'A'),   -- María, Líder de Proyecto
  (1001, 1, 1, 2002, 2, 'A'),   -- Carlos, Analista
  (1001, 1, 1, 2003, 3, 'A'),   -- Lucía, Desarrollador
  (1001, 1, 1, 2006, 5, 'A')    -- Pedro, Control de Calidad
ON CONFLICT (pro_cli_cod, pro_tip_cod, pro_sec, per_cod, car_pro_cod) DO NOTHING;

-- Movimientos de avance del proyecto 1
INSERT INTO g1t_pro_mov
  (pro_cli_cod, pro_tip_cod, pro_sec, per_cod, car_pro_cod,
   etp_cod, sec_etp, fec_reg_etp, hor_tra_etp, min_tra_etp, est_reg_cod)
VALUES
  -- Etapa 1: Análisis (Carlos, Analista)
  (1001, 1, 1, 2002, 2, 1, 1, DATE '2026-04-16', 8, 0,  'A'),
  (1001, 1, 1, 2002, 2, 1, 2, DATE '2026-04-17', 7, 30, 'A'),
  (1001, 1, 1, 2002, 2, 1, 3, DATE '2026-04-20', 8, 0,  'A'),
  -- Etapa 1: Análisis (María, Líder)
  (1001, 1, 1, 2001, 1, 1, 1, DATE '2026-04-16', 4, 0,  'A'),
  -- Etapa 2: Diseño (María, Líder)
  (1001, 1, 1, 2001, 1, 2, 1, DATE '2026-05-04', 6, 0,  'A'),
  -- Etapa 3: Desarrollo (Lucía, Desarrollador)
  (1001, 1, 1, 2003, 3, 3, 1, DATE '2026-05-18', 8, 0,  'A'),
  (1001, 1, 1, 2003, 3, 3, 2, DATE '2026-05-19', 8, 0,  'A'),
  (1001, 1, 1, 2003, 3, 3, 3, DATE '2026-05-20', 6, 45, 'A'),
  -- Etapa 4: Pruebas (Pedro, QA)
  (1001, 1, 1, 2006, 5, 4, 1, DATE '2026-06-22', 5, 15, 'A')
ON CONFLICT (pro_cli_cod, pro_tip_cod, pro_sec, per_cod, car_pro_cod, etp_cod, sec_etp) DO NOTHING;

COMMIT;
