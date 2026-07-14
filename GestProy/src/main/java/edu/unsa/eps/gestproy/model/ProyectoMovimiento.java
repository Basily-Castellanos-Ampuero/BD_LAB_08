package edu.unsa.eps.gestproy.model;

import java.time.LocalDate;

/** Movimiento de avance registrado en g1t_pro_mov (con nombres legibles). */
public record ProyectoMovimiento(
    Integer perCod,
    String perNom,
    Integer carProCod,
    String carProDes,
    Integer etpCod,
    String etpDes,
    Integer secEtp,
    LocalDate fecReg,
    Integer horTra,
    Integer minTra,
    String estReg
) {
}
