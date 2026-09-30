package org.dentalcrm.web.automatizacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.dentalcrm.web.configuracion.IntegracionesConfiguracionService;

import java.util.Properties;

/**
 * Envío de correo por SMTP.
 *
 * <p>Todo es opcional: si no hay host configurado ({@code app.mail.host} / variable
 * de entorno {@code MAIL_HOST}) no hay remitente y {@link #configurado()} devuelve
 * {@code false}, de modo que el sistema sigue funcionando sólo con WhatsApp.
 */
@Service
public class CorreoService {

    private static final Logger log = LoggerFactory.getLogger(CorreoService.class);

    private final IntegracionesConfiguracionService configuracionService;

    public CorreoService(IntegracionesConfiguracionService configuracionService) {
        this.configuracionService = configuracionService;
    }

    public boolean configurado() {
        var config = configuracionService.correo();
        String from = remitente(config);
        return config.host() != null && !config.host().isBlank() && from != null && !from.isBlank();
    }

    /**
     * @throws org.springframework.mail.MailException si el SMTP rechaza el envío
     */
    public void enviar(String para, String asunto, String cuerpo) {
        var config = configuracionService.correo();
        String from = remitente(config);
        if (config.host() == null || config.host().isBlank() || from == null || from.isBlank()) {
            throw new IllegalStateException("El correo no está configurado (falta app.mail.host o app.mail.from)");
        }
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.host());
        sender.setPort(config.port() > 0 ? config.port() : 587);
        if (config.username() != null && !config.username().isBlank()) sender.setUsername(config.username());
        if (config.password() != null && !config.password().isBlank()) sender.setPassword(config.password());
        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", String.valueOf(config.username() != null && !config.username().isBlank()));
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(from);
        mensaje.setTo(para);
        mensaje.setSubject(asunto);
        mensaje.setText(cuerpo);
        sender.send(mensaje);
        log.info("Correo SMTP enviado a {} por {}:{}", para, config.host(), sender.getPort());
    }

    private String remitente(IntegracionesConfiguracionService.MailRuntime config) {
        return config.from() == null || config.from().isBlank() ? config.username() : config.from();
    }
}
