package org.dentalcrm.web.facturacion;

import org.dentalcrm.domain.caja.*;
import org.dentalcrm.domain.cuenta.CuentaDetalle;
import org.dentalcrm.domain.cuenta.FormaPago;
import org.dentalcrm.domain.cuenta.TipoMovimientoCuenta;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.domain.usuario.Usuario;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.service.CurrentUserService;
import org.dentalcrm.web.facturacion.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Caja — Fase F (plan §3.4): apertura con fondo inicial, ingresos y
 * egresos por forma de pago, cierre con monto declarado y conciliación
 * por diferencia.
 *
 * <p>Cobros de la cuenta del paciente (§3.3): cuando hay una sesión
 * abierta, cada abono, anticipo o devolución se refleja solo en la caja
 * vinculando el movimiento de la cuenta, de modo que cobrar y cuadrar
 * son el mismo hecho. Si no hay caja abierta el cobro queda sólo en la
 * cuenta.
 */
@Service
public class CajaService {

    private static final String MODULO = "FACTURACION";
    private static final String ENTIDAD_CAJA = "CAJA";
    private static final String ENTIDAD_MOVIMIENTO = "MOVIMIENTO_CAJA";

    private final CajaRepository cajaRepository;
    private final MovimientoCajaRepository movimientoRepository;
    private final PacienteRepository pacienteRepository;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    public CajaService(CajaRepository cajaRepository,
                       MovimientoCajaRepository movimientoRepository,
                       PacienteRepository pacienteRepository,
                       AuditService auditService,
                       CurrentUserService currentUserService) {
        this.cajaRepository = cajaRepository;
        this.movimientoRepository = movimientoRepository;
        this.pacienteRepository = pacienteRepository;
        this.auditService = auditService;
        this.currentUserService = currentUserService;
    }

    // ------------------------------------------------------------------
    // Sesión
    // ------------------------------------------------------------------

    @Transactional
    public CajaResponse abrir(AbrirCajaRequest request) {
        if (cajaRepository.findTopByEstadoOrderByAbiertoEnDesc(EstadoCaja.ABIERTA).isPresent()) {
            throw new BusinessException(HttpStatus.CONFLICT, "CAJA_ABIERTA",
                    "Ya hay una sesión de caja abierta; ciérrala antes de abrir otra");
        }
        BigDecimal apertura = request.montoApertura() == null
                ? BigDecimal.ZERO : request.montoApertura().setScale(2, RoundingMode.HALF_UP);
        if (apertura.signum() < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "MONTO_INVALIDO",
                    "El fondo de apertura no puede ser negativo");
        }

        Caja caja = new Caja();
        caja.setNombre(request.nombre() == null || request.nombre().isBlank()
                ? "Caja" : request.nombre().trim());
        caja.setEstado(EstadoCaja.ABIERTA);
        caja.setFecha(LocalDate.now());
        caja.setMontoApertura(apertura);
        caja.setObservacionesApertura(vacio(request.observaciones()));
        caja.setAbiertoEn(java.time.Instant.now());
        currentUserService.usuarioActual().ifPresent(u -> {
            caja.setUsuario(u);
            caja.setAbiertoPor(u.getId());
        });
        Caja guardada = cajaRepository.save(caja);

