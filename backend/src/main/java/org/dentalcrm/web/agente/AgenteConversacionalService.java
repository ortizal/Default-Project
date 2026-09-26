package org.dentalcrm.web.agente;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dentalcrm.domain.agente.Intencion;
import org.dentalcrm.domain.cita.Cita;
import org.dentalcrm.domain.cita.CitaRepository;
import org.dentalcrm.domain.cita.EstadoCita;
import org.dentalcrm.domain.horario.HorarioOdontologo;
import org.dentalcrm.domain.horario.HorarioOdontologoRepository;
import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.odontologo.OdontologoRepository;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.domain.servicio.Servicio;
import org.dentalcrm.domain.servicio.ServicioRepository;
import org.dentalcrm.domain.whatsapp.Conversacion;
import org.dentalcrm.domain.whatsapp.ConversacionRepository;
import org.dentalcrm.domain.whatsapp.EstadoConversacion;
import org.dentalcrm.domain.whatsapp.Mensaje;
import org.dentalcrm.domain.whatsapp.MensajeRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.web.agenda.AgendaService;
import org.dentalcrm.web.agenda.HorarioPdfService;
import org.dentalcrm.web.agenda.dto.SlotResponse;
import org.dentalcrm.web.agente.ParserAgendamiento.EntradaAgendamiento;
import org.dentalcrm.web.agente.dto.AgenteConversacionResponse;
import org.dentalcrm.web.cita.CitaService;
import org.dentalcrm.web.cita.dto.CancelarRequest;
import org.dentalcrm.web.cita.dto.CitaRequest;
import org.dentalcrm.web.cita.dto.CitaResponse;
import org.dentalcrm.web.google.GoogleService;
import org.dentalcrm.web.paciente.PacienteService;
import org.dentalcrm.web.paciente.dto.PacienteRequest;
import org.dentalcrm.web.paciente.dto.PacienteResponse;
import org.dentalcrm.web.paciente.dto.TutorRequest;
import org.dentalcrm.util.TelefonoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Agente conversacional para WhatsApp (Fase 8): identifica la intención del
 * paciente, resuelve agendamientos naturales en español usando "tools"
 * (agenda, citas, servicios), y transfiere a un humano cuando hace falta.
 */
@Service
public class AgenteConversacionalService {

    private static final Logger log = LoggerFactory.getLogger(AgenteConversacionalService.class);
    private static final String MODULO = "AGENTE_IA";
    private static final String MENSAJE_MENU = """
            Puedes pedirme:\n1. Ver mis citas\n2. Ver mi perfil\n3. Agendar una cita\n4. Ver servicios\n5. Confirmar o cancelar\n6. Disponibilidad\n7. Hablar con una persona\n\nTambién puedes escribir "citas", "perfil", "google", "agendar", "servicios", etc.\nO envía tu número de cédula para que te reconozca.
            """;
        private static final String MENSAJE_SIN_PACIENTE = """
            ¡Hola! 😊 Soy el asistente de Little Smile CRM. Para ayudarte necesito encontrarte en nuestro sistema.\nEnvía tu número de cédula para localizarte, o escribe "registrarme" si eres nuevo.
            """;
    private static final String MENSAJE_PREGUNTA_CEDULA = """
            No te encuentro en nuestro sistema.\nEnvía tu número de cédula o escribe "registrarme" para crear tu ficha de paciente.
            """;
    private static final String MENSAJE_PREGUNTA_NOMBRE = """
            Perfecto, voy a crear tu ficha de paciente 📝\n¿Cuál es tu nombre?
            """;
    private static final String MENSAJE_PREGUNTA_APELLIDOS = "¿Y tus apellidos?";
    private static final String MENSAJE_PREGUNTA_FECHA = "¿Cuál es tu fecha de nacimiento?\nEscríbela así: 15/08/1990";
    private static final String MENSAJE_PREGUNTA_TUTOR = """
            Como eres menor de edad, necesito registrar a tu tutor legal (padre o madre).\n¿Quién será tu tutor? Responde "padre" o "madre".
            """;
    private static final String MENSAJE_REINICIO = "¡Hola! 😊 Soy el asistente de Little Smile CRM y reinicié nuestra conversación para ayudarte mejor.\n" + MENSAJE_MENU;
    private static final long INACTIVIDAD_REINICIO_MS = 2 * 60 * 1000L;
    private static final Set<String> PALABRAS_REINICIAR = Set.of(
            "reiniciar", "reinicia", "reinicio", "reiniciame", "reiniciar chat",
            "reset", "restablecer", "volver a empezar", "volver a comenzar",
            "volver a iniciar", "iniciar de nuevo", "iniciar otra vez",
            "empezar de nuevo", "empezar otra vez", "comenzar de nuevo",
            "empezar desde cero", "desde cero", "nueva conversacion",
            "borrar la conversacion", "olvidar");
    private static final Set<String> PALABRAS_REGISTRO = Set.of(
            "registrarme", "registrarse", "registrar", "registro",
            "registrar paciente", "registrarme como paciente", "registrarse como paciente",
            "quiero registrarme", "me quiero registrar", "crear paciente", "crear cuenta",
            "soy nuevo", "soy nueva", "darme de alta", "darte de alta", "paciente nuevo",
            "obtener ficha", "inscribirme", "inscripcion", "primera vez");
    private static final Set<String> PALABRAS_SALUDO = Set.of(
            "hola", "holi", "buenos dias", "buen dia", "buenas", "buenas tardes",
            "buenas noches", "saludos", "que tal", "como estas", "ayuda", "menu",
            "opciones", "que puedes hacer");
    private static final Set<String> PALABRAS_CONFIRMAR = Set.of(
            "confirmar", "confirma", "confirmacion", "confirmo", "confirmame");
    private static final Set<String> PALABRAS_CANCELAR = Set.of(
            "cancelar", "cancela", "cancelacion", "cancelo", "eliminar cita", "dar de baja");
    private static final Set<String> PALABRAS_SERVICIOS = Set.of(
            "servicio", "servicios", "precio", "precios", "costo", "costos", "tarifa", "tarifas", "catalogo");
    private static final Set<String> PALABRAS_DISPONIBILIDAD = Set.of(
            "disponibilidad", "cupo", "cupos", "hay a la", "hay campo", "que horario", "que horarios",
            "horario de atencion", "horarios de atencion",
            "disponibilidad del doctor", "disponibilidad doctor", "horario del doctor", "horario doctor",
            "dias de atencion", "dias y horarios", "dias que atiende", "dias que atienden",
            "cuando atiende", "cuando atienden", "atiende", "atienden", "atender", "atencion",
            "horas de atencion", "turnos");
    private static final Set<String> PALABRAS_VER_CITAS = Set.of(
            "ver citas", "mis citas", "ver mis citas", "que citas tengo",
            "citas pendientes", "proximas citas", "consultar citas", "ver agenda",
            "tengo cita", "tengo citas", "verificar citas", "verificar");
    private static final Set<String> PALABRAS_VER_PERFIL = Set.of(
            "ver perfil", "mi perfil", "perfil", "mis datos",
            "quien soy", "como soy", "informacion personal", "datos personales",
            "ver datos", "ver informacion", "quien soy yo", "como me llamo", "datos");
    private static final Set<String> PALABRAS_CONECTAR_GOOGLE = Set.of(
            "conectar google", "google calendar", "google", "conectar con google",
            "vincular google", "calendario google", "google calendar conectar",
            "conexion google", "mi google", "conectar cuenta google");
    private static final Set<String> PALABRAS_TRANSFERIR = Set.of(
            "humano", "persona", "operador", "operadora", "representante", "transferir",
            "transferencia", "que me atienda", "atenderme", "atencion humana", "asistente real");
    private static final Set<String> PALABRAS_AGENDAR = Set.of(
            "cita", "citas", "agendar", "agenda", "reservar", "reserva", "turno", "turnos",
            "queria", "necesito una", "sacar una");
    private static final Set<String> PALABRAS_CEDULA = Set.of(
            "cedula", "identificacion", "password");
    private static final Pattern P_PALABRA_AFIRM = Pattern.compile("(^|[^a-z])(si|sí|claro|seguro|ok|de acuerdo|confirmo|dale)([^a-z]|$)");
    private static final Pattern P_PALABRA_NEG = Pattern.compile("(^|[^a-z])(no|nop|nel|mejor no|no gracias)([^a-z]|$)");
    private static final Pattern PATTERN_CEDULA = Pattern.compile("^\\d{6,13}$");

    private static final Map<String, Intencion> MAPA_NUMEROS = Map.of(
            "1", Intencion.VER_CITAS,
            "2", Intencion.VER_PERFIL,
            "3", Intencion.AGENDAR_CITA,
            "4", Intencion.LISTAR_SERVICIOS,
            "5", Intencion.CONFIRMAR_CITA,
            "6", Intencion.PREGUNTAR_DISPONIBILIDAD,
            "7", Intencion.TRANSFERIR_HUMANO
    );

    private static final Map<String, Intencion> MAPA_CORTAS = Map.ofEntries(
            Map.entry("citas", Intencion.VER_CITAS),
            Map.entry("ver citas", Intencion.VER_CITAS),
            Map.entry("perfil", Intencion.VER_PERFIL),
            Map.entry("datos", Intencion.VER_PERFIL),
            Map.entry("google", Intencion.CONECTAR_GOOGLE),
            Map.entry("agendar", Intencion.AGENDAR_CITA),
            Map.entry("servicios", Intencion.LISTAR_SERVICIOS),
            Map.entry("disponibilidad", Intencion.PREGUNTAR_DISPONIBILIDAD),
            Map.entry("humano", Intencion.TRANSFERIR_HUMANO),
            Map.entry("registrar", Intencion.REGISTRAR_PACIENTE),
            Map.entry("cancelar", Intencion.CANCELAR_CITA),
            Map.entry("confirmar", Intencion.CONFIRMAR_CITA),
            Map.entry("atender", Intencion.CONFIRMAR_CITA),
            Map.entry("menu", Intencion.SALUDO),
            Map.entry("ayuda", Intencion.SALUDO),
            Map.entry("horario", Intencion.PREGUNTAR_DISPONIBILIDAD)
    );

