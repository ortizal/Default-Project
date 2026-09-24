package org.dentalcrm.web.whatsapp.dto;

import java.util.List;

public record CampanaResponse(
        int total,
        int enviados,
        int fallidos,
        List<ResultadoDestinatario> resultados
) {
    public record ResultadoDestinatario(String nombre, String telefono, boolean enviado, String error) {
    }
}