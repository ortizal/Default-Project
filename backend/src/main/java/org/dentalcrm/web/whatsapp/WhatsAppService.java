package org.dentalcrm.web.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.domain.whatsapp.*;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.integration.messaging.MessagingProvider;
import org.dentalcrm.multitenant.TenantContext;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.util.TelefonoUtil;
import org.dentalcrm.web.agente.AgenteConversacionalService;
import org.dentalcrm.web.whatsapp.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);
    private static final String MODULO = "WHATSAPP";

    private final WhatsappSesionRepository sesionRepository;
    private final ConversacionRepository conversacionRepository;
    private final MensajeRepository mensajeRepository;
    private final PacienteRepository pacienteRepository;
    private final MessagingProvider provider;
    private final AuditService auditService;
    private final FlujoWhatsAppService flujoWhatsApp;
    private final AgenteConversacionalService agenteConversacionalService;

    // Deduplicación de webhooks OpenWA (entrega at-least-once): idempotencyKey -> instante.
    private final ConcurrentMap<String, Long> webhooksProcesados = new ConcurrentHashMap<>();

    public WhatsAppService(WhatsappSesionRepository sesionRepository,
                           ConversacionRepository conversacionRepository,
                           MensajeRepository mensajeRepository,
                           PacienteRepository pacienteRepository,
                           MessagingProvider provider,
                           AuditService auditService,
                           FlujoWhatsAppService flujoWhatsApp,
                           AgenteConversacionalService agenteConversacionalService) {
        this.sesionRepository = sesionRepository;
        this.conversacionRepository = conversacionRepository;
        this.mensajeRepository = mensajeRepository;
        this.pacienteRepository = pacienteRepository;
        this.provider = provider;
        this.auditService = auditService;
        this.flujoWhatsApp = flujoWhatsApp;
        this.agenteConversacionalService = agenteConversacionalService;
    }

    // ------------------------------------------------------------------
    // Sesiones
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<WhatsAppSesionResponse> listarSesiones(String busca, String estado,
                                                                                      int page, int size) {
        String q = busca == null ? "" : busca.trim().toLowerCase();
        EstadoSesionWhatsapp estadoTmp = null;
        if (estado != null && !estado.isBlank()) {
            estadoTmp = EstadoSesionWhatsapp.valueOf(estado.trim().toUpperCase());
        }
        final EstadoSesionWhatsapp estadoEnum = estadoTmp;
        Long tenant = TenantContext.actualOrDefault();
        return sesionRepository.findAllByTenantIdOrderByCreatedAtDesc(tenant).stream()
                .filter(s -> estadoEnum == null || s.getEstado() == estadoEnum)
                .filter(s -> q.isEmpty()
                        || (s.getSesionId() != null && s.getSesionId().toLowerCase().contains(q))
                        || (s.getNombre() != null && s.getNombre().toLowerCase().contains(q)))
                .skip((long) Math.max(page, 0) * Math.min(Math.max(size, 1), 100))
                .limit(Math.min(Math.max(size, 1), 100))
                .map(s -> WhatsAppSesionResponse.from(s, provider.nombre()))
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toList(),
                        list -> new org.springframework.data.domain.PageImpl<>(list,
                                org.springframework.data.domain.PageRequest.of(Math.max(page, 0),
                                        Math.min(Math.max(size, 1), 100)),
                                list.size())));
    }

    @Transactional
    public WhatsAppSesionResponse crearSesion(WhatsAppSesionRequest request) {
        String sesionId = request.sesionId().trim();
        if (sesionRepository.existsBySesionId(sesionId)) {
            throw new BusinessException("SESION_YA_EXISTE",
                    "Ya existe una sesión con el identificador '" + sesionId + "'");
        }
        WhatsappSesion sesion = new WhatsappSesion();
        sesion.setTenantId(TenantContext.actualOrDefault());
        sesion.setSesionId(sesionId);
        sesion.setNombre(request.nombre());
        sesion.setEstado(EstadoSesionWhatsapp.DESCONECTADA);
        sesionRepository.save(sesion);
        auditService.registrar("CREAR_SESION", MODULO, "WHATSAPP_SESION", sesion.getId());
        return WhatsAppSesionResponse.from(sesion, provider.nombre());
    }

    @Transactional
    public WhatsAppSesionResponse conectar(Long id) {
        WhatsappSesion sesion = sesionOError(id);
        sesion.setEstado(EstadoSesionWhatsapp.CONECTANDO);
        sesionRepository.save(sesion);

        if (!provider.estaConfigurado()) {
            marcarFalloSesion(sesion, "OpenWA no está configurado (OPENWA_URL/OPENWA_API_KEY)");
            auditService.registrar("CONECTAR_SESION", MODULO, "WHATSAPP_SESION", sesion.getId());
            return WhatsAppSesionResponse.from(sesion, provider.nombre());
        }

        try {
            MessagingProvider.ResultadoConexion resultado = provider.iniciarSesion(sesion.getSesionId());
            sesion.setEstado(EstadoSesionWhatsapp.valueOf(resultado.estado()));
            sesion.setEstadoDetalle(resultado.detalle());
            sesion.setQr(resultado.qr());
            sesion.setLastError(resultado.error());
        } catch (Exception e) {
            log.warn("No se pudo iniciar la sesión WhatsApp {}: {}", sesion.getSesionId(), e.getMessage());
            marcarFalloSesion(sesion, e.getMessage());
        }
        sesionRepository.save(sesion);
        auditService.registrar("CONECTAR_SESION", MODULO, "WHATSAPP_SESION", sesion.getId());
        return WhatsAppSesionResponse.from(sesion, provider.nombre());
    }

    @Transactional
    public WhatsAppSesionResponse desconectar(Long id) {
        WhatsappSesion sesion = sesionOError(id);
        if (provider.estaConfigurado()) {
            try {
                provider.cerrarSesion(sesion.getSesionId());
            } catch (Exception e) {
                log.warn("OpenWA rechazó el cierre de sesión {}: {}", sesion.getSesionId(), e.getMessage());
            }
        }
        sesion.setEstado(EstadoSesionWhatsapp.DESCONECTADA);
        sesion.setEstadoDetalle(null);
        sesion.setQr(null);
        sesion.setLastError(null);
        sesionRepository.save(sesion);
        auditService.registrar("DESCONECTAR_SESION", MODULO, "WHATSAPP_SESION", sesion.getId());
        return WhatsAppSesionResponse.from(sesion, provider.nombre());
    }

    @Transactional
    public void borrarSesion(Long id) {
        WhatsappSesion sesion = sesionOError(id);
        String sesionId = sesion.getSesionId();
        if (provider.estaConfigurado()) {
            try {
                provider.cerrarSesion(sesionId);
            } catch (Exception e) {
                log.warn("OpenWA rechazó el cierre al borrar la sesión {}: {}", sesionId, e.getMessage());
            }
        }
        sesionRepository.deleteById(id);
        auditService.registrar("BORRAR_SESION", MODULO, "WHATSAPP_SESION", id);
    }

    // ------------------------------------------------------------------
    // Conversaciones e inbox
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ConversacionResponse> listarConversaciones(String q) {
        return conversacionRepository.buscar(q == null ? "" : q.trim()).stream()
                .map(c -> ConversacionResponse.from(c, ultimoMensajeTexto(c.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversacionDetalleResponse obtenerConversacion(Long id) {
        Conversacion conversacion = conversacionOError(id);
        List<Mensaje> mensajes = mensajeRepository
                .findByConversacion_IdOrderByReceivedAtAsc(conversacion.getId());
        return ConversacionDetalleResponse.from(
                conversacion,
                mensajes.isEmpty() ? null : mensajes.get(mensajes.size() - 1).getTexto(),
                mensajes.stream().map(MensajeResponse::from).toList());
    }

    @Transactional
    public MensajeResponse enviarMensaje(Long conversacionId, EnviarMensajeRequest request) {
        Conversacion conversacion = conversacionOError(conversacionId);
        WhatsappSesion sesion = conversacion.getSesion();
        if (sesion == null) {
            throw new BusinessException("SIN_SESION", "Esta conversación no tiene sesión WhatsApp");
        }

        Mensaje mensaje = new Mensaje();
        mensaje.setConversacion(conversacion);
        mensaje.setDireccion(DireccionMensaje.SALIDA);
        mensaje.setTexto(request.texto());
        mensaje.setRemitente(sesion.getSesionId());
        mensaje.setEstado(EstadoMensaje.PENDIENTE);
        mensaje.setReceivedAt(Instant.now());
        mensajeRepository.save(mensaje);

        if (!provider.estaConfigurado()) {
            marcarFalloMensaje(mensaje, "OpenWA no está configurado (OPENWA_URL/OPENWA_API_KEY)");
        } else {
            try {
                provider.enviarMensaje(sesion.getSesionId(), conversacion.getTelefono(), request.texto());
                mensaje.setEstado(EstadoMensaje.ENVIADO);
                mensaje.setError(null);
            } catch (Exception e) {
                log.warn("No se pudo enviar mensaje a la conversación {}: {}", conversacionId, e.getMessage());
                marcarFalloMensaje(mensaje, e.getMessage());
            }
        }
        mensajeRepository.save(mensaje);
        actualizarUltimoMensaje(conversacion, mensaje.getReceivedAt());
        auditService.registrar("ENVIAR_MENSAJE", MODULO, "CONVERSACION", conversacion.getId());
        return MensajeResponse.from(mensaje);
    }

    @Transactional
    public ConversacionResponse cambiarEstado(Long conversacionId, CambiarEstadoConversacionRequest request) {
        Conversacion conversacion = conversacionOError(conversacionId);
        conversacion.setEstado(EstadoConversacion.valueOf(request.estado()));
        conversacionRepository.save(conversacion);
        auditService.registrar("CAMBIAR_ESTADO_CONVERSACION", MODULO, "CONVERSACION", conversacion.getId());
        return ConversacionResponse.from(conversacion, ultimoMensajeTexto(conversacion.getId()));
    }

    // ------------------------------------------------------------------
    // Webhook (mensajes entrantes desde OpenWA)
    // ------------------------------------------------------------------

    @Transactional
    public WebhookResponse recibirWebhook(JsonNode body) {
        if (body == null) {
            return new WebhookResponse(true, null);
        }
        String event = body.path("event").asText("");
        if (!"message".equals(event) && !"message.received".equals(event)) {
            return new WebhookResponse(true, null);
        }

        String idempotencia = body.path("idempotencyKey").asText(null);
        if (idempotencia != null) {
            long ahora = System.currentTimeMillis();
            limpiarWebhooksProcesados(ahora);
            if (webhooksProcesados.putIfAbsent(idempotencia, ahora) != null) {
                return new WebhookResponse(true, null);
            }
        }

        boolean fromMe = body.path("data").path("fromMe").asBoolean(false);
        boolean esGrupo = body.path("data").path("isGroup").asBoolean(false);
        if (fromMe || esGrupo) {
            return new WebhookResponse(true, null);
        }

        String sesionId = body.path("session").asText(null);
        String idExterno = body.path("sessionId").asText(null);
        final WhatsappSesion sesion = resolverSesionWebhook(sesionId, idExterno);
        String from = body.path("data").path("from").asText(null);
        String texto = body.path("data").path("body").asText(null);
        String notifyName = body.path("data").path("notifyName").asText(null);

        if (sesion == null || from == null || texto == null) {
            if (sesion == null && sesionId == null) {
                log.warn("Webhook recibido sin sesión resoluble");
            } else if (sesion == null) {
                log.warn("Webhook recibido para una sesión desconocida: {}", sesionId);
            }
            return new WebhookResponse(true, null);
        }
        String local = from.contains("@") ? from.substring(0, from.indexOf('@')) : from;
        if ("me".equalsIgnoreCase(local)) {
            return new WebhookResponse(true, null);
        }
        String telefono = TelefonoUtil.digitos(local);
        if (telefono.isEmpty()) {
            return new WebhookResponse(true, null);
        }

        // Fase 9: la sesión indica a qué tenant pertenece el mensaje entrante
        // (sesion_id es único a nivel global). A partir de aquí los accesos a
        // conversaciones, mensajes, pacientes y citas quedan aislados por tenant.
        TenantContext.set(sesion.getTenantId() != null ? sesion.getTenantId() : TenantContext.actualOrDefault());
        try {
        Conversacion conversacion = conversacionRepository
                .findBySesionIdAndTelefono(sesion.getId(), telefono)
                .orElseGet(() -> {
                    Conversacion nueva = new Conversacion();
                    nueva.setSesion(sesion);
                    nueva.setTelefono(telefono);
                    nueva.setNombreContacto(notifyName);
                    nueva.setEstado(EstadoConversacion.BOT);
                    return conversacionRepository.save(nueva);
                });

        if (conversacion.getPaciente() == null) {
            resolverPaciente(telefono).ifPresent(conversacion::setPaciente);
        }
        if (conversacion.getEstado() == EstadoConversacion.CERRADA) {
            conversacion.setEstado(EstadoConversacion.BOT);
        }
        if (notifyName != null && !notifyName.isBlank()
                && (conversacion.getNombreContacto() == null || conversacion.getNombreContacto().isBlank())) {
            conversacion.setNombreContacto(notifyName);
        }

        Instant ultimoPrev = conversacion.getUltimoMensajeAt();

        Mensaje mensaje = new Mensaje();
        mensaje.setConversacion(conversacion);
        mensaje.setDireccion(DireccionMensaje.ENTRADA);
        mensaje.setTexto(texto);
        mensaje.setRemitente(telefono);
        mensaje.setEstado(EstadoMensaje.RECIBIDO);
        mensaje.setReceivedAt(Instant.now());
        mensajeRepository.save(mensaje);

        actualizarUltimoMensaje(conversacion, mensaje.getReceivedAt());
        auditService.registrar("RECIBIR_MENSAJE", MODULO, "CONVERSACION", conversacion.getId());

        // Fase 8: agente de IA — agenda natural, herramientas y transferencia a humano.
        // Si el agente ya está conversando, responde primero; si no ha entrado en
        // la conversación, el bot simple de la Fase 6 sigue teniendo prioridad.
        boolean conEstadoAgente = conversacion.getIntencion() != null || conversacion.getContextoAgente() != null;
        AgenteConversacionalService.RespuestaAgente respuesta = null;
        if (conEstadoAgente) {
            respuesta = agenteConversacionalService.procesar(conversacion, texto, ultimoPrev);
        }
        if (respuesta == null || respuesta.mensaje() == null || respuesta.mensaje().isBlank()) {
            // Fase 6: bot — confirmar/cancelar la próxima cita del paciente.
            FlujoWhatsAppService.AccionFlujo accion = flujoWhatsApp.procesarEntrada(conversacion, texto);
            if (accion != null && accion != FlujoWhatsAppService.AccionFlujo.IGNORADA) {
                log.info("Acción del bot en conversación {}: {}", conversacion.getId(), accion);
            }
            if (!conEstadoAgente && (accion == null || accion == FlujoWhatsAppService.AccionFlujo.IGNORADA)) {
                respuesta = agenteConversacionalService.procesar(conversacion, texto, ultimoPrev);
            }
        }
        if (respuesta != null) {
            if (respuesta.mensaje() != null && !respuesta.mensaje().isBlank()) {
                enviarMensaje(conversacion.getId(), new EnviarMensajeRequest(respuesta.mensaje()));
            }
            if (respuesta.adjunto() != null) {
                enviarDocumento(conversacion, respuesta.adjunto());
            }
            if (respuesta.transferirHumano()) {
                log.info("Agente: conversación {} transferida a humano", conversacion.getId());
            }
        }
        return new WebhookResponse(true, conversacion.getId());
        } finally {
            TenantContext.clear();
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void limpiarWebhooksProcesados(long ahora) {
        if (webhooksProcesados.size() < 10_000) {
            return;
        }
        webhooksProcesados.entrySet().removeIf(e -> ahora - e.getValue() > 30 * 60 * 1000L);
    }

    private void actualizarUltimoMensaje(Conversacion conversacion, Instant momento) {
        conversacion.setUltimoMensajeAt(momento);
        conversacionRepository.save(conversacion);
    }

    private void enviarDocumento(Conversacion conversacion, AgenteConversacionalService.Adjunto adjunto) {
        if (!provider.estaConfigurado()) {
            log.warn("No se pudo enviar adjunto en la conversación {}: OpenWA no configurado", conversacion.getId());
            return;
        }
        try {
            provider.enviarDocumento(conversacion.getSesion().getSesionId(), conversacion.getTelefono(),
                    adjunto.nombreArchivo(), adjunto.mime(), adjunto.contenido(), adjunto.caption());
            Mensaje registro = new Mensaje();
            registro.setConversacion(conversacion);
            registro.setDireccion(DireccionMensaje.SALIDA);
            registro.setTexto("📎 " + (adjunto.caption() == null ? "Documento" : adjunto.caption())
                    + " (" + adjunto.nombreArchivo() + ")");
            registro.setRemitente(conversacion.getSesion().getSesionId());
            registro.setEstado(EstadoMensaje.ENVIADO);
            registro.setReceivedAt(Instant.now());
            mensajeRepository.save(registro);
            actualizarUltimoMensaje(conversacion, registro.getReceivedAt());
        } catch (Exception e) {
            log.warn("No se pudo enviar el adjunto {} a la conversación {}: {}",
                    adjunto.nombreArchivo(), conversacion.getId(), e.getMessage());
        }
    }

    private String ultimoMensajeTexto(Long conversacionId) {
        return mensajeRepository.ultimos(conversacionId, PageRequest.of(0, 1)).stream()
                .findFirst().map(Mensaje::getTexto).orElse(null);
    }

    private java.util.Optional<Paciente> resolverPaciente(String telefono) {
        return pacienteRepository.activosConTelefono().stream()
                .filter(p -> TelefonoUtil.corresponden(p.getTelefono(), telefono))
                .findFirst();
    }

    private java.util.Optional<WhatsappSesion> buscarSesionPorNombreExterno(String nombreExterno) {
        String objetivo = provider.normalizarSesion(nombreExterno);
        if (objetivo == null || objetivo.isBlank()) {
            return java.util.Optional.empty();
        }
        return sesionRepository.findAll().stream()
                .filter(s -> objetivo.equals(provider.normalizarSesion(s.getSesionId())))
                .findFirst();
    }

    private WhatsappSesion resolverSesionWebhook(String sesionId, String idExterno) {
        WhatsappSesion sesion = sesionId == null ? null
                : sesionRepository.findBySesionId(sesionId).orElse(null);
        if (sesion == null && sesionId != null) {
            sesion = buscarSesionPorNombreExterno(sesionId).orElse(null);
        }
        if (sesion == null && idExterno != null) {
            String nombreExterno = provider.nombreDeSesionExterna(idExterno);
            if (nombreExterno != null) {
                sesion = sesionRepository.findBySesionId(nombreExterno).orElse(null);
                if (sesion == null) {
                    sesion = buscarSesionPorNombreExterno(nombreExterno).orElse(null);
                }
            }
        }
        return sesion;
    }

    private void marcarFalloMensaje(Mensaje mensaje, String error) {
        mensaje.setEstado(EstadoMensaje.ERROR);
        mensaje.setError(trunc(error));
    }

    private void marcarFalloSesion(WhatsappSesion sesion, String error) {
        sesion.setEstado(EstadoSesionWhatsapp.ERROR);
        sesion.setLastError(trunc(error));
    }

    private String trunc(String texto) {
        if (texto == null) {
            return null;
        }
        return texto.length() > 500 ? texto.substring(0, 500) : texto;
    }

    private WhatsappSesion sesionOError(Long id) {
        WhatsappSesion sesion = sesionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("SESION_NO_ENCONTRADA", "Sesión de WhatsApp no encontrada"));
        if (sesion.getTenantId() != null
                && !sesion.getTenantId().equals(TenantContext.actualOrDefault())) {
            throw new BusinessException("SESION_NO_ENCONTRADA", "Sesión de WhatsApp no encontrada");
        }
        return sesion;
    }

    private Conversacion conversacionOError(Long id) {
        return conversacionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("CONVERSACION_NO_ENCONTRADA", "Conversación no encontrada"));
    }
}