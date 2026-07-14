package edu.unsa.eps.gestproy.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Construye y limpia las cookies HttpOnly de access/refresh token.
 *
 * Ambas cookies son HttpOnly (JavaScript no puede leerlas, mitiga
 * robo por XSS) y SameSite=Lax (mitiga que otro sitio las use en
 * un POST forjado). "Secure" se deja configurable porque en
 * desarrollo local la app corre sobre http://localhost, no https.
 */
@Component
public class CookieUtil {

    private final boolean cookieSecure;

    public CookieUtil(@Value("${app.jwt.cookie-secure:false}") boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    public void escribirAccessToken(HttpServletResponse response, String token, long maxAgeSegundos) {
        agregarCookie(response, JwtAuthFilter.COOKIE_ACCESS, token, maxAgeSegundos);
    }

    public void escribirRefreshToken(HttpServletResponse response, String token, long maxAgeSegundos) {
        agregarCookie(response, JwtAuthFilter.COOKIE_REFRESH, token, maxAgeSegundos);
    }

    /** Borra ambas cookies de sesión (logout): mismo nombre/path, Max-Age=0. */
    public void borrarCookiesSesion(HttpServletResponse response) {
        agregarCookie(response, JwtAuthFilter.COOKIE_ACCESS, "", 0);
        agregarCookie(response, JwtAuthFilter.COOKIE_REFRESH, "", 0);
    }

    private void agregarCookie(HttpServletResponse response, String nombre, String valor, long maxAgeSegundos) {
        ResponseCookie cookie = ResponseCookie.from(nombre, valor)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(maxAgeSegundos)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
