package org.dentalcrm.web.facturacion;

import org.dentalcrm.domain.cuenta.*;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.service.CurrentUserService;
import org.dentalcrm.web.facturacion.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CuentaPacienteServiceTest {

    private CuentaPacienteService servicio;

    private CuentaPacienteRepository cuentaRepo;
    private CuentaDetalleRepository detalleRepo;
    private PacienteRepository pacienteRepo;
    private AuditService auditoria;

    private CajaService cajaService;
    private ApplicationEventPublisher eventPublisher;
    private Paciente paciente;
    /** Estado simulado de la BD: una cuenta por paciente y su libro. */
    private CuentaPaciente cuenta;
    private final List<CuentaDetalle> libro = new ArrayList<>();

    @BeforeEach
    void setUp() {
        cuentaRepo = mock(CuentaPacienteRepository.class);
        detalleRepo = mock(CuentaDetalleRepository.class);
        pacienteRepo = mock(PacienteRepository.class);
        auditoria = mock(AuditService.class);
        cajaService = mock(CajaService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);

        paciente = new Paciente();
        paciente.setId(1L);
        paciente.setNombres("Ana");
        paciente.setApellidos("Pérez");
        paciente.setCedula("1712345678");
        when(pacienteRepo.findById(1L)).thenReturn(Optional.of(paciente));

        CurrentUserService usuarioActual = mock(CurrentUserService.class);
        when(usuarioActual.idUsuarioActual()).thenReturn(Optional.of(99L));

        when(cuentaRepo.findByPacienteId(1L)).thenAnswer(inv -> Optional.ofNullable(cuenta));
        when(cuentaRepo.findAllByOrderByUpdatedAtDesc())
                .thenAnswer(inv -> cuenta == null ? List.of() : List.of(cuenta));
        when(cuentaRepo.findByEstadoOrderByUpdatedAtDesc(any(EstadoCuenta.class)))
                .thenAnswer(inv -> {
                    EstadoCuenta estado = inv.getArgument(0);
                    return cuenta != null && cuenta.getEstado() == estado
                            ? List.of(cuenta) : List.of();
                });
        when(cuentaRepo.save(any(CuentaPaciente.class))).thenAnswer(inv -> {
            CuentaPaciente c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(10L);
            }
            cuenta = c;
            return c;
        });
        when(detalleRepo.save(any(CuentaDetalle.class))).thenAnswer(inv -> {
            CuentaDetalle d = inv.getArgument(0);
            if (d.getId() == null) {
                d.setId((long) (libro.size() + 1));
                libro.add(d);
            }
            return d;
        });
        when(detalleRepo.findByCuentaIdOrderByFechaAscIdAsc(any()))
                .thenAnswer(inv -> new ArrayList<>(libro));
        when(detalleRepo.findByCuentaIdAndOrigenAndReferenciaId(any(), any(), any()))
                .thenAnswer(inv -> libro.stream()
                        .filter(d -> d.getOrigen() == inv.getArgument(1)
                                && Objects.equals(d.getReferenciaId(), inv.getArgument(2)))
                        .toList());
        when(detalleRepo.findByIdAndCuentaId(any(), any())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return libro.stream().filter(d -> Objects.equals(d.getId(), id)).findFirst();
        });
        when(detalleRepo.countByCuentaIdAndAnuladoFalse(any()))
                .thenAnswer(inv -> libro.stream().filter(d -> !d.isAnulado()).count());
        when(detalleRepo.findByAnuladoFalseAndFechaBetweenOrderByFechaDescIdDesc(any(), any()))
                .thenAnswer(inv -> libro.stream()
                        .filter(d -> !d.isAnulado())
                        .sorted((a, b) -> b.getFecha().compareTo(a.getFecha()))
                        .toList());

        servicio = new CuentaPacienteService(cuentaRepo, detalleRepo, pacienteRepo,
                auditoria, usuarioActual, cajaService, eventPublisher);
    }

    // ------------------------------ utilidades ------------------------------

    private NuevoMovimientoCuentaRequest cargo(String concepto, String precio, String descuento) {
        return new NuevoMovimientoCuentaRequest(TipoMovimientoCuenta.CARGO, concepto,
                21, "O", 1, new BigDecimal(precio), new BigDecimal(descuento), null,
                null, LocalDate.now(), null);
    }

    private NuevoMovimientoCuentaRequest abono(String monto, FormaPago forma) {
        return new NuevoMovimientoCuentaRequest(TipoMovimientoCuenta.ABONO, "Abono",
                null, null, null, null, null, new BigDecimal(monto),
                forma, LocalDate.now(), null);
    }

    private NuevoMovimientoCuentaRequest anticipo(String monto, FormaPago forma) {
        return new NuevoMovimientoCuentaRequest(TipoMovimientoCuenta.ANTICIPO, "Anticipo",
                null, null, null, null, null, new BigDecimal(monto),
                forma, LocalDate.now(), null);
    }

    private NuevoMovimientoCuentaRequest descuento(String monto) {
        return new NuevoMovimientoCuentaRequest(TipoMovimientoCuenta.DESCUENTO, "Descuento",
                null, null, null, null, null, new BigDecimal(monto),
                null, LocalDate.now(), null);
    }

    private NuevoMovimientoCuentaRequest devolucion(String monto, FormaPago forma) {
        return new NuevoMovimientoCuentaRequest(TipoMovimientoCuenta.DEVOLUCION, "Devolución",
                null, null, null, null, null, new BigDecimal(monto),
                forma, LocalDate.now(), null);
    }

    private BusinessException fallo(Runnable accion) {
        return assertThrows(BusinessException.class, accion::run);
    }

    // ------------------------------ lectura --------------------------------

    @Test
    void cuentaSeCreaAlVueloEnAbiertaConSaldoCero() {
        CuentaPacienteResponse r = servicio.cuenta(1L);

        assertEquals(10L, r.id());
        assertEquals("ABIERTA", r.estado());
        assertEquals(0, r.saldo().compareTo(BigDecimal.ZERO));
        assertEquals(0, r.cantidadMovimientos());
        assertEquals("Ana Pérez", r.paciente());
        assertEquals("1712345678", r.cedula());
        verify(auditoria, never()).registrar(anyString(), anyString(), anyString(), any());
    }

    @Test
    void listarFiltraPorEstadoYTexto() {
        servicio.agregarMovimiento(1L, cargo("Limpieza", "80", "0"));

        assertEquals(1, servicio.listar("ABIERTA", "pérez").size());
        assertEquals(0, servicio.listar("PAGADA", "pérez").size());
        assertEquals(1, servicio.listar(null, "1712").size());
        assertEquals(0, servicio.listar(null, "no-existe").size());

        BusinessException e = fallo(() -> servicio.listar("COSA_RARA", null));
        assertEquals("ESTADO_INVALIDO", e.getCode());
    }

    // ----------------------------- movimientos -----------------------------

    @Test
    void cargoManualRecalculaSaldoYEstado() {
        CuentaPacienteResponse r = servicio.agregarMovimiento(1L,
                cargo("Obturación", "150.50", "0"));

        assertEquals(1, r.cantidadMovimientos());
        assertEquals(0, r.saldo().compareTo(new BigDecimal("150.50")));
        assertEquals(0, r.totalCargos().compareTo(new BigDecimal("150.50")));
        assertEquals("ABIERTA", r.estado());
        assertEquals(OrigenMovimientoCuenta.MANUAL,
                libro.get(0).getOrigen());
        verify(cajaService).registrarDesdeCuenta(libro.get(0));
        verify(auditoria).registrar(eq("AGREGAR_MOVIMIENTO"), eq("FACTURACION"),
                eq("CUENTA_DETALLE"), eq(1L), isNull(), any());
    }

    @Test
    void cargoConDescuentoPorcentualCalculaElTotal() {
        CuentaPacienteResponse r = servicio.agregarMovimiento(1L,
                cargo("Corona", "1000", "10"));

        assertEquals(0, r.totalCargos().compareTo(new BigDecimal("900.00")));
        assertEquals(0, r.saldo().compareTo(new BigDecimal("900.00")));
        assertEquals(0, libro.get(0).getDescuentoPct().compareTo(new BigDecimal("10")));
    }

    @Test
    void abonosParcialesYTotalesDerivanLosEstados() {
        servicio.agregarMovimiento(1L, cargo("Endodoncia", "200", "0"));
        CuentaPacienteResponse parcial = servicio.agregarMovimiento(1L,
                abono("50", FormaPago.EFECTIVO));
        assertEquals("PARCIAL", parcial.estado());
        assertEquals(0, parcial.saldo().compareTo(new BigDecimal("150.00")));
        assertEquals(0, parcial.totalAbonos().compareTo(new BigDecimal("50.00")));

        CuentaPacienteResponse pagada = servicio.agregarMovimiento(1L,
                abono("150", FormaPago.TRANSFERENCIA));
        assertEquals("PAGADA", pagada.estado());
        assertEquals(0, pagada.saldo().compareTo(BigDecimal.ZERO));
    }

    @Test
    void anticipoSobreCuentaSinCargosQuedaACredito() {
        CuentaPacienteResponse r = servicio.agregarMovimiento(1L,
                anticipo("100", FormaPago.EFECTIVO));

        assertEquals("ABIERTA", r.estado());
        assertEquals(0, r.saldo().compareTo(new BigDecimal("-100.00")));
        assertEquals(0, r.totalAbonos().compareTo(new BigDecimal("100.00")));
    }

    @Test
    void devolucionRecuperaSaldoSinExcederElCredito() {
        servicio.agregarMovimiento(1L, cargo("Control", "120", "0"));
        servicio.agregarMovimiento(1L, abono("120", FormaPago.TARJETA));

        CuentaPacienteResponse r = servicio.agregarMovimiento(1L,
                devolucion("30", FormaPago.EFECTIVO));
        assertEquals(0, r.saldo().compareTo(new BigDecimal("30.00")));
        assertEquals(0, r.totalAbonos().compareTo(new BigDecimal("90.00")));

        BusinessException e = fallo(() -> servicio.agregarMovimiento(1L,
                devolucion("1000", FormaPago.EFECTIVO)));
        assertEquals("SALDO_INSUFICIENTE", e.getCode());
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
    }

    @Test
    void abonoSinFormaDePagoYMontoInvalidoSeRechazan() {
        assertEquals("FORMA_PAGO_REQUERIDA",
                fallo(() -> servicio.agregarMovimiento(1L, abono("50", null))).getCode());
        assertEquals("MONTO_INVALIDO",
                fallo(() -> servicio.agregarMovimiento(1L,
                        abono("0", FormaPago.EFECTIVO))).getCode());
        assertEquals("DESCUENTO_EXCEDE",
                fallo(() -> {
                    servicio.agregarMovimiento(1L, cargo("Limpieza", "80", "0"));
                    servicio.agregarMovimiento(1L, descuento("90"));
                }).getCode());
        assertEquals("MONTO_INVALIDO",
                fallo(() -> servicio.agregarMovimiento(1L, cargo("Regalo", "0", "0"))).getCode());
    }

    @Test
    void anularMovimientoDejaElSaldoComoAntes() {
        CuentaPacienteResponse conCargo = servicio.agregarMovimiento(1L,
                cargo("Extracción", "90", "0"));
        CuentaPacienteResponse conAbono = servicio.agregarMovimiento(1L,
                abono("90", FormaPago.EFECTIVO));
        assertEquals("PAGADA", conAbono.estado());

        CuentaPacienteResponse anulada = servicio.anularMovimiento(1L,
                conCargo.movimientos().get(0).id(), new AnularMovimientoRequest("duplicado"));

        assertEquals(1, anulada.cantidadMovimientos());
        assertEquals(0, anulada.saldo().compareTo(new BigDecimal("-90.00")));
        assertEquals("ABIERTA", anulada.estado());
        verify(auditoria).registrar(eq("ANULAR_MOVIMIENTO"), anyString(), anyString(), any(),
                any(), any());
    }

    @Test
    void anularElMismoMovimientoDosVecesESConflicto() {
        CuentaPacienteResponse r = servicio.agregarMovimiento(1L,
                cargo("Sellador", "30", "0"));
        Long id = r.movimientos().get(0).id();

        servicio.anularMovimiento(1L, id, null);
        BusinessException e = fallo(() -> servicio.anularMovimiento(1L, id, null));
        assertEquals("MOVIMIENTO_YA_ANULADO", e.getCode());

        BusinessException otro = fallo(() -> servicio.anularMovimiento(1L, 999L, null));
        assertEquals("MOVIMIENTO_NO_ENCONTRADO", otro.getCode());
        assertEquals(HttpStatus.NOT_FOUND, otro.getStatus());
    }

    // ------------------------------- estados -------------------------------

    @Test
    void cerrarCuentaBloqueaMovimientosYReabrirlaLosVuelveAHabilitar() {
        servicio.agregarMovimiento(1L, cargo("Blanqueo", "200", "0"));
        CuentaPacienteResponse cerrada = servicio.cambiarEstado(1L,
                new EstadoCuentaRequest(EstadoCuenta.CERRADA, "Corte de mes"));
        assertEquals("CERRADA", cerrada.estado());

        BusinessException bloqueada = fallo(() -> servicio.agregarMovimiento(1L,
                abono("100", FormaPago.EFECTIVO)));
        assertEquals("CUENTA_BLOQUEADA", bloqueada.getCode());

        CuentaPacienteResponse reabierta = servicio.cambiarEstado(1L,
                new EstadoCuentaRequest(EstadoCuenta.ABIERTA, null));
        assertEquals("ABIERTA", reabierta.estado());
    }

    @Test
    void cerradaYAnuladaNoSeRepitenNiSeConfunden() {
        servicio.agregarMovimiento(1L, cargo("Ortodoncia", "300", "0"));

        servicio.cambiarEstado(1L, new EstadoCuentaRequest(EstadoCuenta.CERRADA, null));
        assertEquals("ESTADO_ACTUAL",
                fallo(() -> servicio.cambiarEstado(1L,
                        new EstadoCuentaRequest(EstadoCuenta.CERRADA, null))).getCode());
        assertEquals("CUENTA_BLOQUEADA",
                fallo(() -> servicio.agregarMovimiento(1L, abono("1", FormaPago.EFECTIVO)))
                        .getCode());

        CuentaPacienteResponse reabierta = servicio.cambiarEstado(1L,
                new EstadoCuentaRequest(EstadoCuenta.ABIERTA, null));
        assertEquals("ABIERTA", reabierta.estado());
        assertEquals("REAPERTURA_INVALIDA",
                fallo(() -> servicio.cambiarEstado(1L,
                        new EstadoCuentaRequest(EstadoCuenta.ABIERTA, null))).getCode());

        CuentaPacienteResponse anulada = servicio.cambiarEstado(1L,
                new EstadoCuentaRequest(EstadoCuenta.ANULADA, "Paciente duplicado"));
        assertEquals("ANULADA", anulada.estado());
        assertEquals("CUENTA_BLOQUEADA",
                fallo(() -> servicio.agregarMovimiento(1L, abono("1", FormaPago.EFECTIVO)))
                        .getCode());
        assertEquals("CUENTA_BLOQUEADA",
                fallo(() -> servicio.anularMovimiento(1L, 1L, null)).getCode());
        assertEquals("REAPERTURA_INVALIDA",
                fallo(() -> servicio.cambiarEstado(1L,
                        new EstadoCuentaRequest(EstadoCuenta.ABIERTA, null))).getCode());
        assertEquals("CUENTA_BLOQUEADA",
                fallo(() -> servicio.cambiarEstado(1L,
                        new EstadoCuentaRequest(EstadoCuenta.CERRADA, null))).getCode());
    }

    @Test
    void noSePuedeAnularUnaCuentaConDineroRecibido() {
        servicio.agregarMovimiento(1L, cargo("Control", "50", "0"));
        servicio.agregarMovimiento(1L, abono("50", FormaPago.EFECTIVO));

        BusinessException e = fallo(() -> servicio.cambiarEstado(1L,
                new EstadoCuentaRequest(EstadoCuenta.ANULADA, null)));
        assertEquals("CUENTA_CON_DINERO", e.getCode());
        assertEquals(HttpStatus.CONFLICT, e.getStatus());

        assertEquals("ESTADO_INVALIDO",
                fallo(() -> servicio.cambiarEstado(1L,
                        new EstadoCuentaRequest(EstadoCuenta.PAGADA, null))).getCode());
    }

    // ------------------------ integración §5 -------------------------------

    @Test
    void procedimientoCompletadoGeneraUnSoloCargo() {
        CuentaPacienteService.CargoProcedimiento cargo =
                new CuentaPacienteService.CargoProcedimiento(1L, 55L, "Implante",
                        1, new BigDecimal("700"), new BigDecimal("5"),
                        new BigDecimal("665.00"), 46, "O", null, null);

        servicio.cargoPorProcedimiento(cargo);
        servicio.cargoPorProcedimiento(cargo);

        assertEquals(1, libro.size());
        assertEquals(OrigenMovimientoCuenta.PROCEDIMIENTO, libro.get(0).getOrigen());
        assertEquals(55L, libro.get(0).getReferenciaId());
        assertEquals(0, libro.get(0).getTotal().compareTo(new BigDecimal("665.00")));
        assertEquals(0, cuenta.getSaldo().compareTo(new BigDecimal("665.00")));
        verify(auditoria).registrar(eq("CARGO_PROCEDIMIENTO"), eq("FACTURACION"),
                eq("CUENTA_DETALLE"), eq(1L), isNull(), any());
    }

    @Test
    void revertirElProcedimientoAnulaSuCargoYReactivarloAlCompletarDeNuevo() {
        servicio.cargoPorProcedimiento(new CuentaPacienteService.CargoProcedimiento(
                1L, 55L, "Implante", 1, BigDecimal.valueOf(700), BigDecimal.ZERO,
                BigDecimal.valueOf(700), 46, "O", null, null));
        assertEquals(0, cuenta.getSaldo().compareTo(BigDecimal.valueOf(700)));

        servicio.revertirCargoPorProcedimiento(1L, 55L);
        assertEquals(0, cuenta.getSaldo().compareTo(BigDecimal.ZERO));
        assertTrue(libro.get(0).isAnulado());
        assertEquals(0, cuenta.getTotalCargos().compareTo(BigDecimal.ZERO));

        servicio.cargoPorProcedimiento(new CuentaPacienteService.CargoProcedimiento(
                1L, 55L, "Implante", 1, BigDecimal.valueOf(700), BigDecimal.ZERO,
                BigDecimal.valueOf(700), 46, "O", null, null));
        assertEquals(1, libro.size());
        assertFalse(libro.get(0).isAnulado());
        assertEquals(0, cuenta.getSaldo().compareTo(BigDecimal.valueOf(700)));
    }

    @Test
    void elProcedimientoCompletadoCobraAunEnCuentaCerradaPeroNoEnAnulada() {
        servicio.agregarMovimiento(1L, cargo("Limpieza", "40", "0"));
        servicio.cambiarEstado(1L, new EstadoCuentaRequest(EstadoCuenta.CERRADA, null));

        servicio.cargoPorProcedimiento(new CuentaPacienteService.CargoProcedimiento(
                1L, 77L, "Resina", 1, BigDecimal.valueOf(60), BigDecimal.ZERO,
                BigDecimal.valueOf(60), 11, "O", null, null));
        assertEquals(2, libro.size());

        servicio.cambiarEstado(1L, new EstadoCuentaRequest(EstadoCuenta.ABIERTA, null));
        servicio.cambiarEstado(1L, new EstadoCuentaRequest(EstadoCuenta.ANULADA, null));
        long antes = libro.size();

        servicio.cargoPorProcedimiento(new CuentaPacienteService.CargoProcedimiento(
                1L, 88L, "Resina", 1, BigDecimal.valueOf(60), BigDecimal.ZERO,
                BigDecimal.valueOf(60), 11, "O", null, null));
        assertEquals(antes, libro.size());
    }

    @Test
    void sinCuentaPreviaElCargoDelProcedimientoLaCrea() {
        servicio.cargoPorProcedimiento(new CuentaPacienteService.CargoProcedimiento(
                1L, 9L, "Extracción", 1, BigDecimal.valueOf(80), BigDecimal.ZERO,
                BigDecimal.valueOf(80), 48, null, null, null));

        assertNotNull(cuenta);
        assertEquals(EstadoCuenta.ABIERTA, cuenta.getEstado());
        assertEquals(1, libro.size());
    }

    // -------------------------------- pagos --------------------------------

    @Test
    void pagosListaAbonosAnticiposYDevolucionesVigentes() {
        servicio.agregarMovimiento(1L, cargo("Limpieza", "60", "0"));
        servicio.agregarMovimiento(1L, abono("60", FormaPago.EFECTIVO));
        servicio.agregarMovimiento(1L, abono("40", FormaPago.TARJETA));
        CuentaPacienteResponse r = servicio.agregarMovimiento(1L,
                anticipo("25", FormaPago.TRANSFERENCIA));
        Long anticipoId = r.movimientos().stream()
                .filter(m -> "ANTICIPO".equals(m.tipo()))
                .findFirst().orElseThrow().id();
        servicio.agregarMovimiento(1L, cargo("Control", "10", "0"));
        servicio.anularMovimiento(1L, anticipoId, null);   // anula el anticipo

        List<MovimientoCuentaResponse> todos = servicio.pagos(null,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), null);
        assertEquals(2, todos.size());
        assertTrue(todos.stream().allMatch(p -> p.total().signum() > 0));

        List<MovimientoCuentaResponse> efectivo = servicio.pagos(null,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), "EFECTIVO");
        assertEquals(1, efectivo.size());
        assertEquals("EFECTIVO", efectivo.get(0).formaPago());

        assertEquals("RANGO_INVALIDO",
                fallo(() -> servicio.pagos(null, LocalDate.now(), LocalDate.now().minusDays(1),
                        null)).getCode());
        assertEquals("FORMA_PAGO_INVALIDA",
                fallo(() -> servicio.pagos(null, null, null, "BITCOIN")).getCode());
    }

    // --------------------- notificación post-cobro ----------------------

    @Test
    void elAbonoPublicaElEventoQueDisparaElAvisoPostCobro() {
        servicio.agregarMovimiento(1L, cargo("Limpieza", "80", "0"));

        servicio.agregarMovimiento(1L, abono("50", FormaPago.EFECTIVO));

        var captor = org.mockito.ArgumentCaptor.forClass(CobroRealizadoEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        CobroRealizadoEvent e = captor.getValue();
        assertEquals(TipoMovimientoCuenta.ABONO, e.tipo());
        assertEquals(1L, e.pacienteId());
        assertEquals("Abono", e.concepto());
        assertEquals(0, new BigDecimal("50.00").compareTo(e.monto()));
        assertEquals(FormaPago.EFECTIVO, e.formaPago());
        assertNotNull(e.saldo());
        assertNotNull(e.fecha());
    }

    @Test
    void unCargoOUnaDevolucionNoPublicanElEventoDeCobro() {
        servicio.agregarMovimiento(1L, cargo("Limpieza", "80", "0"));
        servicio.agregarMovimiento(1L, abono("80", FormaPago.TARJETA));
        verify(eventPublisher, times(1)).publishEvent(any(CobroRealizadoEvent.class));
        reset(eventPublisher);

        servicio.agregarMovimiento(1L, cargo("Control", "120", "0"));
        servicio.agregarMovimiento(1L, new NuevoMovimientoCuentaRequest(
                TipoMovimientoCuenta.DEVOLUCION, "Devolución", null, null, null, null, null,
                new BigDecimal("30"), FormaPago.EFECTIVO, LocalDate.now(), null));

        verify(eventPublisher, never()).publishEvent(any(CobroRealizadoEvent.class));
    }
}
