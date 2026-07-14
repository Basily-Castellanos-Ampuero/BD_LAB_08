package edu.unsa.eps.gestproy.dao;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import edu.unsa.eps.gestproy.model.Proyecto;

@Repository
public class ProyectoDao {

    private static final String SELECT_BASE =
        "SELECT * FROM v_proyecto_resumen ";

    private static final RowMapper<Proyecto> MAPPER = (rs, i) -> {
        Proyecto p = new Proyecto();
        p.setCliCod(rs.getInt("pro_cli_cod"));
        p.setTipCod(rs.getInt("pro_tip_cod"));
        p.setSec(rs.getInt("pro_sec"));
        p.setClienteNombre(rs.getString("cliente_nombre"));
        p.setTipoDescripcion(rs.getString("tipo_descripcion"));
        p.setEstCod(rs.getString("pro_est_cod"));
        p.setEstadoDescripcion(rs.getString("estado_descripcion"));
        p.setFecCon(rs.getObject("pro_fec_con", java.time.LocalDate.class));
        p.setFecPac(rs.getObject("pro_fec_pac", java.time.LocalDate.class));
        p.setFecIni(rs.getObject("pro_fec_ini", java.time.LocalDate.class));
        p.setFecEnt(rs.getObject("pro_fec_ent", java.time.LocalDate.class));
        p.setFecCer(rs.getObject("pro_fec_cer", java.time.LocalDate.class));
        p.setMonPre(rs.getBigDecimal("pro_mon_pre"));
        p.setMonRea(rs.getBigDecimal("pro_mon_rea"));
        p.setCosPre(rs.getBigDecimal("pro_cos_pre"));
        p.setCosRea(rs.getBigDecimal("pro_cos_rea"));
        p.setGasPre(rs.getBigDecimal("pro_gas_pre"));
        p.setGasRea(rs.getBigDecimal("pro_gas_rea"));
        p.setUtiPre(rs.getBigDecimal("pro_uti_pre"));
        p.setUtiRea(rs.getBigDecimal("pro_uti_rea"));
        p.setEstReg(rs.getString("pro_est_reg_cod"));
        return p;
    };

    private final JdbcTemplate jdbc;

    public ProyectoDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Proyecto> listar() {
        return jdbc.query(SELECT_BASE + "ORDER BY pro_cli_cod, pro_tip_cod, pro_sec", MAPPER);
    }

    public Proyecto buscar(int cliCod, int tipCod, int sec) {
        List<Proyecto> filas = jdbc.query(
            SELECT_BASE + "WHERE pro_cli_cod = ? AND pro_tip_cod = ? AND pro_sec = ?",
            MAPPER, cliCod, tipCod, sec);
        return filas.isEmpty() ? null : filas.get(0);
    }

    /** Crea la cabecera vía sp_proyecto_crear y retorna el pro_sec generado. */
    public int crear(Proyecto p) {
        Integer sec = jdbc.queryForObject(
            "SELECT sp_proyecto_crear(?, ?::smallint, ?::date, ?::date, ?::numeric, ?::numeric, ?::numeric)",
            Integer.class,
            p.getCliCod(), p.getTipCod(), p.getFecCon(), p.getFecPac(),
            p.getMonPre(), p.getCosPre(), p.getGasPre());
        return sec;
    }

    public void editar(Proyecto p) {
        jdbc.queryForObject(
            """
            SELECT sp_proyecto_editar(?, ?::smallint, ?::smallint,
                   ?::date, ?::date, ?::date, ?::date,
                   ?::numeric, ?::numeric, ?::numeric, ?::numeric, ?::numeric, ?::numeric)
            """,
            Object.class,
            p.getCliCod(), p.getTipCod(), p.getSec(),
            p.getFecCon(), p.getFecPac(), p.getFecIni(), p.getFecEnt(),
            p.getMonPre(), p.getMonRea(), p.getCosPre(), p.getCosRea(),
            p.getGasPre(), p.getGasRea());
    }

    public void cambiarEstado(int cliCod, int tipCod, int sec, String nuevoEstado) {
        jdbc.queryForObject(
            "SELECT sp_proyecto_cambiar_estado(?, ?::smallint, ?::smallint, ?::char(2))",
            Object.class, cliCod, tipCod, sec, nuevoEstado);
    }
}
