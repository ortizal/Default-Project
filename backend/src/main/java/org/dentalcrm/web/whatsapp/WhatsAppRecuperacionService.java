package org.dentalcrm.web.whatsapp;

import org.dentalcrm.domain.whatsapp.Conversacion;
import org.dentalcrm.domain.whatsapp.ConversacionRepository;
import org.dentalcrm.domain.whatsapp.DireccionMensaje;
import org.dentalcrm.domain.whatsapp.EstadoConversacion;
import org.dentalcrm.domain.whatsapp.EstadoMensaje;
import org.dentalcrm.domain.whatsapp.EstadoSesionWhatsapp;
import org.dentalcrm.domain.whatsapp.Mensaje;
import org.dentalcrm.domain.whatsapp.MensajeRepository;
import org.dentalcrm.domain.whatsapp.WhatsappSesion;
import org.dentalcrm.integration.messaging.MessagingProvider;
import org.dentalcrm.multitenant.TenantContext;
import org.dentalcrm.multitenant.TenantIdentifierResolver;
import org.dentalcrm.web.agente.AgenteConversacionalService;
import org.dentalcrm.web.whatsapp.dto.EnviarMensajeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Recupera los chats de WhatsApp que quedaron sin respuesta cuando el
 * servicio se reinicia o cuando OpenWA no pudo entregar un envío.
 *
 * <p>Sobre el último mensaje de cada conversación distingue dos situaciones:
 *
 * <ul>
 *   <li><b>Salida fallida</b> — el mensaje de salida quedó en ERROR/PENDIENTE
 *       porque OpenWA no estaba disponible: se vuelve a enviar con su mismo
 *       texto reutilizando el mismo registro.</li>
 *   <li><b>Entrada sin respuesta</b> — el paciente escribió y el bot nunca
 *       contestó (p. ej. confirmar o cancelar una cita no enviaba respuesta):
 *       se reenvía la pregunta del paso en curso del agente o, si no hay nada
 *       que retomar, se vuelve a pedir la cédula.</li>
 * </ul>
 *
 * <p>Corre al arrancar y de forma periódica. Los intentos quedan contabilizados
 * en {@code mensajes.reintentos} y solo se consideran conversaciones cuyo
 * último mensaje tiene al menos unos minutos de antigüedad, de modo que ni un
 * fallo persistente ni un webhook en vuelo pueden provocar reenvíos en bucle.
 */
