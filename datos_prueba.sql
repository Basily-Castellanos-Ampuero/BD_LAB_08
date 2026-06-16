USE gestionProyectos;

-- Tabla prerequisito: estados de registro
INSERT IGNORE INTO GZZ_EST_REG (EstRegCod, EstRegDes, EstRegEstReg) VALUES
  ('A', 'Activo',   'A'),
  ('I', 'Inactivo', 'A'),
  ('*', 'Eliminado','A');

-- Datos de prueba para GZZ_TIP_PRO
-- TipProTam: P=Pequeño, M=Mediano, G=Grande
INSERT INTO GZZ_TIP_PRO (TipProCod, TipProDes, TipProTam, TipProEstReg) VALUES
  (1,  'Desarrollo de Software',          'G', 'A'),
  (2,  'Consultoría Empresarial',         'M', 'A'),
  (3,  'Implementación de Sistemas',      'G', 'A'),
  (4,  'Mantenimiento de Aplicaciones',   'P', 'A'),
  (5,  'Auditoría de Sistemas',           'M', 'A'),
  (6,  'Migración de Datos',              'M', 'A'),
  (7,  'Integración de Plataformas',      'G', 'A'),
  (8,  'Soporte Técnico',                 'P', 'A'),
  (9,  'Análisis de Requerimientos',      'P', 'A'),
  (10, 'Capacitación y Formación',        'P', 'A');

SELECT TipProCod, TipProDes, TipProTam, TipProEstReg FROM GZZ_TIP_PRO ORDER BY TipProCod;
