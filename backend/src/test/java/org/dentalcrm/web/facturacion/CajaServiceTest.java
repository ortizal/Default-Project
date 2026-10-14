package org.dentalcrm.web.facturacion;

import org.dentalcrm.domain.caja.*;
import org.dentalcrm.domain.cuenta.CuentaDetalle;
import org.dentalcrm.domain.cuenta.CuentaPaciente;
import org.dentalcrm.domain.cuenta.FormaPago;
import org.dentalcrm.domain.cuenta.TipoMovimientoCuenta;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.domain.usuario.Usuario;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.service.CurrentUserService;
import org.dentalcrm.web.facturacion.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CajaServiceTest {

    private CajaService servicio;

    private CajaRepository cajaRepo;
    private MovimientoCajaRepository movRepo;
    private PacienteRepository pacienteRepo;
    private AuditService auditoria;

    private Paciente paciente;
    /** Estado simulado de la BD: una sesión de caja y sus movimientos. */
    private Caja caja;
    private final List<MovimientoCaja> movimientos = new ArrayList<>();

    @BeforeEach
    void setUp() {
        cajaRepo = mock(CajaRepository.class);
        movRepo = mock(MovimientoCajaRepository.class);
        pacienteRepo = mock(PacienteRepository.class);
        auditoria = mock(AuditService.class);

        paciente = new Paciente();
        paciente.setId(1L);
        paciente.setNombres("Ana");
        paciente.setApellidos("Pérez");
        when(pacienteRepo.findById(1L)).thenReturn(Optional.of(paciente));

        CurrentUserService usuarioActual = mock(CurrentUserService.class);
        Usuario usuario = new Usuario();
        usuario.setId(99L);
        usuario.setNombres("Laura");
        usuario.setApellidos("Gómez");
        when(usuarioActual.usuarioActual()).thenReturn(Optional.of(usuario));
        when(usuarioActual.idUsuarioActual()).thenReturn(Optional.of(99L));

        when(cajaRepo.findTopByEstadoOrderByAbiertoEnDesc(any(EstadoCaja.class)))
                .thenAnswer(inv -> {
                    EstadoCaja estado = inv.getArgument(0);
                    return caja != null && caja.getEstado() == estado
                            ? Optional.of(caja) : Optional.empty();
                });
        when(cajaRepo.save(any(Caja.class))).thenAnswer(inv -> {
            Caja c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(1L);
            }
            caja = c;
            return c;
        });
        when(cajaRepo.findById(any(Long.class))).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return caja != null && Objects.equals(caja.getId(), id)
                    ? Optional.of(caja) : Optional.empty();
        });
        when(cajaRepo.findByFechaBetweenOrderByFechaDescIdDesc(any(), any()))
                .thenAnswer(inv -> caja == null ? List.of() : List.of(caja));

        when(movRepo.save(any(MovimientoCaja.class))).thenAnswer(inv -> {
            MovimientoCaja m = inv.getArgument(0);
            if (m.getId() == null) {
                m.setId((long) (movimientos.size() + 1));
                movimientos.add(m);
            }
            return m;
        });
        when(movRepo.findByCajaIdOrderByFechaAscIdAsc(any()))
                .thenAnswer(inv -> new ArrayList<>(movimientos));
        when(movRepo.findByIdAndCajaId(any(), any())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return movimientos.stream().filter(m -> Objects.equals(m.getId(), id)).findFirst();
        });
        when(movRepo.findFirstByCuentaDetalleId(any())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return movimientos.stream()
                    .filter(m -> Objects.equals(m.getCuentaDetalleId(), id)).findFirst();
        });

        servicio = new CajaService(cajaRepo, movRepo, pacienteRepo, auditoria, usuarioActual);
    }

    // ------------------------------ utilidades ------------------------------

    private CuentaDetalle detalle(TipoMovimientoCuenta tipo, FormaPago forma, String monto) {
        return detalle(55L, tipo, forma, monto);
    }

    private CuentaDetalle detalle(Long id, TipoMovimientoCuenta tipo, FormaPago forma,
                                  String monto) {
        CuentaPaciente cuenta = new CuentaPaciente();
        cuenta.setId(10L);
        cuenta.setPaciente(paciente);

        CuentaDetalle d = new CuentaDetalle();
        d.setId(id);
        d.setCuenta(cuenta);
        d.setTipo(tipo);
        d.setConcepto("Cobro del tratamiento");
        d.setFormaPago(forma);
        d.setTotal(new BigDecimal(monto));
        d.setFecha(LocalDate.now());
        return d;
    }

    private BusinessException fallo(Runnable accion) {
        return assertThrows(BusinessException.class, accion::run);
    }

    private void movimiento(TipoMovimientoCaja tipo, FormaPago forma, String monto) {
        servicio.registrarMovimiento(caja.getId(), new NuevoMovimientoCajaRequest(tipo,
                "Movimiento", forma, new BigDecimal(monto), null));
    }

    // ------------------------------- sesión --------------------------------

    @Test
    void abrirCreaLaSesionYNoPermiteDobleApertura() {
        CajaResponse r = servicio.abrir(new AbrirCajaRequest("Caja principal",
                new BigDecimal("100"), "Fondo inicial"));

        assertEquals("ABIERTA", r.estado());
        assertEquals(0, r.montoApertura().compareTo(new BigDecimal("100")));
        assertEquals("Laura Gómez", r.responsable());
        assertEquals(0, r.totales().esperadoEfectivo().compareTo(new BigDecimal("100.00")));
        assertNull(r.totales().diferencia());
        assertNull(r.diferencia());
        verify(auditoria).registrar(eq("ABRIR_CAJA"), eq("FACTURACION"), eq("CAJA"), eq(1L),
                any(), any());

        BusinessException e = fallo(() -> servicio.abrir(new AbrirCajaRequest(null,
                BigDecimal.ZERO, null)));
        assertEquals("CAJA_ABIERTA", e.getCode());
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
    }

    @Test
    void fondoNegativoSeRechaza() {
        BusinessException e = fallo(() -> servicio.abrir(new AbrirCajaRequest(null,
                new BigDecimal("-1"), null)));
        assertEquals("MONTO_INVALIDO", e.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
    }

    @Test
    void actualSinSesionResponde404() {
        BusinessException e = fallo(servicio::actual);
        assertEquals("CAJA_NO_ABIERTA", e.getCode());
        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }

    @Test
    void cerrarCalculaEsperadoYDiferencia() {
        servicio.abrir(new AbrirCajaRequest(null, new BigDecimal("100"), null));
        movimiento(TipoMovimientoCaja.INGRESO, FormaPago.EFECTIVO, "50");
        movimiento(TipoMovimientoCaja.INGRESO, FormaPago.TARJETA, "40");
        movimiento(TipoMovimientoCaja.EGRESO, FormaPago.EFECTIVO, "20");

        CajaResponse r = servicio.cerrar(new CerrarCajaRequest(new BigDecimal("125"),
                "Corte de noche"));

        assertEquals("CERRADA", r.estado());
        // esperado = 100 (apertura) + 50 (efectivo) − 20 (efectivo) = 130
        assertEquals(0, r.totales().esperadoEfectivo().compareTo(new BigDecimal("130.00")));
        assertEquals(0, r.totales().ingresos().compareTo(new BigDecimal("90.00")));
        assertEquals(0, r.totales().egresos().compareTo(new BigDecimal("20.00")));
        assertEquals(0, r.diferencia().compareTo(new BigDecimal("-5.00")));
        assertEquals(3, r.totales().movimientos());

        FormaPagoTotal efectivo = r.totales().porForma().stream()
                .filter(f -> "EFECTIVO".equals(f.formaPago())).findFirst().orElseThrow();
        assertEquals(0, efectivo.monto().compareTo(new BigDecimal("30.00")));

        assertEquals("CAJA_NO_ABIERTA",
                fallo(() -> servicio.cerrar(new CerrarCajaRequest(BigDecimal.ZERO, null)))
                        .getCode());
        // cerrada, se puede abrir una sesión nueva
        assertEquals("ABIERTA",
                servicio.abrir(new AbrirCajaRequest(null, BigDecimal.ZERO, null)).estado());
        verify(auditoria).registrar(eq("CERRAR_CAJA"), anyString(), anyString(), any(), any(),
                any());
    }

    @Test
    void cerrarExigeMontoValido() {
        servicio.abrir(new AbrirCajaRequest(null, BigDecimal.ZERO, null));
        assertEquals("MONTO_INVALIDO",
                fallo(() -> servicio.cerrar(new CerrarCajaRequest(new BigDecimal("-1"), null)))
                        .getCode());
        assertEquals("MONTO_INVALIDO",
                fallo(() -> servicio.cerrar(new CerrarCajaRequest(null, null))).getCode());
    }

    // ------------------------------ movimientos ----------------------------

    @Test
    void movimientoManualExigeSesionAbiertaYMontoPositivo() {
        servicio.abrir(new AbrirCajaRequest(null, BigDecimal.ZERO, null));
        CajaResponse r = servicio.registrarMovimiento(1L, new NuevoMovimientoCajaRequest(
                TipoMovimientoCaja.EGRESO, "Compra de insumos", FormaPago.EFECTIVO,
                new BigDecimal("35.25"), null));

        assertEquals(1, r.movimientos().size());
        assertEquals(0, r.totales().egresos().compareTo(new BigDecimal("35.25")));
        assertEquals(0, r.totales().esperadoEfectivo()
                .compareTo(new BigDecimal("-35.25")));

        servicio.cerrar(new CerrarCajaRequest(BigDecimal.ZERO, null));
        assertEquals("CAJA_CERRADA",
                fallo(() -> servicio.registrarMovimiento(1L, new NuevoMovimientoCajaRequest(
                        TipoMovimientoCaja.INGRESO, "Otros", FormaPago.EFECTIVO,
                        BigDecimal.ONE, null))).getCode());
        assertEquals("CAJA_NO_ENCONTRADA",
                fallo(() -> servicio.registrarMovimiento(99L, new NuevoMovimientoCajaRequest(
                        TipoMovimientoCaja.INGRESO, "Otros", FormaPago.EFECTIVO,
                        BigDecimal.ONE, null))).getCode());
        assertEquals("MONTO_INVALIDO",
                fallo(() -> {
                    servicio.abrir(new AbrirCajaRequest(null, BigDecimal.ZERO, null));
                    servicio.registrarMovimiento(1L, new NuevoMovimientoCajaRequest(
                            TipoMovimientoCaja.INGRESO, "Otros", FormaPago.EFECTIVO,
                            BigDecimal.ZERO, null));
                }).getCode());
    }

    @Test
    void anularMovimientoExcluyeElMontoDelCorte() {
        servicio.abrir(new AbrirCajaRequest(null, BigDecimal.ZERO, null));
        servicio.registrarMovimiento(1L, new NuevoMovimientoCajaRequest(
                TipoMovimientoCaja.INGRESO, "Cobro", FormaPago.EFECTIVO,
                new BigDecimal("60"), null));

        CajaResponse r = servicio.anularMovimiento(1L, 1L, "cobro duplicado");
        assertEquals(0, r.totales().ingresos().compareTo(BigDecimal.ZERO));
        assertEquals(0, r.totales().esperadoEfectivo().compareTo(BigDecimal.ZERO));
        assertEquals(0, r.totales().movimientos());
        assertTrue(r.movimientos().get(0).anulado());

        assertEquals("MOVIMIENTO_YA_ANULADO",
                fallo(() -> servicio.anularMovimiento(1L, 1L, null)).getCode());
        assertEquals("MOVIMIENTO_NO_ENCONTRADO",
                fallo(() -> servicio.anularMovimiento(1L, 77L, null)).getCode());
    }

    @Test
    void listarRechazaRangoInvalido() {
        assertEquals("RANGO_INVALIDO",
                fallo(() -> servicio.listar(LocalDate.now(), LocalDate.now().minusDays(2)))
                        .getCode());
        assertEquals(0, servicio.listar(null, null).size());
    }

    // --------------------------- cuenta (§3.3) -----------------------------

    @Test
    void elCobroDeLaCuentaSeReflejaSoloEnLaCajaAbierta() {
        servicio.registrarDesdeCuenta(detalle(TipoMovimientoCuenta.ABONO,
                FormaPago.EFECTIVO, "50"));

        // sin sesión abierta no se registra nada
        assertEquals(0, movimientos.size());

        servicio.abrir(new AbrirCajaRequest(null, new BigDecimal("100"), null));
        servicio.registrarDesdeCuenta(detalle(55L, TipoMovimientoCuenta.ABONO,
                FormaPago.EFECTIVO, "50"));
        servicio.registrarDesdeCuenta(detalle(56L, TipoMovimientoCuenta.DEVOLUCION,
                FormaPago.EFECTIVO, "10"));
        servicio.registrarDesdeCuenta(detalle(55L, TipoMovimientoCuenta.ABONO,
                FormaPago.EFECTIVO, "50"));   // idempotente
        servicio.registrarDesdeCuenta(detalle(57L, TipoMovimientoCuenta.CARGO,
                FormaPago.EFECTIVO, "999"));  // un cargo no es dinero

        assertEquals(2, movimientos.size());
        MovimientoCaja ingreso = movimientos.get(0);
        assertEquals(TipoMovimientoCaja.INGRESO, ingreso.getTipo());
        assertEquals(55L, ingreso.getCuentaDetalleId());
        assertEquals(paciente, ingreso.getPaciente());
        assertEquals("Cobro: Cobro del tratamiento", ingreso.getConcepto());
        assertEquals(TipoMovimientoCaja.EGRESO, movimientos.get(1).getTipo());
    }

    @Test
    void anularElCobroEnLaCuentaAnulaSuMovimientoDeCaja() {
        servicio.abrir(new AbrirCajaRequest(null, BigDecimal.ZERO, null));
        servicio.registrarDesdeCuenta(detalle(TipoMovimientoCuenta.ABONO,
                FormaPago.TARJETA, "80"));

        servicio.anularPorDetalle(55L, "pago con tarjeta reversado");

        assertTrue(movimientos.get(0).isAnulado());
        assertEquals("pago con tarjeta reversado", movimientos.get(0).getAnuladoMotivo());
        // segunda anulación: ya estaba anulado, no vuelve a auditar
        servicio.anularPorDetalle(55L, "otra vez");
        verify(auditoria, times(1)).registrar(eq("ANULAR_MOVIMIENTO_CAJA"), anyString(),
                anyString(), any(), any(), any());
    }
}
