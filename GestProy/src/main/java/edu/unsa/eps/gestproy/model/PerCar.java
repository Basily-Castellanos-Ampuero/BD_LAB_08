package edu.unsa.eps.gestproy.model;

/** Autorización persona ↔ cargo de proyecto (fila de g1c_per_car). */
public record PerCar(
    Integer perCod,
    Integer carProCod,
    String carProDes,
    String estReg
) {
}
