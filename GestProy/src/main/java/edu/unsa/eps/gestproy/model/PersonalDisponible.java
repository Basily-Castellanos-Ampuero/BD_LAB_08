package edu.unsa.eps.gestproy.model;

/** Combinación (persona, cargo) asignable a un proyecto (fn_personal_disponible_proyecto). */
public record PersonalDisponible(
    Integer perCod,
    String perNom,
    Integer carProCod,
    String carProDes
) {
    /** Valor compuesto "per|car" para el <select> del formulario de asignación. */
    public String clave() {
        return perCod + "|" + carProCod;
    }

    public String etiqueta() {
        return perNom + " — " + carProDes;
    }
}
