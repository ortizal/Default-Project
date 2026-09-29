package org.dentalcrm.web.whatsapp;

import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.domain.whatsapp.EstadoSesionWhatsapp;
import org.dentalcrm.domain.whatsapp.WhatsappSesion;
import org.dentalcrm.domain.whatsapp.WhatsappSesionRepository;
import org.dentalcrm.integration.messaging.MessagingProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WhatsAppCampanaServiceTest {

    @Mock
    private PacienteRepository pacienteRepository;
    @Mock
    private WhatsappSesionRepository sesionRepository;
    @Mock
    private MessagingProvider provider;

    private WhatsAppCampanaService servicio;

    @BeforeEach
    void setUp() {
        servicio = new WhatsAppCampanaService(pacienteRepository, sesionRepository, provider);
        when(provider.estaConfigurado()).thenReturn(true);
        WhatsappSesion sesion = new WhatsappSesion();
        sesion.setSesionId("principal");
        when(sesionRepository.findByEstado(EstadoSesionWhatsapp.CONECTADA)).thenReturn(List.of(sesion));
        Paciente paciente = new Paciente();
        paciente.setNombres("Ana");
        paciente.setApellidos("Pérez");
        paciente.setTelefono("0999999999");
        when(pacienteRepository.activosConTelefono()).thenReturn(List.of(paciente));
    }

    @Test
    void enviaImagenComoImagen() {
        MockMultipartFile imagen = new MockMultipartFile("archivo", "promo.jpg", "image/jpeg", new byte[]{1, 2});

        servicio.enviar("Promoción", null, imagen);

        verify(provider).enviarImagen("principal", "0999999999", "promo.jpg", "image/jpeg",
                new byte[]{1, 2}, "Promoción");
        verify(provider, never()).enviarDocumento(anyString(), anyString(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void conservaEnvioDeDocumentos() {
        MockMultipartFile pdf = new MockMultipartFile("archivo", "promo.pdf", "application/pdf", new byte[]{3, 4});

        servicio.enviar("Promoción", null, pdf);

        verify(provider).enviarDocumento("principal", "0999999999", "promo.pdf", "application/pdf",
                new byte[]{3, 4}, "Promoción");
        verify(provider, never()).enviarImagen(anyString(), anyString(), anyString(), anyString(), any(), anyString());
    }
}