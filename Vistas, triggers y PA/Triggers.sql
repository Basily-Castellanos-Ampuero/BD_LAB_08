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
