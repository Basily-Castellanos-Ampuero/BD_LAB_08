package edu.unsa.eps.gestproy.model;

import java.math.BigDecimal;

/** Miembro del equipo de un proyecto (fila de v_proyecto_equipo). */
public record ProyectoEquipoItem(
    Integer perCod,
    String perNom,
    Integer carProCod,
    String carProDes,
    BigDecimal cosHor,
    String estReg,
    String estadoDescripcion
) {
}
