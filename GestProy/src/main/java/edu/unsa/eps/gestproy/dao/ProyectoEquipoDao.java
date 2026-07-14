package edu.unsa.eps.gestproy.dao;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import edu.unsa.eps.gestproy.model.PersonalDisponible;
import edu.unsa.eps.gestproy.model.ProyectoEquipoItem;

@Repository
public class ProyectoEquipoDao {

    private final JdbcTemplate jdbc;

    public ProyectoEquipoDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ProyectoEquipoItem> listar(int cliCod, int tipCod, int sec) {
        return jdbc.query("""
            SELECT per_cod, per_nom, car_pro_cod, car_pro_des, per_cos_hor,
                   pro_per_car_est_reg_cod, estado_descripcion
            FROM v_proyecto_equipo
            WHERE pro_cli_cod = ? AND pro_tip_cod = ? AND pro_sec = ?
            ORDER BY per_nom, car_pro_des
            """,
            (rs, i) -> new ProyectoEquipoItem(
                rs.getInt("per_cod"),
                rs.getString("per_nom"),
                rs.getInt("car_pro_cod"),
                rs.getString("car_pro_des"),
                rs.getBigDecimal("per_cos_hor"),
                rs.getString("pro_per_car_est_reg_cod"),
                rs.getString("estado_descripcion")),
            cliCod, tipCod, sec);
    }

    public List<PersonalDisponible> disponibles(int cliCod, int tipCod, int sec) {
        return jdbc.query(
            "SELECT * FROM fn_personal_disponible_proyecto(?, ?::smallint, ?::smallint)",
            (rs, i) -> new PersonalDisponible(
                rs.getInt("per_cod"),
                rs.getString("per_nom"),
                rs.getInt("car_pro_cod"),
                rs.getString("car_pro_des")),
            cliCod, tipCod, sec);
    }

    public void asignar(int cliCod, int tipCod, int sec, int perCod, int carProCod) {
        jdbc.queryForObject(
            "SELECT sp_proyecto_equipo_asignar(?, ?::smallint, ?::smallint, ?, ?::smallint)",
            Object.class, cliCod, tipCod, sec, perCod, carProCod);
    }

    public void quitar(int cliCod, int tipCod, int sec, int perCod, int carProCod) {
        jdbc.queryForObject(
            "SELECT sp_proyecto_equipo_quitar(?, ?::smallint, ?::smallint, ?, ?::smallint)",
            Object.class, cliCod, tipCod, sec, perCod, carProCod);
    }

    public void reactivar(int cliCod, int tipCod, int sec, int perCod, int carProCod) {
        jdbc.queryForObject(
            "SELECT sp_proyecto_equipo_reactivar(?, ?::smallint, ?::smallint, ?, ?::smallint)",
            Object.class, cliCod, tipCod, sec, perCod, carProCod);
    }
}
