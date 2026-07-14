package edu.unsa.eps.gestproy.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Red de seguridad para errores no capturados por los controladores
 * (los POST de mantenimiento manejan sus errores con mensajes flash;
 * esto cubre los GET y cualquier fallo inesperado).
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({DataAccessException.class, ReglaNegocioException.class})
    public String errorDeDatos(Exception ex, Model model) {
        log.error("Error de base de datos no capturado", ex);
        model.addAttribute("mensaje", ErroresBd.extraerMensaje(ex));
        return "error";
    }

    /** Intento de escritura sin sesión admin activa (ver AutorizacionInterceptor). */
    @ExceptionHandler(NoAutorizadoException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String noAutorizado(NoAutorizadoException ex, Model model) {
        model.addAttribute("mensaje", ex.getMessage());
        return "error";
    }
}
