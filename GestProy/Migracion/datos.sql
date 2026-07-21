-- =====================================================================
-- Migración MySQL -> PostgreSQL - Datos
-- Base de datos origen: proyectos
-- Insertar en este orden respeta las foreign keys
-- =====================================================================

BEGIN;

-- Estados de registro (gzz_est_reg)
INSERT INTO "gzz_est_reg" ("EstRegCod", "EstRegDes", "EstRegEstReg") VALUES ('*','Eliminado','A'),('A','Activo','A'),('I','Inactivo','A');

-- Cargos de personal (gzz_car_per)
INSERT INTO "gzz_car_per" ("CarPerCod", "CarPerDes", "CarPerEstReg") VALUES (1,'Gerente de Proyectos','A'),(2,'Analista de Sistemas','A'),(3,'Desarrollador Senior','A'),(4,'Desarrollador Junior','A'),(5,'Arquitecto de Software','A'),(6,'Tester QA','A'),(7,'Administrador de BD','A'),(8,'Scrum Master','A'),(9,'Disenador UX/UI','A'),(10,'Consultor de Negocios','A');

-- Cargos de proyecto (gzz_car_pro)
INSERT INTO "gzz_car_pro" ("CarProCod", "CarProDes", "CarProEstReg") VALUES (1,'Jefe de Proyecto','A'),(2,'Analista Funcional','A'),(3,'Desarrollador','A'),(4,'Tester','A'),(5,'Arquitecto','A'),(6,'Consultor Senior','A'),(7,'Soporte Tecnico','A');

-- Estados de cliente (gzz_est_cli)
INSERT INTO "gzz_est_cli" ("EstCliCod", "EstCliDes", "EstCliEstReg") VALUES ('A','Activo','A'),('B','Baja','A'),('S','Suspendido','A');

-- Estados de proyecto (gzz_est_pro)
INSERT INTO "gzz_est_pro" ("EstProCod", "EstProDes", "EstProEstReg") VALUES ('CA','Cancelado','A'),('CE','Cerrado','A'),('EJ','En Ejecucion','A'),('PL','Planificacion','A'),('SU','Suspendido','A');

-- Etapas de proyecto (gzz_etp_pro)
INSERT INTO "gzz_etp_pro" ("EtpCod", "EtpDes", "EtpTieEst", "EtpEstReg") VALUES (1,'Levantamiento de Requisitos',10.00,'A'),(2,'Analisis y Diseno',15.00,'A'),(3,'Desarrollo',30.00,'A'),(4,'Pruebas y QA',10.00,'A'),(5,'Implementacion',5.00,'A'),(6,'Capacitacion',4.00,'A'),(7,'Cierre y Entrega',3.00,'A');

-- Lineas de proyecto (gzz_lin_pro)
INSERT INTO "gzz_lin_pro" ("LinProCod", "LinProNom", "LinProTam", "LinProEstRegCod") VALUES (1,'Tecnologia de Informacion','G','A'),(2,'Consultoria de Negocios','M','A'),(3,'Infraestructura y Redes','G','A'),(4,'Capacitacion Corporativa','P','A'),(5,'Investigacion y Desarrollo','M','A');

-- Tipos de cliente (gzz_tip_cli)
INSERT INTO "gzz_tip_cli" ("TipCliCod", "TipCliDes", "TipCliEstReg") VALUES ('GO','Entidad de Gobierno','A'),('OI','Organismo Internacional','A'),('PJ','Persona Juridica','A'),('PN','Persona Natural','A');

-- Tipos de proyecto (gzz_tip_pro)
INSERT INTO "gzz_tip_pro" ("TipProCod", "TipProDes", "TipProTam", "TipProEstReg") VALUES (1,'Desarrollo de Software','G','A'),(2,'Consultoria Empresarial','M','A'),(3,'Implementacion de Sistemas','G','A'),(4,'Mantenimiento de Aplicaciones','P','A'),(5,'Auditoria de Sistemas','M','A'),(6,'Migracion de Datos','M','A'),(7,'Integracion de Plataformas','G','A'),(8,'Soporte Tecnico','P','A'),(9,'Analisis de Requerimientos','P','A'),(10,'Capacitacion y Formacion','P','A');

-- Personal (g1m_personal)
INSERT INTO "g1m_personal" ("PerCod", "PerNom", "PerCarCod", "PerCosHor", "PerFecIng", "PerEstReg") VALUES (101,'Ana Garcia Ramos',1,55.00,'2017-03-01','A'),(102,'Luis Torres Medina',2,38.00,'2018-06-15','A'),(103,'Maria Lopez Flores',3,45.00,'2019-01-10','A'),(104,'Carlos Vargas Cuadros',4,28.00,'2021-03-20','A'),(105,'Rosa Mamani Apaza',5,60.00,'2016-09-05','A'),(106,'Jorge Cardenas Rios',6,25.00,'2022-07-01','A'),(107,'Elena Caceres Pinto',7,42.00,'2018-11-12','A'),(108,'Pedro Salas Huanca',8,50.00,'2019-05-18','A'),(109,'Sofia Paredes Lara',9,32.00,'2023-02-14','A'),(110,'Marcos Quiroz Valdivia',10,65.00,'2015-04-22','A');