    private final ConversacionRepository conversacionRepository;
    private final MensajeRepository mensajeRepository;
    private final PacienteRepository pacienteRepository;
    private final CitaRepository citaRepository;
    private final CitaService citaService;
    private final AgendaService agendaService;
    private final ServicioRepository servicioRepository;
    private final OdontologoRepository odontologoRepository;
    private final HorarioOdontologoRepository horarioRepository;
    private final HorarioPdfService horarioPdfService;
    private final PacienteService pacienteService;
    private final GoogleService googleService;
    private final ObjectMapper objectMapper;
    private final ZoneId zona;

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
                                        @Value("${app.timezone}") String timezone) {
        this.conversacionRepository = conversacionRepository;
        this.mensajeRepository = mensajeRepository;
        this.pacienteRepository = pacienteRepository;
        this.citaRepository = citaRepository;
        this.citaService = citaService;
        this.agendaService = agendaService;
        this.servicioRepository = servicioRepository;
        this.odontologoRepository = odontologoRepository;
        this.horarioRepository = horarioRepository;
        this.horarioPdfService = horarioPdfService;
        this.pacienteService = pacienteService;
        this.googleService = googleService;
        this.objectMapper = objectMapper;
        this.zona = ZoneId.of(timezone);
    }

    public record Adjunto(String nombreArchivo, String mime, byte[] contenido, String caption) {
    }

    public record RespuestaAgente(String mensaje, boolean transferirHumano, Adjunto adjunto) {
        static RespuestaAgente silenciosa() {
            return new RespuestaAgente(null, false, null);
        }
    }

    // ------------------------------------------------------------------
    // Procesamiento principal (llamado desde el webhook de WhatsApp)
    // ------------------------------------------------------------------

    @Transactional
    public RespuestaAgente procesar(Conversacion conversacion, String textoEntrada, Instant ultimoPrev) {
        if (!Boolean.TRUE.equals(conversacion.getAgenteActivo())
                || conversacion.getEstado() == EstadoConversacion.ATENCION_HUMANA
                || conversacion.getEstado() == EstadoConversacion.CERRADA) {
            return RespuestaAgente.silenciosa();
        }
        String t = ParserAgendamiento.normalizar(textoEntrada);
        if (t.isBlank()) {
            return RespuestaAgente.silenciosa();
        }
        if (PATTERN_CEDULA.matcher(t).matches()) {
            return buscarPorCedula(conversacion, t);
        }
        if (inactiva(ultimoPrev)) {
            return reiniciar(conversacion);
        }
        if (contiene(t, PALABRAS_REINICIAR)) {
            return reiniciar(conversacion);
        }
        Intencion intencionDirecta = clasificar(t);
        if (intencionDirecta == Intencion.PREGUNTAR_DISPONIBILIDAD) {
            return preguntarDisponibilidad(conversacion, contexto(conversacion), t);
        }
        Map<String, Object> ctx = contexto(conversacion);

        if (conversacion.getIntencion() == Intencion.REGISTRAR_PACIENTE) {
            return gestionarRegistro(conversacion, ctx, t, textoEntrada);
        }

        if ("CREAR".equals(ctx.get("paso"))) {
            if (esConfirmacion(t)) {
                return crearCita(conversacion, ctx);
            }
            if (esRechazo(t)) {
                limpiarContexto(conversacion, Intencion.DESCONOCIDO);
                return respuesta("No reservé ninguna cita. ¿Puedo ayudarte con algo más?\n" + MENSAJE_MENU, Intencion.DESCONOCIDO);
            }
            return respuesta("¿Confirmas tu cita? Responde 'sí' para agendarla o 'no' para cancelarla.", Intencion.AGENDAR_CITA);
        }

        if (conversacion.getIntencion() == Intencion.AGENDAR_CITA && ctx.containsKey("paso")) {
            return gestionarAgendamiento(conversacion, ctx, t);
        }

        Intencion intencion = clasificar(t);
        if (intencion == Intencion.VER_PERFIL && PATTERN_CEDULA.matcher(t).matches()) {
            return buscarPorCedula(conversacion, t);
        }
        return switch (intencion) {
            case TRANSFERIR_HUMANO -> transferirHumano(conversacion);
            case VER_PERFIL -> verPerfil(conversacion);
            case CONECTAR_GOOGLE -> conectarGoogle(conversacion);
            case VER_CITAS -> verCitas(conversacion);
            case SALUDO -> saludar(conversacion);
            case LISTAR_SERVICIOS -> listarServicios(conversacion);
            case PREGUNTAR_DISPONIBILIDAD -> preguntarDisponibilidad(conversacion, ctx, t);
            case CONFIRMAR_CITA -> confirmarCita(conversacion);
            case CANCELAR_CITA -> cancelarCita(conversacion);
            case AGENDAR_CITA -> gestionarAgendamiento(conversacion, ctx, t);
            case REGISTRAR_PACIENTE -> gestionarRegistro(conversacion, ctx, t, textoEntrada);
            default -> {
                guardar(conversacion, Intencion.DESCONOCIDO, null);
                yield respuesta("No logré entenderte 😅\n" + MENSAJE_MENU, Intencion.DESCONOCIDO);
            }
        };
    }

    // ------------------------------------------------------------------
    // Recuperación de chats: reproduce la pregunta pendiente
    // ------------------------------------------------------------------

    /**
     * Texto con el que se retoma una conversación cuyo último mensaje fue una
     * entrada del paciente que nunca se contestó (p. ej. el servicio se
     * reinició a mitad de un paso).
     *
     * <p>Solo calcula el mensaje: no ejecuta acciones (no crea citas ni fichas)
     * ni modifica el estado guardado, de modo que reintentarlo es inocuo. Si no
     * hay paso en curso pide la cédula, salvo que el paciente ya esté
     * vinculado a la conversación, en cuyo caso se limita a saludar.
     */
    @Transactional(readOnly = true)
    public String mensajeDeReanudacion(Conversacion conversacion) {
        if (conversacion.getPaciente() == null
                && conversacion.getIntencion() != Intencion.REGISTRAR_PACIENTE) {
            return MENSAJE_SIN_PACIENTE;
        }
        Map<String, Object> ctx = contexto(conversacion);
        String paso = String.valueOf(ctx.getOrDefault("paso", ""));
        if (conversacion.getIntencion() == Intencion.REGISTRAR_PACIENTE) {
            return mensajePasoRegistro(paso, ctx);
        }
        if (conversacion.getIntencion() == Intencion.AGENDAR_CITA) {
            String pendiente = mensajePasoAgendamiento(paso, ctx);
            if (pendiente != null) {
                return pendiente;
            }
        }
        return conversacion.getPaciente() == null
                ? MENSAJE_SIN_PACIENTE
                : "¡Hola " + conversacion.getPaciente().getNombres()
                        + "! 😊 ¿En qué puedo ayudarte?\n" + MENSAJE_MENU;
    }

    private String mensajePasoRegistro(String paso, Map<String, Object> ctx) {
        return switch (paso) {
            case "REG_CONFIRMAR" -> {
                String cedula = valor(ctx, "cedula");
                yield cedula.isEmpty()
                        ? MENSAJE_SIN_PACIENTE
                        : "No te encuentro registrado con la cédula " + cedula
                                + ".\n¿Quieres que cree tu ficha de paciente? Responde 'sí'.";
            }
            case "REG_NOMBRES" -> MENSAJE_PREGUNTA_NOMBRE;
            case "REG_APELLIDOS" -> MENSAJE_PREGUNTA_APELLIDOS;
            case "REG_FECHA" -> MENSAJE_PREGUNTA_FECHA;
            case "REG_TUTOR" -> MENSAJE_PREGUNTA_TUTOR;
            case "REG_TUTOR_NOMBRE" -> {
                String parentesco = valor(ctx, "tutorParentesco");
                yield parentesco.isEmpty()
                        ? "¿Cuál es el nombre del tutor?"
                        : "Perfecto. ¿Cuál es el nombre del " + parentesco.toLowerCase() + "?";
            }
            case "REG_TUTOR_APELLIDO" -> "¿Y los apellidos del tutor?";
            case "REG_CONFIRMAR_DATOS" -> valor(ctx, "nombres").isEmpty() || valor(ctx, "apellidos").isEmpty()
                    ? MENSAJE_SIN_PACIENTE
                    : resumenRegistro(ctx, null) + "\n¿Confirmas estos datos? Responde 'sí' para guardarlos.";
            default -> MENSAJE_SIN_PACIENTE;
        };
    }

    private String mensajePasoAgendamiento(String paso, Map<String, Object> ctx) {
        return switch (paso) {
            case "SERVICIO" -> {
                List<Servicio> servicios = serviciosActivos();
                yield servicios.isEmpty()
                        ? "Aún no tenemos servicios activos. Escribe 'hablar con una persona' para más ayuda."
                        : preguntaServicio(servicios, null);
            }
            case "DOCTOR" -> preguntaOdontologo(odontologosActivos(), null);
            case "FECHA" -> preguntaFecha(null);
            case "HORA" -> {
                LocalDate fecha = fechaDeTexto(ctx.get("fecha"));
                String horas = horasDeContexto(ctx);
                int total = ctx.get("slots") instanceof List<?> s ? s.size() : 0;
                yield fecha == null || horas.isEmpty()
                        ? "¿A qué hora te conviene?\n\nResponde la hora con dos puntos (ej: 10:00)."
                        : "¿A qué hora te conviene el " + formatearFecha(fecha) + "?\n\n" + horas
                                + "\n\nResponde el número de la opción (1-" + total
                                + ") o la hora con dos puntos (ej: 10:00).";
            }
            case "CREAR" -> {
                Long servicioId = idDeContexto(ctx.get("servicioId"));
                Long doctorId = idDeContexto(ctx.get("doctorId"));
                LocalDate fecha = fechaDeTexto(ctx.get("fecha"));
                Object hora = ctx.get("hora");
                yield servicioId == null || doctorId == null || fecha == null || hora == null
                        ? null
                        : "Perfecto ✨ ¿Confirmo tu cita?\n• Servicio: " + nombreServicio(servicioId)
                                + "\n• Odontólogo: " + nombreDoctor(doctorId)
                                + "\n• Fecha: " + formatearFecha(fecha)
                                + "\n• Hora: " + hora + "\nResponde 'sí' para confirmarla.";
            }
            default -> null;
        };
    }

    private String valor(Map<String, Object> ctx, String clave) {
        Object valor = ctx.get(clave);
        if (valor == null) {
            return "";
        }
        String texto = String.valueOf(valor);
        return "null".equalsIgnoreCase(texto) ? "" : texto;
    }

    private Long idDeContexto(Object valor) {
        if (valor == null) {
            return null;
        }
        String texto = String.valueOf(valor);
        if (texto.isBlank() || "null".equalsIgnoreCase(texto)) {
            return null;
        }
        try {
            return Long.valueOf(texto);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LocalDate fechaDeTexto(Object valor) {
        if (valor == null) {
            return null;
        }
        String texto = String.valueOf(valor);
        if (texto.isBlank() || "null".equalsIgnoreCase(texto)) {
            return null;
        }
        try {
            return LocalDate.parse(texto);
        } catch (java.time.format.DateTimeParseException ex) {
            return null;
        }
    }

    private String horasDeContexto(Map<String, Object> ctx) {
        if (!(ctx.get("slots") instanceof List<?> slots)) {
            return "";
        }
        return formatearOpcionesEnDosColumnas(slots.stream()
                .filter(java.util.Objects::nonNull)
                .map(String::valueOf)
                .toList());
    }

    private RespuestaAgente buscarPorCedula(Conversacion conversacion, String cedula) {
        conversacion.setPaciente(null);
        conversacion.setContextoAgente(null);
        Optional<Paciente> paciente = pacienteRepository.findByCedulaIgnoreCase(cedula);
        if (paciente.isEmpty()) {
            guardar(conversacion, Intencion.REGISTRAR_PACIENTE,
                    Map.of("paso", "REG_CONFIRMAR", "cedula", cedula));
                return respuesta("No encontré ningún paciente con cédula " + cedula
                    + ".\n¿Quieres que cree tu ficha de paciente? Responde 'sí'.", Intencion.REGISTRAR_PACIENTE);
        }
        Paciente p = paciente.get();
        conversacion.setPaciente(p);
        guardar(conversacion, Intencion.VER_PERFIL, null);
        return respuesta("¡Encontrado! 📋 Datos de " + p.getNombres() + " " + p.getApellidos() + ":\n"
                + "📇 Cédula: " + p.getCedula() + "\n"
                + "📱 Teléfono: " + telefonoLegible(p.getTelefono()) + "\n"
                + "📅 Registrado: " + p.getCreatedAt() + "\n\n"
                + "Responde:\n1. Ver mis citas\n2. Ver mi perfil\n3. Agendar una cita\n4. Ver servicios\n5. Confirmar o cancelar\n6. Disponibilidad\n7. Hablar con una persona",
                Intencion.VER_PERFIL);
    }

    // ------------------------------------------------------------------
    // Clasificación de la intención
    // ------------------------------------------------------------------

    private Intencion clasificar(String t) {
        if (PATTERN_CEDULA.matcher(t).matches()) {
            return Intencion.VER_PERFIL;
        }
        if (MAPA_NUMEROS.containsKey(t)) {
            return MAPA_NUMEROS.get(t);
        }
        if (MAPA_CORTAS.containsKey(t)) {
            return MAPA_CORTAS.get(t);
        }
        if (contiene(t, PALABRAS_TRANSFERIR)) {
            return Intencion.TRANSFERIR_HUMANO;
        }
        if (contiene(t, PALABRAS_REGISTRO)) {
            return Intencion.REGISTRAR_PACIENTE;
        }
        if (contiene(t, PALABRAS_SERVICIOS)) {
            return Intencion.LISTAR_SERVICIOS;
        }
        if (contiene(t, PALABRAS_DISPONIBILIDAD)) {
            return Intencion.PREGUNTAR_DISPONIBILIDAD;
        }
        if (contiene(t, PALABRAS_CONFIRMAR)) {
            return Intencion.CONFIRMAR_CITA;
        }
        if (contiene(t, PALABRAS_CANCELAR)) {
            return Intencion.CANCELAR_CITA;
        }
        if (contiene(t, PALABRAS_VER_PERFIL)) {
            return Intencion.VER_PERFIL;
        }
        if (contiene(t, PALABRAS_CONECTAR_GOOGLE)) {
            return Intencion.CONECTAR_GOOGLE;
        }
        if (contiene(t, PALABRAS_VER_CITAS)) {
            return Intencion.VER_CITAS;
        }
        if (contiene(t, PALABRAS_AGENDAR)) {
            return Intencion.AGENDAR_CITA;
        }
        List<Servicio> servicios = serviciosActivos();
        List<Odontologo> odontologos = odontologosActivos();
        EntradaAgendamiento entrada = ParserAgendamiento.parsear(t, hoy(), servicios, odontologos);
        if (entrada != null && entrada.servicioId() != null) {
            return Intencion.AGENDAR_CITA;
        }
        if (contiene(t, PALABRAS_SALUDO)) {
            return Intencion.SALUDO;
        }
        return Intencion.DESCONOCIDO;
    }

    // ------------------------------------------------------------------
    // Intenciones
    // ------------------------------------------------------------------

    private RespuestaAgente verCitas(Conversacion conversacion) {
        Paciente paciente = conversacion.getPaciente();
        if (paciente == null) {
            return pedirRegistro(conversacion);
        }
        List<Cita> citas = citaRepository.proximasDelPaciente(paciente.getId(), hoy()).stream()
                .filter(c -> c.getEstado().esActiva() || c.getEstado() == EstadoCita.PENDIENTE)
                .toList();
        if (citas.isEmpty()) {
            return respuesta("No encuentro citas próximas para ti. ¿Quieres agendar una nueva?", Intencion.VER_CITAS);
        }
        StringBuilder sb = new StringBuilder("Tienes " + citas.size() + " cita(s) próxima(s):\n");
        for (int i = 0; i < citas.size(); i++) {
            Cita c = citas.get(i);
            sb.append(i + 1).append(". ").append(c.getServicio().getNombre())
                    .append(" — ").append(formatearFecha(c.getFecha()))
                    .append(" a las ").append(c.getHoraInicio().toString())
                    .append(" con ").append(c.getDoctor().getNombres())
                    .append("\n");
        }
        sb.append("Responde 'confirmar' o 'cancelar' para gestionar una.");
        guardar(conversacion, Intencion.VER_CITAS, null);
        return respuesta(sb.toString(), Intencion.VER_CITAS);
    }

    private RespuestaAgente verPerfil(Conversacion conversacion) {
        Paciente paciente = conversacion.getPaciente();
        if (paciente == null) {
            return pedirRegistro(conversacion);
        }
        return respuesta(
                "Datos:\n📋 Nombre: " + paciente.getNombres() + " " + paciente.getApellidos()
                        + "\n📇 Cédula: " + paciente.getCedula()
                        + "\n📱 Teléfono: " + telefonoLegible(paciente.getTelefono())
                        + "\n📅 Registrado: " + paciente.getCreatedAt()
                        + "\n\nResponde \"ver citas\" para ver tus citas o \"cita\" para agendar.",
                Intencion.VER_PERFIL);
    }

    private RespuestaAgente conectarGoogle(Conversacion conversacion) {
        if (!googleService.estaConfigurado()) {
            return respuesta(
                    "La integración con Google Calendar no está configurada todavía.\n"
                    + "Habla con un administrador para activarla.",
                    Intencion.CONECTAR_GOOGLE);
        }
        try {
            String authUrl = googleService.urlAutenticacion();
            return respuesta(
                    "Para conectar tu Google Calendar visita esta URL:\n" + authUrl
                    + "\n\nUna vez autorizado, vuelve aquí.",
                    Intencion.CONECTAR_GOOGLE);
        } catch (Exception ex) {
            log.warn("Agente: no se pudo generar URL de Google: {}", ex.getMessage());
            return respuesta("No pude generar el enlace de Google Calendar. Intenta de nuevo o habla con una persona.",
                    Intencion.CONECTAR_GOOGLE);
        }
    }

    private RespuestaAgente saludar(Conversacion conversacion) {
        guardar(conversacion, Intencion.SALUDO, null);
        return respuesta("¡Hola! 😊 Soy el asistente virtual de Little Smile CRM y te ayudo con tus citas.\n" + MENSAJE_MENU, Intencion.SALUDO);
    }

    private RespuestaAgente listarServicios(Conversacion conversacion) {
        List<Servicio> servicios = serviciosActivos();
        if (servicios.isEmpty()) {
            guardar(conversacion, Intencion.LISTAR_SERVICIOS, null);
            return respuesta("Aún no tenemos servicios publicados. ¿Quieres hablar con una persona?", Intencion.LISTAR_SERVICIOS);
        }
        StringBuilder sb = new StringBuilder("Estos son nuestros servicios:\n");
        for (int i = 0; i < servicios.size(); i++) {
            Servicio s = servicios.get(i);
            sb.append(i + 1).append(". ").append(s.getNombre())
                    .append(" — ").append(s.getDuracionMinutos()).append(" min")
                    .append(" — $").append(s.getPrecio()).append("\n");
        }
        sb.append("¿Quieres agendar alguno? Escríbeme el nombre o el número.");
        guardar(conversacion, Intencion.LISTAR_SERVICIOS, null);
        return respuesta(sb.toString(), Intencion.LISTAR_SERVICIOS);
    }

    private RespuestaAgente confirmarCita(Conversacion conversacion) {
        Paciente paciente = conversacion.getPaciente();
        if (paciente == null) {
            return pedirRegistro(conversacion);
        }
        Cita proxima = proximaCita(paciente);
        guardar(conversacion, Intencion.CONFIRMAR_CITA, null);
        if (proxima == null) {
            return respuesta("No encuentro una cita próxima para confirmar. ¿Quieres agendar una nueva?", Intencion.CONFIRMAR_CITA);
        }
        try {
            CitaResponse confirmada = citaService.confirmar(proxima.getId(), "WHATSAPP");
            log.info("Agente: paciente {} confirmó la cita {}", paciente.getId(), proxima.getId());
            return respuesta("¡Listo! 🙌 Confirmé tu cita de " + confirmada.servicioNombre()
                    + " para el " + formatearFecha(confirmada.fecha()) + " a las " + confirmada.horaInicio(), Intencion.CONFIRMAR_CITA);
        } catch (Exception ex) {
            log.warn("Agente: no se pudo confirmar la cita {}: {}", proxima.getId(), ex.getMessage());
            return respuesta("No pude confirmar tu cita. Puede que ya esté confirmada o cancelada. ¿Hablamos con una persona?", Intencion.CONFIRMAR_CITA);
        }
    }

    private RespuestaAgente cancelarCita(Conversacion conversacion) {
        Paciente paciente = conversacion.getPaciente();
        if (paciente == null) {
            return pedirRegistro(conversacion);
        }
        Cita proxima = proximaCita(paciente);
        guardar(conversacion, Intencion.CANCELAR_CITA, null);
        if (proxima == null) {
            return respuesta("No encuentro una cita próxima para cancelar.", Intencion.CANCELAR_CITA);
        }
        try {
            CitaResponse cancelada = citaService.cancelar(proxima.getId(), new CancelarRequest("Cancelada por el paciente (agente IA)"));
            log.info("Agente: paciente {} canceló la cita {}", paciente.getId(), proxima.getId());
            return respuesta("Listo, cancelé tu cita de " + cancelada.servicioNombre()
                    + " del " + formatearFecha(cancelada.fecha()) + ". Si cambias de opinión, escríbeme para reagendar 😉", Intencion.CANCELAR_CITA);
        } catch (Exception ex) {
            log.warn("Agente: no se pudo cancelar la cita {}: {}", proxima.getId(), ex.getMessage());
            return respuesta("No pude cancelar tu cita. ¿Quieres que una persona te ayude?", Intencion.CANCELAR_CITA);
        }
    }

    // ------------------------------------------------------------------
    // Tool: disponibilidad y horarios de atención
    // ------------------------------------------------------------------

    private RespuestaAgente preguntarDisponibilidad(Conversacion conversacion, Map<String, Object> ctx, String t) {
        List<Odontologo> odontologos = odontologosActivos();
        List<Servicio> servicios = serviciosActivos();

        // Si acabamos de mostrar la lista de odontólogos, la respuesta solo puede
        // ser el nombre o el número de esa lista: sin acotarlo, un "2" se tomaría
        // como fecha, servicio u hora de otro paso.
        boolean preguntandoOdontologo = !ctx.containsKey("doctorId") && ctx.containsKey("desde");
        EntradaAgendamiento entrada = preguntandoOdontologo
                ? ParserAgendamiento.entradaParaPaso("DOCTOR", t, hoy(), servicios, odontologos)
                : ParserAgendamiento.parsear(t, hoy(), servicios, odontologos);

        Long doctorId = numero(ctx, "doctorId", entrada.doctorId());
        if (doctorId == null) {
            if (odontologos.isEmpty()) {
                return respuesta("Aún no tenemos odontólogos activos. Escribe 'hablar con una persona' para más ayuda.",
                        Intencion.PREGUNTAR_DISPONIBILIDAD);
            }
            if (odontologos.size() == 1) {
                doctorId = odontologos.get(0).getId();
            } else {
                Map<String, Object> nuevo = new java.util.HashMap<>(ctx);
                nuevo.put("desde", entrada.fecha() == null ? hoy().plusDays(1).toString() : entrada.fecha().toString());
                guardar(conversacion, Intencion.PREGUNTAR_DISPONIBILIDAD, nuevo);
                String error = preguntandoOdontologo
                        ? "No entendí \"" + recortar(t) + "\" como un odontólogo.\n\n" : "";
                return respuesta(error + preguntaOdontologo(odontologos, null),
                        Intencion.PREGUNTAR_DISPONIBILIDAD);
            }
        }
        Long servicioId = numero(ctx, "servicioId", entrada.servicioId());

        // Consulta general de días y horarios de atención: sin fecha concreta ni
        // un "desde" previo en el contexto → horario semanal + próximos cupos + PDF.
        if (entrada.fecha() == null && !ctx.containsKey("desde")) {
            return responderHorarioSemanal(conversacion, doctorId, servicioId);
        }

        LocalDate fecha = entrada.fecha() != null
                ? entrada.fecha()
                : LocalDate.parse(String.valueOf(ctx.get("desde")));
        Map<String, Object> guardarCtx = new java.util.HashMap<>();
        guardarCtx.put("desde", fecha.toString());
        guardarCtx.put("doctorId", doctorId);
        guardarCtx.put("servicioId", servicioId);
        guardar(conversacion, Intencion.PREGUNTAR_DISPONIBILIDAD, guardarCtx);
        return responderDisponibilidad(conversacion, doctorId, fecha, servicioId);
    }

    private RespuestaAgente responderDisponibilidad(Conversacion conversacion, Long doctorId, LocalDate fecha, Long servicioId) {
        List<SlotResponse> slots = agendaService.disponibilidad(doctorId, fecha, servicioId, null);
        String doctorNombre = nombreDoctor(doctorId);
        if (slots.isEmpty()) {
            return respuesta("No encuentro cupos disponibles el " + formatearFecha(fecha)
                    + " con " + doctorNombre + ". 😕\n¿Quieres probar con 'mañana' o con otro día?", Intencion.PREGUNTAR_DISPONIBILIDAD);
        }
        return respuesta("Para el " + formatearFecha(fecha) + " con " + doctorNombre + " hay estos cupos:\n\n"
                + horasDeSlots(slots) + "\n\nSi quieres agendar una cita ese día, escríbeme 'quiero una cita "
                + formatearFecha(fecha) + "' 😊", Intencion.PREGUNTAR_DISPONIBILIDAD);
    }

    private RespuestaAgente responderHorarioSemanal(Conversacion conversacion, Long doctorId, Long servicioId) {
        Odontologo doctor = odontologoRepository.findById(doctorId).orElse(null);
        if (doctor == null) {
            return respuesta("No encuentro ese odontólogo. ¿Quieres probar con otro?", Intencion.PREGUNTAR_DISPONIBILIDAD);
        }
        List<HorarioOdontologo> horarios = horarioRepository
                .findByOdontologoIdOrderByDiaSemanaAscHoraInicioAsc(doctorId).stream()
                .filter(h -> "ACTIVO".equals(h.getEstado()))
                .toList();
        if (horarios.isEmpty()) {
            guardar(conversacion, Intencion.PREGUNTAR_DISPONIBILIDAD, Map.of("doctorId", doctorId));
            return respuesta("El Dr. " + doctor.getNombres() + " " + doctor.getApellidos()
                    + " no tiene horarios de atención configurados. 😕\n¿Quieres hablar con una persona para agendar?", Intencion.PREGUNTAR_DISPONIBILIDAD);
        }

        List<String> cupos = proximosCupos(doctorId, servicioId);
        StringBuilder sb = new StringBuilder("El Dr. ").append(doctor.getNombres()).append(" ").append(doctor.getApellidos())
                .append(" atiende:\n").append(resumenHorario(horarios)).append("\n");
        if (!cupos.isEmpty()) {
            sb.append("\nPróximos cupos disponibles:\n");
            for (int i = 0; i < Math.min(cupos.size(), 5); i++) {
                sb.append("• ").append(cupos.get(i)).append("\n");
            }
        }
        sb.append("\nTe adjunto el horario completo y la disponibilidad de los próximos 7 días en PDF 😊");

        List<HorarioPdfService.DiaDisponibilidad> dias = disponibilidadPorDia(doctorId, servicioId);
        byte[] pdf = null;
        try {
            pdf = horarioPdfService.generar(doctor, horarios, cupos, dias);
        } catch (Exception ex) {
            log.warn("Agente: no se pudo generar el PDF del horario del doctor {}: {}", doctorId, ex.getMessage());
        }
        Map<String, Object> guardarCtx = new java.util.HashMap<>();
        guardarCtx.put("doctorId", doctorId);
        guardarCtx.put("servicioId", servicioId);
        guardar(conversacion, Intencion.PREGUNTAR_DISPONIBILIDAD, guardarCtx);
        Adjunto adjunto = pdf == null ? null
                : new Adjunto(horarioPdfService.nombreArchivo(doctor), "application/pdf", pdf, "Horario de atención");
        return respuestaConAdjunto(sb.toString(), Intencion.PREGUNTAR_DISPONIBILIDAD, adjunto);
    }

    private String resumenHorario(List<HorarioOdontologo> horarios) {
        Map<String, List<HorarioOdontologo>> porRango = new LinkedHashMap<>();
        for (HorarioOdontologo h : horarios) {
            porRango.computeIfAbsent(h.getHoraInicio() + "-" + h.getHoraFin(), k -> new ArrayList<>()).add(h);
        }
        List<String> filas = new ArrayList<>();
        for (var e : porRango.entrySet()) {
            List<HorarioOdontologo> hs = e.getValue().stream()
                    .sorted(Comparator.comparing(HorarioOdontologo::getDiaSemana))
                    .toList();
            HorarioOdontologo h = hs.get(0);
            filas.add("• " + resumirDias(hs) + " de " + h.getHoraInicio() + " a " + h.getHoraFin());
        }
        return String.join("\n", filas);
    }

    private String resumirDias(List<HorarioOdontologo> hs) {
        List<Integer> dias = hs.stream().map(HorarioOdontologo::getDiaSemana).toList();
        StringBuilder sb = new StringBuilder();
        int inicio = 0;
        while (inicio < dias.size()) {
            int fin = inicio;
            while (fin + 1 < dias.size() && dias.get(fin + 1) == dias.get(fin) + 1) {
                fin++;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(nombreDiaSemana(dias.get(inicio)));
            if (fin > inicio) {
                sb.append(" a ").append(nombreDiaSemana(dias.get(fin)));
            }
            inicio = fin + 1;
        }
        return sb.toString();
    }

    private String nombreDiaSemana(int diaSemana) {
        DayOfWeek d = DayOfWeek.of(diaSemana);
        return d.getDisplayName(TextStyle.FULL, new Locale("es", "EC"));
    }

    private List<String> proximosCupos(Long doctorId, Long servicioId) {
        List<String> cupos = new ArrayList<>();
        for (int offset = 1; offset <= 14 && cupos.size() < 8; offset++) {
            LocalDate f = hoy().plusDays(offset);
            List<SlotResponse> slots = agendaService.disponibilidad(doctorId, f, servicioId, null);
            if (!slots.isEmpty()) {
                cupos.add(formatearFecha(f) + " a las " + slots.get(0).horaInicio());
            }
        }
        return cupos;
    }

    /**
     * Las horas libres y ocupadas de los próximos 7 días, listas para imprimir
     * en el PDF: así el paciente ve de un vistazo cuándo hay espacio sin tener
     * que reconstruir la disponibilidad a partir del horario semanal.
     */
    private List<HorarioPdfService.DiaDisponibilidad> disponibilidadPorDia(Long doctorId, Long servicioId) {
        List<HorarioPdfService.DiaDisponibilidad> dias = new ArrayList<>();
        for (int offset = 1; offset <= 7; offset++) {
            LocalDate fecha = hoy().plusDays(offset);
            String libre = agendaService.disponibilidad(doctorId, fecha, servicioId, null).stream()
                    .map(s -> s.horaInicio().toString())
                    .collect(java.util.stream.Collectors.joining(", "));
            String ocupado = agendaService.ocupados(doctorId, fecha).stream()
                    .map(AgendaService.Ocupado::descripcion)
                    .collect(java.util.stream.Collectors.joining("\n"));
            dias.add(new HorarioPdfService.DiaDisponibilidad(formatearFecha(fecha),
                    libre.isEmpty() ? "—" : libre,
                    ocupado.isEmpty() ? "—" : ocupado));
        }
        return dias;
    }

    // ------------------------------------------------------------------
    // Tool: agendamiento de citas (flujo conversacional)
    // ------------------------------------------------------------------

    private RespuestaAgente gestionarAgendamiento(Conversacion conversacion, Map<String, Object> ctx, String t) {
        Paciente paciente = conversacion.getPaciente();
        if (paciente == null) {
            return pedirRegistro(conversacion);
        }
        List<Servicio> servicios = serviciosActivos();
        List<Odontologo> odontologos = odontologosActivos();
        Map<String, Object> c = new LinkedHashMap<>(ctx);
        String paso = String.valueOf(c.getOrDefault("paso", ""));

        // Mientras hay una pregunta en pantalla solo se interpreta la respuesta
        // de ese paso: así un "2" es la opción 2 de la lista mostrada y no se
        // confunde con el servicio, el odontólogo o la fecha de otros pasos.
        EntradaAgendamiento entrada = ParserAgendamiento.entradaParaPaso(paso, t, hoy(), servicios, odontologos);
        if (entrada.servicioId() != null) {
            c.put("servicioId", entrada.servicioId());
        }
        if (entrada.doctorId() != null) {
            c.put("doctorId", entrada.doctorId());
        }
        if (entrada.fecha() != null) {
            c.put("fecha", entrada.fecha().toString());
        }
        if (entrada.horaInicio() != null) {
            c.put("hora", entrada.horaInicio().toString());
        }

        if (c.get("servicioId") == null) {
            c.put("paso", "SERVICIO");
            guardar(conversacion, Intencion.AGENDAR_CITA, c);
            if (servicios.isEmpty()) {
                return respuesta("Aún no tenemos servicios activos. Escribe 'hablar con una persona' para más ayuda.", Intencion.AGENDAR_CITA);
            }
            return respuesta(preguntaServicio(servicios,
                    "SERVICIO".equals(paso) ? "No entendí \"" + recortar(t) + "\" como un servicio." : null),
                    Intencion.AGENDAR_CITA);
        }

        if (c.get("doctorId") == null) {
            c.put("paso", "DOCTOR");
            if (odontologos.isEmpty()) {
                guardar(conversacion, Intencion.AGENDAR_CITA, c);
                return respuesta("Aún no tenemos odontólogos activos. Escribe 'hablar con una persona' para más ayuda.", Intencion.AGENDAR_CITA);
            }
            if (odontologos.size() == 1) {
                c.put("doctorId", odontologos.get(0).getId());
            } else {
                guardar(conversacion, Intencion.AGENDAR_CITA, c);
                return respuesta(preguntaOdontologo(odontologos,
                        "DOCTOR".equals(paso) ? "No entendí \"" + recortar(t) + "\" como un odontólogo." : null),
                        Intencion.AGENDAR_CITA);
            }
        }

        if (c.get("fecha") == null) {
            c.put("paso", "FECHA");
            guardar(conversacion, Intencion.AGENDAR_CITA, c);
            return respuesta(preguntaFecha(
                    "FECHA".equals(paso) ? "No entendí \"" + recortar(t) + "\" como una fecha." : null),
                    Intencion.AGENDAR_CITA);
        }

        LocalDate fecha = java.time.LocalDate.parse((String) c.get("fecha"));
        Long servicioId = Long.valueOf(String.valueOf(c.get("servicioId")));
        Long doctorId = Long.valueOf(String.valueOf(c.get("doctorId")));

        if (c.get("hora") == null) {
            RespuestaAgente pendiente = resolverOpedirHora(conversacion, c, fecha, servicioId, doctorId,
                    "HORA".equals(paso) ? t : null, null);
            if (pendiente != null) {
                return pendiente;
            }
        }

        LocalTime hora = java.time.LocalTime.parse((String) c.get("hora"));
        if (!agendaService.disponibilidad(doctorId, fecha, servicioId, null)
                .stream().map(SlotResponse::horaInicio).anyMatch(hora::equals)) {
            return resolverOpedirHora(conversacion, c, fecha, servicioId, doctorId, null,
                    "La hora " + hora + " no está disponible el " + formatearFecha(fecha) + ". Elige una de estas:");
        }

        c.put("paso", "CREAR");
        guardar(conversacion, Intencion.AGENDAR_CITA, c);
        return respuesta("Perfecto ✨ ¿Confirmo tu cita?\n• Servicio: " + nombreServicio(servicioId)
                + "\n• Odontólogo: " + nombreDoctor(doctorId)
                + "\n• Fecha: " + formatearFecha(fecha)
                + "\n• Hora: " + hora + "\nResponde 'sí' para confirmarla.", Intencion.AGENDAR_CITA);
    }

    private record HoraResuelta(LocalTime hora, String error) {
    }

    /**
     * Valida la hora contra los cupos reales del día y, si sigue sin resolverse,
     * muestra la lista de opciones (una por línea) junto al error.
     *
     * @param texto respuesta del paciente; solo se interpreta si ya se preguntó
     *              la hora ({@code null} para pedirla de nuevo sin error)
     * @param error aviso previo a la lista; {@code null} si no lo hay
     * @return {@code null} cuando la hora quedó guardada en el contexto
     */
    private RespuestaAgente resolverOpedirHora(Conversacion conversacion, Map<String, Object> c,
                                               LocalDate fecha, Long servicioId, Long doctorId,
                                               String texto, String error) {
        List<SlotResponse> slots = agendaService.disponibilidad(doctorId, fecha, servicioId, null);
        if (slots.isEmpty()) {
            limpiarContexto(conversacion, Intencion.DESCONOCIDO);
            return respuesta("No hay cupos disponibles el " + formatearFecha(fecha) + " con " + nombreDoctor(doctorId)
                    + ". 😕 Puedo probar con 'mañana' o 'pasado mañana'.", Intencion.AGENDAR_CITA);
        }
        List<LocalTime> opciones = slots.stream().map(SlotResponse::horaInicio).toList();

        if (error == null && texto != null) {
            HoraResuelta resuelta = resolverHora(texto, opciones);
            if (resuelta.hora() != null) {
                c.put("hora", resuelta.hora().toString());
                c.put("paso", "HORA");
                guardar(conversacion, Intencion.AGENDAR_CITA, c);
                return null;
            }
            error = resuelta.error();
        }

        c.remove("hora");
        c.put("paso", "HORA");
        c.put("slots", opciones.stream().map(LocalTime::toString).toList());
        guardar(conversacion, Intencion.AGENDAR_CITA, c);
        StringBuilder sb = new StringBuilder();
        if (error != null) {
            sb.append(error).append("\n\n");
        }
        sb.append("¿A qué hora te conviene el ").append(formatearFecha(fecha)).append("?")
                .append("\n\n").append(horasDeSlots(slots))
                .append("\nResponde el número de la opción (1-").append(slots.size())
                .append(") o la hora con dos puntos (ej: 10:00).");
        return respuesta(sb.toString(), Intencion.AGENDAR_CITA);
    }

    /**
     * Un número suelto es el índice de la lista mostrada; si no, la hora escrita
     * tiene que coincidir con un cupo real. En cualquier otro caso se explica qué
     * se esperaba para que el paciente pueda reintentar.
     */
    private HoraResuelta resolverHora(String texto, List<LocalTime> opciones) {
        String t = texto == null ? "" : ParserAgendamiento.normalizar(texto).trim();
        java.util.Optional<LocalTime> escrita = ParserAgendamiento.horaDelTexto(t);
        if (escrita.isPresent()) {
            if (opciones.contains(escrita.get())) {
                return new HoraResuelta(escrita.get(), null);
            }
            return new HoraResuelta(null, "La hora " + escrita.get() + " no está disponible ese día.");
        }
        if (t.matches("\\d{1,2}")) {
            int hora = Integer.parseInt(t);
            if (hora <= 23 && opciones.contains(LocalTime.of(hora, 0))) {
                return new HoraResuelta(LocalTime.of(hora, 0), null);
            }
        }
        if (t.matches("\\d{1,3}")) {
            int indice = Integer.parseInt(t);
            if (indice >= 1 && indice <= opciones.size()) {
                return new HoraResuelta(opciones.get(indice - 1), null);
            }
            return new HoraResuelta(null, "La opción " + indice + " no existe: solo hay "
                    + opciones.size() + " cupos (1-" + opciones.size() + ").");
        }
        Optional<Integer> indice = ParserAgendamiento.indiceDeTexto(t, opciones.size());
        if (indice.isPresent()) {
            return new HoraResuelta(opciones.get(indice.get() - 1), null);
        }
        return new HoraResuelta(null, "No entendí \"" + recortar(t) + "\" como una hora. Escribe el número "
                + "de la opción (1-" + opciones.size() + ") o la hora (ej: 10, 10:00 o 10 am).");
    }

    private String preguntaServicio(List<Servicio> servicios, String error) {
        return (error == null ? "" : error + "\n\n")
                + "¿Qué servicio te gustaría?\n" + listarServicios(servicios)
                + "Envía el número o el nombre del servicio.";
    }

    private String preguntaOdontologo(List<Odontologo> odontologos, String error) {
        return (error == null ? "" : error + "\n\n")
                + "¿Con qué odontólogo prefieres tu cita?\n" + listarOdontologos(odontologos)
                + "Envía el número o el nombre del odontólogo.";
    }

    private String preguntaFecha(String error) {
        return (error == null ? "" : error + "\n\n")
                + "¿Para qué día te gustaría tu cita?\n"
                + "Puede ser 'mañana', 'viernes' o '20/09' (a partir de hoy).";
    }

    private String recortar(String t) {
        String texto = t == null ? "" : t.trim().replaceAll("\\s+", " ");
        if (texto.isEmpty()) {
            texto = "…";
        }
        return texto.length() > 40 ? texto.substring(0, 40) + "…" : texto;
    }

    /**
     * Tool de creación de cita: se ejecuta cuando el paciente confirma la propuesta.
     */
    private RespuestaAgente crearCita(Conversacion conversacion, Map<String, Object> ctx) {
        Paciente paciente = conversacion.getPaciente();
        if (paciente == null) {
            limpiarContexto(conversacion, Intencion.DESCONOCIDO);
            return respuesta("Ocurrió algo con tu registro. Escribe 'hablar con una persona' y te atenderemos.", Intencion.DESCONOCIDO);
        }
        Long doctorId = Long.valueOf(String.valueOf(ctx.get("doctorId")));
        Long servicioId = Long.valueOf(String.valueOf(ctx.get("servicioId")));
        LocalDate fecha = java.time.LocalDate.parse((String) ctx.get("fecha"));
        LocalTime hora = java.time.LocalTime.parse((String) ctx.get("hora"));
        try {
            CitaResponse cita = citaService.crear(new CitaRequest(
                    paciente.getId(), doctorId, servicioId, fecha, hora,
                    "Agendada por el agente de IA (WhatsApp)"));
            log.info("Agente: cita {} creada para el paciente {}", cita.id(), paciente.getId());
            limpiarContexto(conversacion, Intencion.AGENDAR_CITA);
            return respuesta("¡Listo! ✅ Tu cita quedó agendada:\n• Servicio: " + cita.servicioNombre()
                    + "\n• Odontólogo: " + cita.doctorNombre()
                    + "\n• Fecha: " + formatearFecha(cita.fecha())
                    + "\n• Hora: " + cita.horaInicio()
                    + "\nTe esperamos 😊 Si no puedes asistir, avísanos por este chat.", Intencion.AGENDAR_CITA);
        } catch (BusinessException ex) {
            log.warn("Agente: no se pudo crear la cita del paciente {}: {}", paciente.getId(), ex.getMessage());
            limpiarContexto(conversacion, Intencion.DESCONOCIDO);
            return respuesta("No pude agendar tu cita: " + ex.getMessage()
                    + " Puedes intentarlo nuevamente o escribir 'hablar con una persona'.", Intencion.DESCONOCIDO);
        }
    }

    // ------------------------------------------------------------------
    // Registro del paciente por cédula
    // ------------------------------------------------------------------

    private RespuestaAgente pedirRegistro(Conversacion conversacion) {
        guardar(conversacion, Intencion.REGISTRAR_PACIENTE, null);
        return respuesta(MENSAJE_SIN_PACIENTE, Intencion.REGISTRAR_PACIENTE);
    }

    private RespuestaAgente registrarPorCedula(Conversacion conversacion, String t) {
        String cedula = t.replaceAll("\\D", "");
        Paciente encontrado = pacienteRepository.findByCedulaIgnoreCase(cedula).orElse(null);
        if (encontrado != null) {
            conversacion.setPaciente(encontrado);
            guardar(conversacion, Intencion.AGENDAR_CITA, null);
            log.info("Agente: paciente {} registrado en la conversación {}", encontrado.getId(), conversacion.getId());
            return respuesta("¡Hola " + encontrado.getNombres() + "! Ya te encontré en nuestro sistema 😊 "
                    + "¿Quieres agendar una cita? Por ejemplo: 'una limpieza para mañana'.", Intencion.AGENDAR_CITA);
        }
        guardar(conversacion, Intencion.REGISTRAR_PACIENTE,
                Map.of("paso", "REG_CONFIRMAR", "cedula", cedula));
        log.info("Agente: cédula {} no existe, ofreciendo registro en la conversación {}", cedula, conversacion.getId());
        return respuesta("No te encuentro registrado con la cédula " + cedula + ".\n¿Quieres que cree tu ficha de paciente? Responde 'sí'.",
                Intencion.REGISTRAR_PACIENTE);
    }

    // ------------------------------------------------------------------
    // Registro de pacientes desde el chat (incluye tutores si es menor)
    // ------------------------------------------------------------------

    private RespuestaAgente gestionarRegistro(Conversacion conversacion, Map<String, Object> ctx, String t, String textoEntrada) {
        if (contiene(t, PALABRAS_TRANSFERIR)) {
            return transferirHumano(conversacion);
        }
        String paso = String.valueOf(ctx.getOrDefault("paso", ""));

        switch (paso) {
            case "REG_CONFIRMAR" -> {
                if (esConfirmacion(t)) {
                    guardar(conversacion, Intencion.REGISTRAR_PACIENTE, new LinkedHashMap<>(Map.of(
                            "paso", "REG_NOMBRES", "cedula", String.valueOf(ctx.get("cedula")))));
                    return respuesta(MENSAJE_PREGUNTA_NOMBRE, Intencion.REGISTRAR_PACIENTE);
                }
                if (esRechazo(t)) {
                    limpiarContexto(conversacion, Intencion.DESCONOCIDO);
                    return respuesta("Está bien, no registré nada. Si cambias de opinión, escríbeme 'registrarme' 😊", Intencion.DESCONOCIDO);
                }
                return respuesta("¿Confirmas tu registro? Responde 'sí' para crear tu ficha o 'no' para cancelar.", Intencion.REGISTRAR_PACIENTE);
            }
            case "REG_NOMBRES" -> {
                if (t.length() < 2) {
                    return respuesta(MENSAJE_PREGUNTA_NOMBRE, Intencion.REGISTRAR_PACIENTE);
                }
                Map<String, Object> nuevo = new LinkedHashMap<>(ctx);
                nuevo.put("nombres", textoEntrada.trim());
                nuevo.put("paso", "REG_APELLIDOS");
                guardar(conversacion, Intencion.REGISTRAR_PACIENTE, nuevo);
                return respuesta(MENSAJE_PREGUNTA_APELLIDOS, Intencion.REGISTRAR_PACIENTE);
            }
            case "REG_APELLIDOS" -> {
                if (t.length() < 2) {
                    return respuesta(MENSAJE_PREGUNTA_APELLIDOS, Intencion.REGISTRAR_PACIENTE);
                }
                Map<String, Object> nuevo = new LinkedHashMap<>(ctx);
                nuevo.put("apellidos", textoEntrada.trim());
                nuevo.put("paso", "REG_FECHA");
                guardar(conversacion, Intencion.REGISTRAR_PACIENTE, nuevo);
                return respuesta(MENSAJE_PREGUNTA_FECHA, Intencion.REGISTRAR_PACIENTE);
            }
            case "REG_FECHA" -> {
                LocalDate fecha = parsearFechaLibre(t);
                if (fecha == null || fecha.isAfter(hoy())) {
                    return respuesta("No entendí esa fecha 😅 " + MENSAJE_PREGUNTA_FECHA, Intencion.REGISTRAR_PACIENTE);
                }
                Map<String, Object> nuevo = new LinkedHashMap<>(ctx);
                nuevo.put("fechaNacimiento", fecha.toString());
                if (esMenor(fecha)) {
                    nuevo.put("paso", "REG_TUTOR");
                    guardar(conversacion, Intencion.REGISTRAR_PACIENTE, nuevo);
                    return respuesta(MENSAJE_PREGUNTA_TUTOR, Intencion.REGISTRAR_PACIENTE);
                }
                nuevo.put("paso", "REG_CONFIRMAR_DATOS");
                guardar(conversacion, Intencion.REGISTRAR_PACIENTE, nuevo);
                return respuesta(resumenRegistro(nuevo, null) + "\n¿Confirmas tus datos? Responde 'sí'.", Intencion.REGISTRAR_PACIENTE);
            }
            case "REG_TUTOR" -> {
                String parentesco = parsearParentesco(t);
                if (parentesco == null) {
                    return respuesta(MENSAJE_PREGUNTA_TUTOR, Intencion.REGISTRAR_PACIENTE);
                }
                Map<String, Object> nuevo = new LinkedHashMap<>(ctx);
                nuevo.put("tutorParentesco", parentesco);
                nuevo.put("paso", "REG_TUTOR_NOMBRE");
                guardar(conversacion, Intencion.REGISTRAR_PACIENTE, nuevo);
                return respuesta("Perfecto. ¿Cuál es el nombre del " + parentesco.toLowerCase() + "?", Intencion.REGISTRAR_PACIENTE);
            }
            case "REG_TUTOR_NOMBRE" -> {
                if (t.length() < 2) {
                    return respuesta("¿Cuál es el nombre del tutor?", Intencion.REGISTRAR_PACIENTE);
                }
                Map<String, Object> nuevo = new LinkedHashMap<>(ctx);
                nuevo.put("tutorNombres", textoEntrada.trim());
                nuevo.put("paso", "REG_TUTOR_APELLIDO");
                guardar(conversacion, Intencion.REGISTRAR_PACIENTE, nuevo);
                return respuesta("¿Y los apellidos del tutor?", Intencion.REGISTRAR_PACIENTE);
            }
            case "REG_TUTOR_APELLIDO" -> {
                Map<String, Object> nuevo = new LinkedHashMap<>(ctx);
                nuevo.put("tutorApellidos", textoEntrada.trim());
                nuevo.put("paso", "REG_CONFIRMAR_DATOS");
                guardar(conversacion, Intencion.REGISTRAR_PACIENTE, nuevo);
                return respuesta(resumenRegistro(nuevo, null) + "\n¿Confirmas estos datos? Responde 'sí'.", Intencion.REGISTRAR_PACIENTE);
            }
            case "REG_CONFIRMAR_DATOS" -> {
                if (esConfirmacion(t)) {
                    return crearPacientesDesdeChat(conversacion, ctx);
                }
                if (esRechazo(t)) {
                    limpiarContexto(conversacion, Intencion.DESCONOCIDO);
                    return respuesta("Está bien, no registré tus datos. Escríbeme 'registrarme' cuando quieras intentarlo 😊", Intencion.DESCONOCIDO);
                }
                return respuesta(resumenRegistro(ctx, null) + "\n¿Confirmas estos datos? Responde 'sí' para guardarlos.", Intencion.REGISTRAR_PACIENTE);
            }
            default -> {
                if (esCedula(t)) {
                    return registrarPorCedula(conversacion, t);
                }
                if (contiene(t, PALABRAS_REGISTRO) || esRechazo(t)) {
                    guardar(conversacion, Intencion.REGISTRAR_PACIENTE, Map.of("paso", "REG_NOMBRES"));
                    return respuesta(MENSAJE_PREGUNTA_NOMBRE, Intencion.REGISTRAR_PACIENTE);
                }
                return respuesta(MENSAJE_SIN_PACIENTE, Intencion.REGISTRAR_PACIENTE);
            }
        }
    }

    private RespuestaAgente crearPacientesDesdeChat(Conversacion conversacion, Map<String, Object> ctx) {
        String cedula = ctx.get("cedula") == null || "null".equals(ctx.get("cedula").toString())
                ? null
                : String.valueOf(ctx.get("cedula"));
        String nombres = String.valueOf(ctx.get("nombres"));
        String apellidos = String.valueOf(ctx.get("apellidos"));
        LocalDate fechaNacimiento = LocalDate.parse(String.valueOf(ctx.get("fechaNacimiento")));
        boolean menor = esMenor(fechaNacimiento);

        List<TutorRequest> tutores = new ArrayList<>();
        if (menor) {
            tutores.add(new TutorRequest(
                    String.valueOf(ctx.get("tutorParentesco")),
                    String.valueOf(ctx.get("tutorNombres")),
                    ctx.get("tutorApellidos") == null ? null : String.valueOf(ctx.get("tutorApellidos")),
                    null));
        }

        try {
            PacienteResponse creado = pacienteService.crear(new PacienteRequest(
                    cedula, nombres, apellidos,
                    conversacion.getTelefono(), null, fechaNacimiento, null, null,
                    "Registrado por WhatsApp", "ACTIVO", tutores));
            Paciente nuevo = pacienteRepository.findById(creado.id())
                    .orElseThrow(() -> new IllegalStateException("Paciente creado pero no recuperable"));
            conversacion.setPaciente(nuevo);
            guardar(conversacion, Intencion.AGENDAR_CITA, null);
            log.info("Agente: paciente {} creado desde el chat (menor={}) en conversación {}",
                    nuevo.getId(), menor, conversacion.getId());
            String saludo = "¡Listo, " + nombres + "! ✅ Te registré como paciente."
                    + (menor
                    ? "\nComo eres menor de edad, también quedó registrado el tutor (" + ctx.get("tutorParentesco") + " " + ctx.get("tutorNombres") + ")."
                    : "")
                    + "\n¿Quieres agendar una cita? Por ejemplo: 'una limpieza para mañana'.";
            return respuesta(saludo, Intencion.AGENDAR_CITA);
        } catch (Exception ex) {
            log.warn("Agente: no se pudo registrar al paciente {} {}: {}", nombres, apellidos, ex.getMessage());
            return respuesta("No pude registrarte: " + (ex.getMessage() != null ? ex.getMessage() : "error interno")
                    + "\nPuedes intentarlo de nuevo o escribe 'hablar con una persona'.", Intencion.DESCONOCIDO);
        }
    }

    private String resumenRegistro(Map<String, Object> ctx, String ignorar) {
        StringBuilder sb = new StringBuilder("Revisa tus datos:\n");
        sb.append("• Nombre: ").append(ctx.get("nombres")).append(" ").append(ctx.get("apellidos")).append("\n");
        if (ctx.get("cedula") != null && !"null".equalsIgnoreCase(String.valueOf(ctx.get("cedula")))) {
            sb.append("• Cédula: ").append(ctx.get("cedula")).append("\n");
        }
        sb.append("• Fecha de nacimiento: ").append(formatoFechaLatina(fechaDeContexto(ctx))).append("\n");
        if (ctx.get("tutorNombres") != null) {
            sb.append("• Tutor: ").append(ctx.get("tutorParentesco")).append(" ").append(ctx.get("tutorNombres"))
                    .append(" ").append(ctx.get("tutorApellidos") == null ? "" : ctx.get("tutorApellidos")).append("\n");
        }
        return sb.toString().strip();
    }

    private LocalDate fechaDeContexto(Map<String, Object> ctx) {
        Object valor = ctx.get("fechaNacimiento");
        if (valor == null) {
            return null;
        }
        try {
            return LocalDate.parse(String.valueOf(valor));
        } catch (Exception ex) {
            return null;
        }
    }

    private String formatoFechaLatina(LocalDate f) {
        if (f == null) {
            return "-";
        }
        return String.format("%02d/%02d/%04d", f.getDayOfMonth(), f.getMonthValue(), f.getYear());
    }

    private LocalDate parsearFechaLibre(String t) {
        if (t == null || t.isBlank()) {
            return null;
        }
        String[] formas = {
                "(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})",
                "(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2})",
                "(\\d{4})[/-](\\d{1,2})[/-](\\d{1,2})"
        };
        for (String forma : formas) {
            java.util.regex.Matcher m = Pattern.compile(forma).matcher(t.trim());
            if (m.find()) {
                try {
                    int año = Integer.parseInt(m.group(3));
                    int mes = Integer.parseInt(m.group(2));
                    int dia = Integer.parseInt(m.group(1));
                    if (año < 100) {
                        año += año >= 50 ? 1900 : 2000;
                    }
                    return LocalDate.of(año, mes, dia);
                } catch (Exception ex) {
                    return null;
                }
            }
        }
        return null;
    }

    private String parsearParentesco(String t) {
        String n = ParserAgendamiento.normalizar(t);
        if (n.contains("padre") || n.contains("papa") || n.contains("papi") || n.contains("papá")) {
            return "PADRE";
        }
        if (n.contains("madre") || n.contains("mama") || n.contains("mami") || n.contains("mamá")) {
            return "MADRE";
        }
        return null;
    }

    private boolean esMenor(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null) {
            return false;
        }
        return Period.between(fechaNacimiento, hoy()).getYears() < 18;
    }

    // ------------------------------------------------------------------
    // Transferencia a humano
    // ------------------------------------------------------------------

    private RespuestaAgente transferirHumano(Conversacion conversacion) {
        conversacion.setEstado(EstadoConversacion.ATENCION_HUMANA);
        guardar(conversacion, Intencion.TRANSFERIR_HUMANO, null);
        log.info("Agente: conversación {} transferida a atención humana", conversacion.getId());
        return respuesta("Claro 👍 Te voy a comunicar con una persona de nuestro equipo. ¡Un momento por favor!",
                true, Intencion.TRANSFERIR_HUMANO);
    }

    // ------------------------------------------------------------------
    // Operaciones de supervisión (exposición REST)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AgenteConversacionResponse> listarConversacionesAgente(String q) {
        return conversacionRepository.buscar(q == null ? "" : q.trim()).stream()
                .map(c -> new AgenteConversacionResponse(c.getId(),
                        c.getTelefono(), c.getNombreContacto(), c.getEstado().name(),
                        c.getPaciente() == null ? null : c.getPaciente().getId(),
                        c.getPaciente() == null ? null : c.getPaciente().getNombres() + " " + c.getPaciente().getApellidos(),
                        c.getIntencion() == null ? null : c.getIntencion().name(),
                        c.getAgenteActivo(), c.getContextoAgente(),
                        ultimoMensaje(c.getId()), c.getUltimoMensajeAt()))
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
        guardar(conversacion, Intencion.TRANSFERIR_HUMANO, null);
        log.info("Agente: conversación {} transferida a humano (panel)", conversacion.getId());
        return new AgenteConversacionResponse(conversacion.getId(),
                conversacion.getTelefono(), conversacion.getNombreContacto(), conversacion.getEstado().name(),
                conversacion.getPaciente() == null ? null : conversacion.getPaciente().getId(),
                conversacion.getPaciente() == null ? null : conversacion.getPaciente().getNombres() + " " + conversacion.getPaciente().getApellidos(),
                conversacion.getIntencion() == null ? null : conversacion.getIntencion().name(),
                conversacion.getAgenteActivo(), conversacion.getContextoAgente(),
                ultimoMensaje(conversacion.getId()), conversacion.getUltimoMensajeAt());
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
        return new AgenteConversacionResponse(conversacion.getId(),
                conversacion.getTelefono(), conversacion.getNombreContacto(), conversacion.getEstado().name(),
                conversacion.getPaciente() == null ? null : conversacion.getPaciente().getId(),
                conversacion.getPaciente() == null ? null : conversacion.getPaciente().getNombres() + " " + conversacion.getPaciente().getApellidos(),
                conversacion.getIntencion() == null ? null : conversacion.getIntencion().name(),
                conversacion.getAgenteActivo(), conversacion.getContextoAgente(),
                ultimoMensaje(conversacion.getId()), conversacion.getUltimoMensajeAt());
    }

    private String ultimoMensaje(Long conversacionId) {
        return mensajeRepository.ultimos(conversacionId, PageRequest.of(0, 1)).stream()
                .map(Mensaje::getTexto).findFirst().orElse(null);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Teléfono del paciente en formato legible (+593 99 111 2233). */
    private static String telefonoLegible(String telefono) {
        String legible = TelefonoUtil.mostrar(telefono);
        return legible == null || legible.isBlank() ? "—" : legible;
    }

    private RespuestaAgente respuesta(String mensaje, Intencion intencion) {
        return new RespuestaAgente(mensaje, false, null);
    }

    private boolean inactiva(Instant ultimoPrev) {
        if (ultimoPrev == null) {
            return false;
        }
        return Duration.between(ultimoPrev, Instant.now()).toMillis() >= INACTIVIDAD_REINICIO_MS;
    }

    private RespuestaAgente reiniciar(Conversacion conversacion) {
        conversacion.setIntencion(null);
        conversacion.setContextoAgente(null);
        conversacionRepository.save(conversacion);
        log.info("Agente: conversación {} reiniciada (inactividad o solicitud del paciente)", conversacion.getId());
        return respuesta(MENSAJE_REINICIO, Intencion.SALUDO);
    }

    private RespuestaAgente respuesta(String mensaje, boolean transferir, Intencion intencion) {
        return new RespuestaAgente(mensaje, transferir, null);
    }

    private RespuestaAgente respuestaConAdjunto(String mensaje, Intencion intencion, Adjunto adjunto) {
        return new RespuestaAgente(mensaje, false, adjunto);
    }

    private void guardar(Conversacion conversacion, Intencion intencion, Map<String, Object> contexto) {
        conversacion.setIntencion(intencion);
        conversacion.setContextoAgente(escribirJson(contexto));
        conversacionRepository.save(conversacion);
    }

    private void limpiarContexto(Conversacion conversacion, Intencion intencion) {
        conversacion.setIntencion(intencion);
        conversacion.setContextoAgente(null);
        conversacionRepository.save(conversacion);
    }

    private Map<String, Object> contexto(Conversacion conversacion) {
        String json = conversacion.getContextoAgente();
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> mapa = objectMapper.readValue(json, new TypeReference<>() {
            });
            return mapa == null ? new LinkedHashMap<>() : new LinkedHashMap<>(mapa);
        } catch (Exception ex) {
            log.warn("Agente: contexto inválido en conversación {}: {}", conversacion.getId(), ex.getMessage());
            return new LinkedHashMap<>();
        }
    }

    private String escribirJson(Map<String, Object> contexto) {
        if (contexto == null || contexto.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(contexto);
        } catch (Exception ex) {
            log.warn("Agente: no se pudo serializar el contexto: {}", ex.getMessage());
            return null;
        }
    }

    private Cita proximaCita(Paciente paciente) {
        return citaRepository.proximasDelPaciente(paciente.getId(), hoy()).stream().findFirst().orElse(null);
    }

    private List<Servicio> serviciosActivos() {
        return servicioRepository.findByEstadoOrderByNombreAsc("ACTIVO");
    }

    private List<Odontologo> odontologosActivos() {
        return odontologoRepository.findTop5ByEstadoOrderByNombresAsc("ACTIVO");
    }

    private String listarServicios(List<Servicio> servicios) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < servicios.size(); i++) {
            Servicio s = servicios.get(i);
            sb.append(i + 1).append(". ").append(s.getNombre()).append("\n");
        }
        return sb.toString();
    }

    private String listarOdontologos(List<Odontologo> odontologos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < odontologos.size(); i++) {
            Odontologo o = odontologos.get(i);
            sb.append(i + 1).append(". Dr. ").append(o.getNombres()).append(" ").append(o.getApellidos());
            if (o.getEspecialidad() != null && !o.getEspecialidad().isBlank()) {
                sb.append(" (").append(o.getEspecialidad()).append(")");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String nombreServicio(Long servicioId) {
        return servicioRepository.findById(servicioId)
                .map(Servicio::getNombre).orElse("?");
    }

    private String nombreDoctor(Long doctorId) {
        return odontologoRepository.findById(doctorId)
                .map(o -> "Dr. " + o.getNombres() + " " + o.getApellidos())
                .orElse("?");
    }

    private String horasDeSlots(List<SlotResponse> slots) {
        return formatearOpcionesEnDosColumnas(slots.stream()
                .map(slot -> slot.horaInicio().toString())
                .toList());
    }

    private String formatearOpcionesEnDosColumnas(List<String> opciones) {
        StringBuilder sb = new StringBuilder();
        int anchoColumna = 18;
        for (int i = 0; i < opciones.size(); i += 2) {
            if (i > 0) {
                sb.append("\n");
            }
            String izquierda = (i + 1) + ". " + opciones.get(i);
            sb.append(String.format("%-" + anchoColumna + "s", izquierda));
            if (i + 1 < opciones.size()) {
                sb.append((i + 2) + ". ").append(opciones.get(i + 1));
            }
        }
        return sb.toString();
    }

    private String formatearFecha(LocalDate fecha) {
        DayOfWeek dia = fecha.getDayOfWeek();
        String nombreDia = dia.getDisplayName(TextStyle.FULL, new Locale("es", "EC"));
        return nombreDia + " " + fecha.getDayOfMonth() + "/" + fecha.getMonthValue();
    }

    private boolean esCedula(String t) {
        String digitos = t.replaceAll("\\D", "");
        return digitos.length() >= 7 && digitos.length() <= 10;
    }

    private boolean esConfirmacion(String t) {
        return P_PALABRA_AFIRM.matcher(" " + t + " ").find();
    }

    private boolean esRechazo(String t) {
        return P_PALABRA_NEG.matcher(" " + t + " ").find();
    }

    private boolean contiene(String t, Set<String> palabras) {
        for (String p : palabras) {
            if (ParserAgendamiento.contienePalabra(t, p)) {
                return true;
            }
        }
        return false;
    }

    private Long numero(Map<String, Object> ctx, String clave, Long entrada) {
        if (entrada != null) {
            return entrada;
        }
        Object valor = ctx.get(clave);
        return valor == null ? null : Long.valueOf(String.valueOf(valor));
    }

    private LocalDate hoy() {
        return LocalDate.now(zona);
    }

    // ------------------------------------------------------------------
    // Interacción por teléfono/palabra (sin sesión WhatsApp)
    // ------------------------------------------------------------------

    @Transactional
    public RespuestaAgente procesarPorTelefono(String telefono, String texto) {
        String t = ParserAgendamiento.normalizar(texto);
        if (t.isBlank()) {
            return respuesta("Escribe algo para comenzar. Ej: \"ver citas\" o tu número de cédula.", Intencion.SALUDO);
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
        conversacionRepository.save(conversacion);
        if (!Boolean.TRUE.equals(conversacion.getAgenteActivo())
                || conversacion.getEstado() == EstadoConversacion.ATENCION_HUMANA) {
            return respuesta("La conversación no está activa. Habla con un administrador.", Intencion.DESCONOCIDO);
        }
        if (contiene(t, PALABRAS_REINICIAR)) {
            return reiniciar(conversacion);
        }
        Map<String, Object> ctx = contexto(conversacion);
        if (conversacion.getIntencion() == Intencion.REGISTRAR_PACIENTE) {
            return gestionarRegistro(conversacion, ctx, t, texto);
        }
        if ("CREAR".equals(ctx.get("paso"))) {
            if (esConfirmacion(t)) {
                return crearCita(conversacion, ctx);
            }
            if (esRechazo(t)) {
                limpiarContexto(conversacion, Intencion.DESCONOCIDO);
                return respuesta("No reservé ninguna cita. ¿Puedo ayudarte con algo más?\n" + MENSAJE_MENU, Intencion.DESCONOCIDO);
            }
            return respuesta("¿Confirmas tu cita? Responde 'sí' para agendarla o 'no' para cancelarla.", Intencion.AGENDAR_CITA);
        }
        if (conversacion.getIntencion() == Intencion.AGENDAR_CITA && ctx.containsKey("paso")) {
            return gestionarAgendamiento(conversacion, ctx, t);
        }
        Intencion intencion = clasificar(t);
        if (intencion == Intencion.VER_PERFIL && PATTERN_CEDULA.matcher(t).matches()) {
            return buscarPorCedula(conversacion, t);
        }
        return switch (intencion) {
            case TRANSFERIR_HUMANO -> transferirHumano(conversacion);
            case VER_PERFIL -> verPerfil(conversacion);
            case VER_CITAS -> verCitas(conversacion);
            case SALUDO -> saludar(conversacion);
            case LISTAR_SERVICIOS -> listarServicios(conversacion);
            case PREGUNTAR_DISPONIBILIDAD -> preguntarDisponibilidad(conversacion, ctx, t);
            case CONFIRMAR_CITA -> confirmarCita(conversacion);
            case CANCELAR_CITA -> cancelarCita(conversacion);
            case AGENDAR_CITA -> gestionarAgendamiento(conversacion, ctx, t);
            case REGISTRAR_PACIENTE -> gestionarRegistro(conversacion, ctx, t, texto);
            default -> {
                guardar(conversacion, Intencion.DESCONOCIDO, null);
                yield respuesta("No logré entenderte 😅\n" + MENSAJE_MENU, Intencion.DESCONOCIDO);
            }
        };
    }
}