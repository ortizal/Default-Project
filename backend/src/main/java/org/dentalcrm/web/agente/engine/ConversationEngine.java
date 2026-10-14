package org.dentalcrm.web.agente.engine;

import org.dentalcrm.domain.agente.Intencion;
import org.dentalcrm.domain.whatsapp.Conversacion;
import org.dentalcrm.domain.whatsapp.EstadoConversacion;
import org.dentalcrm.domain.whatsapp.MensajeRepository;
import org.dentalcrm.web.agente.AgenteConversacionalService;
import org.dentalcrm.web.agente.AgenteSoporte;
import org.dentalcrm.web.agente.ParserAgendamiento;
import org.dentalcrm.web.agente.contexto.ConversacionContexto;
import org.dentalcrm.web.agente.contexto.ContextManager;
import org.dentalcrm.web.agente.contexto.MenuActual;
import org.dentalcrm.web.agente.contexto.OpcionMenu;
import org.dentalcrm.web.agente.flujo.AgenteEntorno;
import org.dentalcrm.web.agente.flujo.HumanTransferFlow;
import org.dentalcrm.web.google.GoogleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Pipeline del agente (§2 y §17): primero entiende dónde está el usuario en la
 * conversación (contexto, flujo y paso), después interpreta lo que escribió.
 *
 * <ol>
 *   <li>puerta de agente/estado</li>
 *   <li>normalizar y comandos globales (§13)</li>
 *   <li>flujo activo → {@link FlowEngine}</li>
 *   <li>cédula / menú / intención / fallback contextual (§4, §6, §14)</li>
 * </ol>
 */
public class ConversationEngine {

    private static final Logger log = LoggerFactory.getLogger(ConversationEngine.class);
    private static final long INACTIVIDAD_REINICIO_MS = 2 * 60 * 1000L;
    private static final Pattern P_CEDULA = Pattern.compile("^\\d{6,13}$");

    private static final Set<String> PALABRAS_MENU = Set.of(
            "menu", "inicio", "ayuda", "salir", "opciones");
    private static final Set<String> PALABRAS_TRANSFERIR = Set.of(
            "humano", "persona", "operador", "operadora", "representante", "transferir",
            "transferencia", "que me atienda", "atenderme", "atencion humana",
            "asistente real", "recepcion", "asesor", "hablar con recepcion");
    private static final Set<String> PALABRAS_CANCELAR = Set.of(
            "cancelar", "cancela", "cancelo", "cancelar cita", "cancelar mi cita",
            "anular", "anular cita", "no asistire");

    private final AgenteEntorno e;
    private final FlowEngine motor;
    private final IntentClassifier clasificador;
    private final GoogleService googleService;
    private final MensajeRepository mensajeRepository;

    public ConversationEngine(AgenteEntorno e, FlowEngine motor, IntentClassifier clasificador,
                              GoogleService googleService, MensajeRepository mensajeRepository) {
        this.e = e;
        this.motor = motor;
        this.clasificador = clasificador;
        this.googleService = googleService;
        this.mensajeRepository = mensajeRepository;
    }

