package org.dentalcrm.web.whatsapp;

import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.domain.whatsapp.EstadoSesionWhatsapp;
import org.dentalcrm.domain.whatsapp.WhatsappSesion;
import org.dentalcrm.domain.whatsapp.WhatsappSesionRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.integration.messaging.MessagingProvider;
import org.dentalcrm.util.TelefonoUtil;
import org.dentalcrm.web.whatsapp.dto.CampanaResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WhatsAppCampanaService {

    private static final int MAX_DESTINATARIOS = 500;

    private final PacienteRepository pacienteRepository;
    private final WhatsappSesionRepository sesionRepository;
    private final MessagingProvider provider;

    public WhatsAppCampanaService(PacienteRepository pacienteRepository,
                                  WhatsappSesionRepository sesionRepository,
                                  MessagingProvider provider) {
        this.pacienteRepository = pacienteRepository;
        this.sesionRepository = sesionRepository;
        this.provider = provider;
    }

    public CampanaResponse enviar(String texto, String enlace, MultipartFile archivo) {
        if (!provider.estaConfigurado()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "WHATSAPP_NO_CONFIGURADO",
                    "WhatsApp no está configurado");
        }
        if ((texto == null || texto.isBlank()) && (enlace == null || enlace.isBlank())
                && (archivo == null || archivo.isEmpty())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "CAMPANA_VACIA",
                    "Agrega un texto, enlace, imagen o video");
        }
        WhatsappSesion sesion = sesionRepository.findByEstado(EstadoSesionWhatsapp.CONECTADA)
                .stream().findFirst()
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "SESION_NO_CONECTADA",
                        "No hay una sesión de WhatsApp conectada"));
        List<Paciente> destinatarios = destinatariosUnicosPorTelefono(pacienteRepository.activosConTelefono());
        if (destinatarios.size() > MAX_DESTINATARIOS) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "CAMPANA_DEMASIADO_GRANDE",
                    "La campaña supera el límite de " + MAX_DESTINATARIOS + " destinatarios");
        }

        String mensaje = unirContenido(texto, enlace);
        String nombreArchivo = archivo == null ? null : archivo.getOriginalFilename();
        String mime = archivo == null ? null : archivo.getContentType();
        byte[] contenido = null;
        try {
            if (archivo != null && !archivo.isEmpty()) {
                contenido = archivo.getBytes();
            }
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "ARCHIVO_INVALIDO", "No se pudo leer el archivo");
        }

        List<CampanaResponse.ResultadoDestinatario> resultados = new ArrayList<>();
        int enviados = 0;
        for (Paciente paciente : destinatarios) {
            String nombre = (paciente.getNombres() + " " + paciente.getApellidos()).trim();
            try {
                if (contenido != null) {
                    provider.enviarDocumento(sesion.getSesionId(), paciente.getTelefono(), nombreArchivo,
                            mime, contenido, mensaje);
                } else {
                    provider.enviarMensaje(sesion.getSesionId(), paciente.getTelefono(), mensaje);
                }
                enviados++;
                resultados.add(new CampanaResponse.ResultadoDestinatario(nombre, paciente.getTelefono(), true, null));
            } catch (Exception e) {
                resultados.add(new CampanaResponse.ResultadoDestinatario(nombre, paciente.getTelefono(), false,
                        truncar(e.getMessage())));
            }
        }
        return new CampanaResponse(destinatarios.size(), enviados, destinatarios.size() - enviados, resultados);
    }

    private List<Paciente> destinatariosUnicosPorTelefono(List<Paciente> pacientes) {
        Map<String, Paciente> unicos = new LinkedHashMap<>();
        for (Paciente paciente : pacientes) {
            String clave = claveTelefono(paciente.getTelefono());
            if (!clave.isBlank()) {
                unicos.putIfAbsent(clave, paciente);
            }
        }
        return new ArrayList<>(unicos.values());
    }

    private String claveTelefono(String telefono) {
        String e164 = TelefonoUtil.aE164(telefono);
        return e164 == null ? "" : e164.replaceAll("\\D", "");
    }

    private String unirContenido(String texto, String enlace) {
        String a = texto == null ? "" : texto.trim();
        String b = enlace == null ? "" : enlace.trim();
        if (a.isEmpty()) return b;
        if (b.isEmpty()) return a;
        return a + "\n" + b;
    }

    private String truncar(String valor) {
        if (valor == null) return "Error desconocido";
        return valor.length() <= 300 ? valor : valor.substring(0, 300);
    }
}