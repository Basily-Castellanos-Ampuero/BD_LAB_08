package edu.unsa.eps.gestproy.web;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import edu.unsa.eps.gestproy.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Expone "admin" a todas las plantillas Thymeleaf, para mostrar u
 * ocultar los controles de edición según haya o no sesión admin
 * activa. El valor ya lo calculó JwtAuthFilter por cada petición;
 * aquí solo se lee el atributo de request y se agrega al modelo.
 */
@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute("admin")
    public boolean admin(HttpServletRequest request) {
        return Boolean.TRUE.equals(request.getAttribute(JwtAuthFilter.ADMIN_ATTR));
    }
}