@Service
public class WhatsAppRecuperacionService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppRecuperacionService.class);
    private static final int MAX_REINTENTOS = 10;
    private static final int LOTE = 100;
    private static final long ANTIGUEDAD_MIN_MS = 5 * 60 * 1000L;

    private final ConversacionRepository conversacionRepository;
    private final MensajeRepository mensajeRepository;
    private final WhatsAppService whatsappService;
    private final AgenteConversacionalService agente;
    private final MessagingProvider provider;
    private final JdbcTemplate jdbc;
    private final long ventanaHoras;

    public WhatsAppRecuperacionService(ConversacionRepository conversacionRepository,
                                       MensajeRepository mensajeRepository,
                                       WhatsAppService whatsappService,
                                       AgenteConversacionalService agente,
                                       MessagingProvider provider,
                                       JdbcTemplate jdbc,
                                       @Value("${app.openwa.recuperacion.ventana-horas:48}") long ventanaHoras) {
        this.conversacionRepository = conversacionRepository;
        this.mensajeRepository = mensajeRepository;
        this.whatsappService = whatsappService;
        this.agente = agente;
        this.provider = provider;
        this.jdbc = jdbc;
        this.ventanaHoras = ventanaHoras;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recuperarAlArrancar() {
        recuperar("arranque");
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000L, initialDelay = 60 * 1000L)
    public void recuperarPeriodicamente() {
        recuperar("barrido periódico");
    }

    /**
     * Barre todos los tenants. Nunca propaga la excepción: un fallo aquí no
     * debe impedir que el servicio arranque ni detener el barrido siguiente.
     */
    public void recuperar(String motivo) {
        try {
            if (!provider.estaConfigurado()) {
                log.debug("Recuperación de chats ({}): OpenWA sin configurar, se omite", motivo);
                return;
            }
            Instant desde = Instant.now().minus(Duration.ofHours(ventanaHoras));
            int reenviadas = 0;
            int retomadas = 0;
            for (Long tenant : tenants()) {
                TenantContext.set(tenant);
                try {
                    int[] parcial = recuperarTenant(desde);
                    reenviadas += parcial[0];
                    retomadas += parcial[1];
                } finally {
                    TenantContext.clear();
                }
            }
            if (reenviadas > 0 || retomadas > 0) {
                log.info("Recuperación de chats ({}): {} salidas reenviadas y {} conversaciones retomadas",
                        motivo, reenviadas, retomadas);
            } else {
                log.debug("Recuperación de chats ({}): nada pendiente", motivo);
            }
        } catch (Exception e) {
            log.warn("Recuperación de chats ({}) falló: {}", motivo, e.getMessage(), e);
        }
    }

    private int[] recuperarTenant(Instant desde) {
        List<Conversacion> candidatas = conversacionRepository.candidatasRecuperacion(
                desde,
                List.of(EstadoConversacion.BOT, EstadoConversacion.ATENCION_HUMANA),
                PageRequest.of(0, LOTE));
        long antiguo = Instant.now().minusMillis(ANTIGUEDAD_MIN_MS).toEpochMilli();
        int reenviadas = 0;
        int retomadas = 0;
        for (Conversacion conversacion : candidatas) {
            try {
                Decision decision = evaluar(conversacion, antiguo);
                if (decision == null) {
                    continue;
                }
                if (decision.accion() == Accion.REENVIAR) {
                    if (whatsappService.reenviarFallido(decision.mensaje().getId())) {
                        reenviadas++;
                    }
                } else if (retomar(conversacion, decision.mensaje())) {
                    retomadas++;
                }
            } catch (Exception e) {
                log.warn("No se pudo recuperar la conversación {}: {}", conversacion.getId(), e.getMessage());
            }
        }
        return new int[]{reenviadas, retomadas};
    }

    /**
     * Decide qué hacer con la conversación, o {@code null} si no corresponde
     * tocarla (sin sesión conectada, mensaje reciente o reintentos agotados).
     */
    private Decision evaluar(Conversacion conversacion, long antiguoMs) {
        WhatsappSesion sesion = conversacion.getSesion();
        if (sesion == null || sesion.getEstado() != EstadoSesionWhatsapp.CONECTADA) {
            return null;
        }
        Mensaje ultimo = mensajeRepository.ultimos(conversacion.getId(), PageRequest.of(0, 1))
                .stream().findFirst().orElse(null);
        if (ultimo == null || ultimo.getReceivedAt() == null
                || ultimo.getReceivedAt().toEpochMilli() > antiguoMs) {
            return null;
        }
        if (ultimo.getReintentos() != null && ultimo.getReintentos() >= MAX_REINTENTOS) {
            return null;
        }
        if (ultimo.getDireccion() == DireccionMensaje.SALIDA) {
            return ultimo.getEstado() == EstadoMensaje.ERROR || ultimo.getEstado() == EstadoMensaje.PENDIENTE
                    ? new Decision(Accion.REENVIAR, ultimo)
                    : null;
        }
        if (conversacion.getEstado() == EstadoConversacion.BOT
                && Boolean.TRUE.equals(conversacion.getAgenteActivo())) {
            return new Decision(Accion.RETOMAR, ultimo);
        }
        return null;
    }

    private boolean retomar(Conversacion conversacion, Mensaje entrada) {
        whatsappService.registrarIntentoRetoma(entrada.getId());
        String texto = agente.mensajeDeReanudacion(conversacion);
        if (texto == null || texto.isBlank()) {
            return false;
        }
        whatsappService.enviarMensaje(conversacion.getId(), new EnviarMensajeRequest(texto));
        log.info("Conversación {} retomada tras quedar sin respuesta", conversacion.getId());
        return true;
    }

    /**
     * El listado de tenants va por JDBC: cualquier consulta JPA quedaría
     * filtrada por el tenant actual y no serviría para enumerarlos.
     */
    private List<Long> tenants() {
        try {
            List<Long> ids = jdbc.queryForList(
                    "SELECT id FROM tenants WHERE estado = 'ACTIVO' ORDER BY id", Long.class);
            if (!ids.isEmpty()) {
                return ids;
            }
        } catch (Exception e) {
            log.warn("No se pudo listar los tenants, se usa el tenant por defecto: {}", e.getMessage());
        }
        return List.of(TenantIdentifierResolver.TENANT_DEFECTO);
    }

    private record Decision(Accion accion, Mensaje mensaje) {
    }

    private enum Accion {
        REENVIAR, RETOMAR
    }
}
