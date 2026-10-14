package org.dentalcrm.web.plan;

import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.odontologo.OdontologoRepository;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.domain.plan.*;
import org.dentalcrm.domain.servicio.Servicio;
import org.dentalcrm.domain.servicio.ServicioRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.service.CurrentUserService;
import org.dentalcrm.util.HashUtil;
import org.dentalcrm.web.facturacion.CuentaPacienteService;
import org.dentalcrm.web.plan.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Planes de tratamiento (Fase D, plan §4.8-§4.9): CRUD con líneas por
 * procedimiento, matriz de estados, presupuestos versionados con
 * snapshot y aceptación firmada.
 */
@Service
public class PlanTratamientoService {

    private static final String MODULO = "PLAN_TRATAMIENTO";
    private static final String ENTIDAD_PLAN = "PLAN_TRATAMIENTO";
    private static final String ENTIDAD_LINEA = "PLAN_LINEA";
    private static final String ENTIDAD_PRESUPUESTO = "PRESUPUESTO";

    /** Matriz de transiciones permitidas del plan (plan §4.8). */
    private static final Map<EstadoPlanTratamiento, Set<EstadoPlanTratamiento>> TRANSICIONES =
            Map.of(
                    EstadoPlanTratamiento.BORRADOR,
                    Set.of(EstadoPlanTratamiento.PRESENTADO, EstadoPlanTratamiento.CANCELADO),
                    EstadoPlanTratamiento.PRESENTADO,
                    Set.of(EstadoPlanTratamiento.ACEPTADO,
                            EstadoPlanTratamiento.PARCIALMENTE_ACEPTADO,
                            EstadoPlanTratamiento.RECHAZADO,
                            EstadoPlanTratamiento.BORRADOR,
                            EstadoPlanTratamiento.CANCELADO),
                    EstadoPlanTratamiento.ACEPTADO,
                    Set.of(EstadoPlanTratamiento.EN_EJECUCION, EstadoPlanTratamiento.CANCELADO),
                    EstadoPlanTratamiento.PARCIALMENTE_ACEPTADO,
                    Set.of(EstadoPlanTratamiento.EN_EJECUCION, EstadoPlanTratamiento.CANCELADO),
                    EstadoPlanTratamiento.RECHAZADO,
                    Set.of(EstadoPlanTratamiento.BORRADOR, EstadoPlanTratamiento.CANCELADO),
                    EstadoPlanTratamiento.EN_EJECUCION,
                    Set.of(EstadoPlanTratamiento.COMPLETADO, EstadoPlanTratamiento.CANCELADO),
                    EstadoPlanTratamiento.COMPLETADO, Set.of(),
                    EstadoPlanTratamiento.CANCELADO, Set.of());

    private static final Set<EstadoPlanTratamiento> ESTADOS_EDITABLES =
            Set.of(EstadoPlanTratamiento.BORRADOR, EstadoPlanTratamiento.PRESENTADO,
                    EstadoPlanTratamiento.RECHAZADO);

    private final PlanTratamientoRepository planRepository;
    private final PlanLineaRepository lineaRepository;
    private final PresupuestoRepository presupuestoRepository;
    private final PresupuestoLineaRepository presupuestoLineaRepository;
    private final PacienteRepository pacienteRepository;
    private final OdontologoRepository odontologoRepository;
    private final ServicioRepository servicioRepository;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;
    private final PlanPdfService pdfService;
    private final CuentaPacienteService cuentaPacienteService;

