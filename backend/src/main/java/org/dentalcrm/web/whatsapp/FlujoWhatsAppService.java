package org.dentalcrm.web.whatsapp;

import org.dentalcrm.domain.cita.Cita;
import org.dentalcrm.domain.cita.CitaRepository;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.whatsapp.Conversacion;
import org.dentalcrm.web.cita.CitaService;
import org.dentalcrm.web.cita.dto.CancelarRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Bot conversacional para WhatsApp (Fase 6): los pacientes pueden confirmar o
 * cancelar su próxima cita escribiendo 1/SI/CONFIRMAR o 3/CANCELAR.
 */
@Service
public class FlujoWhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(FlujoWhatsAppService.class);
    private static final Set<String> CONFIRMAR = Set.of("1", "si", "sí", "confirmo", "confirmar", "confirm");
    private static final Set<String> CANCELAR = Set.of("3", "cancelar", "cancelo", "cancel", "dar de baja");

    private final CitaRepository citaRepository;
    private final CitaService citaService;

    public FlujoWhatsAppService(CitaRepository citaRepository, CitaService citaService) {
        this.citaRepository = citaRepository;
        this.citaService = citaService;
    }

    public ResultadoFlujo procesarEntrada(Conversacion conversacion, String texto) {
        Paciente paciente = conversacion.getPaciente();
        if (paciente == null) {
            return ResultadoFlujo.ignorada();
        }
        String normalizado = normalizar(texto);
        if (esConfirmar(normalizado)) {
            return confirmarProxima(paciente);
        }
        if (esCancelar(normalizado)) {
            return cancelarProxima(paciente);
        }
        return ResultadoFlujo.ignorada();
    }

    private ResultadoFlujo confirmarProxima(Paciente paciente) {
        Cita proxima = proximaCita(paciente);
        if (proxima == null) {
            log.info("Paciente {} pidió confirmar pero no tiene cita próxima", paciente.getId());
            return ResultadoFlujo.ignorada();
        }
        try {
            citaService.confirmar(proxima.getId(), "WHATSAPP");
            log.info("Paciente {} confirmó la cita {} por WhatsApp", paciente.getId(), proxima.getId());
            return new ResultadoFlujo(AccionFlujo.CONFIRMADA,
                    "¡Listo! ✅ Confirmé tu cita del " + cuando(proxima) + ".\n"
                            + "Te esperamos 😊 Si no puedes asistir, avísanos por este chat.");
        } catch (Exception ex) {
            log.warn("No se pudo confirmar la cita {} por WhatsApp: {}", proxima.getId(), ex.getMessage());
            return ResultadoFlujo.ignorada();
        }
    }

    private ResultadoFlujo cancelarProxima(Paciente paciente) {
        Cita proxima = proximaCita(paciente);
        if (proxima == null) {
            log.info("Paciente {} pidió cancelar pero no tiene cita próxima", paciente.getId());
            return ResultadoFlujo.ignorada();
        }
        try {
            citaService.cancelar(proxima.getId(), new CancelarRequest("Cancelada por el paciente (WhatsApp)"));
            log.info("Paciente {} canceló la cita {} por WhatsApp", paciente.getId(), proxima.getId());
            return new ResultadoFlujo(AccionFlujo.CANCELADA,
                    "Listo ✔ Cancelé tu cita del " + cuando(proxima) + ".\n"
                            + "Si cambias de opinión, escríbeme y te ayudo a agendar otra.");
        } catch (Exception ex) {
            log.warn("No se pudo cancelar la cita {} por WhatsApp: {}", proxima.getId(), ex.getMessage());
            return ResultadoFlujo.ignorada();
        }
    }

    private String cuando(Cita cita) {
        return String.format("%02d/%02d a las %s",
                cita.getFecha().getDayOfMonth(), cita.getFecha().getMonthValue(), cita.getHoraInicio());
    }

    @Transactional(readOnly = true)
    private Cita proximaCita(Paciente paciente) {
        List<Cita> proximas = citaRepository.proximasDelPaciente(paciente.getId(), LocalDate.now());
        return proximas.stream().findFirst().orElse(null);
    }

    private boolean esConfirmar(String t) {
        return tokens(t).stream().anyMatch(CONFIRMAR::contains);
    }

    private boolean esCancelar(String t) {
        return tokens(t).stream().anyMatch(CANCELAR::contains);
    }

    private java.util.Set<String> tokens(String texto) {
        return java.util.Arrays.stream(texto.split("[^a-z0-9]+"))
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toSet());
    }

    private String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.trim().toLowerCase(Locale.ROOT);
    }

    public enum AccionFlujo {
        IGNORADA, CONFIRMADA, CANCELADA
    }

    /**
     * Resultado del bot: la acción detectada y la respuesta que hay que enviar
     * al paciente. Devolver la acción sin texto dejaba al paciente sin
     * respuesta cuando solo el bot simple (sin agente) gestionaba el chat.
     */
    public record ResultadoFlujo(AccionFlujo accion, String mensaje) {

        public static ResultadoFlujo ignorada() {
            return new ResultadoFlujo(AccionFlujo.IGNORADA, null);
        }
    }
}