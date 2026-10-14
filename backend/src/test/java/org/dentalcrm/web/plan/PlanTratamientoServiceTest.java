package org.dentalcrm.web.plan;

import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.odontologo.OdontologoRepository;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.domain.plan.*;
import org.dentalcrm.domain.servicio.ServicioRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.service.CurrentUserService;
import org.dentalcrm.web.facturacion.CuentaPacienteService;
import org.dentalcrm.web.plan.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PlanTratamientoServiceTest {

    private PlanTratamientoService servicio;
    private PlanTratamientoRepository planRepo;
    private PlanLineaRepository lineaRepo;
    private PresupuestoRepository presupuestoRepo;
    private PresupuestoLineaRepository presupuestoLineaRepo;
    private PacienteRepository pacienteRepo;
    private OdontologoRepository odontologoRepo;
    private AuditService auditoria;
    private PlanPdfService pdfService;

    @BeforeEach
    void setUp() {
        planRepo = mock(PlanTratamientoRepository.class);
        lineaRepo = mock(PlanLineaRepository.class);
        presupuestoRepo = mock(PresupuestoRepository.class);
        presupuestoLineaRepo = mock(PresupuestoLineaRepository.class);
        pacienteRepo = mock(PacienteRepository.class);
        odontologoRepo = mock(OdontologoRepository.class);
        auditoria = mock(AuditService.class);
        pdfService = mock(PlanPdfService.class);

        CurrentUserService usuarioActual = mock(CurrentUserService.class);
        when(usuarioActual.idUsuarioActual()).thenReturn(Optional.of(77L));

        servicio = new PlanTratamientoService(planRepo, lineaRepo, presupuestoRepo,
                presupuestoLineaRepo, pacienteRepo, odontologoRepo,
                mock(ServicioRepository.class), auditoria, usuarioActual, pdfService,
                mock(CuentaPacienteService.class));
    }

    // ------------------------------ Plan -------------------------------

    @Test
    void crearPlanEmpiezaEnBorradorConTotalesEnCero() {
        when(pacienteRepo.findById(1L)).thenReturn(Optional.of(paciente()));
        when(planRepo.save(any(PlanTratamiento.class))).thenAnswer(inv -> {
            PlanTratamiento p = inv.getArgument(0);
            p.setId(9L);
            ultimoPlan = p;
            return p;
        });
        when(planRepo.findById(9L)).thenAnswer(inv -> Optional.ofNullable(ultimoPlan));
        when(lineaRepo.findByPlanIdOrderByNumeroAsc(9L)).thenReturn(List.of());

        PlanResponse r = servicio.crear(1L,
                new PlanRequest("Ortodoncia fija", null, "Primera fase", null));

        assertEquals(9L, r.id());
        assertEquals("BORRADOR", r.estado());
        assertEquals(BigDecimal.ZERO, r.total());
        assertEquals(LocalDate.now(), r.fecha());
        verify(auditoria).registrar(eq("CREAR_PLAN"), any(), any(), any(), any(), any());
    }

    @Test
    void listarPlanesDevuelveConteosPorPlan() {
        when(pacienteRepo.findById(1L)).thenReturn(Optional.of(paciente()));
        PlanTratamiento plan = plan(EstadoPlanTratamiento.BORRADOR);
        plan.setId(3L);
        when(planRepo.findByPacienteIdOrderByFechaDescIdDesc(1L)).thenReturn(List.of(plan));
        when(lineaRepo.countByPlanId(3L)).thenReturn(2L);
        when(presupuestoRepo.countByPlanId(3L)).thenReturn(1L);

        List<PlanResumenResponse> planes = servicio.listar(1L);

        assertEquals(1, planes.size());
        assertEquals(2, planes.get(0).lineas());
        assertEquals(1, planes.get(0).presupuestos());
        assertEquals("BORRADOR", planes.get(0).estado());
        verify(auditoria, never()).registrar(any(), any(), any(), any(), any(), any());
    }

    @Test
    void eliminarPlanDistintoDeBorradorSeRechaza() {
        PlanTratamiento plan = plan(EstadoPlanTratamiento.ACEPTADO);
        plan.setId(4L);
        when(planRepo.findById(4L)).thenReturn(Optional.of(plan));

        BusinessException e = assertThrows(BusinessException.class, () -> servicio.eliminar(4L));

        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        assertEquals("PLAN_NO_BORRADOR", e.getCode());
        verify(planRepo, never()).delete(any());
    }

    @Test
    void transicionInvalidaSeRechaza() {
        PlanTratamiento plan = plan(EstadoPlanTratamiento.BORRADOR);
        plan.setId(5L);
        when(planRepo.findById(5L)).thenReturn(Optional.of(plan));

        BusinessException e = assertThrows(BusinessException.class, () -> servicio.cambiarEstado(5L,
                new EstadoPlanRequest(EstadoPlanTratamiento.COMPLETADO, null)));

        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        assertEquals("TRANSICION_INVALIDA", e.getCode());
        verify(planRepo, never()).save(any(PlanTratamiento.class));
    }

    @Test
    void estadoActualSeRechaza() {
        PlanTratamiento plan = plan(EstadoPlanTratamiento.PRESENTADO);
        plan.setId(6L);
        when(planRepo.findById(6L)).thenReturn(Optional.of(plan));

        BusinessException e = assertThrows(BusinessException.class, () -> servicio.cambiarEstado(6L,
                new EstadoPlanRequest(EstadoPlanTratamiento.PRESENTADO, null)));

        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        assertEquals("ESTADO_ACTUAL", e.getCode());
    }

    @Test
    void planNoEncontradoDevuelve404() {
        when(planRepo.findById(404L)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class, () -> servicio.obtener(404L));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
        assertEquals("NO_ENCONTRADO", e.getCode());
    }

    // ------------------------------ Líneas ------------------------------

    @Test
    void agregarLineaRecalculaTotalesDelPlan() {
        PlanTratamiento plan = plan(EstadoPlanTratamiento.BORRADOR);
        plan.setId(1L);
        when(planRepo.findById(1L)).thenReturn(Optional.of(plan));
        when(lineaRepo.save(any(PlanLinea.class))).thenAnswer(inv -> {
            PlanLinea l = inv.getArgument(0);
            l.setId(30L);
            return l;
        });
        when(lineaRepo.findByPlanIdOrderByNumeroAsc(1L))
                .thenAnswer(inv -> new ArrayList<>(plan.getLineas()));

        LineaRequest request = new LineaRequest(null, 1, 26, null, "Caries oclusal",
                "Obturación compuesta", null, PrioridadLinea.MEDIA, 2,
                new BigDecimal("80.00"), new BigDecimal("10.00"), null);

        LineaResponse r = servicio.agregarLinea(1L, request);

        assertEquals(1, r.numero());
        assertEquals(new BigDecimal("144.00"), r.subtotal());  // 80 × 2 × 0.90
        assertEquals(new BigDecimal("144.00"), plan.getTotal());
        verify(auditoria).registrar(eq("AGREGAR_LINEA"), any(), any(), any(), any(), any());
    }

    @Test
    void agregarLineaConDienteInvalidoSeRechaza() {
        PlanTratamiento plan = plan(EstadoPlanTratamiento.BORRADOR);
        plan.setId(1L);
        when(planRepo.findById(1L)).thenReturn(Optional.of(plan));

        BusinessException e = assertThrows(BusinessException.class, () -> servicio.agregarLinea(1L,
                new LineaRequest(null, null, 49, null, null, "Empaste", null, null, 1,
                        BigDecimal.TEN, null, null)));

        assertEquals("DIENTE_INVALIDO", e.getCode());
        verify(lineaRepo, never()).save(any(PlanLinea.class));
    }

    @Test
    void editarLineaFueraDeBorradorSeRechaza() {
        PlanTratamiento plan = plan(EstadoPlanTratamiento.ACEPTADO);
        plan.setId(1L);
        when(planRepo.findById(1L)).thenReturn(Optional.of(plan));

        BusinessException e = assertThrows(BusinessException.class, () -> servicio.editarLinea(1L, 8L,
                new LineaRequest(null, null, null, null, null, "Implante", null, null, 1,
                        BigDecimal.ONE, null, null)));

        assertEquals("PLAN_NO_BORRADOR", e.getCode());
        verify(lineaRepo, never()).save(any(PlanLinea.class));
    }

    @Test
    void eliminarLineaRecalculaTotales() {
        PlanTratamiento plan = plan(EstadoPlanTratamiento.BORRADOR);
        plan.setId(1L);
        PlanLinea linea = linea(plan, 1);
        plan.getLineas().add(linea);
        when(planRepo.findById(1L)).thenReturn(Optional.of(plan));
        when(lineaRepo.findByIdAndPlanId(10L, 1L)).thenReturn(Optional.of(linea));
        when(lineaRepo.findByPlanIdOrderByNumeroAsc(1L))
                .thenAnswer(inv -> new ArrayList<>(plan.getLineas()));

        servicio.eliminarLinea(1L, 10L);

        assertTrue(plan.getLineas().isEmpty());
        assertEquals(BigDecimal.ZERO, plan.getTotal());
        verify(auditoria).registrar(eq("ELIMINAR_LINEA"), any(), any(), any(), any(), any());
    }

    @Test
    void cambiarEstadoLineaExigePlanEnEjecucion() {
        PlanTratamiento plan = plan(EstadoPlanTratamiento.BORRADOR);
        plan.setId(1L);
        when(planRepo.findById(1L)).thenReturn(Optional.of(plan));
        PlanLinea linea = linea(plan, 1);
        when(lineaRepo.findByIdAndPlanId(10L, 1L)).thenReturn(Optional.of(linea));

        BusinessException e = assertThrows(BusinessException.class, () ->
                servicio.cambiarEstadoLinea(1L, 10L,
                        new EstadoLineaRequest(EstadoLineaPlan.COMPLETADO)));

        assertEquals("PLAN_NO_EN_EJECUCION", e.getCode());
    }

    @Test
    void lineaCambiaDeEstadoCuandoElPlanEstaEnEjecucion() {
        PlanTratamiento plan = plan(EstadoPlanTratamiento.EN_EJECUCION);
        plan.setId(1L);
        when(planRepo.findById(1L)).thenReturn(Optional.of(plan));
        PlanLinea linea = linea(plan, 1);
        linea.setEstado(EstadoLineaPlan.PENDIENTE);
        when(lineaRepo.findByIdAndPlanId(10L, 1L)).thenReturn(Optional.of(linea));
        when(lineaRepo.save(any(PlanLinea.class))).thenAnswer(inv -> inv.getArgument(0));

        LineaResponse r = servicio.cambiarEstadoLinea(1L, 10L,
                new EstadoLineaRequest(EstadoLineaPlan.EN_EJECUCION));

        assertEquals("EN_EJECUCION", r.estado());
        verify(auditoria).registrar(eq("CAMBIAR_ESTADO_LINEA"), any(), any(), any(), any(), any());
    }

    // --------------------------- Presupuestos ---------------------------

    @Test
    void emitirPresupuestoSinLineasSeRechaza() {
        PlanTratamiento plan = plan(EstadoPlanTratamiento.PRESENTADO);
        plan.setId(1L);
        when(planRepo.findById(1L)).thenReturn(Optional.of(plan));
        when(lineaRepo.countByPlanId(1L)).thenReturn(0L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> servicio.emitirPresupuesto(1L, new PresupuestoRequest(null)));

        assertEquals("PLAN_SIN_LINEAS", e.getCode());
        verify(presupuestoRepo, never()).save(any(Presupuesto.class));
    }

    @Test
    void emitirPresupuestoCongelaLineasYCalculaTotales() {
        PlanTratamiento plan = plan(EstadoPlanTratamiento.PRESENTADO);
        plan.setId(1L);
        PlanLinea linea = linea(plan, 1);
        when(planRepo.findById(1L)).thenReturn(Optional.of(plan));
        when(lineaRepo.countByPlanId(1L)).thenReturn(1L);
        when(lineaRepo.findByPlanIdOrderByNumeroAsc(1L)).thenReturn(List.of(linea));
        when(presupuestoRepo.findTopByPlanIdOrderByNumeroDesc(1L)).thenReturn(Optional.empty());
        when(presupuestoRepo.save(any(Presupuesto.class))).thenAnswer(inv -> {
            Presupuesto p = inv.getArgument(0);
            p.setId(50L);
            ultimoPresupuesto = p;
            return p;
        });
        when(presupuestoLineaRepo.findByPresupuestoIdOrderByNumeroAsc(50L))
                .thenAnswer(inv -> ultimoPresupuesto.getLineas());

        PresupuestoResponse r = servicio.emitirPresupuesto(1L, new PresupuestoRequest("Oferta"));

        assertEquals(1, r.numero());
        assertEquals("PRESENTADO", r.estado());
        assertEquals(1, r.lineas().size());
        assertEquals("Obturación compuesta", r.lineas().get(0).procedimiento());
        assertEquals(new BigDecimal("160.00"), r.subtotal());
        assertEquals(new BigDecimal("144.00"), r.total());
        assertEquals(new BigDecimal("16.00"), r.descuento());  // 160 − 144
        verify(auditoria).registrar(eq("EMITIR_PRESUPUESTO"), any(), any(), any(), any(), any());
    }

    @Test
    void emitirPresupuestoPresentaElPlanEnBorrador() {
        PlanTratamiento plan = plan(EstadoPlanTratamiento.BORRADOR);
        plan.setId(1L);
        PlanLinea linea = linea(plan, 1);
        when(planRepo.findById(1L)).thenReturn(Optional.of(plan));
        when(lineaRepo.countByPlanId(1L)).thenReturn(1L);
        when(lineaRepo.findByPlanIdOrderByNumeroAsc(1L)).thenReturn(List.of(linea));
        when(presupuestoRepo.findTopByPlanIdOrderByNumeroDesc(1L)).thenReturn(Optional.empty());
        when(presupuestoRepo.save(any(Presupuesto.class))).thenAnswer(inv -> {
            Presupuesto p = inv.getArgument(0);
            p.setId(60L);
            ultimoPresupuesto = p;
            return p;
        });
        when(presupuestoLineaRepo.findByPresupuestoIdOrderByNumeroAsc(60L)).thenReturn(List.of());
        when(planRepo.save(any(PlanTratamiento.class))).thenAnswer(inv -> inv.getArgument(0));

        servicio.emitirPresupuesto(1L, new PresupuestoRequest(null));

        assertEquals(EstadoPlanTratamiento.PRESENTADO, plan.getEstado());
    }

    @Test
    void aceptarPresupuestoRequiereFirmante() {
        PlanTratamiento plan = plan(EstadoPlanTratamiento.PRESENTADO);
        plan.setId(1L);
        Presupuesto presupuesto = presupuesto(plan);
        when(planRepo.findById(1L)).thenReturn(Optional.of(plan));
        when(presupuestoRepo.findByIdAndPlanId(50L, 1L)).thenReturn(Optional.of(presupuesto));

        BusinessException e = assertThrows(BusinessException.class, () ->
                servicio.cambiarEstadoPresupuesto(1L, 50L,
                        new AceptacionRequest(EstadoPresupuesto.ACEPTADO, "  ", null)));

        assertEquals("FIRMANTE_REQUERIDO", e.getCode());
        verify(presupuestoRepo, never()).save(any(Presupuesto.class));
    }

    @Test
    void aceptarPresupuestoFirmaMarcaVigenteYSincronizaElPlan() {
        PlanTratamiento plan = plan(EstadoPlanTratamiento.PRESENTADO);
        plan.setId(1L);
        Presupuesto presupuesto = presupuesto(plan);
        when(planRepo.findById(1L)).thenReturn(Optional.of(plan));
        when(presupuestoRepo.findByIdAndPlanId(50L, 1L)).thenReturn(Optional.of(presupuesto));
        when(presupuestoRepo.findByPlanIdOrderByNumeroDesc(1L)).thenReturn(List.of(presupuesto));
        when(presupuestoRepo.save(any(Presupuesto.class))).thenAnswer(inv -> inv.getArgument(0));
        when(planRepo.save(any(PlanTratamiento.class))).thenAnswer(inv -> inv.getArgument(0));
        when(presupuestoLineaRepo.findByPresupuestoIdOrderByNumeroAsc(50L)).thenReturn(List.of());

        PresupuestoResponse r = servicio.cambiarEstadoPresupuesto(1L, 50L,
                new AceptacionRequest(EstadoPresupuesto.ACEPTADO, "María Pérez", "De acuerdo"));

        assertEquals("ACEPTADO", r.estado());
        assertTrue(r.vigente());
        assertNotNull(r.firmaHash());
        assertEquals(64, r.firmaHash().length());
        assertEquals("María Pérez", r.firmante());
        assertEquals(EstadoPlanTratamiento.ACEPTADO, plan.getEstado());
        verify(auditoria).registrar(eq("CAMBIAR_ESTADO_PRESUPUESTO"), any(), any(), any(), any(), any());
    }

    @Test
    void presupuestoEnBorradorNoSePuedeAceptarDirectamente() {
        PlanTratamiento plan = plan(EstadoPlanTratamiento.PRESENTADO);
        plan.setId(1L);
        Presupuesto presupuesto = presupuesto(plan);
        presupuesto.setEstado(EstadoPresupuesto.BORRADOR);
        when(planRepo.findById(1L)).thenReturn(Optional.of(plan));
        when(presupuestoRepo.findByIdAndPlanId(50L, 1L)).thenReturn(Optional.of(presupuesto));

        BusinessException e = assertThrows(BusinessException.class, () ->
                servicio.cambiarEstadoPresupuesto(1L, 50L,
                        new AceptacionRequest(EstadoPresupuesto.ACEPTADO, "Firmante", null)));

        assertEquals("TRANSICION_INVALIDA", e.getCode());
    }

    // -------------------------------- PDF --------------------------------

    @Test
    void pdfDelegaEnElServicioDePdf() {
        PlanTratamiento plan = plan(EstadoPlanTratamiento.ACEPTADO);
        plan.setId(1L);
        when(planRepo.findById(1L)).thenReturn(Optional.of(plan));
        when(lineaRepo.findByPlanIdOrderByNumeroAsc(1L)).thenReturn(List.of());
        when(presupuestoRepo.findTopByPlanIdAndVigenteTrueOrderByNumeroDesc(1L))
                .thenReturn(Optional.empty());
        when(pdfService.generar(any(), any(), any(), any(), any()))
                .thenReturn(new byte[]{1, 2, 3});
        when(pdfService.nombreArchivo(plan)).thenReturn("plan-tratamiento-1.pdf");

        byte[] contenido = servicio.pdf(1L);

        assertEquals(3, contenido.length);
        assertEquals("plan-tratamiento-1.pdf", servicio.nombreArchivoPdf(1L));
    }

    // ------------------------------ Auxiliares --------------------------

    private PlanTratamiento ultimoPlan;
    private Presupuesto ultimoPresupuesto;

    private Paciente paciente() {
        Paciente p = new Paciente();
        p.setId(1L);
        p.setNombres("Ana");
        p.setApellidos("Torres");
        return p;
    }

    private PlanTratamiento plan(EstadoPlanTratamiento estado) {
        PlanTratamiento p = new PlanTratamiento();
        p.setPaciente(paciente());
        p.setTitulo("Plan de prueba");
        p.setEstado(estado);
        p.setFecha(LocalDate.now());
        p.setSubtotal(BigDecimal.ZERO);
        p.setTotal(BigDecimal.ZERO);
        p.setLineas(new ArrayList<>());
        ultimoPlan = p;
        return p;
    }

    private PlanLinea linea(PlanTratamiento plan, int numero) {
        PlanLinea l = new PlanLinea();
        l.setId(10L);
        l.setPlan(plan);
        l.setNumero(numero);
        l.setProcedimiento("Obturación compuesta");
        l.setDiente(26);
        l.setPrioridad(PrioridadLinea.MEDIA);
        l.setSesiones(2);
        l.setPrecioUnitario(new BigDecimal("80.00"));
        l.setDescuentoPct(new BigDecimal("10.00"));
        l.setEstado(EstadoLineaPlan.PENDIENTE);
        l.recalcularSubtotal();
        return l;
    }

    private Presupuesto presupuesto(PlanTratamiento plan) {
        Presupuesto p = new Presupuesto();
        p.setId(50L);
        p.setPlan(plan);
        p.setNumero(1);
        p.setEstado(EstadoPresupuesto.PRESENTADO);
        p.setFecha(LocalDate.now());
        p.setVigente(false);
        p.setSubtotal(new BigDecimal("160.00"));
        p.setDescuento(new BigDecimal("16.00"));
        p.setTotal(new BigDecimal("144.00"));
        p.setLineas(new ArrayList<>());
        ultimoPresupuesto = p;
        return p;
    }
}