    public AgenteConversacionalService.RespuestaAgente procesar(Conversacion conversacion,
                                                                String textoEntrada,
                                                                Instant ultimoPrev) {
        if (!Boolean.TRUE.equals(conversacion.getAgenteActivo())
                || conversacion.getEstado() == EstadoConversacion.ATENCION_HUMANA
                || conversacion.getEstado() == EstadoConversacion.CERRADA) {
            return AgenteConversacionalService.RespuestaAgente.silenciosa();
        }
        String t = ParserAgendamiento.normalizar(textoEntrada);
        if (t.isBlank()) {
            return AgenteConversacionalService.RespuestaAgente.silenciosa();
        }
        ConversacionContexto ctx = e.contexto.cargar(conversacion);
        inferirLegado(ctx);

        if (clasificador.esReiniciar(t)) {
            return reiniciar(conversacion);
        }
        if (inactiva(ultimoPrev)) {
            return reiniciarOReanudar(conversacion, ctx);
        }

        AgenteConversacionalService.RespuestaAgente global = global(conversacion, ctx, t);
        if (global != null) {
            return global;
        }

        if (ctx.isReanudando()) {
            AgenteConversacionalService.RespuestaAgente trasPausa = continuarTrasPausa(conversacion, ctx, t);
            if (trasPausa != null) {
                return trasPausa;
            }
        }

        // §5 del plan: un cambio explícito de intención manda sobre el paso.
        Intencion cambio = cambioDeIntencion(ctx, t);
        if (cambio != null) {
            return iniciar(conversacion, ctx, cambio, t, textoEntrada);
        }

        // §22: mientras hay flujo activo se responde dentro de él.
        if (ctx.tieneFlujoActivo()) {
            PasoResultado resultado = motor.procesar(conversacion, ctx, t, textoEntrada);
            if (resultado != null && !resultado.esIgnorado()) {
                return responder(conversacion, ctx, resultado, null);
            }
        }

        if (P_CEDULA.matcher(t).matches()) {
            return buscarPorCedula(conversacion, t);
        }

        MenuResolver.ResultadoMenu menu = e.menus.resolver(ctx, t);
        OpcionMenu opcion = menu != null && menu.encontrado()
                ? menu.opcion()
                : opcionPrincipal(t);
        if (opcion != null) {
            return iniciar(conversacion, ctx, ContextManager.intencionDe(opcion.valor()),
                    t, textoEntrada);
        }

        Intencion intencion = clasificador.clasificar(t, e.servicios.activos(), e.odontologos.activos());
        return iniciar(conversacion, ctx, intencion, t, textoEntrada);
    }

    // ------------------------------------------------------------------
    // Comandos globales (§13)
    // ------------------------------------------------------------------

    private AgenteConversacionalService.RespuestaAgente global(Conversacion conv,
                                                               ConversacionContexto ctx, String t) {
        if (e.soporte.contiene(t, PALABRAS_TRANSFERIR)) {
            return iniciar(conv, ctx, Intencion.TRANSFERIR_HUMANO, t, t);
        }
        if (e.soporte.contiene(t, PALABRAS_MENU)) {
            return menu(conv, ctx, Intencion.SALUDO);
        }
        if (e.soporte.contiene(t, PALABRAS_CANCELAR)
                && !GESTIONAR_CITA.equals(ctx.getFlujo())) {
            if (ctx.tieneFlujoActivo()) {
                // §13: "cancelar" siempre sale del proceso en curso, conservando el paciente.
                ctx.reiniciar();
                String mensaje = "Cancelé el proceso en curso 😌 ¿Puedo ayudarte con algo más?\n"
                        + e.soporte.menuTexto();
                ctx.setUltimaPregunta(mensaje);
                e.contexto.guardar(conv, ctx, Intencion.DESCONOCIDO);
                return new AgenteConversacionalService.RespuestaAgente(mensaje, false, null);
            }
            return iniciar(conv, ctx, Intencion.CANCELAR_CITA, t, t);
        }
        return null;
    }

    private static final String GESTIONAR_CITA = "GESTIONAR_CITA";

    private AgenteConversacionalService.RespuestaAgente menu(Conversacion conv,
                                                             ConversacionContexto ctx,
                                                             Intencion intencion) {
        ctx.setFlujo(null);
        ctx.setPaso(null);
        ctx.setExpectedInput(null);
        ctx.setIntentosFallidos(0);
        ctx.setMenuActual(MenuActual.PRINCIPAL);
        ctx.setOpciones(e.soporte.opcionesPrincipales());
        String mensaje = e.soporte.mensajeSaludo();
        ctx.setUltimaPregunta(mensaje);
        e.contexto.guardar(conv, ctx, intencion);
        return new AgenteConversacionalService.RespuestaAgente(mensaje, false, null);
    }

    // ------------------------------------------------------------------
    // Arranque de flujos e intenciones
    // ------------------------------------------------------------------

