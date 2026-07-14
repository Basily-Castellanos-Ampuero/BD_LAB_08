package edu.unsa.eps.gestproy.dao;

import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import edu.unsa.eps.gestproy.model.ProyectoAvance;
import edu.unsa.eps.gestproy.model.ProyectoMovimiento;

@Repository
public class ProyectoAvanceDao {

    private final JdbcTemplate jdbc;

    public ProyectoAvanceDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public ProyectoAvance resumen(int cliCod, int tipCod, int sec) {
        List<ProyectoAvance> filas = jdbc.query("""
            SELECT horas_estimadas, horas_trabajadas, pct_avance
            FROM v_proyecto_avance
            WHERE pro_cli_cod = ? AND pro_tip_cod = ? AND pro_sec = ?
            """,
            (rs, i) -> new ProyectoAvance(
                rs.getBigDecimal("horas_estimadas"),
                rs.getBigDecimal("horas_trabajadas"),
                rs.getBigDecimal("pct_avance")),
            cliCod, tipCod, sec);
        return filas.isEmpty() ? null : filas.get(0);
    }

    public List<ProyectoMovimiento> movimientos(int cliCod, int tipCod, int sec) {
        return jdbc.query("""
            SELECT m.per_cod, p.per_nom, m.car_pro_cod, c.car_pro_des,
                   m.etp_cod, e.etp_des, m.sec_etp, m.fec_reg_etp,
                   m.hor_tra_etp, m.min_tra_etp, m.est_reg_cod
            FROM g1t_pro_mov m
            JOIN g1m_personal p ON p.per_cod = m.per_cod
            JOIN gzz_car_pro  c ON c.car_pro_cod = m.car_pro_cod
            JOIN gzz_etp_pro  e ON e.etp_cod = m.etp_cod
            WHERE m.pro_cli_cod = ? AND m.pro_tip_cod = ? AND m.pro_sec = ?
            ORDER BY m.fec_reg_etp DESC, m.etp_cod, m.sec_etp DESC
            """,
            (rs, i) -> new ProyectoMovimiento(
                rs.getInt("per_cod"),
                rs.getString("per_nom"),
                rs.getInt("car_pro_cod"),
                rs.getString("car_pro_des"),
                rs.getInt("etp_cod"),
                rs.getString("etp_des"),
                rs.getInt("sec_etp"),
                rs.getObject("fec_reg_etp", LocalDate.class),
                rs.getInt("hor_tra_etp"),
                rs.getInt("min_tra_etp"),
                rs.getString("est_reg_cod")),
            cliCod, tipCod, sec);
    }

    /** Registra el movimiento vía sp_proyecto_avance_registrar; retorna el sec_etp asignado. */
    public int registrar(int cliCod, int tipCod, int sec, int perCod, int carProCod,
                         int etpCod, LocalDate fecReg, int horTra, int minTra) {
        return jdbc.queryForObject("""
            SELECT sp_proyecto_avance_registrar(?, ?::smallint, ?::smallint, ?, ?::smallint,
                                                ?::smallint, ?::date, ?::smallint, ?::smallint)
            """,
            Integer.class,
            cliCod, tipCod, sec, perCod, carProCod, etpCod, fecReg, horTra, minTra);
    }
}
