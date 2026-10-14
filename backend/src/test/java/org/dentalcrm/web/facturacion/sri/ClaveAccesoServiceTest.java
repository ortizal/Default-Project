package org.dentalcrm.web.facturacion.sri;

import org.dentalcrm.domain.configuracion.AmbienteSri;
import org.dentalcrm.domain.configuracion.TipoEmisionSri;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ClaveAccesoServiceTest {

    private final ClaveAccesoService servicio = new ClaveAccesoService();

    @Test
    void generaClaveDe49DigitosConLosCamposDeLaFichaTecnica() {
        var clave = servicio.generar(FixturesFactura.FECHA, FixturesFactura.RUC,
                "01", AmbienteSri.PRUEBAS, "001", "001", 1, TipoEmisionSri.NORMAL);

        assertEquals("08102026", clave.valor().substring(0, 8), "fecha ddmmaaaa");
        assertEquals("01", clave.valor().substring(8, 10), "tipo de comprobante");
        assertEquals(FixturesFactura.RUC, clave.valor().substring(10, 23), "RUC");
        assertEquals("1", clave.valor().substring(23, 24), "ambiente pruebas");
        assertEquals("001001", clave.valor().substring(24, 30), "serie estab+ptoEmi");
        assertEquals("000000001", clave.valor().substring(30, 39), "secuencial");
        assertEquals(8, clave.valor().substring(39, 47).length(), "código numérico");
        assertEquals("1", clave.valor().substring(47, 48), "emisión normal");
        assertEquals(49, clave.valor().length(), "49 dígitos");
        assertEquals(FixturesFactura.FECHA, clave.fechaEmision());
    }

    @Test
    void digitVerificadorSigueElModulo11DelSri() {
        // Ejemplo de la ficha técnica del SRI (tabla 1): el DV calculado debe ser 2.
        String sinVerificador = "280820140017921043940011001001000000009123456781";

        assertEquals(2, servicio.digitoVerificador(sinVerificador));
        assertEquals(4, servicio.digitoVerificador("1".repeat(48)));
    }

    @Test
    void claveGeneradaTieneSuPropioDigitoVerificador() {
        var clave = servicio.generar(FixturesFactura.FECHA, FixturesFactura.RUC,
                "01", AmbienteSri.PRODUCCION, "002", "005", 42, TipoEmisionSri.NORMAL);

        String cuerpo = clave.valor().substring(0, 48);
        assertEquals(String.valueOf(servicio.digitoVerificador(cuerpo)), clave.valor().substring(48));
        assertEquals("2", clave.valor().substring(23, 24), "ambiente producción");
    }

    @Test
    void sabadoYDomingoSeTrasladanAlSiguienteDiaHabil() {
        LocalDate sabado = LocalDate.of(2026, 10, 10);

        assertEquals(LocalDate.of(2026, 10, 12), servicio.fechaEmisionValida(sabado));
        assertEquals(FixturesFactura.FECHA, servicio.fechaEmisionValida(FixturesFactura.FECHA));
    }

    @Test
    void codigosDeAmbienteYEmisionSiguenLasTablasDelSri() {
        assertEquals("1", ClaveAccesoService.codigoAmbiente(AmbienteSri.PRUEBAS));
        assertEquals("2", ClaveAccesoService.codigoAmbiente(AmbienteSri.PRODUCCION));
        assertEquals("1", ClaveAccesoService.codigoEmision(TipoEmisionSri.NORMAL));
        assertEquals("2", ClaveAccesoService.codigoEmision(TipoEmisionSri.CONTINGENCIA));
    }
}