-- Clientes (g1m_clientes)
INSERT INTO "g1m_clientes" ("CliCod", "CliNom", "CliTipCod", "CliFecIng", "CliFecCes", "CliFecUltProCer", "CliEstCod", "CliEstRegCod") VALUES (1001,'Banco Continental S.A.','PJ','2019-03-15',NULL,'2024-06-30','A','A'),(1002,'Municipalidad de Arequipa','GO','2018-07-01',NULL,'2023-11-20','A','A'),(1003,'Minera del Sur S.A.C.','PJ','2021-02-10',NULL,NULL,'A','A'),(1004,'Universidad Nacional San Agustin','GO','2017-08-20',NULL,'2024-03-01','A','A'),(1005,'Clinica San Pablo S.A.C.','PJ','2022-01-05',NULL,NULL,'A','A'),(1006,'Gobierno Regional de Arequipa','GO','2016-06-01',NULL,'2023-09-15','A','A'),(1007,'Tech Solutions Peru S.A.C.','PJ','2020-11-12',NULL,NULL,'A','A'),(1008,'Agroindustrial Andina S.A.','PJ','2023-04-18',NULL,NULL,'A','A'),(1009,'Carlos Mendoza Quispe','PN','2024-01-10',NULL,NULL,'A','A'),(1010,'Instituto Superior Tecnologico AQP','GO','2020-05-25',NULL,'2023-06-30','S','A');

-- Personal-Cargo (g1c_per_car)
INSERT INTO "g1c_per_car" ("PerCod", "CarProCod", "PerCarProEstReg") VALUES (101,1,'A'),(102,2,'A'),(103,3,'A'),(104,3,'A'),(105,5,'A'),(106,4,'A'),(107,3,'A'),(107,7,'A'),(108,1,'A'),(109,2,'A'),(110,6,'A');

-- Proyectos (cabecera) (g1t_pro_cab)
INSERT INTO "g1t_pro_cab" ("ProCliCod", "ProTipCod", "ProSec", "ProFecCon", "ProFecPac", "ProFecIni", "ProFecEnt", "ProFecCer", "ProMonPre", "ProMonRea", "ProCosPre", "ProCosRea", "ProGasPre", "ProGasRea", "ProUtiPre", "ProUtiRea", "ProEstCod", "ProEstRegCod") VALUES (1001,1,1,'2024-01-10','2024-12-31','2024-02-01',NULL,NULL,180000.00,95000.00,120000.00,68000.00,15000.00,9500.00,45000.00,17500.00,'EJ','A'),(1001,2,1,'2023-03-01','2023-08-31','2023-04-01','2023-08-20','2023-09-05',50000.00,48500.00,30000.00,31200.00,5000.00,4800.00,15000.00,12500.00,'CE','A'),(1002,3,1,'2024-03-15','2025-03-14','2024-04-01',NULL,NULL,250000.00,110000.00,170000.00,78000.00,20000.00,12000.00,60000.00,20000.00,'EJ','A'),(1003,6,1,'2025-01-20','2025-07-31',NULL,NULL,NULL,75000.00,NULL,50000.00,NULL,8000.00,NULL,17000.00,NULL,'PL','A'),(1004,1,1,'2023-08-01','2024-07-31','2023-09-01',NULL,NULL,120000.00,88000.00,80000.00,62000.00,10000.00,8500.00,30000.00,17500.00,'EJ','A'),(1004,10,1,'2023-01-10','2023-04-30','2023-02-01','2023-04-28','2023-05-03',18000.00,17500.00,11000.00,10800.00,2000.00,1900.00,5000.00,4800.00,'CE','A'),(1005,4,1,'2024-06-01','2024-11-30','2024-06-15',NULL,NULL,35000.00,18000.00,22000.00,12500.00,3000.00,2200.00,10000.00,3300.00,'EJ','A'),(1006,7,1,'2024-02-01','2025-01-31','2024-03-01',NULL,NULL,200000.00,130000.00,140000.00,92000.00,18000.00,14000.00,42000.00,24000.00,'EJ','A'),(1007,1,1,'2025-02-10','2025-09-30',NULL,NULL,NULL,90000.00,NULL,60000.00,NULL,8000.00,NULL,22000.00,NULL,'PL','A'),(1008,5,1,'2023-05-01','2023-10-31','2023-05-15','2023-10-25','2023-11-02',40000.00,38000.00,25000.00,24500.00,4000.00,3800.00,11000.00,9700.00,'CE','A');

