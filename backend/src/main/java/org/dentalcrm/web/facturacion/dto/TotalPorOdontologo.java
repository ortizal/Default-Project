package org.dentalcrm.web.facturacion.dto;

import java.math.BigDecimal;

/** Total facturado por odontólogo en un rango (§3.13). */
public record TotalPorOdontologo(String odontologo, BigDecimal facturado, int cargos) {
}
