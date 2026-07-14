package edu.unsa.eps.gestproy.exception;

/**
 * Se lanza cuando una petición de escritura llega sin la sesión
 * admin activa (ver AutorizacionInterceptor). Distinta de
 * ReglaNegocioException: esta no es una regla de negocio violada,
 * es un intento de mutación sin autorización.
 */
public class NoAutorizadoException extends RuntimeException {

    public NoAutorizadoException(String mensaje) {
        super(mensaje);
    }
}
