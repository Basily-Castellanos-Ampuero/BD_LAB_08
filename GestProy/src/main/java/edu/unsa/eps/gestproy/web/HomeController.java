package edu.unsa.eps.gestproy.web;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Página de inicio: resumen general del sistema.
 * Sirve además de prueba de humo de la conexión a PostgreSQL.
 */
@Controller
public class HomeController {

    private final JdbcTemplate jdbc;

    public HomeController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("totalProyectos",
            jdbc.queryForObject("SELECT COUNT(*) FROM g1t_pro_cab WHERE pro_est_reg_cod = 'A'", Integer.class));
        model.addAttribute("totalClientes",
            jdbc.queryForObject("SELECT COUNT(*) FROM g1m_clientes WHERE cli_est_reg_cod = 'A'", Integer.class));
        model.addAttribute("totalPersonal",
            jdbc.queryForObject("SELECT COUNT(*) FROM g1m_personal WHERE per_est_reg_cod = 'A'", Integer.class));
        return "index";
    }
}
