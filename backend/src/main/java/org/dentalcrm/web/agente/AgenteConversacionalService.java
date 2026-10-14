package org.dentalcrm.web.agente;

import org.dentalcrm.domain.agente.Intencion;
import org.dentalcrm.domain.cita.CitaRepository;
import org.dentalcrm.domain.horario.HorarioOdontologoRepository;
import org.dentalcrm.domain.odontologo.OdontologoRepository;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.domain.servicio.ServicioRepository;
import org.dentalcrm.domain.whatsapp.Conversacion;
import org.dentalcrm.domain.whatsapp.ConversacionRepository;
import org.dentalcrm.domain.whatsapp.EstadoConversacion;
import org.dentalcrm.domain.whatsapp.Mensaje;
import org.dentalcrm.domain.whatsapp.MensajeRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.web.agenda.AgendaService;
import org.dentalcrm.web.agenda.HorarioPdfService;
import org.dentalcrm.web.agente.contexto.ConversacionContexto;
import org.dentalcrm.web.agente.contexto.ContextManager;
import org.dentalcrm.web.agente.dto.AgenteConversacionResponse;
import org.dentalcrm.web.agente.engine.ConfirmationResolver;
import org.dentalcrm.web.agente.engine.ConversationEngine;
import org.dentalcrm.web.agente.engine.EntityExtractor;
import org.dentalcrm.web.agente.engine.FlowEngine;
import org.dentalcrm.web.agente.engine.IntentClassifier;
import org.dentalcrm.web.agente.engine.MenuResolver;
import org.dentalcrm.web.agente.flujo.AgenteEntorno;
import org.dentalcrm.web.agente.flujo.DisponibilidadFlow;
import org.dentalcrm.web.agente.flujo.Flujo;
import org.dentalcrm.web.agente.flujo.HumanTransferFlow;
import org.dentalcrm.web.agente.flujo.ManageAppointmentFlow;
import org.dentalcrm.web.agente.flujo.PatientIdentificationFlow;
import org.dentalcrm.web.agente.flujo.ProfileFlow;
import org.dentalcrm.web.agente.flujo.RegisterPatientFlow;
import org.dentalcrm.web.agente.flujo.ScheduleAppointmentFlow;
import org.dentalcrm.web.agente.flujo.ServicesFlow;
import org.dentalcrm.web.agente.flujo.ViewAppointmentsFlow;
import org.dentalcrm.web.agenda.publica.AgendaPublicaService;
import org.dentalcrm.web.agente.tool.AppointmentTool;
import org.dentalcrm.web.agente.tool.AvailabilityTool;
import org.dentalcrm.web.agente.tool.DentistTool;
import org.dentalcrm.web.agente.tool.PatientTool;
import org.dentalcrm.web.agente.tool.ServiceTool;
import org.dentalcrm.web.cita.CitaService;
import org.dentalcrm.web.google.GoogleService;
import org.dentalcrm.web.paciente.PacienteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Fachada del agente WhatsApp (§17): arma el entorno (contexto, menús,
 * entidades, tools y flujos) y delega cada mensaje en el {@link ConversationEngine}.
 *
 * <p>Los mensajes, adjuntos y la supervisión de conversaciones viven aquí; la
 * lógica conversacional está en {@code engine} y {@code flujo}.
 */
@Service
public class AgenteConversacionalService {

    private static final Logger log = LoggerFactory.getLogger(AgenteConversacionalService.class);

    private final ConversacionRepository conversacionRepository;
    private final MensajeRepository mensajeRepository;
    private final ContextManager contexto;
    private final AgenteSoporte soporte;
    private final ConversationEngine engine;

