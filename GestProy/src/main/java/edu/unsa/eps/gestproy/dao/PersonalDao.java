package edu.unsa.eps.gestproy.dao;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import edu.unsa.eps.gestproy.model.PerCar;
import edu.unsa.eps.gestproy.model.Personal;

@Repository
public class PersonalDao {

    private static final String SELECT_BASE = """
        SELECT p.per_cod, p.per_nom, p.per_car_cod, c.car_per_des,
               p.per_cos_hor, p.per_fec_ing, p.per_est_reg_cod
        FROM g1m_personal p
        JOIN gzz_car_per c ON c.car_per_cod = p.per_car_cod
        """;

    private static final RowMapper<Personal> MAPPER = (rs, i) -> {
        Personal p = new Personal();
        p.setCod(rs.getInt("per_cod"));
        p.setNom(rs.getString("per_nom"));
        p.setCarCod(rs.getInt("per_car_cod"));
        p.setCarDes(rs.getString("car_per_des"));
        p.setCosHor(rs.getBigDecimal("per_cos_hor"));
        p.setFecIng(rs.getObject("per_fec_ing", java.time.LocalDate.class));
        p.setEstReg(rs.getString("per_est_reg_cod"));
        return p;
    };

    private final JdbcTemplate jdbc;

    public PersonalDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Personal> listar() {
        return jdbc.query(SELECT_BASE + " ORDER BY p.per_cod", MAPPER);
    }

    public Personal buscar(int cod) {
        List<Personal> filas = jdbc.query(SELECT_BASE + " WHERE p.per_cod = ?", MAPPER, cod);
        return filas.isEmpty() ? null : filas.get(0);
    }

    public void mantener(String operacion, Personal p) {
        jdbc.queryForObject(
            "SELECT sp_personal_mant(?, ?, ?, ?::smallint, ?::numeric, ?::date)",
            Object.class,
            operacion, p.getCod(), p.getNom(), p.getCarCod(), p.getCosHor(), p.getFecIng());
    }

    public void cambiarEstado(int cod, String operacion) {
        jdbc.queryForObject(
            "SELECT sp_personal_mant(?, ?)",
            Object.class, operacion, cod);
    }

    // ---- autorizaciones de cargo (g1c_per_car) ----

    public List<PerCar> listarCargos(int perCod) {
        return jdbc.query("""
            SELECT pc.per_cod, pc.car_pro_cod, c.car_pro_des, pc.per_car_pro_est_reg_cod
            FROM g1c_per_car pc
            JOIN gzz_car_pro c ON c.car_pro_cod = pc.car_pro_cod
            WHERE pc.per_cod = ?
            ORDER BY c.car_pro_des
            """,
            (rs, i) -> new PerCar(
                rs.getInt("per_cod"),
                rs.getInt("car_pro_cod"),
                rs.getString("car_pro_des"),
                rs.getString("per_car_pro_est_reg_cod")),
            perCod);
    }

    public void mantenerCargo(String operacion, int perCod, int carProCod) {
        jdbc.queryForObject(
            "SELECT sp_per_car_mant(?, ?, ?::smallint)",
            Object.class, operacion, perCod, carProCod);
    }
}
