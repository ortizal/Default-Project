package org.dentalcrm.web.historia.dto;

import java.util.List;

public record ComparacionResponse(
        OdontogramaResumenResponse desde,
        OdontogramaResumenResponse hasta,
        List<CambioResponse> cambios
) {
}
