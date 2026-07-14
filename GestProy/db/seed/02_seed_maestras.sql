-- ============================================================
--  GestProy — Datos semilla
--  02_seed_maestras.sql : clientes, personal y autorizaciones
--  de cargo por persona (g1c_per_car).
--  Requiere: 01_seed_referenciales.sql
-- ============================================================
BEGIN;

-- Clientes
INSERT INTO g1m_clientes
  (cli_cod, cli_nom, cli_tip_cod, cli_fec_ing, cli_fec_ces, cli_fec_ult_pro_cer, cli_est_cod, cli_est_reg_cod)
VALUES
  (1001, 'Corporación Andina SAC',      'EP', DATE '2024-03-15', NULL, NULL, 'A', 'A'),
  (1002, 'Municipalidad de Arequipa',   'EE', DATE '2023-08-01', NULL, NULL, 'A', 'A'),
  (1003, 'ONG Ayuda y Desarrollo',      'ON', DATE '2024-11-20', NULL, NULL, 'A', 'A'),
  (1004, 'Textiles del Sur EIRL',       'EP', DATE '2025-02-10', NULL, NULL, 'A', 'A'),
  (1005, 'Juan Pérez Consultores',      'PN', DATE '2025-06-05', NULL, NULL, 'A', 'A')
ON CONFLICT (cli_cod) DO NOTHING;

-- Personal
INSERT INTO g1m_personal
  (per_cod, per_nom, per_car_cod, per_cos_hor, per_fec_ing, per_est_reg_cod)
VALUES
  (2001, 'María Quispe Huamán',    1, 95.00, DATE '2022-01-10', 'A'),
  (2002, 'Carlos Mamani Flores',   2, 70.00, DATE '2022-07-01', 'A'),
  (2003, 'Lucía Condori Apaza',    3, 55.00, DATE '2023-03-15', 'A'),
  (2004, 'Jorge Choque Ramos',     3, 50.00, DATE '2023-09-01', 'A'),
  (2005, 'Ana Ccama Torres',       4, 48.00, DATE '2024-02-20', 'A'),
  (2006, 'Pedro Villanueva Soto',  5, 45.00, DATE '2024-08-11', 'A')
ON CONFLICT (per_cod) DO NOTHING;

-- Autorizaciones persona <-> cargo de proyecto
-- (qué roles puede ejercer cada persona dentro de un proyecto)
INSERT INTO g1c_per_car (per_cod, car_pro_cod, per_car_pro_est_reg_cod) VALUES
  (2001, 1, 'A'),   -- María: Líder de Proyecto
  (2001, 2, 'A'),   -- María: Analista
  (2002, 2, 'A'),   -- Carlos: Analista
  (2002, 4, 'A'),   -- Carlos: Documentador
  (2003, 3, 'A'),   -- Lucía: Desarrollador
  (2004, 3, 'A'),   -- Jorge: Desarrollador
  (2005, 4, 'A'),   -- Ana: Documentador
  (2006, 5, 'A')    -- Pedro: Control de Calidad
ON CONFLICT (per_cod, car_pro_cod) DO NOTHING;

COMMIT;
