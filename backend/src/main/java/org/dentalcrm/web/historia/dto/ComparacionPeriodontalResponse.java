package org.dentalcrm.web.historia.dto;

import java.util.List;

public record ComparacionPeriodontalResponse(
        ExamenPeriodontalResumenResponse desde,
        ExamenPeriodontalResumenResponse hasta,
        List<CambioPeriodontalResponse> cambios,
        int agregados,
        int eliminados
) {
}
