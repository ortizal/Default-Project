package org.dentalcrm.web.automatizacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

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

    private final JavaMailSenderImpl sender;
    private final String from;

    public CorreoService(@Value("${app.mail.host:}") String host,
                         @Value("${app.mail.port:587}") int port,
                         @Value("${app.mail.username:}") String username,
                         @Value("${app.mail.password:}") String password,
                         @Value("${app.mail.from:}") String from) {
        if (host == null || host.isBlank()) {
            this.sender = null;
            this.from = from == null ? "" : from;
            return;
        }
        JavaMailSenderImpl impl = new JavaMailSenderImpl();
        impl.setHost(host);
        impl.setPort(port > 0 ? port : 587);
        if (username != null && !username.isBlank()) {
            impl.setUsername(username);
        }
        if (password != null && !password.isBlank()) {
            impl.setPassword(password);
        }
        Properties props = impl.getJavaMailProperties();
        props.put("mail.smtp.auth", String.valueOf(username != null && !username.isBlank()));
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        this.sender = impl;
        this.from = from == null || from.isBlank() ? username : from;
        log.info("Correo SMTP configurado hacia {}:{} (from={})", host, impl.getPort(), this.from);
    }

    public boolean configurado() {
        return sender != null && from != null && !from.isBlank();
    }

    /**
     * @throws org.springframework.mail.MailException si el SMTP rechaza el envío
     */
    public void enviar(String para, String asunto, String cuerpo) {
        if (!configurado()) {
            throw new IllegalStateException("El correo no está configurado (falta app.mail.host o app.mail.from)");
        }
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(from);
        mensaje.setTo(para);
        mensaje.setSubject(asunto);
        mensaje.setText(cuerpo);
        sender.send(mensaje);
    }
}
