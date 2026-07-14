package edu.unsa.eps.gestproy.model;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

/** Fila de g1m_clientes; las descripciones vienen de los JOIN del listado. */
public class Cliente {

    private Integer cod;
    private String nom;
    private String tipCod;
    private String tipDes;
    private String estCod;
    private String estDes;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecIng;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecCes;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecUltProCer;
    private String estReg;

    public Integer getCod()               { return cod; }
    public void setCod(Integer cod)       { this.cod = cod; }
    public String getNom()                { return nom; }
    public void setNom(String nom)        { this.nom = nom; }
    public String getTipCod()             { return tipCod; }
    public void setTipCod(String tipCod)  { this.tipCod = tipCod; }
    public String getTipDes()             { return tipDes; }
    public void setTipDes(String tipDes)  { this.tipDes = tipDes; }
    public String getEstCod()             { return estCod; }
    public void setEstCod(String estCod)  { this.estCod = estCod; }
    public String getEstDes()             { return estDes; }
    public void setEstDes(String estDes)  { this.estDes = estDes; }
    public LocalDate getFecIng()          { return fecIng; }
    public void setFecIng(LocalDate f)    { this.fecIng = f; }
    public LocalDate getFecCes()          { return fecCes; }
    public void setFecCes(LocalDate f)    { this.fecCes = f; }
    public LocalDate getFecUltProCer()    { return fecUltProCer; }
    public void setFecUltProCer(LocalDate f) { this.fecUltProCer = f; }
    public String getEstReg()             { return estReg; }
    public void setEstReg(String estReg)  { this.estReg = estReg; }
}