    public PlanTratamientoService(PlanTratamientoRepository planRepository,
                                  PlanLineaRepository lineaRepository,
                                  PresupuestoRepository presupuestoRepository,
                                  PresupuestoLineaRepository presupuestoLineaRepository,
                                  PacienteRepository pacienteRepository,
                                  OdontologoRepository odontologoRepository,
                                  ServicioRepository servicioRepository,
                                  AuditService auditService,
                                  CurrentUserService currentUserService,
                                  PlanPdfService pdfService,
                                  CuentaPacienteService cuentaPacienteService) {
        this.planRepository = planRepository;
        this.lineaRepository = lineaRepository;
        this.presupuestoRepository = presupuestoRepository;
        this.presupuestoLineaRepository = presupuestoLineaRepository;
        this.pacienteRepository = pacienteRepository;
        this.odontologoRepository = odontologoRepository;
        this.servicioRepository = servicioRepository;
        this.auditService = auditService;
        this.currentUserService = currentUserService;
        this.pdfService = pdfService;
        this.cuentaPacienteService = cuentaPacienteService;
    }

    // ------------------------------- Plan -------------------------------

    @Transactional(readOnly = true)
    public List<PlanResumenResponse> listar(Long pacienteId) {
        paciente(pacienteId);
        return planRepository.findByPacienteIdOrderByFechaDescIdDesc(pacienteId).stream()
                .map(p -> PlanResumenResponse.from(p,
                        (int) lineaRepository.countByPlanId(p.getId()),
                        (int) presupuestoRepository.countByPlanId(p.getId()),
                        nombreOdontologo(p.getOdontologo())))
                .toList();
    }

    @Transactional
    public PlanResponse crear(Long pacienteId, PlanRequest request) {
        Paciente paciente = paciente(pacienteId);
        PlanTratamiento plan = new PlanTratamiento();
        plan.setPaciente(paciente);
        if (request.odontologoId() != null) {
            plan.setOdontologo(odontologoRepository.findById(request.odontologoId())
                    .orElseThrow(() -> noEncontrado("Odontólogo")));
        }
        plan.setTitulo(request.titulo().trim());
        plan.setObservaciones(request.observaciones());
        plan.setFecha(request.fecha() == null ? LocalDate.now() : request.fecha());
        plan.setEstado(EstadoPlanTratamiento.BORRADOR);
        plan.setSubtotal(BigDecimal.ZERO);
        plan.setTotal(BigDecimal.ZERO);
        currentUserService.idUsuarioActual().ifPresent(plan::setCreadoPor);

        PlanTratamiento guardado = planRepository.save(plan);
        auditService.registrar("CREAR_PLAN", MODULO, ENTIDAD_PLAN, guardado.getId(),
                null, PlanResumenResponse.from(guardado, 0, 0, null));
        return obtener(guardado.getId());
    }

    @Transactional(readOnly = true)
    public PlanResponse obtener(Long id) {
        PlanTratamiento plan = plan(id);
        return PlanResponse.from(plan,
                nombrePaciente(plan.getPaciente()),
                nombreOdontologo(plan.getOdontologo()),
                plan.getLineas().stream().map(LineaResponse::from).toList(),
                presupuestos(id));
    }

    @Transactional
    public PlanResponse editar(Long id, PlanRequest request) {
        PlanTratamiento plan = plan(id);
        soloEditables(plan);
        PlanResponse antes = PlanResponse.from(plan, nombrePaciente(plan.getPaciente()),
                nombreOdontologo(plan.getOdontologo()),
                plan.getLineas().stream().map(LineaResponse::from).toList(),
                Collections.emptyList());
        if (request.odontologoId() != null) {
            plan.setOdontologo(odontologoRepository.findById(request.odontologoId())
                    .orElseThrow(() -> noEncontrado("Odontólogo")));
        }
        plan.setTitulo(request.titulo().trim());
        plan.setObservaciones(request.observaciones());
        PlanTratamiento guardado = planRepository.save(plan);
        auditService.registrar("MODIFICAR_PLAN", MODULO, ENTIDAD_PLAN, guardado.getId(),
                antes, PlanResponse.from(guardado, nombrePaciente(guardado.getPaciente()),
                        nombreOdontologo(guardado.getOdontologo()), Collections.emptyList(),
                        Collections.emptyList()));
        return obtener(id);
    }