    public AgenteConversacionalService(ConversacionRepository conversacionRepository,
                                        MensajeRepository mensajeRepository,
                                        PacienteRepository pacienteRepository,
                                        CitaRepository citaRepository,
                                        CitaService citaService,
                                        AgendaService agendaService,
                                        ServicioRepository servicioRepository,
                                        OdontologoRepository odontologoRepository,
                                        HorarioOdontologoRepository horarioRepository,
                                        HorarioPdfService horarioPdfService,
                                        PacienteService pacienteService,
                                        GoogleService googleService,
                                        ObjectMapper objectMapper,
                                        @Value("${app.timezone}") String timezone,
                                        AgendaPublicaService agendaPublica,
                                        @Value("${app.agenda-publica.frontend-base-url:http://localhost:4200/dental_crm/agenda-publica}") String agendaPublicaUrl) {
        this.conversacionRepository = conversacionRepository;
        this.mensajeRepository = mensajeRepository;
        this.soporte = new AgenteSoporte(timezone, agendaPublicaUrl);
        this.contexto = new ContextManager(objectMapper);

        DentistTool odontologos = new DentistTool(odontologoRepository);
        PatientTool pacientes = new PatientTool(pacienteRepository, pacienteService, soporte);
        ServiceTool servicios = new ServiceTool(servicioRepository);
        AvailabilityTool disponibilidad =
                new AvailabilityTool(agendaService, horarioRepository, horarioPdfService, odontologos);
        AppointmentTool citas = new AppointmentTool(citaRepository, citaService, disponibilidad);
        EntityExtractor extractor = new EntityExtractor(soporte);
        ConfirmationResolver confirmaciones = new ConfirmationResolver(soporte);
        MenuResolver menus = new MenuResolver(soporte);

        AgenteEntorno entorno = new AgenteEntorno(soporte, contexto, pacientes, servicios,
                odontologos, disponibilidad, citas, extractor, confirmaciones, menus, agendaPublica);

        List<Flujo> flujos = List.of(
                new PatientIdentificationFlow(entorno),
                new ScheduleAppointmentFlow(entorno),
                new DisponibilidadFlow(entorno),
                new ViewAppointmentsFlow(entorno),
                new ManageAppointmentFlow(entorno),
                new ServicesFlow(entorno),
                new ProfileFlow(entorno),
                new RegisterPatientFlow(entorno),
                new HumanTransferFlow(entorno));

        IntentClassifier clasificador = new IntentClassifier(soporte);
        FlowEngine motorFlujos = new FlowEngine(entorno, flujos);
        this.engine = new ConversationEngine(entorno, motorFlujos, clasificador,
                googleService, mensajeRepository);
    }

    public record Adjunto(String nombreArchivo, String mime, byte[] contenido, String caption) {
    }

    public record RespuestaAgente(String mensaje, boolean transferirHumano, Adjunto adjunto) {
        public static RespuestaAgente silenciosa() {
            return new RespuestaAgente(null, false, null);
        }
    }

    // ------------------------------------------------------------------
    // Procesamiento principal (llamado desde el webhook de WhatsApp)
    // ------------------------------------------------------------------

    @Transactional
    public RespuestaAgente procesar(Conversacion conversacion, String textoEntrada, Instant ultimoPrev) {
        return engine.procesar(conversacion, textoEntrada, ultimoPrev);
    }

    // ------------------------------------------------------------------
    // Recuperación de chats: reproduce la pregunta pendiente (§15)
    // ------------------------------------------------------------------

    /**
     * Texto con el que se retoma una conversación interrumpida: la última
     * pregunta del contexto. Es inocuo de repetir (no ejecuta acciones).
     */
    @Transactional
    public String mensajeDeReanudacion(Conversacion conversacion) {
        ConversacionContexto ctx = contexto.cargar(conversacion);
        String pendiente = ctx.getUltimaPregunta();
        if (pendiente != null && !pendiente.isBlank()) {
            if (ctx.getFlujo() != null) {
                contexto.marcarReanudacion(conversacion, ctx);
            }
            return AgenteSoporte.mensajeReanudacion(pendiente);
        }
        if (conversacion.getPaciente() == null) {
            return AgenteSoporte.MENSAJE_SIN_PACIENTE;
        }
        return soporte.mensajeSaludo();
    }

    // ------------------------------------------------------------------
    // Interacción por teléfono (sin sesión WhatsApp)
    // ------------------------------------------------------------------

