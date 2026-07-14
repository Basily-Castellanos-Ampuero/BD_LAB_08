package edu.unsa.eps.gestproy.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UsuarioDao {

    private final JdbcTemplate jdbc;

    public UsuarioDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Verifica login+contraseña contra el hash guardado (fn_usuario_autenticar, pgcrypto). */
    public boolean autenticar(String login, String password) {
        Boolean ok = jdbc.queryForObject(
                "SELECT fn_usuario_autenticar(?, ?)", Boolean.class, login, password);
        return Boolean.TRUE.equals(ok);
    }
}
