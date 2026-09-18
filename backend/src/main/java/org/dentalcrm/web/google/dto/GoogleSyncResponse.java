package org.dentalcrm.web.google.dto;

public record GoogleSyncResponse(
        int creados,
        int actualizados,
        int cancelados,
        int errores,
        int sincronizadas
) {
}