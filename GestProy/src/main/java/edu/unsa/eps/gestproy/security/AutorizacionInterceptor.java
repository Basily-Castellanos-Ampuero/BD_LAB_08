package edu.unsa.eps.gestproy.security;

import org.springframework.web.servlet.HandlerInterceptor;

import edu.unsa.eps.gestproy.exception.NoAutorizadoException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Bloquea del lado del servidor cualquier operación de escritura
 * si la petición no viene de la cuenta admin autenticada (ver
 * {@link JwtAuthFilter}, que calcula el atributo ADMIN_ATTR antes
 * de que este interceptor se ejecute).
 *
 * Esta es la barrera real del "modo solo vista": ocultar los
 * botones de Adicionar/Modificar/Eliminar en las plantillas
 * Thymeleaf es solo cosmético (evita que un usuario normal se
 * confunda), pero cualquiera podría forjar el POST igual con
 * curl/Postman si no existiera este control en el servidor.
 *
 * Se consideran "escritura":
 *   - Cualquier método POST (las confirmaciones de Adicionar,
 *     Modificar, Eliminar, Inactivar, Reactivar, Asignar equipo,
 *     Registrar avance, Cambiar estado, etc. — todas las
 *     mutaciones de la app son POST).
 *   - Los GET a formularios de alta/edición ("/nuevo", ".../editar"),
 *     que no tienen ningún valor de solo lectura: mostrarlos a
 *     alguien sin sesión sería una pantalla muerta (todo lo que
 *     se envíe desde ahí terminaría bloqueado igual).
 *
 * "/login" y "/logout" quedan explícitamente fuera de esta regla:
 * son las únicas rutas de escritura que un visitante sin sesión
 * necesita poder usar.
 */
public class AutorizacionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        if ("/login".equals(path) || "/logout".equals(path)) {
            return true;
        }

        boolean admin = Boolean.TRUE.equals(request.getAttribute(JwtAuthFilter.ADMIN_ATTR));
        if (admin) {
            return true;
        }

        boolean esEscritura = "POST".equalsIgnoreCase(request.getMethod())
                || path.endsWith("/nuevo")
                || path.endsWith("/editar");
        if (esEscritura) {
            throw new NoAutorizadoException(
                    "Debes iniciar sesión como administrador para realizar esta acción.");
        }
        return true;
    }
}