    private AgenteConversacionalService.RespuestaAgente iniciar(Conversacion conv,
                                                                ConversacionContexto ctx,
                                                                Intencion intencion,
                                                                String t, String crudo) {
        if (intencion == null) {
            return fallback(conv, ctx);
        }
        return switch (intencion) {
            case SALUDO -> menu(conv, ctx, Intencion.SALUDO);
            case CONECTAR_GOOGLE -> conectarGoogle(conv, ctx);
            case AGENDAR_CITA -> protegido(conv, ctx, "AGENDAR_CITA", Intencion.AGENDAR_CITA, t, crudo, true);
            case LISTAR_SERVICIOS -> flujo(conv, ctx, "VER_SERVICIOS", Intencion.LISTAR_SERVICIOS, t, crudo, false);
            case VER_CITAS -> protegido(conv, ctx, "VER_CITAS", Intencion.VER_CITAS, t, crudo, false);
            case PREGUNTAR_DISPONIBILIDAD ->
                    flujo(conv, ctx, "CONSULTAR_DISPONIBILIDAD", Intencion.PREGUNTAR_DISPONIBILIDAD, t, crudo, true);
            case GESTIONAR_CITA -> protegido(conv, ctx, GESTIONAR_CITA, Intencion.GESTIONAR_CITA, t, crudo, false);
            case CONFIRMAR_CITA -> protegido(conv, ctx, GESTIONAR_CITA, Intencion.CONFIRMAR_CITA, t, crudo, false);
            case CANCELAR_CITA -> protegido(conv, ctx, GESTIONAR_CITA, Intencion.CANCELAR_CITA, t, crudo, false);
            case VER_PERFIL -> protegido(conv, ctx, "VER_PERFIL", Intencion.VER_PERFIL, t, crudo, false);
            case REGISTRAR_PACIENTE -> flujo(conv, ctx, "REGISTRAR_PACIENTE", Intencion.REGISTRAR_PACIENTE, t, crudo, false);
            case TRANSFERIR_HUMANO -> flujo(conv, ctx, HumanTransferFlow.CODIGO, Intencion.TRANSFERIR_HUMANO, t, crudo, false);
            default -> fallback(conv, ctx);
        };
    }

    // ------------------------------------------------------------------
    // Guard de identidad (§16) y cambio de intención (§5)
    // ------------------------------------------------------------------

    /** Flujos privados: exigen identidad verificada antes de tocar datos (§16). */
    private static final java.util.Set<String> FLUJOS_PROTEGIDOS = java.util.Set.of(
            "AGENDAR_CITA", "VER_CITAS", "GESTIONAR_CITA", "VER_PERFIL");

    private AgenteConversacionalService.RespuestaAgente protegido(Conversacion conv,
                                                                  ConversacionContexto ctx,
                                                                  String codigo, Intencion intencion,
                                                                  String t, String crudo,
                                                                  boolean reiniciarReserva) {
        if (identidadValida(conv, ctx)) {
            return flujo(conv, ctx, codigo, intencion, t, crudo, reiniciarReserva);
        }
        return identificar(conv, ctx, codigo, intencion);
    }

    /** Suspende el flujo privado y pide la cédula (§2, §20). */
    private AgenteConversacionalService.RespuestaAgente identificar(Conversacion conv,
                                                                   ConversacionContexto ctx,
                                                                   String codigoOrigen,
                                                                   Intencion intencion) {
        if (codigoOrigen != null && ctx.getFlujo() != null && !ctx.getFlujo().equals(codigoOrigen)
                && FLUJOS_PROTEGIDOS.contains(ctx.getFlujo())) {
            codigoOrigen = ctx.getFlujo();
        }
        PasoResultado solicitud = org.dentalcrm.web.agente.flujo.PatientIdentificationFlow
                .pedirCedulaEn(ctx, codigoOrigen, null);
        e.contexto.guardar(conv, ctx, intencion);
        return new AgenteConversacionalService.RespuestaAgente(solicitud.mensaje(), false, null);
    }

    /**
     * Identidad vigente: la sesión verificada por cédula (§9) o la ficha que
     * esta misma conversación ya verificó/creó.
     */
    private boolean identidadValida(Conversacion conv, ConversacionContexto ctx) {
        return conv.getPaciente() != null || e.contexto.identidadValida(ctx);
    }

