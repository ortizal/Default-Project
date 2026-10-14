package org.dentalcrm.web.facturacion.dto;

import java.time.LocalDate;

/**
 * Filtros de la bandeja de comprobantes (plan §3.7): fechas de emisión,
 * texto libre (número, clave, cédula o paciente), tipo, estados,
 * establecimiento y usuario. Los campos nulos o vacíos no filtran.
 */
public record ComprobanteFiltroRequest(
        String q,
        LocalDate desde,
        LocalDate hasta,
        String tipo,
        String estado,
        String generacion,
        Long pacienteId,
        String establecimiento,
        Long usuarioId,
        Integer page,
        Integer size
) {
}
