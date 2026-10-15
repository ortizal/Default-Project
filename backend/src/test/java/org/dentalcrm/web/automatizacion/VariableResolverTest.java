package org.dentalcrm.web.automatizacion;

import org.dentalcrm.domain.cuenta.CobroRealizadoEvent;
import org.dentalcrm.domain.cuenta.FormaPago;
import org.dentalcrm.domain.cuenta.TipoMovimientoCuenta;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.web.configuracion.ConsultorioService;
import org.dentalcrm.web.configuracion.dto.ConsultorioResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VariableResolverTest {

    @Mock
    private ConsultorioService consultorioService;

    @Test
    void variablesCobroFormateaElDineroYNoDependeDeCita() {
        when(consultorioService.obtener()).thenReturn(new ConsultorioResponse(
                "Little Smile", null, null, null, "Av. Siempre Viva 742",
                "+593 2 234 5678", "https://maps.example", "Lun–Vie 9h–18h",
                null, false, null));
        Paciente paciente = new Paciente();
        paciente.setNombres("Ana María");
        paciente.setApellidos("Pérez López");
        VariableResolver resolver = new VariableResolver(consultorioService);

        Map<String, String> v = resolver.variablesCobro(paciente, new CobroRealizadoEvent(
                55L, 7L, TipoMovimientoCuenta.ABONO, "Limpieza dental",
                new BigDecimal("40.5"), FormaPago.EFECTIVO,
                new BigDecimal("10"), LocalDate.of(2026, 10, 14)));

        assertEquals("Ana", v.get("nombre"));
        assertEquals("Pérez", v.get("apellido"));
        assertEquals("Ana María Pérez López", v.get("nombre_completo"));
        assertEquals("$ 40.50", v.get("monto"));
        assertEquals("$ 10.00", v.get("saldo"));
        assertEquals("Efectivo", v.get("forma_pago"));
        assertEquals("Limpieza dental", v.get("concepto"));
        assertEquals("Abono", v.get("tipo_movimiento"));
        assertEquals("14/10/2026", v.get("fecha"));
        assertEquals("55", v.get("codigo_movimiento"));
        assertEquals("Little Smile", v.get("clinica"));
        assertNull(v.get("codigo_cita"));
    }

    @Test
    void variablesCobroSoportaCamposNulos() {
        when(consultorioService.obtener()).thenReturn(new ConsultorioResponse(
                "Little Smile", null, null, null, null, null, null, null,
                null, false, null));
        Paciente paciente = new Paciente();
        VariableResolver resolver = new VariableResolver(consultorioService);

        Map<String, String> v = resolver.variablesCobro(paciente, new CobroRealizadoEvent(
                null, null, null, null, null, null, null, null));

        assertEquals("", v.get("nombre"));
        assertEquals("$ 0.00", v.get("monto"));
        assertEquals("$ 0.00", v.get("saldo"));
        assertEquals("—", v.get("forma_pago"));
        assertEquals("—", v.get("concepto"));
        assertEquals("Pago", v.get("tipo_movimiento"));
        assertEquals("", v.get("fecha"));
        assertEquals("", v.get("codigo_movimiento"));
    }

    @Test
    void renderizarSustituyeLasVariablesConocidas() {
        VariableResolver resolver = new VariableResolver(consultorioService);

        String salida = resolver.renderizar(
                "Hola {{nombre}}, pagaste {{monto}} ({{inesistente}})",
                Map.of("nombre", "Ana", "monto", "$ 40.00"));

        assertEquals("Hola Ana, pagaste $ 40.00 ({{inesistente}})", salida);
    }
}
