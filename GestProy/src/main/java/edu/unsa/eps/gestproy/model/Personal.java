package edu.unsa.eps.gestproy.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

/** Fila de g1m_personal; la descripción del cargo viene del JOIN del listado. */
public class Personal {

    private Integer cod;
    private String nom;
    private Integer carCod;
    private String carDes;
    private BigDecimal cosHor;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecIng;
    private String estReg;

    public Integer getCod()                { return cod; }
    public void setCod(Integer cod)        { this.cod = cod; }
    public String getNom()                 { return nom; }
    public void setNom(String nom)         { this.nom = nom; }
    public Integer getCarCod()             { return carCod; }
    public void setCarCod(Integer carCod)  { this.carCod = carCod; }
    public String getCarDes()              { return carDes; }
    public void setCarDes(String carDes)   { this.carDes = carDes; }
    public BigDecimal getCosHor()          { return cosHor; }
    public void setCosHor(BigDecimal c)    { this.cosHor = c; }
    public LocalDate getFecIng()           { return fecIng; }
    public void setFecIng(LocalDate f)     { this.fecIng = f; }
    public String getEstReg()              { return estReg; }
    public void setEstReg(String estReg)   { this.estReg = estReg; }
}
