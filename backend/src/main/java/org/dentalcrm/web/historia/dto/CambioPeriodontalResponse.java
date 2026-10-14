package org.dentalcrm.web.historia.dto;

/** Un cambio concreto entre dos exámenes (plan §4.11: comparación). */
public record CambioPeriodontalResponse(
        int diente,
        String sitio,
        String tipo,
        String campo,
        String anterior,
        String nuevo
) {
}