    @Transactional
    public void eliminar(Long id) {
        PlanTratamiento plan = plan(id);
        if (plan.getEstado() != EstadoPlanTratamiento.BORRADOR) {
            throw new BusinessException(HttpStatus.CONFLICT, "PLAN_NO_BORRADOR",
                    "Sólo se pueden eliminar planes en borrador");
        }
        PlanResumenResponse antes = PlanResumenResponse.from(plan,
                plan.getLineas().size(), (int) presupuestoRepository.countByPlanId(id),
                nombreOdontologo(plan.getOdontologo()));
        planRepository.delete(plan);
        auditService.registrar("ELIMINAR_PLAN", MODULO, ENTIDAD_PLAN, id, antes, null);
    }

    @Transactional
    public PlanResponse cambiarEstado(Long id, EstadoPlanRequest request) {
        PlanTratamiento plan = plan(id);
        EstadoPlanTratamiento destino = request.estado();
        if (destino == plan.getEstado()) {
            throw new BusinessException(HttpStatus.CONFLICT, "ESTADO_ACTUAL",
                    "El plan ya está en " + destino.name());
        }
        if (!TRANSICIONES.getOrDefault(plan.getEstado(), Set.of()).contains(destino)) {
            throw new BusinessException(HttpStatus.CONFLICT, "TRANSICION_INVALIDA",
                    "No se puede pasar de " + plan.getEstado().name() + " a " + destino.name());
        }
        EstadoPlanTratamiento anterior = plan.getEstado();
        plan.setEstado(destino);
        plan.setComentario(request.comentario());
        PlanTratamiento guardado = planRepository.save(plan);
        auditService.registrar("CAMBIAR_ESTADO_PLAN", MODULO, ENTIDAD_PLAN, id,
                Map.of("estado", anterior.name()),
                Map.of("estado", destino.name(),
                        "comentario", request.comentario() == null ? "" : request.comentario()));
        return obtener(id);
    }

    // ------------------------------ Líneas ------------------------------

    @Transactional
    public LineaResponse agregarLinea(Long planId, LineaRequest request) {
        PlanTratamiento plan = plan(planId);
        soloBorrador(plan);

        PlanLinea linea = new PlanLinea();
        linea.setPlan(plan);
        linea.setNumero(request.numero() != null && request.numero() > 0
                ? request.numero()
                : siguienteNumero(plan));
        aplicarLinea(linea, request);

        plan.getLineas().add(linea);
        PlanLinea guardada = lineaRepository.save(linea);
        actualizarTotales(plan);
        auditService.registrar("AGREGAR_LINEA", MODULO, ENTIDAD_LINEA, guardada.getId(),
                null, LineaResponse.from(guardada));
        return LineaResponse.from(guardada, nombreServicio(guardada));
    }

    @Transactional
    public LineaResponse editarLinea(Long planId, Long lineaId, LineaRequest request) {
        PlanTratamiento plan = plan(planId);
        soloBorrador(plan);
        PlanLinea linea = linea(planId, lineaId);
        LineaResponse antes = LineaResponse.from(linea, nombreServicio(linea));
        aplicarLinea(linea, request);

        PlanLinea guardada = lineaRepository.save(linea);
        actualizarTotales(plan);
        auditService.registrar("MODIFICAR_LINEA", MODULO, ENTIDAD_LINEA, guardada.getId(),
                antes, LineaResponse.from(guardada, nombreServicio(guardada)));
        return LineaResponse.from(guardada, nombreServicio(guardada));
    }

    @Transactional
    public void eliminarLinea(Long planId, Long lineaId) {
        PlanTratamiento plan = plan(planId);
        soloBorrador(plan);
        PlanLinea linea = linea(planId, lineaId);
        LineaResponse antes = LineaResponse.from(linea, nombreServicio(linea));
        plan.getLineas().remove(linea);
        planRepository.save(plan);
        actualizarTotales(plan);
        auditService.registrar("ELIMINAR_LINEA", MODULO, ENTIDAD_LINEA, lineaId, antes, null);
    }