    @Transactional
    public RespuestaAgente procesarPorTelefono(String telefono, String texto) {
        String t = ParserAgendamiento.normalizar(texto);
        if (t.isBlank()) {
            return new RespuestaAgente(
                    "Escribe algo para comenzar. Ej: \"ver citas\" o tu número de cédula.", false, null);
        }
        Conversacion conversacion = conversacionRepository.findByTelefono(telefono)
                .filter(c -> c.getEstado() != EstadoConversacion.CERRADA)
                .orElseGet(() -> {
                    Conversacion nueva = new Conversacion();
                    nueva.setTelefono(telefono);
                    nueva.setEstado(EstadoConversacion.BOT);
                    nueva.setAgenteActivo(true);
                    nueva.setCreatedAt(Instant.now());
                    nueva.setUpdatedAt(Instant.now());
                    return conversacionRepository.save(nueva);
                });
        conversacion.setUltimoMensajeAt(Instant.now());
        if (!Boolean.TRUE.equals(conversacion.getAgenteActivo())
                || conversacion.getEstado() == EstadoConversacion.ATENCION_HUMANA) {
            return new RespuestaAgente("La conversación no está activa. Habla con un administrador.",
                    false, null);
        }
        RespuestaAgente respuesta = engine.procesar(conversacion, texto, null);
        return respuesta.mensaje() == null
                ? new RespuestaAgente("Escribe algo para comenzar. Ej: \"ver citas\" o tu número de cédula.",
                false, null)
                : respuesta;
    }

    // ------------------------------------------------------------------
    // Operaciones de supervisión (exposición REST)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AgenteConversacionResponse> listarConversacionesAgente(String q) {
        return conversacionRepository.buscar(q == null ? "" : q.trim()).stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional
    public AgenteConversacionResponse transferirAHumano(Long conversacionId) {
        Conversacion conversacion = conversacionRepository.findById(conversacionId)
                .orElseThrow(() -> new BusinessException("CONVERSACION_NO_ENCONTRADA", "Conversación no encontrada"));
        return transferirAHumano(conversacion);
    }

    @Transactional
    public AgenteConversacionResponse transferirAHumano(Conversacion conversacion) {
        conversacion.setEstado(EstadoConversacion.ATENCION_HUMANA);
        conversacion.setIntencion(Intencion.TRANSFERIR_HUMANO);
        conversacionRepository.save(conversacion);
        log.info("Agente: conversación {} transferida a humano (panel)", conversacion.getId());
        return aRespuesta(conversacion);
    }

    @Transactional
    public AgenteConversacionResponse activarAgente(Long conversacionId, boolean activo) {
        Conversacion conversacion = conversacionRepository.findById(conversacionId)
                .orElseThrow(() -> new BusinessException("CONVERSACION_NO_ENCONTRADA", "Conversación no encontrada"));
        conversacion.setAgenteActivo(activo);
        if (!activo) {
            conversacion.setEstado(EstadoConversacion.ATENCION_HUMANA);
        }
        conversacionRepository.save(conversacion);
        log.info("Agente: agente {} en conversación {}", activo ? "activado" : "desactivado",
                conversacion.getId());
        return aRespuesta(conversacion);
    }

    private AgenteConversacionResponse aRespuesta(Conversacion c) {
        return new AgenteConversacionResponse(c.getId(),
                c.getTelefono(), c.getNombreContacto(), c.getEstado().name(),
                c.getPaciente() == null ? null : c.getPaciente().getId(),
                c.getPaciente() == null ? null : c.getPaciente().getNombres() + " " + c.getPaciente().getApellidos(),
                c.getIntencion() == null ? null : c.getIntencion().name(),
                c.getAgenteActivo(), c.getContextoAgente(),
                ultimoMensaje(c.getId()), c.getUltimoMensajeAt());
    }

    private String ultimoMensaje(Long conversacionId) {
        return mensajeRepository.ultimos(conversacionId, PageRequest.of(0, 1)).stream()
                .map(Mensaje::getTexto).findFirst().orElse(null);
    }

    /** Teléfono del paciente en formato legible (+593 99 111 2233). */
    static String telefonoLegible(String telefono) {
        String legible = org.dentalcrm.util.TelefonoUtil.mostrar(telefono);
        return legible == null || legible.isBlank() ? "—" : legible;
    }
}
