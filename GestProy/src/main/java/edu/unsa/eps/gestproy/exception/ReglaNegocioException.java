package edu.unsa.eps.gestproy.exception;

/**
 * Regla de negocio violada detectada en la capa de aplicación
 * (las reglas a nivel de datos las lanza PostgreSQL vía RAISE EXCEPTION
 * y llegan como DataAccessException).
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