    @Transactional
    public LineaResponse cambiarEstadoLinea(Long planId, Long lineaId, EstadoLineaRequest request) {
        PlanTratamiento plan = plan(planId);
        if (plan.getEstado() != EstadoPlanTratamiento.EN_EJECUCION) {
            throw new BusinessException(HttpStatus.CONFLICT, "PLAN_NO_EN_EJECUCION",
                    "Las líneas sólo cambian de estado mientras el plan está en ejecución");
        }
        PlanLinea linea = linea(planId, lineaId);
        EstadoLineaPlan anterior = linea.getEstado();
        if (anterior == request.estado()) {
            throw new BusinessException(HttpStatus.CONFLICT, "ESTADO_ACTUAL",
                    "La línea ya está en " + request.estado().name());
        }
        linea.setEstado(request.estado());
        PlanLinea guardada = lineaRepository.save(linea);
        auditService.registrar("CAMBIAR_ESTADO_LINEA", MODULO, ENTIDAD_LINEA, guardada.getId(),
                Map.of("estado", anterior.name()),
                Map.of("estado", request.estado().name()));
        integrarCuenta(plan, anterior, guardada);
        return LineaResponse.from(guardada, nombreServicio(guardada));
    }

    /**
     * Integración §5 "Procedimiento → cuenta": una línea COMPLETADA genera
     * su cargo en la cuenta del paciente y si el procedimiento se revierte
     * ese cargo se anula.
     */
    private void integrarCuenta(PlanTratamiento plan, EstadoLineaPlan anterior, PlanLinea linea) {
        if (anterior == linea.getEstado()) {
            return;
        }
        if (linea.getEstado() == EstadoLineaPlan.COMPLETADO) {
            cuentaPacienteService.cargoPorProcedimiento(new CuentaPacienteService.CargoProcedimiento(
                    plan.getPaciente().getId(), linea.getId(), linea.getProcedimiento(),
                    linea.getSesiones(), linea.getPrecioUnitario(), linea.getDescuentoPct(),
                    linea.getSubtotal(), linea.getDiente(), linea.getSuperficie(),
                    plan.getOdontologo(), linea.getServicio()));
        } else if (anterior == EstadoLineaPlan.COMPLETADO) {
            cuentaPacienteService.revertirCargoPorProcedimiento(plan.getPaciente().getId(),
                    linea.getId());
        }
    }

    // --------------------------- Presupuestos ---------------------------

    @Transactional
    public PresupuestoResponse emitirPresupuesto(Long planId, PresupuestoRequest request) {
        PlanTratamiento plan = plan(planId);
        if (lineaRepository.countByPlanId(planId) == 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "PLAN_SIN_LINEAS",
                    "El plan no tiene líneas para presupuestar");
        }
        if (plan.getEstado() == EstadoPlanTratamiento.COMPLETADO
                || plan.getEstado() == EstadoPlanTratamiento.CANCELADO) {
            throw new BusinessException(HttpStatus.CONFLICT, "PLAN_FINALIZADO",
                    "El plan está finalizado y no admite nuevos presupuestos");
        }

