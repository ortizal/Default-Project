package org.dentalcrm.web.facturacion.sri;

import org.dentalcrm.domain.configuracion.AmbienteSri;
import org.dentalcrm.domain.configuracion.TipoEmisionSri;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Datos de ejemplo compartidos por las pruebas del motor electrónico (Fase G). */
final class FixturesFactura {

    static final LocalDate FECHA = LocalDate.of(2026, 10, 8); // jueves
    static final String RUC = "1791234567001";
    static final String CLAVE = "0810202601179123456700110010010000000011234567814";

    private FixturesFactura() {
    }

    /** Una línea de 100.00 con IVA 15% y otra de 45.90 tras descontar 5.10. */
    static DatosFactura factura() {
        return new DatosFactura(
                CLAVE,
                FECHA,
                emisor(),
                comprador(),
                List.of(
                        new DatosFactura.Linea("CONS-001", "Consulta odontológica", new BigDecimal("1"),
                                new BigDecimal("100.00"), new BigDecimal("0.00"), 2, 4, new BigDecimal("15")),
                        new DatosFactura.Linea("LIM-02", "Profilaxis & revisión", new BigDecimal("2"),
                                new BigDecimal("25.50"), new BigDecimal("5.10"), 2, 4, new BigDecimal("15"))),
                List.of(new DatosFactura.Pago("01", new BigDecimal("167.79"), null, null)),
                List.of(new DatosFactura.CampoAdicional("Correo", "juan@ejemplo.com")));
    }

    static DatosFactura.Emisor emisor() {
        return new DatosFactura.Emisor(
                "Little Smile S.A.", "Little Smile Odontología", RUC,
                "Av. República 123 y Av. Principal, Quito",
                AmbienteSri.PRUEBAS, TipoEmisionSri.NORMAL,
                ClaveAccesoService.COD_DOC_FACTURA, "001", "001", 1,
                "Av. República 123 y Av. Principal, Quito",
                false, null);
    }

    static DatosFactura.Comprador comprador() {
        return new DatosFactura.Comprador("05", "Juan Pérez", "1712345678", "Calle Falsa 123");
    }
}
