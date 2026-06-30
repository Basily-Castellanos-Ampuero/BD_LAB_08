DELIMITER //
CREATE TRIGGER trg_nuevo_proyecto
AFTER INSERT ON G1T_PRO_CAB
FOR EACH ROW
BEGIN
    SET @ultimo_proyecto_insertado = CONCAT(
        'Nuevo proyecto registrado: Cliente=', NEW.ProCliCod,
        ' Tipo=', NEW.ProTipCod,
        ' Sec=', NEW.ProSec,
        ' Estado=', NEW.ProEstCod
    );
END;
//
DELIMITER ;
INSERT INTO G1T_PRO_CAB VALUES (1, 1, 2, '2025-01-01', '2025-02-01', '2025-02-10', '2025-08-10', NULL, 30000.00, NULL, 20000.00, NULL, 3000.00, NULL, 7000.00, NULL, 'PE', 'A');
SELECT @ultimo_proyecto_insertado;

DELIMITER //
CREATE TRIGGER trg_validar_fechas_proyecto
BEFORE INSERT ON G1T_PRO_CAB
FOR EACH ROW
BEGIN
    IF NEW.ProFecEnt IS NOT NULL 
       AND NEW.ProFecIni IS NOT NULL 
       AND NEW.ProFecEnt < NEW.ProFecIni THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Error: La fecha de entrega no puede ser anterior a la fecha de inicio del proyecto.';
    END IF;
END;
//
DELIMITER ;
INSERT INTO G1T_PRO_CAB VALUES (12, 1, 3, '2025-01-01', '2025-02-01', '2025-06-01', '2025-01-01', NULL, 10000.00, NULL, 6000.00, NULL, 1000.00, NULL, 3000.00, NULL, 'PE', 'A');

DELIMITER //
CREATE TRIGGER trg_cierre_proyecto
AFTER UPDATE ON G1T_PRO_CAB
FOR EACH ROW
BEGIN
    IF NEW.ProEstCod = 'CE' AND OLD.ProEstCod <> 'CE' THEN
        SET @resumen_cierre = CONCAT(
            'Proyecto cerrado — Cliente: ', NEW.ProCliCod,
            ' | Tipo: ', NEW.ProTipCod,
            ' | Sec: ', NEW.ProSec,
            ' | Utilidad Real: ', IFNULL(NEW.ProUtiRea, 0)
        );
        CALL PA_Resumen_Proyecto(NEW.ProCliCod, NEW.ProTipCod, NEW.ProSec);
    END IF;
END;
//
DELIMITER ;
UPDATE G1T_PRO_CAB 
SET ProEstCod = 'CE',
    ProFecCer = '2025-06-29',
    ProUtiRea = 14600.00
WHERE ProCliCod = 2 AND ProTipCod = 1 AND ProSec = 1;
SELECT @resumen_cierre;