        Presupuesto presupuesto = new Presupuesto();
        presupuesto.setPlan(plan);
        presupuesto.setNumero(siguienteNumeroPresupuesto(plan));
        presupuesto.setEstado(EstadoPresupuesto.PRESENTADO);
        presupuesto.setFecha(LocalDate.now());
        presupuesto.setVigente(false);
        presupuesto.setObservaciones(request == null ? null : request.observaciones());
        currentUserService.idUsuarioActual().ifPresent(presupuesto::setCreadoPor);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (PlanLinea origen : lineaRepository.findByPlanIdOrderByNumeroAsc(plan.getId())) {
            PresupuestoLinea linea = new PresupuestoLinea();
            linea.setPresupuesto(presupuesto);
            linea.setPlanLinea(origen);
            linea.setNumero(origen.getNumero());
            linea.setFase(origen.getFase());
            linea.setDiente(origen.getDiente());
            linea.setSuperficie(origen.getSuperficie());
            linea.setDiagnostico(origen.getDiagnostico());
            linea.setProcedimiento(origen.getProcedimiento());
            linea.setPrioridad(origen.getPrioridad());
            linea.setSesiones(origen.getSesiones());
            linea.setPrecioUnitario(origen.getPrecioUnitario());
            linea.setDescuentoPct(origen.getDescuentoPct());
            linea.setSubtotal(origen.getSubtotal());
            presupuesto.getLineas().add(linea);

            BigDecimal bruto = origen.getPrecioUnitario()
                    .multiply(BigDecimal.valueOf(origen.getSesiones()));
            subtotal = subtotal.add(bruto);
            total = total.add(origen.getSubtotal());
        }
        presupuesto.setSubtotal(subtotal);
        presupuesto.setDescuento(subtotal.subtract(total));
        presupuesto.setTotal(total);

