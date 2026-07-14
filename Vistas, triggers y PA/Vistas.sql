CREATE OR REPLACE VIEW VI_PERSONAL_COMPLETO AS
SELECT 
    p.PerCod      AS 'Código',
    p.PerNom      AS 'Nombre',
    cp.CarPerDes  AS 'Cargo',
    p.PerCosHor   AS 'Costo x Hora',
    p.PerFecIng   AS 'Fecha Ingreso',
    er.EstRegDes  AS 'Estado Registro'
FROM G1M_PERSONAL p
JOIN GZZ_CAR_PER  cp ON p.PerCarCod = cp.CarPerCod
JOIN GZZ_EST_REG  er ON p.PerEstReg = er.EstRegCod;
SELECT * FROM VI_PERSONAL_COMPLETO;

CREATE OR REPLACE VIEW VI_CLIENTES_TIPO AS
SELECT 
    c.CliCod      AS 'Código',
    c.CliNom      AS 'Cliente',
    tc.TipCliDes  AS 'Tipo',
    c.CliFecIng   AS 'Fecha Ingreso',
    c.CliFecCes   AS 'Fecha Cese'
FROM G1M_CLIENTES c
JOIN GZZ_TIP_CLI  tc ON c.CliTipCod = tc.TipCliCod;
SELECT * FROM VI_CLIENTES_TIPO;

CREATE OR REPLACE VIEW VI_CLIENTES_TIPO AS
SELECT 
    c.CliCod      AS 'Código',
    c.CliNom      AS 'Cliente',
    tc.TipCliDes  AS 'Tipo',
    c.CliFecIng   AS 'Fecha Ingreso',
    c.CliFecCes   AS 'Fecha Cese'
FROM G1M_CLIENTES c
JOIN GZZ_TIP_CLI  tc ON c.CliTipCod = tc.TipCliCod;
SELECT * FROM VI_CLIENTES_TIPO;

CREATE OR REPLACE VIEW VI_PROYECTOS_COMPLETO AS
SELECT 
    cab.ProCliCod   AS 'Cód. Cliente',
    cli.CliNom      AS 'Cliente',
    tc.TipCliDes    AS 'Tipo Cliente',
    tp.TipProDes    AS 'Tipo Proyecto',
    cab.ProSec      AS 'Secuencia',
    cab.ProFecCon   AS 'F. Contrato',
    cab.ProFecIni   AS 'F. Inicio',
    cab.ProFecEnt   AS 'F. Entrega',
    cab.ProFecCer   AS 'F. Cierre',
    ep.EstProDes    AS 'Estado',
    cab.ProMonPre   AS 'Monto Presup.',
    cab.ProMonRea   AS 'Monto Real',
    cab.ProCosPre   AS 'Costo Presup.',
    cab.ProCosRea   AS 'Costo Real',
    cab.ProUtiPre   AS 'Utilidad Presup.',
    cab.ProUtiRea   AS 'Utilidad Real'
FROM G1T_PRO_CAB  cab
JOIN G1M_CLIENTES cli ON cab.ProCliCod = cli.CliCod
JOIN GZZ_TIP_CLI  tc  ON cli.CliTipCod = tc.TipCliCod
JOIN GZZ_TIP_PRO  tp  ON cab.ProTipCod = tp.TipProCod
JOIN GZZ_EST_PRO  ep  ON cab.ProEstCod = ep.EstProCod;
SELECT * FROM VI_PROYECTOS_COMPLETO;
