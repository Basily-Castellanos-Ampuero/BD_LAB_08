package edu.unsa.eps.gestproy.model.referencial;

/**
 * Metadatos de las 9 tablas referenciales (catálogos GZZ_*).
 * Permite un solo DAO/controlador/vista genéricos: cada constante
 * conoce su slug de URL, su tabla y columnas reales, y qué función
 * PL/pgSQL de mantenimiento le corresponde.
 *
 * Grupo A (forma idéntica Cod/Des/EstReg) -> sp_ref_grupoa_mant.
 * Grupo B (columna extra)                 -> función específica.
 */
public enum ReferencialTabla {

    EST_REG("est_reg", "gzz_est_reg", "Estado de Registro", "Estados de Registro",
            "est_reg_cod", "est_reg_des", "est_reg_est_reg", false, false, false),
    TIP_CLI("tip_cli", "gzz_tip_cli", "Tipo de Cliente", "Tipos de Cliente",
            "tip_cli_cod", "tip_cli_des", "tip_cli_est_reg", false, false, false),
    EST_CLI("est_cli", "gzz_est_cli", "Estado de Cliente", "Estados de Cliente",
            "est_cli_cod", "est_cli_des", "est_cli_est_reg", false, false, false),
    EST_PRO("est_pro", "gzz_est_pro", "Estado de Proyecto", "Estados de Proyecto",
            "est_pro_cod", "est_pro_des", "est_pro_est_reg", false, false, false),
    CAR_PER("car_per", "gzz_car_per", "Cargo de Personal", "Cargos de Personal",
            "car_per_cod", "car_per_des", "car_per_est_reg", true, false, false),
    CAR_PRO("car_pro", "gzz_car_pro", "Cargo de Proyecto", "Cargos de Proyecto",
            "car_pro_cod", "car_pro_des", "car_pro_est_reg", true, false, false),
    TIP_PRO("tip_pro", "gzz_tip_pro", "Tipo de Proyecto", "Tipos de Proyecto",
            "tip_pro_cod", "tip_pro_des", "tip_pro_est_reg", true, true, false),
    LIN_PRO("lin_pro", "gzz_lin_pro", "Línea de Proyecto", "Líneas de Proyecto",
            "lin_pro_cod", "lin_pro_nom", "lin_pro_est_reg_cod", true, true, false),
    ETP_PRO("etp_pro", "gzz_etp_pro", "Etapa de Proyecto", "Etapas de Proyecto",
            "etp_cod", "etp_des", "etp_est_reg", true, false, true);

    private final String slug;
    private final String tabla;
    private final String etiqueta;
    private final String etiquetaPlural;
    private final String colCod;
    private final String colDes;
    private final String colEstReg;
    private final boolean codNumerico;
    private final boolean tieneTam;
    private final boolean tieneTieEst;

    ReferencialTabla(String slug, String tabla, String etiqueta, String etiquetaPlural,
                     String colCod, String colDes, String colEstReg,
                     boolean codNumerico, boolean tieneTam, boolean tieneTieEst) {
        this.slug = slug;
        this.tabla = tabla;
        this.etiqueta = etiqueta;
        this.etiquetaPlural = etiquetaPlural;
        this.colCod = colCod;
        this.colDes = colDes;
        this.colEstReg = colEstReg;
        this.codNumerico = codNumerico;
        this.tieneTam = tieneTam;
        this.tieneTieEst = tieneTieEst;
    }

    /** Resuelve el slug de la URL; null si no existe. */
    public static ReferencialTabla porSlug(String slug) {
        for (ReferencialTabla t : values()) {
            if (t.slug.equals(slug)) {
                return t;
            }
        }
        return null;
    }

    /** Columna de tamaño (solo TIP_PRO y LIN_PRO). */
    public String getColTam() {
        return switch (this) {
            case TIP_PRO -> "tip_pro_tam";
            case LIN_PRO -> "lin_pro_tam";
            default -> null;
        };
    }

    public boolean esGrupoA() {
        return this != TIP_PRO && this != LIN_PRO && this != ETP_PRO;
    }

    public String getSlug()           { return slug; }
    public String getTabla()          { return tabla; }
    public String getEtiqueta()       { return etiqueta; }
    public String getEtiquetaPlural() { return etiquetaPlural; }
    public String getColCod()         { return colCod; }
    public String getColDes()         { return colDes; }
    public String getColEstReg()      { return colEstReg; }
    public boolean isCodNumerico()    { return codNumerico; }
    public boolean isTieneTam()       { return tieneTam; }
    public boolean isTieneTieEst()    { return tieneTieEst; }
}
