package edu.unsa.eps.gestproy.model.referencial;

import java.math.BigDecimal;

/**
 * Fila genérica de una tabla referencial: código, descripción/nombre,
 * columnas extra del Grupo B (tam, tieEst) y estado de registro.
 */
public class RegistroReferencial {

    private String cod;
    private String des;
    private String tam;          // solo TIP_PRO / LIN_PRO
    private BigDecimal tieEst;   // solo ETP_PRO
    private String estReg;

    public String getCod()            { return cod; }
    public void setCod(String cod)    { this.cod = cod; }
    public String getDes()            { return des; }
    public void setDes(String des)    { this.des = des; }
    public String getTam()            { return tam; }
    public void setTam(String tam)    { this.tam = tam; }
    public BigDecimal getTieEst()     { return tieEst; }
    public void setTieEst(BigDecimal tieEst) { this.tieEst = tieEst; }
    public String getEstReg()         { return estReg; }
    public void setEstReg(String estReg) { this.estReg = estReg; }
}
