package org.dentalcrm.web.facturacion;

import org.dentalcrm.domain.cuenta.*;
import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.servicio.Servicio;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.web.facturacion.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReporteFinancieroServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Guayaquil");

    private ReporteFinancieroService servicio;

    private CuentaDetalleRepository detalleRepo;
    private CuentaPacienteRepository cuentaRepo;
    private CajaService cajaService;

    private final List<CuentaDetalle> movimientos = new ArrayList<>();
    private final List<CuentaPaciente> cuentas = new ArrayList<>();
    private final LocalDate hoy = LocalDate.now(ZONA);

    @BeforeEach
    void setUp() {
        detalleRepo = mock(CuentaDetalleRepository.class);
        cuentaRepo = mock(CuentaPacienteRepository.class);
        cajaService = mock(CajaService.class);

        when(detalleRepo.findByAnuladoFalseAndFechaBetweenOrderByFechaAscIdAsc(any(), any()))
                .thenAnswer(inv -> {
                    LocalDate desde = inv.getArgument(0);
                    LocalDate hasta = inv.getArgument(1);
                    return movimientos.stream()
                            .filter(m -> !m.getFecha().isBefore(desde))
                            .filter(m -> !m.getFecha().isAfter(hasta))
                            .toList();
                });
        when(detalleRepo.findByAnuladoFalseAndFecha(any())).thenAnswer(inv ->
                movimientos.stream()
                        .filter(m -> m.getFecha().equals(inv.getArgument(0)))
                        .toList());
        when(detalleRepo.findByAnuladoFalse()).thenAnswer(inv -> new ArrayList<>(movimientos));
        when(detalleRepo.anticiposNetos()).thenAnswer(inv -> movimientos.stream()
                .filter(m -> !m.isAnulado())
                .map(m -> switch (m.getTipo()) {
                    case ANTICIPO -> m.getTotal();
                    case DEVOLUCION -> m.getTotal().negate();
                    default -> BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        when(detalleRepo.countByCuentaIdAndAnuladoFalse(any()))
                .thenAnswer(inv -> (long) movimientos.size());
        when(cuentaRepo.findAll()).thenAnswer(inv -> new ArrayList<>(cuentas));
        when(cuentaRepo.findAllByOrderByUpdatedAtDesc()).thenAnswer(inv -> new ArrayList<>(cuentas));
        when(cuentaRepo.resumirSaldosPendientes()).thenAnswer(inv -> {
            BigDecimal total = cuentas.stream()
                    .map(CuentaPaciente::getSaldo)
                    .filter(saldo -> saldo.signum() > 0)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long cantidad = cuentas.stream().filter(c -> c.getSaldo().signum() > 0).count();
            return new CuentaPacienteRepository.ResumenSaldosPendientes() {
                @Override
                public BigDecimal getTotal() {
                    return total;
                }

                @Override
                public long getCantidad() {
                    return cantidad;
                }
            };
        });

        servicio = new ReporteFinancieroService(detalleRepo, cuentaRepo, cajaService, ZONA.getId());
    }

    // ------------------------------ utilidades ------------------------------

    private CuentaDetalle mov(TipoMovimientoCuenta tipo, String monto, LocalDate fecha,
                              FormaPago forma) {
        CuentaDetalle d = new CuentaDetalle();
        d.setId((long) (movimientos.size() + 1));
        d.setTipo(tipo);
        d.setTotal(new BigDecimal(monto));
        d.setFecha(fecha);
        d.setFormaPago(forma);
        d.setConcepto("Movimiento");
        movimientos.add(d);
        return d;
    }

    private void cargoConDimension(String monto, LocalDate fecha, String nombres,
                                   String apellidos, String servicio) {
        CuentaDetalle d = mov(TipoMovimientoCuenta.CARGO, monto, fecha, null);
        Odontologo odontologo = new Odontologo();
        odontologo.setNombres(nombres);
        odontologo.setApellidos(apellidos);
        d.setOdontologo(odontologo);
        if (servicio != null) {
            Servicio s = new Servicio();
            s.setNombre(servicio);
            d.setServicio(s);
        }
    }

    private CuentaPaciente cuenta(long id, String saldo, String nombres, String apellidos) {
        Paciente paciente = new Paciente();
        paciente.setId(id);
        paciente.setNombres(nombres);
        paciente.setApellidos(apellidos);
        paciente.setCedula("17" + id);

        CuentaPaciente c = new CuentaPaciente();
        c.setId(id);
        c.setPaciente(paciente);
        c.setSaldo(new BigDecimal(saldo));
        cuentas.add(c);
        return c;
    }

    private CajaResponse sesion(long id, String ingresos, String egresos, String diferencia) {
        TotalesCaja totales = new TotalesCaja(BigDecimal.ZERO, new BigDecimal(ingresos),
                new BigDecimal(egresos), null, BigDecimal.ZERO, new BigDecimal(diferencia),
                List.of(), 3);
        return new CajaResponse(id, "Caja", "CERRADA", hoy, BigDecimal.ZERO, null,
                new BigDecimal(diferencia), "Laura Gómez", null, null, null, null, null, null,
                totales, List.of(), null, null);
    }

    private FormaPagoTotal forma(List<FormaPagoTotal> lista, String forma) {
        return lista.stream().filter(f -> forma.equals(f.formaPago())).findFirst()
                .orElseThrow();
    }

    // ------------------------------ dashboard ------------------------------

    @Test
    void dashboardResumeFacturadoCobradoYDescuentosDelRango() {
        mov(TipoMovimientoCuenta.CARGO, "100", hoy.minusDays(10), null);
        mov(TipoMovimientoCuenta.ABONO, "40", hoy.minusDays(5), FormaPago.EFECTIVO);
        mov(TipoMovimientoCuenta.DEVOLUCION, "10", hoy.minusDays(2), FormaPago.EFECTIVO);
        mov(TipoMovimientoCuenta.DESCUENTO, "5", hoy.minusDays(1), null);

        DashboardFinancieroResponse d = servicio.dashboard(null, null);

        assertEquals(0, d.facturado().compareTo(new BigDecimal("100.00")));
        assertEquals(0, d.descuentos().compareTo(new BigDecimal("5.00")));
        assertEquals(0, d.cobrado().compareTo(new BigDecimal("30.00")));
        assertEquals(0, d.devoluciones().compareTo(new BigDecimal("10.00")));
        assertEquals(1, d.cobros());
        assertEquals(0, d.ticketPromedio().compareTo(new BigDecimal("30.00")));
        assertEquals(0, forma(d.porFormaPago(), "EFECTIVO").monto()
                .compareTo(new BigDecimal("30.00")));
    }

    @Test
    void dashboardIncluyeHoyMesYPorCobrar() {
        cargoConDimension("25", hoy, "Laura", "Gómez", "Limpieza");
        mov(TipoMovimientoCuenta.ABONO, "25", hoy, FormaPago.TARJETA);
        cuenta(10L, "120.00", "Ana", "Pérez");
        cuenta(11L, "0.00", "Luis", "Díaz");
        mov(TipoMovimientoCuenta.ANTICIPO, "200", hoy.minusDays(30), FormaPago.EFECTIVO);

        DashboardFinancieroResponse d = servicio.dashboard(null, null);

        assertEquals(0, d.facturadoHoy().compareTo(new BigDecimal("25.00")));
        assertEquals(0, d.cobradoHoy().compareTo(new BigDecimal("25.00")));
        assertEquals(0, d.facturadoMes().compareTo(new BigDecimal("25.00")));
        assertEquals(0, d.cuentasPorCobrar().compareTo(new BigDecimal("120.00")));
        assertEquals(1, d.cuentasPendientes());
        assertEquals(0, d.anticipos().compareTo(new BigDecimal("200.00")));
        assertEquals(1, d.porServicio().size());
        assertEquals("Limpieza", d.porServicio().get(0).servicio());
        assertEquals("Laura Gómez", d.porOdontologo().get(0).odontologo());
        org.mockito.Mockito.verify(cuentaRepo, org.mockito.Mockito.never()).findAll();
        org.mockito.Mockito.verify(detalleRepo, org.mockito.Mockito.never()).findByAnuladoFalse();
    }

    @Test
    void dashboardRechazaRangoInvalido() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> servicio.dashboard(hoy, hoy.minusDays(1)));
        assertEquals("RANGO_INVALIDO", e.getCode());
    }

    // -------------------------------- reportes ------------------------------

    @Test
    void ventasAgrupaPorDiaYFormaDePago() {
        mov(TipoMovimientoCuenta.CARGO, "80", hoy.minusDays(2), null);
        mov(TipoMovimientoCuenta.ABONO, "30", hoy.minusDays(2), FormaPago.EFECTIVO);
        cargoConDimension("60", hoy, "Laura", "Gómez", "Empaste");
        mov(TipoMovimientoCuenta.ABONO, "60", hoy, FormaPago.TARJETA);

        ReporteVentasResponse r = servicio.ventas(null, null);

        assertEquals(0, r.facturado().compareTo(new BigDecimal("140.00")));
        assertEquals(0, r.cobrado().compareTo(new BigDecimal("90.00")));
        assertEquals(2, r.cargos());
        assertEquals(2, r.cobros());
        assertEquals(2, r.porDia().size());
        assertEquals(hoy, r.porDia().get(1).fecha());
        assertEquals(0, r.porDia().get(1).facturado().compareTo(new BigDecimal("60.00")));
        assertEquals(0, forma(r.porFormaPago(), "TARJETA").monto()
                .compareTo(new BigDecimal("60.00")));
        assertEquals(1, r.porServicio().size());
    }

    @Test
    void produccionAgrupaPorOdontologoYOrdenaDescendente() {
        cargoConDimension("200", hoy, "Laura", "Gómez", "Endodoncia");
        cargoConDimension("100", hoy, "Marco", "Ruiz", "Empaste");
        cargoConDimension("50", hoy, "Laura", "Gómez", "Limpieza");
        cargoConDimension("30", hoy, "Laura", "Gómez", null);   // sin servicio

        ReporteProduccionResponse r = servicio.produccion(null, null);

        assertEquals(0, r.total().compareTo(new BigDecimal("380.00")));
        assertEquals(4, r.cargos());
        assertEquals(2, r.porOdontologo().size());
        assertEquals("Laura Gómez", r.porOdontologo().get(0).odontologo());
        assertEquals(0, r.porOdontologo().get(0).facturado().compareTo(new BigDecimal("280.00")));
        assertEquals(3, r.porOdontologo().get(0).cargos());
        assertEquals("Marco Ruiz", r.porOdontologo().get(1).odontologo());
    }

    @Test
    void saldosDevuelveSoloCuentasPendientes() {
        cuenta(10L, "150.00", "Ana", "Pérez");
        cuenta(11L, "-20.00", "Luis", "Díaz");
        cuenta(12L, "0.00", "Rosa", "Vega");

        ReporteSaldosResponse r = servicio.saldos();

        assertEquals(0, r.totalPorCobrar().compareTo(new BigDecimal("150.00")));
        assertEquals(1, r.cantidadCuentas());
        assertEquals("Ana Pérez", r.cuentas().get(0).paciente());
        assertEquals("1710", r.cuentas().get(0).cedula());
    }

    @Test
    void reporteDeCajaSumaLosCortesDeLasSesiones() {
        when(cajaService.listar(any(), any())).thenReturn(List.of(
                sesion(1L, "100.00", "20.00", "-5.00"),
                sesion(2L, "50.00", "0.00", "2.50")));

        ReporteCajaResponse r = servicio.reporteCaja(null, null);

        assertEquals(2, r.cantidadSesiones());
        assertEquals(0, r.ingresos().compareTo(new BigDecimal("150.00")));
        assertEquals(0, r.egresos().compareTo(new BigDecimal("20.00")));
        assertEquals(0, r.diferenciaTotal().compareTo(new BigDecimal("-2.50")));
        assertEquals(2, r.sesiones().stream().filter(s -> "CERRADA".equals(s.estado())).count());
        assertEquals(0, r.aperturas().compareTo(BigDecimal.ZERO));
    }
}
