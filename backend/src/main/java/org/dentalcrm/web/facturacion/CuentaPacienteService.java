package org.dentalcrm.web.facturacion;

import org.dentalcrm.domain.cuenta.*;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.service.CurrentUserService;
import org.dentalcrm.web.facturacion.dto.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Cuenta del paciente — Fase F (plan §3.3): cargos clínicos y manuales,
 * descuentos, anticipos, abonos y devoluciones previos al comprobante.
 *
 * <p>El saldo no se recibe nunca del cliente: se recalcula siempre desde
 * el libro {@link CuentaDetalle} con el signo propio de cada tipo. Los
 * estados derivados (ABIERTA/PARCIAL/PAGADA) siguen al saldo; CERRADA y
 * ANULADA se fijan a mano y bloquean los movimientos manuales.
 *
 * <p>Integración §5 "Procedimiento → cuenta": al completar una línea del
 * plan se genera automáticamente su cargo (origen PROCEDIMIENTO) y al
 * revertir la línea se anula ese cargo.
 */
@Service
public class CuentaPacienteService {

    private static final String MODULO = "FACTURACION";
    private static final String ENTIDAD_CUENTA = "CUENTA_PACIENTE";
    private static final String ENTIDAD_MOVIMIENTO = "CUENTA_DETALLE";

    private final CuentaPacienteRepository cuentaRepository;
    private final CuentaDetalleRepository detalleRepository;
    private final PacienteRepository pacienteRepository;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;
    private final CajaService cajaService;
    private final ApplicationEventPublisher eventPublisher;

    public CuentaPacienteService(CuentaPacienteRepository cuentaRepository,
                                 CuentaDetalleRepository detalleRepository,
                                 PacienteRepository pacienteRepository,
                                 AuditService auditService,
                                 CurrentUserService currentUserService,
                                 CajaService cajaService,
                                 ApplicationEventPublisher eventPublisher) {
        this.cuentaRepository = cuentaRepository;
        this.detalleRepository = detalleRepository;
        this.pacienteRepository = pacienteRepository;
        this.auditService = auditService;
        this.currentUserService = currentUserService;
        this.cajaService = cajaService;
        this.eventPublisher = eventPublisher;
    }

    /** Cargo que genera automáticamente una línea de plan completada (§5). */
    public record CargoProcedimiento(
            Long pacienteId,
            Long lineaId,
            String concepto,
            Integer cantidad,
            BigDecimal precioUnitario,
            BigDecimal descuentoPct,
            BigDecimal total,
            Integer diente,
            String superficie,
            org.dentalcrm.domain.odontologo.Odontologo odontologo,
            org.dentalcrm.domain.servicio.Servicio servicio
    ) {
    }

    // ------------------------------------------------------------------
    // Lectura
    // ------------------------------------------------------------------

    /**
     * Cuenta del paciente. Se crea al vuelo si todavía no existe, de modo
     * que la ficha y los reportes siempre encuentran una cuenta.
     */
    @Transactional
    public CuentaPacienteResponse cuenta(Long pacienteId) {
        Paciente paciente = pacienteOArrojar(pacienteId);
        CuentaPaciente cuenta = buscarOCrear(paciente);
        return responder(cuenta, paciente);
    }

    @Transactional(readOnly = true)
    public List<CuentaResumenResponse> listar(String estado, String buscar) {
        List<CuentaPaciente> cuentas = (estado == null || estado.isBlank())
                ? cuentaRepository.findAllByOrderByUpdatedAtDesc()
                : cuentaRepository.findByEstadoOrderByUpdatedAtDesc(estadoDe(estado));

        String texto = buscar == null ? "" : buscar.trim().toLowerCase(Locale.ROOT);
        return cuentas.stream()
                .filter(c -> texto.isEmpty()
                        || nombrePaciente(c.getPaciente()).toLowerCase(Locale.ROOT).contains(texto)
                        || valor(c.getPaciente().getCedula()).contains(texto))
                .map(c -> CuentaResumenResponse.from(c, nombrePaciente(c.getPaciente()),
                        c.getPaciente().getCedula(),
                        (int) detalleRepository.countByCuentaIdAndAnuladoFalse(c.getId())))
                .toList();
    }

