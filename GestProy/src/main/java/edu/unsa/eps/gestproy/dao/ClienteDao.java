package edu.unsa.eps.gestproy.dao;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import edu.unsa.eps.gestproy.model.Cliente;

@Repository
public class ClienteDao {

    private static final String SELECT_BASE = """
        SELECT c.cli_cod, c.cli_nom, c.cli_tip_cod, t.tip_cli_des,
               c.cli_est_cod, e.est_cli_des,
               c.cli_fec_ing, c.cli_fec_ces, c.cli_fec_ult_pro_cer,
               c.cli_est_reg_cod
        FROM g1m_clientes c
        JOIN gzz_tip_cli t ON t.tip_cli_cod = c.cli_tip_cod
        JOIN gzz_est_cli e ON e.est_cli_cod = c.cli_est_cod
        """;

    private static final RowMapper<Cliente> MAPPER = (rs, i) -> {
        Cliente c = new Cliente();
        c.setCod(rs.getInt("cli_cod"));
        c.setNom(rs.getString("cli_nom"));
        c.setTipCod(rs.getString("cli_tip_cod"));
        c.setTipDes(rs.getString("tip_cli_des"));
        c.setEstCod(rs.getString("cli_est_cod"));
        c.setEstDes(rs.getString("est_cli_des"));
        c.setFecIng(rs.getObject("cli_fec_ing", java.time.LocalDate.class));
        c.setFecCes(rs.getObject("cli_fec_ces", java.time.LocalDate.class));
        c.setFecUltProCer(rs.getObject("cli_fec_ult_pro_cer", java.time.LocalDate.class));
        c.setEstReg(rs.getString("cli_est_reg_cod"));
        return c;
    };

    private final JdbcTemplate jdbc;

    public ClienteDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Cliente> listar() {
        return jdbc.query(SELECT_BASE + " ORDER BY c.cli_cod", MAPPER);
    }

    public Cliente buscar(int cod) {
        List<Cliente> filas = jdbc.query(SELECT_BASE + " WHERE c.cli_cod = ?", MAPPER, cod);
        return filas.isEmpty() ? null : filas.get(0);
    }

    /** Escrituras siempre vía la función PL/pgSQL sp_cliente_mant. */
    public void mantener(String operacion, Cliente c) {
        jdbc.queryForObject(
            "SELECT sp_cliente_mant(?, ?, ?, ?::char(2), ?::char(1), ?::date, ?::date, ?::date)",
            Object.class,
            operacion, c.getCod(), c.getNom(), c.getTipCod(), c.getEstCod(),
            c.getFecIng(), c.getFecCes(), c.getFecUltProCer());
    }

    public void cambiarEstado(int cod, String operacion) {
        jdbc.queryForObject(
            "SELECT sp_cliente_mant(?, ?)",
            Object.class, operacion, cod);
    }
}
