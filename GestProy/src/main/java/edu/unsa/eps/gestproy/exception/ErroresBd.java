package edu.unsa.eps.gestproy.exception;

import java.sql.SQLException;

import org.springframework.dao.DataAccessException;

/**
 * Extrae el mensaje legible de un error de base de datos.
 * Las funciones PL/pgSQL lanzan RAISE EXCEPTION con mensajes en
 * español pensados para el usuario final; aquí se recupera ese
 * texto desde la cadena de causas de la DataAccessException.
 */
public final class ErroresBd {

    private ErroresBd() {
    }

    public static String extraerMensaje(Throwable ex) {
        Throwable causa = (ex instanceof DataAccessException dae)
                ? dae.getMostSpecificCause()
                : ex;
        String mensaje = causa.getMessage();
        if (causa instanceof SQLException && mensaje != null) {
            // el driver antepone "ERROR: " y agrega líneas "Where:" con el
            // contexto PL/pgSQL; solo interesa la primera línea del mensaje
            mensaje = mensaje.lines().findFirst().orElse(mensaje);
            if (mensaje.startsWith("ERROR: ")) {
                mensaje = mensaje.substring("ERROR: ".length());
            }
        }
        return mensaje;
    }
}
