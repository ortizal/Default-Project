package org.dentalcrm.web.agenda.publica;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.dentalcrm.multitenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Enlaces temporales firmados para la agenda pública (§11, §17).
 *
 * <p>El token no expone identificadores en la URL: sólo lleva la firma y el
 * payload codificado; nunca concede acceso a historia clínica, otros
 * pacientes ni administración. Dos propósitos: {@code BOOK_APPOINTMENT}
 * (consulta + reserva, con paciente identificado) y
 * {@code VIEW_AVAILABILITY} (sólo consulta, puede ser anónimo).
 */
@Service
public class AgendaPublicaTokenService {

    private static final Logger log = LoggerFactory.getLogger(AgendaPublicaTokenService.class);
    public static final String PROPOSITO = "BOOK_APPOINTMENT";
    public static final String PROPOSITO_CONSULTA = "VIEW_AVAILABILITY";
    /** Propósito interno de los cupos que devuelve /slots (§15 del plan). */
    public static final String PROPOSITO_SLOT = "SLOT";

    private static final long TTL_POR_DEFECTO = 15;

    private final ObjectMapper mapper;
    private final String secretoBase;
    private final long ttlMinutos;
    private byte[] secreto;

    public AgendaPublicaTokenService(ObjectMapper mapper,
                                     @Value("${app.agenda-publica.secret:}") String secreto,
                                     @Value("${app.agenda-publica.ttl-minutes:15}") long ttlMinutos,
                                     @Value("${app.jwt.secret:little-smile}") String jwtSecret) {
        this.mapper = mapper;
        this.secretoBase = secreto == null || secreto.isBlank() ? jwtSecret : secreto;
        this.ttlMinutos = ttlMinutos <= 0 ? TTL_POR_DEFECTO : ttlMinutos;
    }

    @PostConstruct
    public void derivarSecreto() {
        try {
            String base = "agenda-publica:" + (secretoBase == null ? "" : secretoBase);
            this.secreto = MessageDigest.getInstance("SHA-256").digest(base.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo derivar el secreto de la agenda pública", ex);
        }
    }

    /** Payload mínimo del enlace: tenant, paciente, odontólogo, propósito y expiración (§11). */
    public record Payload(Long tenant, Long pacienteId, Long odontologoId, Long servicioId,
                          String proposito, long exp) {
        public boolean vencido() {
            return exp <= Instant.now().getEpochSecond();
        }
    }

    public String firmar(Long pacienteId, Long odontologoId, Long servicioId) {
        // Con paciente identificado el enlace también permite reservar; sin él,
        // sólo consulta disponibilidad (§13: reservar exige identificación).
        String proposito = pacienteId == null ? PROPOSITO_CONSULTA : PROPOSITO;
        return firmar(pacienteId, odontologoId, servicioId, proposito);
    }

    public String firmar(Long pacienteId, Long odontologoId, Long servicioId, String proposito) {
        Payload payload = new Payload(TenantContext.actualOrDefault(), pacienteId, odontologoId,
                servicioId, proposito, Instant.now().getEpochSecond() + ttlMinutos * 60);
        try {
            String cuerpo = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mapper.writeValueAsBytes(payload));
            return cuerpo + "." + firma(cuerpo);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo firmar el enlace de agenda", ex);
        }
    }

    /** Valida firma, propósito, expiración y tenant; vacío si el token no sirve (§17). */
    public Optional<Payload> validar(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String[] partes = token.split("\\.");
        if (partes.length != 2) {
            return Optional.empty();
        }
        byte[] esperada = firma(partes[0]).getBytes(StandardCharsets.UTF_8);
        byte[] recibida = partes[1].getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(esperada, recibida)) {
            return Optional.empty();
        }
        try {
            Payload payload = mapper.readValue(Base64.getUrlDecoder().decode(partes[0]), Payload.class);
            if (!esPropositoDeEnlace(payload.proposito()) || payload.vencido()) {
                return Optional.empty();
            }
            if (payload.tenant() != null && !payload.tenant().equals(TenantContext.actualOrDefault())) {
                return Optional.empty();
            }
            return Optional.of(payload);
        } catch (Exception ex) {
            log.warn("Token de agenda pública inválido: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    // ------------------------------------------------------------------
    // Slot tokens (§15): el navegador no manda fecha/hora "a ojo"
    // ------------------------------------------------------------------

    /** Payload de un cupo ofrecido en /slots: odontólogo, fecha y hora firmadas. */
    public record SlotPayload(Long tenant, Long odontologoId, String fecha, String hora,
                              String proposito, long exp) {
        public boolean vencido() {
            return exp <= Instant.now().getEpochSecond();
        }
    }

    public String firmarSlot(Long odontologoId, LocalDate fecha, LocalTime horaInicio) {
        SlotPayload payload = new SlotPayload(TenantContext.actualOrDefault(), odontologoId,
                fecha.toString(), horaInicio.toString(), PROPOSITO_SLOT,
                Instant.now().getEpochSecond() + ttlMinutos * 60);
        try {
            String cuerpo = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mapper.writeValueAsBytes(payload));
            return cuerpo + "." + firma(cuerpo);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo firmar el cupo de agenda", ex);
        }
    }

    /** Valida un cupo firmado; vacío si caducó, cambió de tenant o no es un slot. */
    public Optional<SlotPayload> validarSlot(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String[] partes = token.split("\\.");
        if (partes.length != 2) {
            return Optional.empty();
        }
        byte[] esperada = firma(partes[0]).getBytes(StandardCharsets.UTF_8);
        byte[] recibida = partes[1].getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(esperada, recibida)) {
            return Optional.empty();
        }
        try {
            SlotPayload payload = mapper.readValue(Base64.getUrlDecoder().decode(partes[0]),
                    SlotPayload.class);
            if (!PROPOSITO_SLOT.equals(payload.proposito()) || payload.vencido()) {
                return Optional.empty();
            }
            if (payload.tenant() != null && !payload.tenant().equals(TenantContext.actualOrDefault())) {
                return Optional.empty();
            }
            return Optional.of(payload);
        } catch (Exception ex) {
            log.warn("Cupo de agenda pública inválido: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private static boolean esPropositoDeEnlace(String proposito) {
        return PROPOSITO.equals(proposito) || PROPOSITO_CONSULTA.equals(proposito);
    }

    public long ttlMinutos() {
        return ttlMinutos;
    }

    private String firma(String cuerpo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto, "HmacSHA256"));
            byte[] hash = mac.doFinal(cuerpo.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo firmar el token de agenda", ex);
        }
    }
}