    /** Cambio explícito de intención (§5): manda sobre el dato esperado del paso. */
    private Intencion cambioDeIntencion(ConversacionContexto ctx, String t) {
        if (!ctx.tieneFlujoActivo()) {
            return null;
        }
        String texto = AgenteSoporte.compactar(t);
        if (texto.isEmpty()) {
            return null;
        }
        Intencion objetivo = null;
        if (texto.contains("disponibilidad")) {
            objetivo = Intencion.PREGUNTAR_DISPONIBILIDAD;
        } else if (texto.contains("mis citas") || texto.contains("ver mis citas")) {
            objetivo = Intencion.VER_CITAS;
        } else if (texto.contains("agendar") || texto.contains("nueva cita")) {
            objetivo = Intencion.AGENDAR_CITA;
        } else if (texto.contains("cancelar una cita") || texto.contains("confirmar una cita")
                || texto.contains("gestionar")) {
            objetivo = Intencion.GESTIONAR_CITA;
        } else if (texto.contains("mi informacion") || texto.contains("mis datos")
                || texto.contains("mi perfil")) {
            objetivo = Intencion.VER_PERFIL;
        }
        if (objetivo == null || objetivo == ContextManager.intencionDe(ctx.getFlujo())) {
            return null;
        }
        return objetivo;
    }

    /**
     * Arranca un flujo con estado limpio y lo despacha con la entrada original
     * (§10: sólo faltan los datos que el usuario no escribió).
     *
     * @param reiniciarReserva limpia también fecha/servicio/odontólogo de una
     *                          reserva anterior (los reenvíos §7 no pasan por aquí)
     */
    private AgenteConversacionalService.RespuestaAgente flujo(Conversacion conv, ConversacionContexto ctx,
                                                              String codigo, Intencion intencion,
                                                              String t, String crudo,
                                                              boolean reiniciarReserva) {
        // §13: antes de pisar el flujo en curso, conserva el punto exacto en el
        // que quedó la conversación para que recepción lo retome.
        if (HumanTransferFlow.CODIGO.equals(codigo)) {
            if (ctx.tieneFlujoActivo() && !HumanTransferFlow.CODIGO.equals(ctx.getFlujo())) {
                ctx.setFlujoAnteriorTransferencia(ctx.getFlujo());
            }
            if (crudo != null && !crudo.isBlank()) {
                ctx.setMotivoTransferencia(e.soporte.recortar(crudo));
            }
        }
        ctx.setFlujo(codigo);
        ctx.setPaso(null);
        ctx.setOpciones(new ArrayList<>());
        ctx.setExpectedInput(null);
        ctx.setIntentosFallidos(0);
        ctx.setCitaId(null);
        if (reiniciarReserva) {
            reiniciarReserva(ctx);
        }
        PasoResultado resultado = motor.arrancar(conv, ctx, t, crudo);
        if (resultado == null || resultado.esIgnorado()) {
            e.contexto.guardar(conv, ctx, intencion);
            return new AgenteConversacionalService.RespuestaAgente(e.soporte.menuTexto(), false, null);
        }
        return responder(conv, ctx, resultado, intencion);
    }

    private void reiniciarReserva(ConversacionContexto ctx) {
        ctx.setFecha(null);
        ctx.setFranja(null);
        ctx.setSlotId(null);
        ctx.setServicioId(null);
        ctx.setServicioLibre(null);
        ctx.setServicioResuelto(false);
        ctx.setOdontologoId(null);
        ctx.setOdontologoLibre(null);
        ctx.setOdontologoResuelto(false);
        ctx.setConsultaHorario(false);
    }

    private AgenteConversacionalService.RespuestaAgente responder(Conversacion conv,
                                                                  ConversacionContexto ctx,
                                                                  PasoResultado resultado,
                                                                  Intencion intencion) {
        if (resultado.completado()) {
            conv.setContextoAgente(null);
        } else if (resultado.persistir()) {
            e.contexto.guardar(conv, ctx, intencion);
        }
        if (resultado.transferirHumano()) {
            conv.setEstado(EstadoConversacion.ATENCION_HUMANA);
            log.info("Agente: conversación {} transferida a atención humana", conv.getId());
        }
        if (resultado.mensaje() == null || resultado.mensaje().isBlank()) {
            return AgenteConversacionalService.RespuestaAgente.silenciosa();
        }
        return new AgenteConversacionalService.RespuestaAgente(
                resultado.mensaje(), resultado.transferirHumano(), resultado.adjunto());
    }

    // ------------------------------------------------------------------
    // Cédula sin flujo activo (§7)
    // ------------------------------------------------------------------

