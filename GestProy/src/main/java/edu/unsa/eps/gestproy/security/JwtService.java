package edu.unsa.eps.gestproy.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Emisión y validación de los JWT de la cuenta admin única.
 *
 * Dos tipos de token, distinguidos por el claim "tipo" (para que
 * un refresh token robado no pueda usarse directamente como access
 * token, y viceversa):
 *   - access:  vida corta, autoriza las operaciones de escritura.
 *   - refresh: vida larga, solo sirve para renovar el access token.
 *
 * El refresh token NUNCA se persiste en el servidor (ni en una
 * tabla, ni asociado al usuario): es stateless, igual que el
 * comportamiento por defecto de Django SimpleJWT sin la app de
 * blacklist. Toda la validez del token vive en su propia firma y
 * fecha de expiración.
 */
@Component
public class JwtService {

    private static final String CLAIM_TIPO = "tipo";
    private static final String TIPO_ACCESS = "access";
    private static final String TIPO_REFRESH = "refresh";

    private final SecretKey clave;
    private final long accessMinutos;
    private final long refreshDias;

    public JwtService(
            @Value("${app.jwt.secret}") String secreto,
            @Value("${app.jwt.access-minutos:15}") long accessMinutos,
            @Value("${app.jwt.refresh-dias:7}") long refreshDias) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.accessMinutos = accessMinutos;
        this.refreshDias = refreshDias;
    }

    public String generarAccessToken(String login) {
        return generar(login, TIPO_ACCESS, accessMinutos * 60);
    }

    public String generarRefreshToken(String login) {
        return generar(login, TIPO_REFRESH, refreshDias * 24 * 3600);
    }

    private String generar(String login, String tipo, long duracionSegundos) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(login)
                .claim(CLAIM_TIPO, tipo)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusSeconds(duracionSegundos)))
                .signWith(clave)
                .compact();
    }

    /**
     * Valida la firma, la expiración y que el claim "tipo" coincida con el
     * esperado. Retorna el login (subject) si todo es válido, o {@code null}
     * ante cualquier problema (expirado, firma inválida, tipo incorrecto...).
     */
    public String validarYObtenerLogin(String token, String tipoEsperado) {
        try {
            Claims claims = Jwts.parser().verifyWith(clave).build()
                    .parseSignedClaims(token).getPayload();
            if (!tipoEsperado.equals(claims.get(CLAIM_TIPO, String.class))) {
                return null;
            }
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    public long accessMaxAgeSegundos() {
        return accessMinutos * 60;
    }

    public long refreshMaxAgeSegundos() {
        return refreshDias * 24 * 3600;
    }

    public static String tipoAccess() {
        return TIPO_ACCESS;
    }

    public static String tipoRefresh() {
        return TIPO_REFRESH;
    }
}
