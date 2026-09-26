package org.dentalcrm.web.whatsapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.domain.whatsapp.*;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.integration.messaging.MessagingProvider;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.web.agente.AgenteConversacionalService;
import org.dentalcrm.web.whatsapp.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WhatsAppServiceTest {

    @Mock
    private WhatsappSesionRepository sesionRepository;
    @Mock
    private ConversacionRepository conversacionRepository;
    @Mock
    private MensajeRepository mensajeRepository;
    @Mock
    private PacienteRepository pacienteRepository;
    @Mock
    private MessagingProvider provider;
    @Mock
    private AuditService auditService;
    @Mock
    private FlujoWhatsAppService flujoWhatsApp;
    @Mock
    private AgenteConversacionalService agenteConversacionalService;

    private WhatsAppService servicio;

    @BeforeEach
    void setUp() {
        servicio = new WhatsAppService(sesionRepository, conversacionRepository,
                mensajeRepository, pacienteRepository, provider, auditService, flujoWhatsApp,
                agenteConversacionalService);
        lenient().when(provider.nombre()).thenReturn("OPENWA");
        lenient().when(mensajeRepository.ultimos(anyLong(), any(PageRequest.class))).thenReturn(List.of());
    }

    private WhatsappSesion sesion(Long id, String sesionId) {
        WhatsappSesion s = new WhatsappSesion();
        s.setId(id);
        s.setSesionId(sesionId);
        s.setNombre("Recepción");
        return s;
    }

    private Conversacion conversacion(Long id, WhatsappSesion sesion) {
        Conversacion c = new Conversacion();
        c.setId(id);
        c.setSesion(sesion);
        c.setTelefono("593999999999");
        c.setNombreContacto("Juan Pérez");
        return c;
    }

    @Test
    void crearSesionGuardaSesionDesconectada() {
        when(sesionRepository.existsBySesionId("principal")).thenReturn(false);

        WhatsAppSesionResponse r = servicio.crearSesion(new WhatsAppSesionRequest("principal", "Recepcion"));

        assertEquals("principal", r.sesionId());
        assertEquals("DESCONECTADA", r.estado());
        verify(sesionRepository).save(any(WhatsappSesion.class));
        verify(auditService).registrar(eq("CREAR_SESION"), eq("WHATSAPP"), eq("WHATSAPP_SESION"), isNull());
    }

    @Test
    void crearSesionRechazaDuplicada() {
        when(sesionRepository.existsBySesionId("principal")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> servicio.crearSesion(new WhatsAppSesionRequest("principal", "Recepcion")));

        assertEquals("SESION_YA_EXISTE", ex.getCode());
    }

    @Test
    void conectarLlamaProviderYQuedaConectada() {
        WhatsappSesion s = sesion(1L, "principal");
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(s));
        when(provider.estaConfigurado()).thenReturn(true);
        when(provider.iniciarSesion("principal"))
                .thenReturn(new MessagingProvider.ResultadoConexion("CONECTADA", "ok", "qr-mock", null));

        WhatsAppSesionResponse r = servicio.conectar(1L);

        assertEquals("CONECTADA", r.estado());
        assertEquals("qr-mock", r.qr());
        verify(provider).iniciarSesion("principal");
    }

    @Test
    void conectarCuandoProviderFallaMarcaErrorYNoLanza() {
        WhatsappSesion s = sesion(1L, "principal");
        when(sesionRepository.findById(1L)).thenReturn(Optional.of(s));
        when(provider.estaConfigurado()).thenReturn(true);
        when(provider.iniciarSesion("principal"))
                .thenThrow(new BusinessException("OPENWA_API_ERROR", "boom de red"));

        WhatsAppSesionResponse r = servicio.conectar(1L);

        assertEquals("ERROR", r.estado());
        assertTrue(r.lastError().contains("boom"));
    }

    @Test
    void enviarMensajeExitosoQuedaEnviado() {
        WhatsappSesion s = sesion(1L, "principal");
        Conversacion c = conversacion(1L, s);
        when(conversacionRepository.findById(1L)).thenReturn(Optional.of(c));
        when(provider.estaConfigurado()).thenReturn(true);
        when(mensajeRepository.save(any(Mensaje.class))).thenAnswer(a -> a.getArgument(0));
        when(conversacionRepository.save(any(Conversacion.class))).thenReturn(c);

        MensajeResponse r = servicio.enviarMensaje(1L, new EnviarMensajeRequest("Hola"));

        assertEquals("SALIDA", r.direccion());
        assertEquals("ENVIADO", r.estado());
        assertEquals("Hola", r.texto());
        verify(provider).enviarMensaje("principal", "593999999999", "Hola");
    }

    @Test
    void enviarMensajeConErrorConservaElMensajeEnError() {
        WhatsappSesion s = sesion(1L, "principal");
        Conversacion c = conversacion(1L, s);
        when(conversacionRepository.findById(1L)).thenReturn(Optional.of(c));
        when(provider.estaConfigurado()).thenReturn(true);
        doThrow(new BusinessException("OPENWA_API_ERROR", "sin conexión"))
                .when(provider).enviarMensaje(anyString(), anyString(), anyString());
        when(mensajeRepository.save(any(Mensaje.class))).thenAnswer(a -> a.getArgument(0));
        when(conversacionRepository.save(any(Conversacion.class))).thenReturn(c);

        MensajeResponse r = servicio.enviarMensaje(1L, new EnviarMensajeRequest("Hola"));

        assertEquals("ERROR", r.estado());
        assertTrue(r.error().contains("sin conexión"));
        assertEquals("Hola", r.texto());
    }

    @Test
    void webhookMensajeCreaConversacionSinVincularPacientePorTelefono() throws Exception {
        WhatsappSesion s = sesion(1L, "principal");
        when(sesionRepository.findBySesionId("principal")).thenReturn(Optional.of(s));
        when(conversacionRepository.findBySesionIdAndTelefono(1L, "593999999999"))
                .thenReturn(Optional.empty());
        when(conversacionRepository.save(any(Conversacion.class))).thenAnswer(a -> a.getArgument(0));
        when(mensajeRepository.save(any(Mensaje.class))).thenAnswer(a -> a.getArgument(0));

        ObjectMapper mapper = new ObjectMapper();
        WebhookResponse r = servicio.recibirWebhook(mapper.readTree("""
                {"event":"message","session":"principal","data":{
                    "from":"593999999999@c.us","body":"Hola, quiero una cita","notifyName":"Juan"}}"""));

        assertTrue(r.recibido());
        verify(conversacionRepository, atLeastOnce()).save(argThat(c ->
            "593999999999".equals(c.getTelefono()) && c.getPaciente() == null
                        && c.getEstado() == EstadoConversacion.BOT));
        verify(mensajeRepository).save(argThat(m ->
                m.getDireccion() == DireccionMensaje.ENTRADA
                        && "Hola, quiero una cita".equals(m.getTexto())
                        && m.getEstado() == EstadoMensaje.RECIBIDO));
        verify(auditService).registrar(eq("RECIBIR_MENSAJE"), eq("WHATSAPP"), eq("CONVERSACION"), isNull());
    }

    @Test
    void webhookResuelveSesionTolerandoMayusculasYSeparadores() throws Exception {
        WhatsappSesion s = sesion(1L, "ALANTEK_DENTAL");
        when(sesionRepository.findBySesionId("alantek-dental")).thenReturn(Optional.empty());
        lenient().when(provider.normalizarSesion(anyString())).thenAnswer(inv ->
                inv.getArgument(0).toString()
                        .toLowerCase()
                        .replaceAll("[^a-z0-9-]", "-"));
        when(sesionRepository.findAll()).thenReturn(List.of(s));
        when(conversacionRepository.findBySesionIdAndTelefono(1L, "593987269948"))
                .thenReturn(Optional.empty());
        when(conversacionRepository.save(any(Conversacion.class))).thenAnswer(a -> a.getArgument(0));
        when(mensajeRepository.save(any(Mensaje.class))).thenAnswer(a -> a.getArgument(0));
        ObjectMapper mapper = new ObjectMapper();
        WebhookResponse r = servicio.recibirWebhook(mapper.readTree("""
                {"event":"message","session":"alantek-dental","data":{
                    "from":"593987269948@c.us","body":"Hola","notifyName":"NuevoNumero"}}"""));

        assertTrue(r.recibido());
        verify(conversacionRepository, atLeastOnce()).save(argThat(c ->
                "593987269948".equals(c.getTelefono())));
        verify(auditService).registrar(eq("RECIBIR_MENSAJE"), eq("WHATSAPP"), eq("CONVERSACION"), isNull());
    }

    @Test
    void webhookIgnoraEventosQueNoSonMensaje() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        WebhookResponse r = servicio.recibirWebhook(mapper.readTree(
                "{\"event\":\"connection.update\",\"session\":\"principal\"}"));

        assertTrue(r.recibido());
        assertNull(r.conversacionId());
        verifyNoInteractions(sesionRepository);
        verifyNoInteractions(mensajeRepository);
    }

    @Test
    void webhookConRespuestaConAdjuntoEnviaDocumento() throws Exception {
        WhatsappSesion s = sesion(1L, "principal");
        Conversacion c = conversacion(1L, s);
        c.setEstado(EstadoConversacion.BOT);
        c.setIntencion(org.dentalcrm.domain.agente.Intencion.PREGUNTAR_DISPONIBILIDAD);
        when(sesionRepository.findBySesionId("principal")).thenReturn(Optional.of(s));
        when(conversacionRepository.findBySesionIdAndTelefono(1L, "593999999999"))
                .thenReturn(Optional.of(c));
        when(conversacionRepository.findById(1L)).thenReturn(Optional.of(c));
        when(conversacionRepository.save(any(Conversacion.class))).thenReturn(c);
        when(mensajeRepository.save(any(Mensaje.class))).thenAnswer(a -> a.getArgument(0));
        when(provider.estaConfigurado()).thenReturn(true);
        when(agenteConversacionalService.procesar(any(Conversacion.class), anyString(), any()))
                .thenReturn(new AgenteConversacionalService.RespuestaAgente("Tu horario",
                        false, new AgenteConversacionalService.Adjunto(
                        "horario-perez.pdf", "application/pdf", new byte[]{1, 2, 3}, "Horario")));

        ObjectMapper mapper = new ObjectMapper();
        WebhookResponse r = servicio.recibirWebhook(mapper.readTree("""
                {"event":"message","session":"principal","data":{
                    "from":"593999999999@c.us","body":"cuando atiende","notifyName":"Juan"}}"""));

        assertTrue(r.recibido());
        verify(provider).enviarDocumento(eq("principal"), eq("593999999999"), eq("horario-perez.pdf"),
                eq("application/pdf"), eq(new byte[]{1, 2, 3}), eq("Horario"));
    }

    @Test
    void cambiarEstadoConversacionActualizaEstado() {
        WhatsappSesion s = sesion(1L, "principal");
        Conversacion c = conversacion(1L, s);
        when(conversacionRepository.findById(1L)).thenReturn(Optional.of(c));
        when(conversacionRepository.save(any(Conversacion.class))).thenReturn(c);

        ConversacionResponse r = servicio.cambiarEstado(1L, new CambiarEstadoConversacionRequest("ATENCION_HUMANA"));

        assertEquals("ATENCION_HUMANA", r.estado());
        assertEquals(EstadoConversacion.ATENCION_HUMANA, c.getEstado());
    }
}