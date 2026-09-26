package org.dentalcrm.web.automatizacion;

import org.dentalcrm.domain.automatizacion.EstadoNotificacion;
import org.dentalcrm.domain.automatizacion.EventoAutomatizacion;
import org.dentalcrm.domain.automatizacion.Notificacion;
import org.dentalcrm.domain.automatizacion.NotificacionRepository;
import org.dentalcrm.domain.cita.*;
import org.dentalcrm.domain.whatsapp.EstadoSesionWhatsapp;
import org.dentalcrm.domain.whatsapp.WhatsappSesion;
import org.dentalcrm.domain.whatsapp.WhatsappSesionRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.integration.messaging.MessagingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);
    private static final int MAX_INTENTOS = 3;
    private static final int LOTE = 10;
    private static final Duration COLGADA = Duration.ofMinutes(10);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final NotificacionRepository notificacionRepository;
    private final AutomatizacionService automatizacionService;
    private final CitaRepository citaRepository;
    private final MessagingProvider messagingProvider;
    private final WhatsappSesionRepository sesionRepository;
    private final CorreoService correoService;

    public NotificacionService(NotificacionRepository notificacionRepository,
                               AutomatizacionService automatizacionService,
                               CitaRepository citaRepository,
                               MessagingProvider messagingProvider,
                               WhatsappSesionRepository sesionRepository,
                               CorreoService correoService) {
        this.notificacionRepository = notificacionRepository;
        this.automatizacionService = automatizacionService;
        this.citaRepository = citaRepository;
        this.messagingProvider = messagingProvider;
        this.sesionRepository = sesionRepository;
        this.correoService = correoService;
    }

    // ------------------------------------------------------------------
    // Eventos de cita
    // ------------------------------------------------------------------

    @EventListener
    public void onCitaCreada(CitaCreadaEvent e) {
        Cita cita = citaOError(e.citaId());
        int n = automatizacionService.generar(cita, EventoAutomatizacion.CITA_CREADA, EventoAutomatizacion.CITA_PROXIMA);
        log.info("Cita {} creada: {} notificaciones generadas", e.citaId(), n);
    }

    @EventListener
    public void onCitaModificada(CitaModificadaEvent e) {
        Cita cita = citaOError(e.citaId());
        cancelarActivasDeCita(e.citaId(), "Cita modificada");
        if (e.cambioHorario()) {
            // El paciente cambió de día u hora: hay que decirle el horario nuevo
            // y el anterior. La automatización va con minutos_antes=0, así que se
            // agenda ya mismo y sale en el siguiente lote (≤ 60 s).
            int n = automatizacionService.generar(cita, horarioAnterior(e),
                    EventoAutomatizacion.CITA_MODIFICADA,
                    EventoAutomatizacion.CITA_CREADA,
                    EventoAutomatizacion.CITA_PROXIMA);
            log.info("Cita {} reprogramada de {} {} a {} {}: {} notificaciones",
                    e.citaId(), e.fechaAnterior(), e.horaAnterior(), e.fechaNueva(), e.horaNueva(), n);
            return;
        }
        int n = automatizacionService.generar(cita, EventoAutomatizacion.CITA_CREADA, EventoAutomatizacion.CITA_PROXIMA);
        log.info("Cita {} modificada: regeneradas {} notificaciones", e.citaId(), n);
    }

    private Map<String, String> horarioAnterior(CitaModificadaEvent e) {
        Map<String, String> extra = new java.util.HashMap<>();
        extra.put("fecha_anterior", e.fechaAnterior() == null ? "—" : e.fechaAnterior().format(FECHA));
        extra.put("hora_anterior", e.horaAnterior() == null ? "—" : e.horaAnterior().format(HORA));
        return extra;
    }

    @EventListener
    public void onCitaConfirmada(CitaConfirmadaEvent e) {
        cancelarActivasDeCita(e.citaId(), "Cita confirmada");
        Cita cita = citaOError(e.citaId());
        int n = automatizacionService.generar(cita, EventoAutomatizacion.CITA_CONFIRMADA);
        log.info("Cita {} confirmada: {} notificaciones generadas", e.citaId(), n);
    }

    @EventListener
    public void onCitaCancelada(CitaCanceladaEvent e) {
        cancelarActivasDeCita(e.citaId(), "Cita cancelada");
        Cita cita = citaOError(e.citaId());
        int n = automatizacionService.generar(cita, EventoAutomatizacion.CITA_CANCELADA);
        log.info("Cita {} cancelada: {} notificaciones generadas", e.citaId(), n);
    }

    @EventListener
    public void onCitaAtendida(CitaAtendidaEvent e) {
        Cita cita = citaOError(e.citaId());
        int n = automatizacionService.generar(cita, EventoAutomatizacion.CITA_ATENDIDA);
        log.info("Cita {} atendida: {} notificaciones generadas", e.citaId(), n);
    }

    @EventListener
    public void onNoAsistio(CitaNoAsistioEvent e) {
        Cita cita = citaOError(e.citaId());
        int n = automatizacionService.generar(cita, EventoAutomatizacion.NO_ASISTIO);
        log.info("Cita {} sin asistencia: {} notificaciones generadas", e.citaId(), n);
    }

    // ------------------------------------------------------------------
    // Procesamiento programado
    // ------------------------------------------------------------------

    @Scheduled(fixedDelay = 60_000, initialDelay = 10_000)
    public void procesarPendientes() {
        List<Notificacion> aProcesar = notificacionRepository
                .findByEstadoAndProgramadaAtLessThanEqual(EstadoNotificacion.PENDIENTE, Instant.now(), PageRequest.of(0, LOTE));
        if (aProcesar.isEmpty()) {
            return;
        }
        log.info("Procesando {} notificaciones pendientes", aProcesar.size());
        for (Notificacion n : aProcesar) {
            procesar(n);
        }
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 10_000)
    public void repararEnviando() {
        List<Notificacion> colgadas = notificacionRepository
                .enviandoColgadas(Instant.now().minus(COLGADA), PageRequest.of(0, 20));
        for (Notificacion n : colgadas) {
            n.setEstado(EstadoNotificacion.PENDIENTE);
            n.setProgramadaAt(Instant.now().plusSeconds(60));
            n.setError("Timeout al enviar: reintentando");
            notificacionRepository.save(n);
            log.warn("Notificación {} devuelta a PENDIENTE por quedarse colgada", n.getId());
        }
    }

    @Transactional
    protected void procesar(Notificacion n) {
        if (n.getTelefono() == null || n.getTelefono().isBlank()) {
            fallar(n, "El paciente no tiene teléfono");
            return;
        }
        var sesion = sesionConectada();
        if (sesion.isEmpty()) {
            reintentar(n, "No hay sesión de WhatsApp conectada");
            return;
        }
        n.setEstado(EstadoNotificacion.ENVIANDO);
        notificacionRepository.save(n);
        try {
            messagingProvider.enviarMensaje(sesion.get().getSesionId(), n.getTelefono(), n.getMensajeGenerado());
            // El correo es el canal secundario del aviso de cambio de hora: se
            // envía sólo cuando el WhatsApp salió bien, para no duplicarlo en
            // los reintentos del canal principal.
            enviarCorreoSiAplica(n);
            n.setEstado(EstadoNotificacion.ENVIADA);
            n.setEnviadaAt(Instant.now());
            n.setError(null);
            notificacionRepository.save(n);
            log.info("Notificación {} (cita {}) enviada a {}", n.getId(), n.getCita().getId(), n.getTelefono());
        } catch (Exception ex) {
            log.warn("Fallo al enviar notificación {}: {}", n.getId(), ex.getMessage());
            n.setEstado(EstadoNotificacion.PENDIENTE);
            reintentar(n, truncar(ex.getMessage()));
        }
    }

    /**
     * El aviso de que una cita cambió de día u hora también llega por correo al
     * paciente, si tiene email y el SMTP está configurado. Si no, se omite sin
     * tumbar el envío por WhatsApp.
     */
    private void enviarCorreoSiAplica(Notificacion n) {
        if (n.getAutomatizacion() == null
                || n.getAutomatizacion().getEvento() != EventoAutomatizacion.CITA_MODIFICADA) {
            return;
        }
        var paciente = n.getPaciente() != null
                ? n.getPaciente()
                : (n.getCita() == null ? null : n.getCita().getPaciente());
        String email = paciente == null ? null : paciente.getEmail();
        if (email == null || email.isBlank()) {
            log.debug("Notificación {}: el paciente no tiene correo, se omite el aviso por email", n.getId());
            return;
        }
        if (!correoService.configurado()) {
            log.debug("Notificación {}: SMTP no configurado, se omite el aviso por email", n.getId());
            return;
        }
        try {
            correoService.enviar(email, "Cita reprogramada", n.getMensajeGenerado());
            log.info("Notificación {}: aviso enviado por correo a {}", n.getId(), email);
        } catch (Exception ex) {
            // El WhatsApp ya llegó: el fallo de correo sólo queda en el log.
            log.warn("Notificación {}: no se pudo enviar el correo a {}: {}", n.getId(), email, ex.getMessage());
        }
    }

    private void reintentar(Notificacion n, String motivo) {
        if (n.getIntentos() >= MAX_INTENTOS) {
            fallar(n, motivo);
            return;
        }
        n.setIntentos(n.getIntentos() + 1);
        n.setEstado(EstadoNotificacion.PENDIENTE);
        n.setProgramadaAt(Instant.now().plusSeconds(60));
        n.setError(truncar(motivo));
        notificacionRepository.save(n);
    }

    private void fallar(Notificacion n, String motivo) {
        n.setEstado(EstadoNotificacion.ERROR);
        n.setError(truncar(motivo));
        notificacionRepository.save(n);
    }

    @Transactional
    public void cancelarActivasDeCita(Long citaId, String motivo) {
        List<Notificacion> activas = notificacionRepository.activasDeCita(citaId);
        for (Notificacion n : activas) {
            n.setEstado(EstadoNotificacion.CANCELADA);
            n.setError(truncar("Cancelada: " + motivo));
            notificacionRepository.save(n);
        }
        if (!activas.isEmpty()) {
            log.info("Cita {}: {} notificaciones pendientes canceladas ({})", citaId, activas.size(), motivo);
        }
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private java.util.Optional<WhatsappSesion> sesionConectada() {
        return sesionRepository.findByEstado(EstadoSesionWhatsapp.CONECTADA).stream().findFirst();
    }

    private Cita citaOError(Long id) {
        return citaRepository.findById(id)
                .orElseThrow(() -> new BusinessException("CITA_NO_ENCONTRADA", "Cita no encontrada"));
    }

    private String truncar(String s) {
        if (s == null) {
            return null;
        }
        return s.length() <= 500 ? s : s.substring(0, 500);
    }
}