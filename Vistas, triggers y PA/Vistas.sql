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
