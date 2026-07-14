package edu.unsa.eps.gestproy.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

/**
 * Cabecera de proyecto (g1t_pro_cab / v_proyecto_resumen).
 * PK compuesta: (cliCod, tipCod, sec); sec la genera sp_proyecto_crear.
 */
public class Proyecto {

    private Integer cliCod;
    private Integer tipCod;
    private Integer sec;
    private String clienteNombre;
    private String tipoDescripcion;
    private String estCod;
    private String estadoDescripcion;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecCon;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecPac;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecIni;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecEnt;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecCer;
    private BigDecimal monPre;
    private BigDecimal monRea;
    private BigDecimal cosPre;
    private BigDecimal cosRea;
    private BigDecimal gasPre;
    private BigDecimal gasRea;
    private BigDecimal utiPre;
    private BigDecimal utiRea;
    private String estReg;

    public Integer getCliCod()                  { return cliCod; }
    public void setCliCod(Integer cliCod)       { this.cliCod = cliCod; }
    public Integer getTipCod()                  { return tipCod; }
    public void setTipCod(Integer tipCod)       { this.tipCod = tipCod; }
    public Integer getSec()                     { return sec; }
    public void setSec(Integer sec)             { this.sec = sec; }
    public String getClienteNombre()            { return clienteNombre; }
    public void setClienteNombre(String v)      { this.clienteNombre = v; }
    public String getTipoDescripcion()          { return tipoDescripcion; }
    public void setTipoDescripcion(String v)    { this.tipoDescripcion = v; }
    public String getEstCod()                   { return estCod; }
    public void setEstCod(String estCod)        { this.estCod = estCod; }
    public String getEstadoDescripcion()        { return estadoDescripcion; }
    public void setEstadoDescripcion(String v)  { this.estadoDescripcion = v; }
    public LocalDate getFecCon()                { return fecCon; }
    public void setFecCon(LocalDate f)          { this.fecCon = f; }
    public LocalDate getFecPac()                { return fecPac; }
    public void setFecPac(LocalDate f)          { this.fecPac = f; }
    public LocalDate getFecIni()                { return fecIni; }
    public void setFecIni(LocalDate f)          { this.fecIni = f; }
    public LocalDate getFecEnt()                { return fecEnt; }
    public void setFecEnt(LocalDate f)          { this.fecEnt = f; }
    public LocalDate getFecCer()                { return fecCer; }
    public void setFecCer(LocalDate f)          { this.fecCer = f; }
    public BigDecimal getMonPre()               { return monPre; }
    public void setMonPre(BigDecimal v)         { this.monPre = v; }
    public BigDecimal getMonRea()               { return monRea; }
    public void setMonRea(BigDecimal v)         { this.monRea = v; }
    public BigDecimal getCosPre()               { return cosPre; }
    public void setCosPre(BigDecimal v)         { this.cosPre = v; }
    public BigDecimal getCosRea()               { return cosRea; }
    public void setCosRea(BigDecimal v)         { this.cosRea = v; }
    public BigDecimal getGasPre()               { return gasPre; }
    public void setGasPre(BigDecimal v)         { this.gasPre = v; }
    public BigDecimal getGasRea()               { return gasRea; }
    public void setGasRea(BigDecimal v)         { this.gasRea = v; }
    public BigDecimal getUtiPre()               { return utiPre; }
    public void setUtiPre(BigDecimal v)         { this.utiPre = v; }
    public BigDecimal getUtiRea()               { return utiRea; }
    public void setUtiRea(BigDecimal v)         { this.utiRea = v; }
    public String getEstReg()                   { return estReg; }
    public void setEstReg(String estReg)        { this.estReg = estReg; }
}
