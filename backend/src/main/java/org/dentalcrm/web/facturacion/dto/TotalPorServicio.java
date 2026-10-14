package org.dentalcrm.web.facturacion.dto;

import java.math.BigDecimal;

/** Total facturado por servicio en un rango (§3.12/§3.13). */
public record TotalPorServicio(String servicio, BigDecimal facturado, int cargos) {
}
