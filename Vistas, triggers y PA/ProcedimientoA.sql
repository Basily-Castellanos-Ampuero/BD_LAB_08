DELIMITER //
CREATE PROCEDURE PA_Proyectos_Por_Estado(IN p_estado CHAR(2))
BEGIN
    SELECT *
    FROM VI_PROYECTOS_COMPLETO
    WHERE `Estado` = (
        SELECT EstProDes FROM GZZ_EST_PRO WHERE EstProCod = p_estado
    );
END;
//
DELIMITER ;


DELIMITER //
CREATE PROCEDURE PA_Reporte_Personal_Cargo()
BEGIN
    SELECT 
        `Cargo`,
        COUNT(*)             AS 'Total Personal',
        AVG(`Costo x Hora`) AS 'Costo Promedio x Hora',
        MAX(`Costo x Hora`) AS 'Costo Máximo x Hora',
        MIN(`Costo x Hora`) AS 'Costo Mínimo x Hora'
    FROM VI_PERSONAL_COMPLETO
    GROUP BY `Cargo`
    ORDER BY `Total Personal` DESC;
END;
//
DELIMITER ;


DELIMITER //
CREATE PROCEDURE PA_Resumen_Proyecto(
    IN p_cli  INT,
    IN p_tip  SMALLINT,
    IN p_sec  SMALLINT
)
BEGIN
    SELECT 
        `Cliente`,
        `Tipo Proyecto`,
        `Secuencia`,
        `F. Inicio`,
        `F. Cierre`,
        `Estado`,
        `Monto Presup.`,
        `Monto Real`,
        (`Monto Real` - `Monto Presup.`)        AS 'Variación Monto',
        `Utilidad Presup.`,
        `Utilidad Real`,
        (`Utilidad Real` - `Utilidad Presup.`)  AS 'Variación Utilidad'
    FROM VI_PROYECTOS_COMPLETO
    WHERE `Cód. Cliente` = p_cli
      AND `Tipo Proyecto` = (SELECT TipProDes FROM GZZ_TIP_PRO WHERE TipProCod = p_tip)
      AND `Secuencia`     = p_sec;
END;
//
DELIMITER ;
CALL PA_Resumen_Proyecto(1, 1, 1);
CALL PA_Proyectos_Por_Estado('AC');
CALL PA_Reporte_Personal_Cargo();