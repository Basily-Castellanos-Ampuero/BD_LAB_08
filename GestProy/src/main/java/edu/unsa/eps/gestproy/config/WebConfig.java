package edu.unsa.eps.gestproy.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import edu.unsa.eps.gestproy.security.AutorizacionInterceptor;

/**
 * Registra el interceptor que bloquea escrituras sin sesión admin.
 * La identidad (¿quién sos?) la resuelve JwtAuthFilter, que Spring
 * Boot ya registra solo por ser un bean Filter; este interceptor
 * solo decide autorización (¿podés hacer esto?) y no necesita
 * registrarse como Filter porque corre dentro del ciclo de
 * Spring MVC (así sus excepciones llegan a GlobalExceptionHandler).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AutorizacionInterceptor());
    }
}