    /**
     * Abonos, anticipos y devoluciones de un rango (§3.15
     * {@code /api/v1/facturacion/pagos}). Por defecto: último año.
     */
    @Transactional(readOnly = true)
    public List<MovimientoCuentaResponse> pagos(Long pacienteId, LocalDate desde, LocalDate hasta,
                                                String formaPago) {
        LocalDate fin = hasta == null ? LocalDate.now() : hasta;
        LocalDate ini = desde == null ? fin.minusYears(1) : desde;
        if (ini.isAfter(fin)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "RANGO_INVALIDO",
                    "El inicio no puede ser posterior al fin");
        }
        FormaPago forma = formaPago == null || formaPago.isBlank() ? null : formaDe(formaPago);

        return detalleRepository.findByAnuladoFalseAndFechaBetweenOrderByFechaDescIdDesc(ini, fin)
                .stream()
                .filter(m -> m.getTipo().esCredito())
                .filter(m -> pacienteId == null || m.getCuenta().getPaciente().getId().equals(pacienteId))
                .filter(m -> forma == null || m.getFormaPago() == forma)
                .map(MovimientoCuentaResponse::from)
                .toList();
    }

    // ------------------------------------------------------------------
    // Movimientos
    // ------------------------------------------------------------------

    @Transactional
    public CuentaPacienteResponse agregarMovimiento(Long pacienteId,
                                                    NuevoMovimientoCuentaRequest request) {
        Paciente paciente = pacienteOArrojar(pacienteId);
        CuentaPaciente cuenta = buscarOCrear(paciente);
        if (cuenta.bloqueada()) {
            throw new BusinessException(HttpStatus.CONFLICT, "CUENTA_BLOQUEADA",
                    "La cuenta está en " + cuenta.getEstado().name() + " y no admite movimientos");
        }

        CuentaDetalle movimiento = construir(cuenta, request);
        detalleRepository.save(movimiento);
        // §3.4: el cobro se refleja solo en la caja si hay sesión abierta.
        cajaService.registrarDesdeCuenta(movimiento);
        recalcular(cuenta);
        cuentaRepository.save(cuenta);

        // Notificación post-cobro (openwa + correo): sólo dinero recibido.
        if (movimiento.getTipo().esDineroRecibido()) {
            eventPublisher.publishEvent(new CobroRealizadoEvent(
                    movimiento.getId(), paciente.getId(), movimiento.getTipo(),
                    movimiento.getConcepto(), movimiento.getTotal(), movimiento.getFormaPago(),
                    cuenta.getSaldo(), movimiento.getFecha()));
        }

        auditService.registrar("AGREGAR_MOVIMIENTO", MODULO, ENTIDAD_MOVIMIENTO, movimiento.getId(),
                null,
                Map.of("cuentaId", cuenta.getId(),
                        "tipo", movimiento.getTipo().name(),
                        "concepto", movimiento.getConcepto(),
                        "total", movimiento.getTotal()));
        return responder(cuenta, paciente);
    }

    /**
     * Anulación con traza: el movimiento sigue en el libro pero deja de
     * contar en el saldo. Sólo la cuenta ANULADA está congelada; una
     * cuenta CERRADA sí admite correcciones.
     */
    @Transactional
    public CuentaPacienteResponse anularMovimiento(Long pacienteId, Long movimientoId,
                                                   AnularMovimientoRequest request) {
        Paciente paciente = pacienteOArrojar(pacienteId);
        CuentaPaciente cuenta = buscarOCrear(paciente);
        if (cuenta.getEstado() == EstadoCuenta.ANULADA) {
            throw new BusinessException(HttpStatus.CONFLICT, "CUENTA_BLOQUEADA",
                    "La cuenta está ANULADA y no admite correcciones");
        }
        CuentaDetalle movimiento = detalleRepository
                .findByIdAndCuentaId(movimientoId, cuenta.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "MOVIMIENTO_NO_ENCONTRADO",
                        "El movimiento no pertenece a la cuenta de este paciente"));
        if (movimiento.isAnulado()) {
            throw new BusinessException(HttpStatus.CONFLICT, "MOVIMIENTO_YA_ANULADO",
                    "El movimiento ya fue anulado");
        }

        String motivo = request == null || request.motivo() == null || request.motivo().isBlank()
                ? null : request.motivo().trim();
        movimiento.setAnulado(true);
        movimiento.setAnuladoMotivo(motivo);
        detalleRepository.save(movimiento);
        cajaService.anularPorDetalle(movimiento.getId(), motivo);
        recalcular(cuenta);
        cuentaRepository.save(cuenta);

        auditService.registrar("ANULAR_MOVIMIENTO", MODULO, ENTIDAD_MOVIMIENTO, movimiento.getId(),
                Map.of("anulado", false),
                Map.of("anulado", true, "motivo", motivo == null ? "" : motivo));
        return responder(cuenta, paciente);
    }

    /**
     * CERRADA / ANULADA (o reabrirla con ABIERTA). ANULADA exige no tener
     * dinero recibido: no se borra historial con saldo a favor.
     */
    @Transactional
    public CuentaPacienteResponse cambiarEstado(Long pacienteId, EstadoCuentaRequest request) {
        Paciente paciente = pacienteOArrojar(pacienteId);
        CuentaPaciente cuenta = buscarOCrear(paciente);
        EstadoCuenta destino = request.estado();
        EstadoCuenta actual = cuenta.getEstado();
        String motivo = request.motivo() == null || request.motivo().isBlank()
                ? null : request.motivo().trim();

        if (destino == EstadoCuenta.CERRADA) {
            if (actual == EstadoCuenta.ANULADA) {
                throw new BusinessException(HttpStatus.CONFLICT, "CUENTA_BLOQUEADA",
                        "Una cuenta ANULADA no puede cerrarse");
            }
            if (actual == EstadoCuenta.CERRADA) {
                throw new BusinessException(HttpStatus.CONFLICT, "ESTADO_ACTUAL",
                        "La cuenta ya está CERRADA");
            }
            cuenta.setEstado(EstadoCuenta.CERRADA);
        } else if (destino == EstadoCuenta.ANULADA) {
            if (actual == EstadoCuenta.ANULADA) {
                throw new BusinessException(HttpStatus.CONFLICT, "ESTADO_ACTUAL",
                        "La cuenta ya está ANULADA");
            }
            if (cuenta.getTotalAbonos().signum() != 0) {
                throw new BusinessException(HttpStatus.CONFLICT, "CUENTA_CON_DINERO",
                        "La cuenta tiene dinero recibido (abonos/anticipos) y no puede anularse");
            }
            cuenta.setEstado(EstadoCuenta.ANULADA);
        } else if (destino == EstadoCuenta.ABIERTA) {
            if (actual != EstadoCuenta.CERRADA) {
                throw new BusinessException(HttpStatus.CONFLICT, "REAPERTURA_INVALIDA",
                        "Sólo una cuenta CERRADA puede reabrirse");
            }
            cuenta.setEstado(EstadoCuenta.ABIERTA);
            recalcular(cuenta);
        } else {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "ESTADO_INVALIDO",
                    "Sólo se permite CERRADA, ANULADA o reabrirla con ABIERTA");
        }
        cuentaRepository.save(cuenta);

        auditService.registrar("CAMBIAR_ESTADO_CUENTA", MODULO, ENTIDAD_CUENTA, cuenta.getId(),
                Map.of("estado", actual.name()),
                Map.of("estado", cuenta.getEstado().name(), "motivo", motivo == null ? "" : motivo));
        return responder(cuenta, paciente);
    }

    // ------------------------------------------------------------------
    // Integración §5: Procedimiento → cuenta
    // ------------------------------------------------------------------

    /**
     * Cargo automático al completar una línea de plan. Es idempotente
     * (unicidad de referencia) y también se ejecuta sobre una cuenta
     * CERRADA: el flujo clínico nunca depende de la tesorería. Una cuenta
     * ANULADA, en cambio, no recibe movimientos.
     */
    @Transactional
    public void cargoPorProcedimiento(CargoProcedimiento cargo) {
        Paciente paciente = pacienteOArrojar(cargo.pacienteId());
        CuentaPaciente cuenta = buscarOCrear(paciente);
        if (cuenta.getEstado() == EstadoCuenta.ANULADA) {
            return;
        }

        List<CuentaDetalle> existentes = detalleRepository.findByCuentaIdAndOrigenAndReferenciaId(
                cuenta.getId(), OrigenMovimientoCuenta.PROCEDIMIENTO, cargo.lineaId());
        if (!existentes.isEmpty()) {
            CuentaDetalle existente = existentes.get(0);
            if (existente.isAnulado()) {
                // Se completó de nuevo tras revertirse: se reactiva el cargo.
                existente.setAnulado(false);
                existente.setAnuladoMotivo(null);
                existente.setConcepto(concepto(cargo.concepto()));
                existente.setOdontologo(cargo.odontologo());
                existente.setServicio(cargo.servicio());
                existente.setCantidad(cantidad(cargo.cantidad()));
                existente.setPrecioUnitario(nz(cargo.precioUnitario()));
                existente.setDescuentoPct(nz(cargo.descuentoPct()));
                existente.setTotal(nz(cargo.total()));
                detalleRepository.save(existente);
                recalcular(cuenta);
                cuentaRepository.save(cuenta);
            }
            return;
        }

        CuentaDetalle movimiento = new CuentaDetalle();
        movimiento.setCuenta(cuenta);
        movimiento.setTipo(TipoMovimientoCuenta.CARGO);
        movimiento.setOrigen(OrigenMovimientoCuenta.PROCEDIMIENTO);
        movimiento.setReferenciaId(cargo.lineaId());
        movimiento.setOdontologo(cargo.odontologo());
        movimiento.setServicio(cargo.servicio());
        movimiento.setConcepto(concepto(cargo.concepto()));
        movimiento.setDiente(cargo.diente());
        movimiento.setSuperficie(cargo.superficie());
        movimiento.setCantidad(cantidad(cargo.cantidad()));
        movimiento.setPrecioUnitario(nz(cargo.precioUnitario()));
        movimiento.setDescuentoPct(nz(cargo.descuentoPct()));
        movimiento.setTotal(nz(cargo.total()));
        movimiento.setFecha(LocalDate.now());
        currentUserService.idUsuarioActual().ifPresent(movimiento::setCreatedBy);
        detalleRepository.save(movimiento);
        recalcular(cuenta);
        cuentaRepository.save(cuenta);

        auditService.registrar("CARGO_PROCEDIMIENTO", MODULO, ENTIDAD_MOVIMIENTO,
                movimiento.getId(), null,
                Map.of("pacienteId", paciente.getId(),
                        "lineaId", cargo.lineaId(),
                        "concepto", movimiento.getConcepto(),
                        "total", movimiento.getTotal()));
    }

    /** La línea del plan dejó de estar COMPLETADA: se anula su cargo. */
    @Transactional
    public void revertirCargoPorProcedimiento(Long pacienteId, Long lineaId) {
        Paciente paciente = pacienteOArrojar(pacienteId);
        Optional<CuentaPaciente> cuentaOpt = cuentaRepository.findByPacienteId(paciente.getId());
        if (cuentaOpt.isEmpty()) {
            return;
        }
        CuentaPaciente cuenta = cuentaOpt.get();
        boolean alguno = false;
        for (CuentaDetalle movimiento : detalleRepository.findByCuentaIdAndOrigenAndReferenciaId(
                cuenta.getId(), OrigenMovimientoCuenta.PROCEDIMIENTO, lineaId)) {
            if (movimiento.isAnulado()) {
                continue;
            }
            movimiento.setAnulado(true);
            movimiento.setAnuladoMotivo("Procedimiento revertido en el plan de tratamiento");
            detalleRepository.save(movimiento);
            alguno = true;
            auditService.registrar("ANULAR_MOVIMIENTO", MODULO, ENTIDAD_MOVIMIENTO,
                    movimiento.getId(), Map.of("anulado", false),
                    Map.of("anulado", true, "motivo", "Procedimiento revertido"));
        }
        if (alguno) {
            recalcular(cuenta);
            cuentaRepository.save(cuenta);
        }
    }

    // ------------------------------------------------------------------
    // Internos
    // ------------------------------------------------------------------

    private CuentaDetalle construir(CuentaPaciente cuenta, NuevoMovimientoCuentaRequest r) {
        CuentaDetalle movimiento = new CuentaDetalle();
        movimiento.setCuenta(cuenta);
        movimiento.setTipo(r.tipo());
        movimiento.setOrigen(OrigenMovimientoCuenta.MANUAL);
        movimiento.setConcepto(concepto(r.concepto()));
        movimiento.setFecha(r.fecha() == null ? LocalDate.now() : r.fecha());
        movimiento.setObservaciones(r.observaciones() == null || r.observaciones().isBlank()
                ? null : r.observaciones().trim());
        movimiento.setDiente(r.diente());
        movimiento.setSuperficie(r.superficie());
        currentUserService.idUsuarioActual().ifPresent(movimiento::setCreatedBy);

        switch (r.tipo()) {
            case CARGO -> {
                BigDecimal precio = nz(r.precioUnitario());
                BigDecimal descuento = nz(r.descuentoPct());
                if (precio.signum() <= 0) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "MONTO_INVALIDO",
                            "El cargo requiere un precio unitario mayor que cero");
                }
                if (descuento.signum() < 0 || descuento.compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "DESCUENTO_INVALIDO",
                            "El descuento debe estar entre 0 y 100");
                }
                int cantidad = cantidad(r.cantidad());
                BigDecimal bruto = precio.multiply(BigDecimal.valueOf(cantidad));
                BigDecimal total = bruto.multiply(BigDecimal.ONE.subtract(descuento.movePointLeft(2)))
                        .setScale(2, RoundingMode.HALF_UP);
                movimiento.setCantidad(cantidad);
                movimiento.setPrecioUnitario(precio);
                movimiento.setDescuentoPct(descuento);
                movimiento.setTotal(total);
            }
            case DESCUENTO -> {
                BigDecimal total = monto(r.total());
                BigDecimal disponibles = cuenta.getTotalCargos()
                        .subtract(cuenta.getTotalDescuentos());
                if (total.compareTo(disponibles) > 0) {
                    throw new BusinessException(HttpStatus.CONFLICT, "DESCUENTO_EXCEDE",
                            "El descuento supera los cargos vigentes de la cuenta");
                }
                rellenarMonto(movimiento, total, null);
            }
            case ABONO, ANTICIPO -> {
                if (r.formaPago() == null) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "FORMA_PAGO_REQUERIDA",
                            "Abonos, anticipos y devoluciones requieren forma de pago");
                }
                rellenarMonto(movimiento, monto(r.total()), r.formaPago());
            }
            case DEVOLUCION -> {
                if (r.formaPago() == null) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "FORMA_PAGO_REQUERIDA",
                            "Abonos, anticipos y devoluciones requieren forma de pago");
                }
                BigDecimal total = monto(r.total());
                if (total.compareTo(cuenta.getTotalAbonos()) > 0) {
                    throw new BusinessException(HttpStatus.CONFLICT, "SALDO_INSUFICIENTE",
                            "La devolución supera el dinero recibido del paciente ("
                                    + cuenta.getTotalAbonos().setScale(2, RoundingMode.HALF_UP) + ")");
                }
                rellenarMonto(movimiento, total, r.formaPago());
            }
        }
        return movimiento;
    }

    private void rellenarMonto(CuentaDetalle movimiento, BigDecimal total, FormaPago formaPago) {
        movimiento.setCantidad(1);
        movimiento.setPrecioUnitario(total);
        movimiento.setDescuentoPct(BigDecimal.ZERO);
        movimiento.setTotal(total);
        movimiento.setFormaPago(formaPago);
    }

    /**
     * Recalcula los totales denormalizados y, salvo que la cuenta esté
     * CERRADA o ANULADA, deriva el estado desde el saldo.
     */
    private void recalcular(CuentaPaciente cuenta) {
        BigDecimal cargos = BigDecimal.ZERO;
        BigDecimal descuentos = BigDecimal.ZERO;
        BigDecimal creditos = BigDecimal.ZERO;

        for (CuentaDetalle movimiento : detalleRepository
                .findByCuentaIdOrderByFechaAscIdAsc(cuenta.getId())) {
            if (movimiento.isAnulado()) {
                continue;
            }
            switch (movimiento.getTipo()) {
                case CARGO -> cargos = cargos.add(movimiento.getTotal());
                case DESCUENTO -> descuentos = descuentos.add(movimiento.getTotal());
                case ABONO, ANTICIPO -> creditos = creditos.add(movimiento.getTotal());
                case DEVOLUCION -> creditos = creditos.subtract(movimiento.getTotal());
            }
        }

        cuenta.setTotalCargos(cargos);
        cuenta.setTotalDescuentos(descuentos);
        cuenta.setTotalAbonos(creditos);
        cuenta.setSaldo(cargos.subtract(descuentos).subtract(creditos));

        if (cuenta.getEstado() == EstadoCuenta.CERRADA
                || cuenta.getEstado() == EstadoCuenta.ANULADA) {
            return;
        }
        if (cargos.signum() == 0) {
            cuenta.setEstado(EstadoCuenta.ABIERTA);
        } else if (cuenta.getSaldo().signum() <= 0) {
            cuenta.setEstado(EstadoCuenta.PAGADA);
        } else if (creditos.signum() > 0) {
            cuenta.setEstado(EstadoCuenta.PARCIAL);
        } else {
            cuenta.setEstado(EstadoCuenta.ABIERTA);
        }
    }

    private CuentaPaciente buscarOCrear(Paciente paciente) {
        return cuentaRepository.findByPacienteId(paciente.getId())
                .orElseGet(() -> {
                    CuentaPaciente cuenta = new CuentaPaciente();
                    cuenta.setPaciente(paciente);
                    cuenta.setEstado(EstadoCuenta.ABIERTA);
                    currentUserService.idUsuarioActual().ifPresent(cuenta::setCreatedBy);
                    return cuentaRepository.save(cuenta);
                });
    }

    private CuentaPacienteResponse responder(CuentaPaciente cuenta, Paciente paciente) {
        List<CuentaDetalle> movimientos =
                detalleRepository.findByCuentaIdOrderByFechaAscIdAsc(cuenta.getId());
        return CuentaPacienteResponse.from(cuenta, nombrePaciente(paciente),
                paciente.getCedula(), movimientos);
    }

    private Paciente pacienteOArrojar(Long id) {
        if (id == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "PACIENTE_REQUERIDO",
                    "El paciente es obligatorio");
        }
        return pacienteRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "PACIENTE_NO_ENCONTRADO", "Paciente no encontrado"));
    }

    private EstadoCuenta estadoDe(String estado) {
        try {
            return EstadoCuenta.valueOf(estado.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "ESTADO_INVALIDO",
                    "Estado de cuenta desconocido: " + estado);
        }
    }

    private FormaPago formaDe(String formaPago) {
        try {
            return FormaPago.valueOf(formaPago.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "FORMA_PAGO_INVALIDA",
                    "Forma de pago desconocida: " + formaPago);
        }
    }

    private BigDecimal monto(BigDecimal total) {
        if (total == null || total.signum() <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "MONTO_INVALIDO",
                    "El monto debe ser mayor que cero");
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private String concepto(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "CONCEPTO_REQUERIDO",
                    "El concepto del movimiento es obligatorio");
        }
        String texto = valor.trim();
        return texto.length() > 255 ? texto.substring(0, 255) : texto;
    }

    private int cantidad(Integer cantidad) {
        if (cantidad == null || cantidad < 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "CANTIDAD_INVALIDA",
                    "La cantidad debe ser al menos 1");
        }
        return cantidad;
    }

    private BigDecimal nz(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private String valor(String texto) {
        return texto == null ? "" : texto;
    }

    private String nombrePaciente(Paciente paciente) {
        return (paciente.getNombres() + " " + paciente.getApellidos()).trim();
    }
}
