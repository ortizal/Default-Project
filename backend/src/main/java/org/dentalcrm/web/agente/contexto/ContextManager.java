package org.dentalcrm.web.agente.contexto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.dentalcrm.domain.agente.Intencion;
import org.dentalcrm.domain.whatsapp.Conversacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

/**
 * Lee y escribe {@link ConversacionContexto} en el JSONB de la conversación,
 * garantizando las marcas de tiempo de la §15 ({@code updatedAt}/{@code expiresAt}).
 */
public class ContextManager {

    private static final Logger log = LoggerFactory.getLogger(ContextManager.class);
    /** El estado se considera caducado tras un día de silencio. */
    public static final long VIDA_MS = 24 * 60 * 60 * 1000L;
    /** La sesión de identidad verificada dura 15 minutos (§9). */
    public static final long VIDA_IDENTIDAD_MS = 15 * 60 * 1000L;

    private final ObjectMapper mapper;

    public ContextManager(ObjectMapper base) {
        this.mapper = (base == null ? new ObjectMapper() : base.copy())
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public ConversacionContexto cargar(Conversacion conversacion) {
        String json = conversacion.getContextoAgente();
        if (json == null || json.isBlank()) {
            ConversacionContexto ctx = new ConversacionContexto();
            ctx.reiniciar();
            return ctx;
        }
        try {
            ConversacionContexto ctx = mapper.readValue(json, ConversacionContexto.class);
            if (ctx.getOpciones() == null) {
                ctx.setOpciones(new java.util.ArrayList<>());
            }
            return ctx;
        } catch (Exception ex) {
            log.warn("Contexto ilegible en conversación {}: {}", conversacion.getId(), ex.getMessage());
            ConversacionContexto ctx = new ConversacionContexto();
            ctx.reiniciar();
            return ctx;
        }
    }

    /** Guarda el contexto y lo marca con {@code updatedAt}/{@code expiresAt} actuales. */
    public void guardar(Conversacion conversacion, ConversacionContexto ctx) {
        guardar(conversacion, ctx, null);
    }

    /**
     * Guarda el contexto; si se pasa una intención explícita se usa en lugar de
     * la derivada del flujo (saludos, Google, respuestas sin flujo).
     */
    public void guardar(Conversacion conversacion, ConversacionContexto ctx, Intencion explicita) {
        Instant ahora = Instant.now();
        ctx.setUpdatedAt(ahora);
        ctx.setExpiresAt(ahora.plusMillis(VIDA_MS));
        ctx.setReanudando(false);
        try {
            conversacion.setContextoAgente(mapper.writeValueAsString(ctx));
        } catch (Exception ex) {
            log.warn("No se pudo serializar el contexto de la conversación {}: {}",
                    conversacion.getId(), ex.getMessage());
        }
        conversacion.setIntencion(explicita != null ? explicita : intencionDe(ctx.getFlujo()));
    }

    /** ¿La identidad verificada por cédula sigue vigente? (§9) */
    public boolean identidadValida(ConversacionContexto ctx) {
        return ctx.isIdentidadVerificada()
                && ctx.getPacienteId() != null
                && ctx.getIdentidadVerificadaEn() != null
                && java.time.Duration.between(ctx.getIdentidadVerificadaEn(), Instant.now()).toMillis()
                        < VIDA_IDENTIDAD_MS;
    }

    public void limpiar(Conversacion conversacion) {
        conversacion.setContextoAgente(null);
        conversacion.setIntencion(Intencion.DESCONOCIDO);
    }

    public void marcarReanudacion(Conversacion conversacion, ConversacionContexto ctx) {
        ctx.setReanudando(true);
        try {
            conversacion.setContextoAgente(mapper.writeValueAsString(ctx));
        } catch (Exception ex) {
            log.warn("No se pudo marcar la reanudación de la conversación {}", conversacion.getId());
        }
    }

    public String escribir(ConversacionContexto ctx) {
        try {
            return mapper.writeValueAsString(ctx);
        } catch (Exception ex) {
            return "{}";
        }
    }

    public static Intencion intencionDe(String flujo) {
        if (flujo == null || flujo.isBlank()) {
            return Intencion.DESCONOCIDO;
        }
        return switch (flujo) {
            case "AGENDAR_CITA" -> Intencion.AGENDAR_CITA;
            case "CONSULTAR_DISPONIBILIDAD" -> Intencion.PREGUNTAR_DISPONIBILIDAD;
            case "PREGUNTAR_DISPONIBILIDAD" -> Intencion.PREGUNTAR_DISPONIBILIDAD;
            case "CONFIRMAR_CITA" -> Intencion.CONFIRMAR_CITA;
            case "CANCELAR_CITA" -> Intencion.CANCELAR_CITA;
            case "VER_CITAS" -> Intencion.VER_CITAS;
            case "GESTIONAR_CITA" -> Intencion.GESTIONAR_CITA;
            case "VER_SERVICIOS" -> Intencion.LISTAR_SERVICIOS;
            case "LISTAR_SERVICIOS" -> Intencion.LISTAR_SERVICIOS;
            case "VER_PERFIL" -> Intencion.VER_PERFIL;
            case "REGISTRAR_PACIENTE" -> Intencion.REGISTRAR_PACIENTE;
            case "TRANSFERIR_HUMANO" -> Intencion.TRANSFERIR_HUMANO;
            default -> Intencion.DESCONOCIDO;
        };
    }

    /** Solo para tests: expone el mapa que se guarda en el JSONB. */
    public Map<String, Object> mapaDe(ConversacionContexto ctx) {
        return mapper.convertValue(ctx, Map.class);
    }
}