    private AgenteConversacionalService.RespuestaAgente buscarPorCedula(Conversacion conv, String cedula) {
        conv.setPaciente(null);
        conv.setContextoAgente(null);
        var encontrado = e.pacientes.porCedula(cedula);
        if (encontrado.isEmpty()) {
            ConversacionContexto ctx = new ConversacionContexto();
            ctx.reiniciar();
            ctx.setFlujo("REGISTRAR_PACIENTE");
            ctx.setPaso("REG_CONFIRMAR");
            ctx.setCedula(cedula);
            ctx.setMenuActual(MenuActual.REGISTRO);
            ctx.setOpciones(new ArrayList<>());
            ctx.setExpectedInput("CONFIRMACION");
            String mensaje = "No encontré ningún paciente con cédula " + cedula
                    + ".\n¿Quieres que cree tu ficha de paciente? Responde 'sí'.";
            ctx.setUltimaPregunta(mensaje);
            e.contexto.guardar(conv, ctx, Intencion.REGISTRAR_PACIENTE);
            return new AgenteConversacionalService.RespuestaAgente(mensaje, false, null);
        }
        var p = encontrado.get();
        // §7: verificar la identidad no muestra datos sensibles, sólo saluda.
        conv.setPaciente(p);
        ConversacionContexto ctx = new ConversacionContexto();
        ctx.reiniciar();
        ctx.setIdentidadVerificada(true);
        ctx.setIdentidadVerificadaEn(Instant.now());
        ctx.setPacienteId(p.getId());
        ctx.setCedula(p.getCedula());
        ctx.setNombrePaciente(e.pacientes.nombre(p));
        ctx.setMenuActual(MenuActual.PRINCIPAL);
        ctx.setOpciones(e.soporte.opcionesPrincipales());
        String mensaje = "¡Hola, " + e.pacientes.nombreCorto(p) + "! 👋 Ya encontré tu información.\n\n"
                + e.soporte.menuTexto();
        ctx.setUltimaPregunta(mensaje);
        e.contexto.guardar(conv, ctx, Intencion.VER_PERFIL);
        return new AgenteConversacionalService.RespuestaAgente(mensaje, false, null);
    }

    // ------------------------------------------------------------------
    // Otras intenciones sin flujo
    // ------------------------------------------------------------------

    private AgenteConversacionalService.RespuestaAgente conectarGoogle(Conversacion conv,
                                                                       ConversacionContexto ctx) {
        String mensaje;
        if (!googleService.estaConfigurado()) {
            mensaje = "La integración con Google Calendar no está configurada todavía.\n"
                    + "Habla con un administrador para activarla.";
        } else {
            try {
                String url = googleService.urlAutenticacion();
                mensaje = "Para conectar tu Google Calendar visita esta URL:\n" + url
                        + "\n\nUna vez autorizado, vuelve aquí.";
            } catch (Exception ex) {
                log.warn("Agente: no se pudo generar URL de Google Calendar: {}", ex.getMessage());
                mensaje = "No pude generar el enlace de Google Calendar. "
                        + "Intenta de nuevo o habla con una persona.";
            }
        }
        e.contexto.guardar(conv, ctx, Intencion.CONECTAR_GOOGLE);
        return new AgenteConversacionalService.RespuestaAgente(mensaje, false, null);
    }

    /** Fallback contextual (§14): nunca un error genérico si hay pregunta pendiente. */
    private AgenteConversacionalService.RespuestaAgente fallback(Conversacion conv,
                                                                 ConversacionContexto ctx) {
        String pendiente = ctx.getUltimaPregunta();
        int fallidos = ctx.getIntentosFallidos();
        if (pendiente != null && !pendiente.isBlank() && fallidos < 3) {
            ctx.setIntentosFallidos(fallidos + 1);
            StringBuilder sb = new StringBuilder("No entendí 😅\n\n").append(pendiente);
            if (fallidos + 1 >= 3) {
                sb.append("\n\nSi prefieres, escribe 'humano' y te atiende una persona.");
            }
            e.contexto.guardar(conv, ctx, null);
            return new AgenteConversacionalService.RespuestaAgente(sb.toString(), false, null);
        }
        e.contexto.guardar(conv, ctx, Intencion.DESCONOCIDO);
        return new AgenteConversacionalService.RespuestaAgente(
                "No logré entenderte 😅\n" + e.soporte.menuTexto(), false, null);
    }

