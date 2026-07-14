package edu.unsa.eps.gestproy.model;

import java.math.BigDecimal;

/** Resumen de avance de un proyecto (fila de v_proyecto_avance). */
public record ProyectoAvance(
    BigDecimal horasEstimadas,
    BigDecimal horasTrabajadas,
    BigDecimal pctAvance
) {
    /** % acotado a [0,100] para el ancho de la barra de progreso. */
    public int pctBarra() {
        if (pctAvance == null) {
            return 0;
        }
        return pctAvance.min(BigDecimal.valueOf(100)).max(BigDecimal.ZERO).intValue();
    }

    public boolean excedido() {
        return pctAvance != null && pctAvance.compareTo(BigDecimal.valueOf(100)) > 0;
    }
}
