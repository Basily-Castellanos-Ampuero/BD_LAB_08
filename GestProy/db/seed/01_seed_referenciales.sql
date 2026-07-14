-- ============================================================
--  GestProy — Datos semilla
--  01_seed_referenciales.sql : catálogos (tablas GZZ_*)
--  Extiende datos_prueba.sql del laboratorio anterior (que solo
--  cubría gzz_est_reg y gzz_tip_pro) a los 9 catálogos.
--  INSERT IGNORE (MySQL) -> ON CONFLICT DO NOTHING (PostgreSQL):
--  el script es idempotente, se puede re-ejecutar sin error.
-- ============================================================
BEGIN;

-- Estados de registro: base de la eliminación lógica en todo el esquema
INSERT INTO gzz_est_reg (est_reg_cod, est_reg_des, est_reg_est_reg) VALUES
  ('A', 'Activo',    'A'),
  ('I', 'Inactivo',  'A'),
  ('*', 'Eliminado', 'A')
ON CONFLICT (est_reg_cod) DO NOTHING;

-- Tipos de cliente
INSERT INTO gzz_tip_cli (tip_cli_cod, tip_cli_des, tip_cli_est_reg) VALUES
  ('EP', 'Empresa Privada',  'A'),
  ('EE', 'Empresa Estatal',  'A'),
  ('ON', 'ONG',              'A'),
  ('PN', 'Persona Natural',  'A')
ON CONFLICT (tip_cli_cod) DO NOTHING;

-- Estados de cliente
INSERT INTO gzz_est_cli (est_cli_cod, est_cli_des, est_cli_est_reg) VALUES
  ('A', 'Activo',     'A'),
  ('S', 'Suspendido', 'A'),
  ('C', 'Cesado',     'A')
ON CONFLICT (est_cli_cod) DO NOTHING;

-- Estados de proyecto (la matriz de transiciones vive en
-- sp_proyecto_cambiar_estado: 01->02/05, 02->03/05, 05->02, 03->04)
INSERT INTO gzz_est_pro (est_pro_cod, est_pro_des, est_pro_est_reg) VALUES
  ('01', 'Planificado',  'A'),
  ('02', 'En Ejecución', 'A'),
  ('03', 'Entregado',    'A'),
  ('04', 'Cerrado',      'A'),
  ('05', 'Suspendido',   'A')
ON CONFLICT (est_pro_cod) DO NOTHING;

-- Cargos del personal (cargo de planilla)
INSERT INTO gzz_car_per (car_per_cod, car_per_des, car_per_est_reg) VALUES
  (1, 'Jefe de Proyecto',     'A'),
  (2, 'Analista de Sistemas', 'A'),
  (3, 'Programador',          'A'),
  (4, 'Diseñador',            'A'),
  (5, 'Tester QA',            'A')
ON CONFLICT (car_per_cod) DO NOTHING;

-- Cargos en el proyecto (rol que ejerce dentro de un proyecto)
INSERT INTO gzz_car_pro (car_pro_cod, car_pro_des, car_pro_est_reg) VALUES
  (1, 'Líder de Proyecto',  'A'),
  (2, 'Analista',           'A'),
  (3, 'Desarrollador',      'A'),
  (4, 'Documentador',       'A'),
  (5, 'Control de Calidad', 'A')
ON CONFLICT (car_pro_cod) DO NOTHING;

-- Líneas de proyecto
INSERT INTO gzz_lin_pro (lin_pro_cod, lin_pro_nom, lin_pro_tam, lin_pro_est_reg_cod) VALUES
  (1, 'Sistemas de Información',      'G', 'A'),
  (2, 'Infraestructura TI',           'M', 'A'),
  (3, 'Consultoría de Procesos',      'M', 'A'),
  (4, 'Analítica de Datos',           'G', 'A'),
  (5, 'Soporte y Mantenimiento',      'P', 'A')
ON CONFLICT (lin_pro_cod) DO NOTHING;

-- Tipos de proyecto (mismos 10 registros de datos_prueba.sql)
INSERT INTO gzz_tip_pro (tip_pro_cod, tip_pro_des, tip_pro_tam, tip_pro_est_reg) VALUES
  (1,  'Desarrollo de Software',        'G', 'A'),
  (2,  'Consultoría Empresarial',       'M', 'A'),
  (3,  'Implementación de Sistemas',    'G', 'A'),
  (4,  'Mantenimiento de Aplicaciones', 'P', 'A'),
  (5,  'Auditoría de Sistemas',         'M', 'A'),
  (6,  'Migración de Datos',            'M', 'A'),
  (7,  'Integración de Plataformas',    'G', 'A'),
  (8,  'Soporte Técnico',               'P', 'A'),
  (9,  'Análisis de Requerimientos',    'P', 'A'),
  (10, 'Capacitación y Formación',      'P', 'A')
ON CONFLICT (tip_pro_cod) DO NOTHING;

-- Etapas de proyecto con su tiempo estimado en horas
-- (total: 200 horas — denominador del % de avance)
INSERT INTO gzz_etp_pro (etp_cod, etp_des, etp_tie_est, etp_est_reg) VALUES
  (1, 'Análisis',       40.00, 'A'),
  (2, 'Diseño',         32.00, 'A'),
  (3, 'Desarrollo',     80.00, 'A'),
  (4, 'Pruebas',        24.00, 'A'),
  (5, 'Implantación',   16.00, 'A'),
  (6, 'Capacitación',    8.00, 'A')
ON CONFLICT (etp_cod) DO NOTHING;

COMMIT;
