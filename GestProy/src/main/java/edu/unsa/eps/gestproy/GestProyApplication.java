package edu.unsa.eps.gestproy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * GestProy — Sistema de Gestión de Proyectos.
 * Proyecto final del curso de Base de Datos (UNSA).
 *
 * Toda la lógica de negocio de datos vive en PostgreSQL como
 * funciones/triggers/vistas (ver GestProy/db); esta aplicación
 * es la capa web (Spring MVC + Thymeleaf) que los invoca vía
 * JdbcTemplate, sin ORM.
 */
@SpringBootApplication
public class GestProyApplication {

    public static void main(String[] args) {
        SpringApplication.run(GestProyApplication.class, args);
    }
}