-- Equipos de proyecto (g1t_pro_eqp)
INSERT INTO "g1t_pro_eqp" ("ProCliCod", "ProTipCod", "ProSec", "PerCod", "CarProCod", "ProPerCarEstRegCod") VALUES (1001,1,1,101,1,'A'),(1001,1,1,102,2,'A'),(1001,1,1,103,3,'A'),(1001,1,1,104,3,'A'),(1001,1,1,106,4,'A'),(1001,2,1,102,2,'A'),(1001,2,1,110,6,'A'),(1002,3,1,103,3,'A'),(1002,3,1,106,4,'A'),(1002,3,1,107,3,'A'),(1002,3,1,108,1,'A'),(1002,3,1,109,2,'A'),(1004,1,1,101,1,'A'),(1004,1,1,104,3,'A'),(1004,1,1,105,5,'A'),(1004,1,1,106,4,'A'),(1004,10,1,109,2,'A'),(1004,10,1,110,6,'A'),(1005,4,1,107,7,'A'),(1005,4,1,108,1,'A'),(1006,7,1,101,1,'A'),(1006,7,1,103,3,'A'),(1006,7,1,105,5,'A'),(1006,7,1,107,7,'A'),(1008,5,1,102,2,'A'),(1008,5,1,110,6,'A');

-- Movimientos de proyecto (g1t_pro_mov)
INSERT INTO "g1t_pro_mov" ("ProCliCod", "ProTipCod", "ProSec", "PerCod", "CarProCod", "EtpCod", "SecEtp", "FecRegEtp", "HorTraEtp", "MinTraEtp", "EstRegCod") VALUES (1001,1,1,101,1,1,1,'2024-02-01',8,0,'A'),(1001,1,1,101,1,1,2,'2024-02-02',8,30,'A'),(1001,1,1,102,2,1,1,'2024-02-01',8,0,'A'),(1001,1,1,102,2,2,1,'2024-02-12',8,0,'A'),(1001,1,1,102,2,2,2,'2024-02-13',7,30,'A'),(1001,1,1,103,3,3,1,'2024-03-01',8,0,'A'),(1001,1,1,103,3,3,2,'2024-03-04',8,0,'A'),(1001,1,1,104,3,3,1,'2024-03-01',8,0,'A'),(1001,1,1,104,3,3,2,'2024-03-05',6,45,'A'),(1001,1,1,106,4,4,1,'2024-06-10',8,0,'A'),(1001,1,1,106,4,4,2,'2024-06-11',7,0,'A'),(1002,3,1,103,3,3,1,'2024-05-01',8,0,'A'),(1002,3,1,103,3,3,2,'2024-05-02',8,0,'A'),(1002,3,1,106,4,4,1,'2024-08-01',8,0,'A'),(1002,3,1,107,3,3,1,'2024-05-01',8,0,'A'),(1002,3,1,108,1,1,1,'2024-04-01',8,0,'A'),(1002,3,1,109,2,1,1,'2024-04-01',8,0,'A'),(1002,3,1,109,2,2,1,'2024-04-15',8,0,'A'),(1004,1,1,101,1,1,1,'2023-09-01',8,0,'A'),(1004,1,1,104,3,3,1,'2023-10-01',8,0,'A'),(1004,1,1,104,3,3,2,'2023-10-03',8,0,'A'),(1004,1,1,105,5,2,1,'2023-09-15',8,0,'A'),(1004,1,1,105,5,2,2,'2023-09-18',6,0,'A'),(1004,1,1,106,4,4,1,'2024-01-10',8,0,'A'),(1004,10,1,109,2,6,1,'2023-02-01',6,0,'A'),(1004,10,1,110,6,6,1,'2023-02-01',8,0,'A'),(1004,10,1,110,6,6,2,'2023-02-08',8,0,'A'),(1004,10,1,110,6,7,1,'2023-04-25',4,0,'A'),(1005,4,1,107,7,3,1,'2024-07-01',8,0,'A'),(1005,4,1,107,7,3,2,'2024-07-03',7,30,'A'),(1005,4,1,108,1,1,1,'2024-06-15',6,0,'A'),(1006,7,1,101,1,1,1,'2024-03-01',8,0,'A'),(1006,7,1,103,3,3,1,'2024-04-01',8,0,'A'),(1006,7,1,105,5,2,1,'2024-03-15',8,0,'A'),(1006,7,1,107,7,5,1,'2024-09-01',8,0,'A'),(1008,5,1,102,2,2,1,'2023-06-02',7,0,'A'),(1008,5,1,110,6,1,1,'2023-05-15',8,0,'A'),(1008,5,1,110,6,2,1,'2023-06-01',8,0,'A'),(1008,5,1,110,6,7,1,'2023-10-20',4,0,'A');

COMMIT;
