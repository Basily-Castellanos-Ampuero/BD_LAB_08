package edu.unsa.eps.gestproy.web;

import org.springframework.dao.DataAccessException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import edu.unsa.eps.gestproy.exception.ErroresBd;
import edu.unsa.eps.gestproy.exception.ReglaNegocioException;

/**
 * Base de los controladores de mantenimiento: ejecuta una operación
 * de escritura y traduce el resultado a mensajes flash (éxito o el
 * error lanzado por la función PL/pgSQL vía RAISE EXCEPTION).
 */
public abstract class MantenimientoControllerBase {

    /**
     * @return true si la operación se ejecutó sin errores.
     */
    protected boolean ejecutar(Runnable operacion, String mensajeExito, RedirectAttributes ra) {
        try {
            operacion.run();
            ra.addFlashAttribute("exito", mensajeExito);
            return true;
        } catch (DataAccessException | ReglaNegocioException ex) {
            ra.addFlashAttribute("error", ErroresBd.extraerMensaje(ex));
            return false;
        }
    }
}