        desmarcarVigentes(plan);
        Presupuesto guardado = presupuestoRepository.save(presupuesto);
        // Emitir el presupuesto presenta el plan: así la aceptación posterior
        // sí puede llevarlo a ACEPTADO (§4.9: Plan → Presupuesto → Aceptación).
        sincronizarPlan(plan, EstadoPlanTratamiento.PRESENTADO);
        auditService.registrar("EMITIR_PRESUPUESTO", MODULO, ENTIDAD_PRESUPUESTO,
                guardado.getId(), null, PresupuestoResumenResponse.from(guardado));
        return presupuestoResponse(guardado);
    }

    @Transactional(readOnly = true)
    public PresupuestoResponse obtenerPresupuesto(Long planId, Long presupuestoId) {
        return presupuestoResponse(presupuesto(planId, presupuestoId));
    }

    @Transactional(readOnly = true)
    public List<PresupuestoResumenResponse> presupuestos(Long planId) {
        return presupuestoRepository.findByPlanIdOrderByNumeroDesc(planId).stream()
                .map(PresupuestoResumenResponse::from)
                .toList();
    }

    @Transactional
    public PresupuestoResponse cambiarEstadoPresupuesto(Long planId, Long presupuestoId,
                                                        AceptacionRequest request) {
        PlanTratamiento plan = plan(planId);
        Presupuesto presupuesto = presupuesto(planId, presupuestoId);
        EstadoPresupuesto destino = request.estado();
        if (destino == presupuesto.getEstado()) {
            throw new BusinessException(HttpStatus.CONFLICT, "ESTADO_ACTUAL",
                    "El presupuesto ya está en " + destino.name());
        }
        validaTransicionPresupuesto(presupuesto.getEstado(), destino);

        EstadoPresupuesto anterior = presupuesto.getEstado();
        presupuesto.setEstado(destino);
        presupuesto.setComentario(request.comentario());

        switch (destino) {
            case ACEPTADO -> {
                if (request.firmante() == null || request.firmante().isBlank()) {
                    throw new BusinessException("FIRMANTE_REQUERIDO",
                            "La aceptación requiere el nombre de quien firma");
                }
                presupuesto.setFirmante(request.firmante().trim());
                presupuesto.setAceptadoEn(Instant.now());
                presupuesto.setFirmaHash(firma(presupuesto));
                desmarcarVigentes(plan);
                presupuesto.setVigente(true);
                sincronizarPlan(plan, EstadoPlanTratamiento.ACEPTADO);
            }
            case RECHAZADO -> sincronizarPlan(plan, EstadoPlanTratamiento.RECHAZADO);
            case ANULADO -> presupuesto.setVigente(false);
            default -> { /* PRESENTADO: sólo cambia el estado */ }
        }

        Presupuesto guardado = presupuestoRepository.save(presupuesto);
        auditService.registrar("CAMBIAR_ESTADO_PRESUPUESTO", MODULO, ENTIDAD_PRESUPUESTO,
                guardado.getId(), Map.of("estado", anterior.name()),
                Map.of("estado", destino.name(),
                        "firmante", guardado.getFirmante() == null ? "" : guardado.getFirmante()));
        return presupuestoResponse(guardado);
    }

    // -------------------------------- PDF -------------------------------

    @Transactional(readOnly = true)
    public byte[] pdf(Long id) {
        PlanTratamiento plan = plan(id);
        Presupuesto vigente = presupuestoRepository
                .findTopByPlanIdAndVigenteTrueOrderByNumeroDesc(id).orElse(null);
        return pdfService.generar(plan,
                nombrePaciente(plan.getPaciente()),
                nombreOdontologo(plan.getOdontologo()),
                lineaRepository.findByPlanIdOrderByNumeroAsc(id),
                vigente);
    }

    @Transactional(readOnly = true)
    public String nombreArchivoPdf(Long id) {
        return pdfService.nombreArchivo(plan(id));
    }

    // ------------------------------ Auxiliares --------------------------

    private void aplicarLinea(PlanLinea linea, LineaRequest request) {
        if (request.diente() != null) {
            validarDiente(request.diente());
        }
        linea.setFase(request.fase() == null || request.fase() < 1 ? 1 : request.fase());
        linea.setDiente(request.diente());
        linea.setSuperficie(request.superficie() == null ? null : request.superficie().name());
        linea.setDiagnostico(request.diagnostico());
        linea.setProcedimiento(request.procedimiento().trim());
        if (request.servicioId() != null) {
            Servicio servicio = servicioRepository.findById(request.servicioId())
                    .orElseThrow(() -> noEncontrado("Servicio"));
            linea.setServicio(servicio);
        } else {
            linea.setServicio(null);
        }
        linea.setPrioridad(request.prioridad() == null ? PrioridadLinea.MEDIA : request.prioridad());
        linea.setSesiones(request.sesiones() == null || request.sesiones() < 1
                ? 1 : request.sesiones());
        linea.setPrecioUnitario(request.precioUnitario() == null
                ? BigDecimal.ZERO : request.precioUnitario());
        linea.setDescuentoPct(request.descuentoPct() == null
                ? BigDecimal.ZERO : request.descuentoPct());
        linea.setNotas(request.notas());
        linea.recalcularSubtotal();
    }

    private void actualizarTotales(PlanTratamiento plan) {
        BigDecimal total = lineaRepository.findByPlanIdOrderByNumeroAsc(plan.getId()).stream()
                .map(PlanLinea::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        plan.setSubtotal(total);
        plan.setTotal(total);
        planRepository.save(plan);
    }

    private void sincronizarPlan(PlanTratamiento plan, EstadoPlanTratamiento destino) {
        if (plan.getEstado() == destino) {
            return;
        }
        if (TRANSICIONES.getOrDefault(plan.getEstado(), Set.of()).contains(destino)) {
            plan.setEstado(destino);
            planRepository.save(plan);
        }
    }

    private void validaTransicionPresupuesto(EstadoPresupuesto origen, EstadoPresupuesto destino) {
        boolean valido = switch (origen) {
            case BORRADOR -> destino == EstadoPresupuesto.PRESENTADO
                    || destino == EstadoPresupuesto.ANULADO;
            case PRESENTADO -> destino == EstadoPresupuesto.ACEPTADO
                    || destino == EstadoPresupuesto.RECHAZADO
                    || destino == EstadoPresupuesto.ANULADO;
            case ACEPTADO -> destino == EstadoPresupuesto.ANULADO;
            case RECHAZADO, ANULADO -> false;
        };
        if (!valido) {
            throw new BusinessException(HttpStatus.CONFLICT, "TRANSICION_INVALIDA",
                    "No se puede pasar el presupuesto de " + origen.name() + " a " + destino.name());
        }
    }

    private void desmarcarVigentes(PlanTratamiento plan) {
        for (Presupuesto otro : presupuestoRepository.findByPlanIdOrderByNumeroDesc(plan.getId())) {
            if (Boolean.TRUE.equals(otro.getVigente())) {
                otro.setVigente(false);
                presupuestoRepository.save(otro);
            }
        }
    }

    private String firma(Presupuesto p) {
        String contenido = String.join("|",
                "PLAN", String.valueOf(p.getPlan().getId()),
                "PRESUPUESTO", String.valueOf(p.getNumero()),
                "TOTAL", p.getTotal().toPlainString(),
                "FIRMANTE", p.getFirmante(),
                "FECHA", p.getFecha().toString());
        return HashUtil.sha256Hex(contenido.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private int siguienteNumero(PlanTratamiento plan) {
        return plan.getLineas().stream()
                .mapToInt(PlanLinea::getNumero)
                .max().orElse(0) + 1;
    }

    private int siguienteNumeroPresupuesto(PlanTratamiento plan) {
        return presupuestoRepository.findTopByPlanIdOrderByNumeroDesc(plan.getId())
                .map(p -> p.getNumero() + 1)
                .orElse(1);
    }

    private void validarDiente(int diente) {
        int cuadrante = diente / 10;
        int pieza = diente % 10;
        boolean valido = (cuadrante >= 1 && cuadrante <= 4 && pieza >= 1 && pieza <= 8)
                || (cuadrante >= 5 && cuadrante <= 8 && pieza >= 1 && pieza <= 5);
        if (!valido) {
            throw new BusinessException("DIENTE_INVALIDO",
                    "El número FDI " + diente + " no corresponde a una pieza válida");
        }
    }

    private PresupuestoResponse presupuestoResponse(Presupuesto p) {
        List<LineaPresupuestoResponse> lineas =
                presupuestoLineaRepository.findByPresupuestoIdOrderByNumeroAsc(p.getId()).stream()
                        .map(LineaPresupuestoResponse::from)
                        .toList();
        return PresupuestoResponse.from(p, lineas);
    }

    private Presupuesto presupuesto(Long planId, Long presupuestoId) {
        return presupuestoRepository.findByIdAndPlanId(presupuestoId, planId)
                .orElseThrow(() -> noEncontrado("Presupuesto"));
    }

    private PlanLinea linea(Long planId, Long lineaId) {
        return lineaRepository.findByIdAndPlanId(lineaId, planId)
                .orElseThrow(() -> noEncontrado("Línea del plan"));
    }

    private PlanTratamiento plan(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> noEncontrado("Plan de tratamiento"));
    }

    private Paciente paciente(Long id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> noEncontrado("Paciente"));
    }

    private void soloBorrador(PlanTratamiento plan) {
        if (plan.getEstado() != EstadoPlanTratamiento.BORRADOR) {
            throw new BusinessException(HttpStatus.CONFLICT, "PLAN_NO_BORRADOR",
                    "Las líneas sólo se editan mientras el plan está en borrador");
        }
    }

    private void soloEditables(PlanTratamiento plan) {
        if (!ESTADOS_EDITABLES.contains(plan.getEstado())) {
            throw new BusinessException(HttpStatus.CONFLICT, "PLAN_NO_EDITABLE",
                    "El plan en estado " + plan.getEstado().name() + " no es editable");
        }
    }

    private String nombreServicio(PlanLinea linea) {
        return linea.getServicio() == null ? null : linea.getServicio().getNombre();
    }

    private String nombrePaciente(Paciente paciente) {
        return (paciente.getNombres() + " " + paciente.getApellidos()).trim();
    }

    private String nombreOdontologo(Odontologo odontologo) {
        if (odontologo == null) {
            return null;
        }
        String nombres = odontologo.getNombres() == null ? "" : odontologo.getNombres().trim();
        String apellidos = odontologo.getApellidos() == null ? "" : odontologo.getApellidos().trim();
        return (nombres + " " + apellidos).trim();
    }

    private BusinessException noEncontrado(String entidad) {
        return new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO",
                entidad + " no encontrado");
    }
}
