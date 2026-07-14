package edu.unsa.eps.gestproy.dao;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import edu.unsa.eps.gestproy.model.referencial.ReferencialTabla;
import edu.unsa.eps.gestproy.model.referencial.RegistroReferencial;

/**
 * DAO genérico de las 9 tablas referenciales.
 *
 * Los nombres de tabla/columna provienen SIEMPRE del enum
 * ReferencialTabla (nunca de texto libre del usuario), por lo que
 * concatenarlos en el SELECT de listado es seguro. Las escrituras
 * van por las funciones PL/pgSQL con parámetros bind.
 */
@Repository
public class ReferencialDao {

    private final JdbcTemplate jdbc;

    public ReferencialDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<RegistroReferencial> listar(ReferencialTabla t) {
        StringBuilder sql = new StringBuilder("SELECT ")
            .append(t.getColCod()).append(" AS cod, ")
            .append(t.getColDes()).append(" AS des, ");
        if (t.isTieneTam()) {
            sql.append(t.getColTam()).append(" AS tam, ");
        }
        if (t.isTieneTieEst()) {
            sql.append("etp_tie_est AS tie_est, ");
        }
        sql.append(t.getColEstReg()).append(" AS est_reg FROM ")
           .append(t.getTabla()).append(" ORDER BY ").append(t.getColCod());

        return jdbc.query(sql.toString(), mapper(t));
    }

    public RegistroReferencial buscar(ReferencialTabla t, String cod) {
        StringBuilder sql = new StringBuilder("SELECT ")
            .append(t.getColCod()).append(" AS cod, ")
            .append(t.getColDes()).append(" AS des, ");
        if (t.isTieneTam()) {
            sql.append(t.getColTam()).append(" AS tam, ");
        }
        if (t.isTieneTieEst()) {
            sql.append("etp_tie_est AS tie_est, ");
        }
        sql.append(t.getColEstReg()).append(" AS est_reg FROM ")
           .append(t.getTabla()).append(" WHERE ").append(t.getColCod())
           .append(t.isCodNumerico() ? " = ?::smallint" : " = ?");

        List<RegistroReferencial> filas = jdbc.query(sql.toString(), mapper(t), cod);
        return filas.isEmpty() ? null : filas.get(0);
    }

    /** Ejecuta la operación de mantenimiento vía la función PL/pgSQL que corresponda. */
    public void mantener(ReferencialTabla t, String operacion, RegistroReferencial r) {
        switch (t) {
            case TIP_PRO -> jdbc.queryForObject(
                "SELECT sp_gzz_tip_pro_mant(?, ?::smallint, ?, ?)",
                Object.class, operacion, r.getCod(), r.getDes(), r.getTam());
            case LIN_PRO -> jdbc.queryForObject(
                "SELECT sp_gzz_lin_pro_mant(?, ?::smallint, ?, ?)",
                Object.class, operacion, r.getCod(), r.getDes(), r.getTam());
            case ETP_PRO -> jdbc.queryForObject(
                "SELECT sp_gzz_etp_pro_mant(?, ?::smallint, ?, ?::numeric)",
                Object.class, operacion, r.getCod(), r.getDes(), r.getTieEst());
            default -> jdbc.queryForObject(
                "SELECT sp_ref_grupoa_mant(?, ?, ?, ?)",
                Object.class, t.getTabla(), operacion, r.getCod(), r.getDes());
        }
    }

    private RowMapper<RegistroReferencial> mapper(ReferencialTabla t) {
        return (rs, i) -> {
            RegistroReferencial r = new RegistroReferencial();
            r.setCod(rs.getString("cod").trim());
            r.setDes(rs.getString("des"));
            if (t.isTieneTam()) {
                r.setTam(rs.getString("tam"));
            }
            if (t.isTieneTieEst()) {
                r.setTieEst(rs.getBigDecimal("tie_est"));
            }
            r.setEstReg(rs.getString("est_reg"));
            return r;
        };
    }
}