        auditService.registrar("ABRIR_CAJA", MODULO, ENTIDAD_CAJA, guardada.getId(), null,
                Map.of("fecha", guardada.getFecha().toString(),
                        "apertura", guardada.getMontoApertura()));
        return responder(guardada, List.of());
    }

    @Transactional
    public CajaResponse actual() {
        Caja caja = cajaOAbiertaOArrojar();
        return responder(caja, movimientoRepository.findByCajaIdOrderByFechaAscIdAsc(caja.getId()));
    }

    @Transactional
    public CajaResponse cerrar(CerrarCajaRequest request) {
        Caja caja = cajaOAbiertaOArrojar();
        if (request.montoDeclarado() == null || request.montoDeclarado().signum() < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "MONTO_INVALIDO",
                    "El efectivo declarado es obligatorio y no puede ser negativo");
        }
        List<MovimientoCaja> movimientos =
                movimientoRepository.findByCajaIdOrderByFechaAscIdAsc(caja.getId());
        TotalesCaja totales = totales(caja, movimientos);

        BigDecimal declarado = request.montoDeclarado().setScale(2, RoundingMode.HALF_UP);
        BigDecimal diferencia = declarado.subtract(totales.esperadoEfectivo())
                .setScale(2, RoundingMode.HALF_UP);
        caja.setMontoDeclarado(declarado);
        caja.setDiferencia(diferencia);
        caja.setObservacionesCierre(vacio(request.observaciones()));
        caja.setEstado(EstadoCaja.CERRADA);
        caja.setCerradoEn(java.time.Instant.now());
        currentUserService.idUsuarioActual().ifPresent(caja::setCerradoPor);
        cajaRepository.save(caja);

        auditService.registrar("CERRAR_CAJA", MODULO, ENTIDAD_CAJA, caja.getId(),
                Map.of("estado", EstadoCaja.ABIERTA.name()),
                Map.of("estado", EstadoCaja.CERRADA.name(),
                        "declarado", declarado,
                        "esperado", totales.esperadoEfectivo(),
                        "diferencia", diferencia));
        return responder(caja, movimientos);
    }

    @Transactional(readOnly = true)
    public CajaResponse obtener(Long id) {
        Caja caja = cajaRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "CAJA_NO_ENCONTRADA",
                        "Sesión de caja no encontrada"));
        return responder(caja, movimientoRepository.findByCajaIdOrderByFechaAscIdAsc(caja.getId()));
    }

    /** Historial de sesiones por rango (por defecto: último año). */
    @Transactional(readOnly = true)
    public List<CajaResponse> listar(LocalDate desde, LocalDate hasta) {
        LocalDate fin = hasta == null ? LocalDate.now() : hasta;
        LocalDate ini = desde == null ? fin.minusYears(1) : desde;
        if (ini.isAfter(fin)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "RANGO_INVALIDO",
                    "El inicio no puede ser posterior al fin");
        }
        List<CajaResponse> respuesta = new ArrayList<>();
        for (Caja caja : cajaRepository.findByFechaBetweenOrderByFechaDescIdDesc(ini, fin)) {
            respuesta.add(responder(caja,
                    movimientoRepository.findByCajaIdOrderByFechaAscIdAsc(caja.getId())));
        }
        return respuesta;
    }

    // ------------------------------------------------------------------
    // Movimientos
    // ------------------------------------------------------------------

    @Transactional
    public CajaResponse registrarMovimiento(Long cajaId, NuevoMovimientoCajaRequest request) {
        Caja caja = caja(cajaId);
        exigirAbierta(caja);
        if (request.monto() == null || request.monto().signum() <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "MONTO_INVALIDO",
                    "El monto debe ser mayor que cero");
        }
        Paciente paciente = request.pacienteId() == null ? null
                : pacienteRepository.findById(request.pacienteId())
                        .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                                "PACIENTE_NO_ENCONTRADO", "Paciente no encontrado"));

        MovimientoCaja movimiento = new MovimientoCaja();
        movimiento.setCaja(caja);
        movimiento.setTipo(request.tipo());
        movimiento.setConcepto(request.concepto().trim());
        movimiento.setFormaPago(request.formaPago());
        movimiento.setMonto(request.monto().setScale(2, RoundingMode.HALF_UP));
        movimiento.setPaciente(paciente);
        movimiento.setFecha(LocalDate.now());
        currentUserService.idUsuarioActual().ifPresent(movimiento::setCreatedBy);
        movimientoRepository.save(movimiento);

        auditService.registrar("REGISTRAR_MOVIMIENTO_CAJA", MODULO, ENTIDAD_MOVIMIENTO,
                movimiento.getId(), null,
                Map.of("cajaId", caja.getId(),
                        "tipo", movimiento.getTipo().name(),
                        "concepto", movimiento.getConcepto(),
                        "monto", movimiento.getMonto()));
        return responder(caja, movimientoRepository.findByCajaIdOrderByFechaAscIdAsc(caja.getId()));
    }

    @Transactional
    public CajaResponse anularMovimiento(Long cajaId, Long movimientoId, String motivo) {
        Caja caja = caja(cajaId);
        MovimientoCaja movimiento = movimientoRepository.findByIdAndCajaId(movimientoId, caja.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "MOVIMIENTO_NO_ENCONTRADO", "El movimiento no pertenece a esta sesión"));
        if (movimiento.isAnulado()) {
            throw new BusinessException(HttpStatus.CONFLICT, "MOVIMIENTO_YA_ANULADO",
                    "El movimiento ya fue anulado");
        }
        movimiento.setAnulado(true);
        movimiento.setAnuladoMotivo(vacio(motivo));
        movimientoRepository.save(movimiento);

        auditService.registrar("ANULAR_MOVIMIENTO_CAJA", MODULO, ENTIDAD_MOVIMIENTO,
                movimiento.getId(), Map.of("anulado", false),
                Map.of("anulado", true, "motivo", motivo == null ? "" : motivo));
        return responder(caja, movimientoRepository.findByCajaIdOrderByFechaAscIdAsc(caja.getId()));
    }

    // --------------------------- cuenta (§3.3) --------------------------

    /**
     * Refleja en la caja abierta un abono, anticipo o devolución de la
     * cuenta del paciente. Idempotente: si el movimiento de caja ya
     * existe no se duplica. Sin sesión abierta no hace nada.
     */
    @Transactional
    public void registrarDesdeCuenta(CuentaDetalle detalle) {
        if (detalle.getTipo() == TipoMovimientoCuenta.CARGO
                || detalle.getTipo() == TipoMovimientoCuenta.DESCUENTO) {
            return;
        }
        if (movimientoRepository.findFirstByCuentaDetalleId(detalle.getId()).isPresent()) {
            return;
        }
        Optional<Caja> abierta =
                cajaRepository.findTopByEstadoOrderByAbiertoEnDesc(EstadoCaja.ABIERTA);
        if (abierta.isEmpty() || detalle.getFormaPago() == null) {
            return;
        }

        MovimientoCaja movimiento = new MovimientoCaja();
        movimiento.setCaja(abierta.get());
        boolean egreso = detalle.getTipo() == TipoMovimientoCuenta.DEVOLUCION;
        movimiento.setTipo(egreso ? TipoMovimientoCaja.EGRESO : TipoMovimientoCaja.INGRESO);
        movimiento.setConcepto((egreso ? "Devolución: " : "Cobro: ") + detalle.getConcepto());
        movimiento.setFormaPago(detalle.getFormaPago());
        movimiento.setMonto(detalle.getTotal());
        movimiento.setCuentaDetalleId(detalle.getId());
        movimiento.setPaciente(detalle.getCuenta().getPaciente());
        movimiento.setFecha(detalle.getFecha());
        currentUserService.idUsuarioActual().ifPresent(movimiento::setCreatedBy);
        movimientoRepository.save(movimiento);
    }

    /**
     * Anula el movimiento de caja que nació de un movimiento de la
     * cuenta (también si la sesión ya se cerró: es una corrección y
     * queda registrada, sin reabrir el corte).
     */
    @Transactional
    public void anularPorDetalle(Long cuentaDetalleId, String motivo) {
        movimientoRepository.findFirstByCuentaDetalleId(cuentaDetalleId).ifPresent(m -> {
            if (m.isAnulado()) {
                return;
            }
            m.setAnulado(true);
            m.setAnuladoMotivo(vacio(motivo));
            movimientoRepository.save(m);
            auditService.registrar("ANULAR_MOVIMIENTO_CAJA", MODULO, ENTIDAD_MOVIMIENTO,
                    m.getId(), Map.of("anulado", false),
                    Map.of("anulado", true, "motivo", motivo == null ? "" : motivo,
                            "origen", "cuenta_paciente"));
        });
    }

    // ------------------------------------------------------------------
    // Internos
    // ------------------------------------------------------------------

    private TotalesCaja totales(Caja caja, List<MovimientoCaja> movimientos) {
        BigDecimal apertura = nz(caja.getMontoApertura());
        BigDecimal ingresos = BigDecimal.ZERO;
        BigDecimal egresos = BigDecimal.ZERO;
        BigDecimal efectivo = BigDecimal.ZERO;
        Map<FormaPago, BigDecimal> porForma = new EnumMap<>(FormaPago.class);
        int activos = 0;

        for (MovimientoCaja movimiento : movimientos) {
            if (movimiento.isAnulado()) {
                continue;
            }
            activos++;
            BigDecimal monto = nz(movimiento.getMonto());
            boolean efectivoMov = movimiento.getFormaPago() == FormaPago.EFECTIVO;
            if (movimiento.getTipo() == TipoMovimientoCaja.INGRESO) {
                ingresos = ingresos.add(monto);
                porForma.merge(movimiento.getFormaPago(), monto, BigDecimal::add);
                if (efectivoMov) {
                    efectivo = efectivo.add(monto);
                }
            } else {
                egresos = egresos.add(monto);
                porForma.merge(movimiento.getFormaPago(), monto.negate(), BigDecimal::add);
                if (efectivoMov) {
                    efectivo = efectivo.subtract(monto);
                }
            }
        }

        BigDecimal esperado = apertura.add(efectivo);
        BigDecimal declarado = caja.getMontoDeclarado();
        BigDecimal diferencia = declarado == null ? null
                : declarado.subtract(esperado).setScale(2, RoundingMode.HALF_UP);
        List<FormaPagoTotal> detallePorForma = porForma.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new FormaPagoTotal(e.getKey().name(), e.getValue()
                        .setScale(2, RoundingMode.HALF_UP)))
                .toList();

        return new TotalesCaja(apertura.setScale(2, RoundingMode.HALF_UP),
                ingresos.setScale(2, RoundingMode.HALF_UP),
                egresos.setScale(2, RoundingMode.HALF_UP),
                declarado == null ? null : declarado.setScale(2, RoundingMode.HALF_UP),
                esperado.setScale(2, RoundingMode.HALF_UP),
                diferencia, detallePorForma, activos);
    }

    private CajaResponse responder(Caja caja, List<MovimientoCaja> movimientos) {
        List<MovimientoCajaResponse> filas = movimientos.stream()
                .map(m -> MovimientoCajaResponse.from(m,
                        m.getPaciente() == null ? null : nombrePaciente(m.getPaciente())))
                .toList();
        return new CajaResponse(caja.getId(), caja.getNombre(), caja.getEstado().name(),
                caja.getFecha(), caja.getMontoApertura(), caja.getMontoDeclarado(),
                caja.getDiferencia(),
                nombreUsuario(caja.getUsuario()),
                caja.getAbiertoPor() == null ? null : String.valueOf(caja.getAbiertoPor()),
                caja.getAbiertoEn(),
                caja.getCerradoPor() == null ? null : String.valueOf(caja.getCerradoPor()),
                caja.getCerradoEn(), caja.getObservacionesApertura(),
                caja.getObservacionesCierre(), totales(caja, movimientos), filas,
                caja.getCreatedAt(), caja.getUpdatedAt());
    }

    private Caja caja(Long id) {
        return cajaRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "CAJA_NO_ENCONTRADA",
                        "Sesión de caja no encontrada"));
    }

    private Caja cajaOAbiertaOArrojar() {
        return cajaRepository.findTopByEstadoOrderByAbiertoEnDesc(EstadoCaja.ABIERTA)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "CAJA_NO_ABIERTA",
                        "No hay ninguna sesión de caja abierta"));
    }

    private void exigirAbierta(Caja caja) {
        if (caja.getEstado() != EstadoCaja.ABIERTA) {
            throw new BusinessException(HttpStatus.CONFLICT, "CAJA_CERRADA",
                    "La sesión de caja está cerrada y no admite movimientos");
        }
    }

    private String nombrePaciente(Paciente paciente) {
        return (paciente.getNombres() + " " + paciente.getApellidos()).trim();
    }

    private String nombreUsuario(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        String nombre = (val(usuario.getNombres()) + " " + val(usuario.getApellidos())).trim();
        return nombre.isBlank() ? usuario.getUsername() : nombre;
    }

    private String val(String texto) {
        return texto == null ? "" : texto;
    }

    private String vacio(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private BigDecimal nz(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
