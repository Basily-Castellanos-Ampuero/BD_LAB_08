package edu.unsa.eps.gestproy.security;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Determina, en cada petición, si viene de la cuenta admin
 * autenticada. Lee la cookie de access token; si falta o expiró,
 * intenta renovarla en silencio con la cookie de refresh token
 * (el mismo flujo que Django SimpleJWT con cookies HttpOnly: el
 * refresh nunca se expone a JavaScript ni se envía a mano, el
 * servidor lo usa para emitir un access token nuevo sin pedir
 * credenciales de nuevo).
 *
 * El resultado se deja en un atributo de request que el resto de
 * la aplicación puede leer sin volver a tocar cookies ni JWT:
 * {@link AutorizacionInterceptor} lo usa para bloquear escrituras,
 * y GlobalModelAttributes lo expone a las plantillas Thymeleaf
 * como "admin" para mostrar/ocultar los controles de edición.
 *
 * Como GestProy tiene una sola cuenta, "autenticado" y "es admi"
 * son lo mismo: no hay distintos niveles de permiso que resolver.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String ADMIN_ATTR = "gestproy.admin";
    public static final String COOKIE_ACCESS = "gp_access";
    public static final String COOKIE_REFRESH = "gp_refresh";
    private static final String LOGIN_ADMIN = "admi";

    private final JwtService jwtService;
    private final CookieUtil cookieUtil;

    public JwtAuthFilter(JwtService jwtService, CookieUtil cookieUtil) {
        this.jwtService = jwtService;
        this.cookieUtil = cookieUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {
        String access = leerCookie(request, COOKIE_ACCESS);
        String login = access != null ? jwtService.validarYObtenerLogin(access, JwtService.tipoAccess()) : null;

        if (login == null) {
            String refresh = leerCookie(request, COOKIE_REFRESH);
            String loginDesdeRefresh = refresh != null
                    ? jwtService.validarYObtenerLogin(refresh, JwtService.tipoRefresh())
                    : null;
            if (loginDesdeRefresh != null) {
                login = loginDesdeRefresh;
                // renovación silenciosa: nuevo access token sin pedir credenciales de nuevo
                cookieUtil.escribirAccessToken(response, jwtService.generarAccessToken(login),
                        jwtService.accessMaxAgeSegundos());
            }
        }

        request.setAttribute(ADMIN_ATTR, LOGIN_ADMIN.equals(login));
        chain.doFilter(request, response);
    }

    private String leerCookie(HttpServletRequest request, String nombre) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (nombre.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}