    // ------------------------------------------------------------------
    // Reinicio (inactividad o "reiniciar")
    // ------------------------------------------------------------------

    private AgenteConversacionalService.RespuestaAgente reiniciar(Conversacion conversacion) {
        mensajeRepository.deleteByConversacion_Id(conversacion.getId());
        conversacion.setIntencion(null);
        conversacion.setContextoAgente(null);
        log.info("Agente: conversación {} reiniciada (inactividad o solicitud del paciente)",
                conversacion.getId());
        return new AgenteConversacionalService.RespuestaAgente(
                "¡Hola! 😊 Soy el asistente virtual de " + AgenteSoporte.MARCA
                        + " y reinicié nuestra conversación para ayudarte mejor.\n"
                        + AgenteSoporte.MENSAJE_MENU,
                false, null);
    }

    /**
     * Tras una pausa larga se ofrece reanudar el proceso en curso (§15); si no
     * había nada que reanudar, se saluda y se limpia la conversación.
     */
    private AgenteConversacionalService.RespuestaAgente reiniciarOReanudar(Conversacion conv,
                                                                          ConversacionContexto ctx) {
        if (ctx.getFlujo() == null || ctx.getUltimaPregunta() == null || ctx.getUltimaPregunta().isBlank()) {
            return reiniciar(conv);
        }
        String mensaje = AgenteSoporte.mensajeReanudacion(ctx.getUltimaPregunta());
        e.contexto.marcarReanudacion(conv, ctx);
        log.info("Agente: conversación {} ofrece reanudación tras inactividad", conv.getId());
        return new AgenteConversacionalService.RespuestaAgente(mensaje, false, null);
    }

    /** "1" reenvía la pregunta pendiente; "2" empieza de nuevo; el resto continúa normal. */
    private AgenteConversacionalService.RespuestaAgente continuarTrasPausa(Conversacion conv,
                                                                          ConversacionContexto ctx,
                                                                          String t) {
        String limpio = AgenteSoporte.compactar(t);
        if ("2".equals(limpio) || limpio.startsWith("empezar") || "nuevo".equals(limpio)
                || "de nuevo".equals(limpio) || "reinicio".equals(limpio)) {
            return reiniciar(conv);
        }
        String pendiente = ctx.getUltimaPregunta();
        if ("1".equals(limpio) || limpio.startsWith("continuar") || limpio.startsWith("seguir")) {
            ctx.setReanudando(false);
            ctx.setIntentosFallidos(0);
            e.contexto.guardar(conv, ctx, null);
            return new AgenteConversacionalService.RespuestaAgente(pendiente, false, null);
        }
        ctx.setReanudando(false);
        e.contexto.guardar(conv, ctx, null);
        return null;
    }

    private boolean inactiva(Instant ultimoPrev) {
        if (ultimoPrev == null) {
            return false;
        }
        return Duration.between(ultimoPrev, Instant.now()).toMillis() >= INACTIVIDAD_REINICIO_MS;
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    /** Contextos escritos por el bot anterior: sólo se recupera el paso de registro (§15). */
    private void inferirLegado(ConversacionContexto ctx) {
        if (ctx.getFlujo() == null && ctx.getPaso() != null && ctx.getPaso().startsWith("REG_")) {
            ctx.setFlujo("REGISTRAR_PACIENTE");
            ctx.setMenuActual(MenuActual.REGISTRO);
            ctx.setOpciones(new ArrayList<>());
        }
        if (ctx.getMenuActual() == null) {
            ctx.setMenuActual(MenuActual.PRINCIPAL);
        }
    }

    /** Opción del menú principal escrita como número, aunque el contexto no la cargue (§4). */
    private OpcionMenu opcionPrincipal(String t) {
        Integer numero = MenuResolver.numeroDe(t);
        List<OpcionMenu> principales = e.soporte.opcionesPrincipales();
        if (numero == null || numero < 1 || numero > principales.size()) {
            return null;
        }
        return principales.get(numero - 1);
    }
}
