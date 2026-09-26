package org.dentalcrm.web.agente;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dentalcrm.domain.agente.Intencion;
import org.dentalcrm.domain.cita.Cita;
import org.dentalcrm.domain.cita.CitaRepository;
import org.dentalcrm.domain.cita.EstadoCita;
import org.dentalcrm.domain.horario.HorarioOdontologo;
import org.dentalcrm.domain.horario.HorarioOdontologoRepository;
import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.odontologo.OdontologoRepository;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.domain.servicio.Servicio;
import org.dentalcrm.domain.servicio.ServicioRepository;
import org.dentalcrm.domain.whatsapp.Conversacion;
import org.dentalcrm.domain.whatsapp.ConversacionRepository;
import org.dentalcrm.domain.whatsapp.EstadoConversacion;
import org.dentalcrm.domain.whatsapp.MensajeRepository;
import org.dentalcrm.domain.whatsapp.WhatsappSesion;
import org.dentalcrm.web.agenda.AgendaService;
import org.dentalcrm.web.agenda.HorarioPdfService;
import org.dentalcrm.web.agenda.dto.SlotResponse;
import org.dentalcrm.web.agente.dto.AgenteConversacionResponse;
import org.dentalcrm.web.cita.CitaService;
import org.dentalcrm.web.cita.dto.CancelarRequest;
import org.dentalcrm.web.cita.dto.CitaRequest;
import org.dentalcrm.web.cita.dto.CitaResponse;
import org.dentalcrm.web.google.GoogleService;
import org.dentalcrm.web.paciente.PacienteService;
import org.dentalcrm.web.paciente.dto.PacienteRequest;
import org.dentalcrm.web.paciente.dto.PacienteResponse;
import org.dentalcrm.web.paciente.dto.TutorRequest;
import org.dentalcrm.web.paciente.dto.TutorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgenteConversacionalServiceTest {

    @Mock
    private ConversacionRepository conversacionRepository;
    @Mock
    private MensajeRepository mensajeRepository;
    @Mock
    private PacienteRepository pacienteRepository;
    @Mock
    private CitaRepository citaRepository;
    @Mock
    private CitaService citaService;
    @Mock
    private AgendaService agendaService;
    @Mock
    private ServicioRepository servicioRepository;
    @Mock
    private OdontologoRepository odontologoRepository;
    @Mock
    private HorarioOdontologoRepository horarioRepository;
    @Mock
    private HorarioPdfService horarioPdfService;
    @Mock
    private PacienteService pacienteService;
    @Mock
    private GoogleService googleService;

    private AgenteConversacionalService servicio;

    private final LocalDate hoy = LocalDate.now();
    private final LocalDate manana = hoy.plusDays(1);

    @BeforeEach
    void setUp() {
        servicio = new AgenteConversacionalService(conversacionRepository, mensajeRepository,
                pacienteRepository, citaRepository, citaService, agendaService,
                servicioRepository, odontologoRepository, horarioRepository, horarioPdfService,
                pacienteService, googleService, new ObjectMapper(), "America/Guayaquil");
        lenient().when(googleService.estaConfigurado()).thenReturn(false);
        lenient().when(mensajeRepository.ultimos(anyLong(), any()))
                .thenReturn(List.of());
        lenient().when(conversacionRepository.save(any(Conversacion.class)))
                .thenAnswer(a -> a.getArgument(0));
        lenient().when(horarioPdfService.nombreArchivo(any(Odontologo.class))).thenReturn("horario-perez.pdf");
    }

    // ------------------------------------------------------------------

    private Servicio servicioLimpieza() {
        Servicio s = new Servicio();
        s.setId(1L);
        s.setNombre("Limpieza Dental");
        s.setDuracionMinutos(45);
        s.setPrecio(new BigDecimal("50.00"));
        s.setEstado("ACTIVO");
        return s;
    }

    private Odontologo doctorPerez() {
        Odontologo o = new Odontologo();
        o.setId(10L);
        o.setNombres("Carlos");
        o.setApellidos("Perez");
        o.setEspecialidad("Odontología General");
        o.setEstado("ACTIVO");
        return o;
    }

    private Paciente paciente() {
        Paciente p = new Paciente();
        p.setId(7L);
        p.setNombres("Maria");
        p.setApellidos("Lopez");
        p.setCedula("1711111111");
        p.setEstado("ACTIVO");
        return p;
    }

    private Cita citaProxima() {
        Cita c = new Cita();
        c.setId(42L);
        c.setPaciente(paciente());
        c.setDoctor(doctorPerez());
        c.setServicio(servicioLimpieza());
        c.setFecha(manana);
        c.setHoraInicio(LocalTime.of(10, 0));
        c.setHoraFin(LocalTime.of(10, 45));
        c.setEstado(EstadoCita.PENDIENTE);
        c.setConfirmada(false);
        c.setVersion(0L);
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        return c;
    }

    private Conversacion conversacion() {
        Conversacion c = new Conversacion();
        c.setId(1L);
        WhatsappSesion s = new WhatsappSesion();
        s.setId(5L);
        c.setSesion(s);
        c.setTelefono("593999999999");
        c.setEstado(EstadoConversacion.BOT);
        return c;
    }

    // ------------------------------------------------------------------

    @Test
    void saludoRespondeConMenuYGuardaIntencion() {
        Conversacion conv = conversacion();
        lenient().when(servicioRepository.findByEstadoOrderByNombreAsc("ACTIVO"))
                .thenReturn(List.of(servicioLimpieza()));
        lenient().when(odontologoRepository.findTop5ByEstadoOrderByNombresAsc("ACTIVO"))
                .thenReturn(List.of(doctorPerez()));

        AgenteConversacionalService.RespuestaAgente r = servicio.procesar(conv, "hola", null);

        assertTrue(r.mensaje().contains("asistente virtual"));
        assertEquals(Intencion.SALUDO, conv.getIntencion());
    }

    @Test
    void sinPacientePideCedulaYLaRegistra() {
        Conversacion conv = conversacion();
        Paciente p = paciente();

        AgenteConversacionalService.RespuestaAgente primero = servicio.procesar(conv, "quiero una cita", null);

        assertTrue(primero.mensaje().contains("cédula"));
        assertEquals(Intencion.REGISTRAR_PACIENTE, conv.getIntencion());

        when(pacienteRepository.findByCedulaIgnoreCase("1711111111")).thenReturn(Optional.of(p));
        AgenteConversacionalService.RespuestaAgente segundo = servicio.procesar(conv, "1711111111", null);

        assertTrue(segundo.mensaje().contains("Maria"));
        assertEquals(p, conv.getPaciente());
        assertEquals(Intencion.VER_PERFIL, conv.getIntencion());
    }

    @Test
    void cedulaNoEncontradaOfreceRegistrarNuevoPaciente() {
        Conversacion conv = conversacion();
        servicio.procesar(conv, "quiero una cita", null);

        when(pacienteRepository.findByCedulaIgnoreCase("0000000000")).thenReturn(Optional.empty());
        AgenteConversacionalService.RespuestaAgente r = servicio.procesar(conv, "0000000000", null);

        assertTrue(r.mensaje().contains("¿Quieres que cree tu ficha"));
        assertEquals(Intencion.REGISTRAR_PACIENTE, conv.getIntencion());
        assertTrue(conv.getContextoAgente().contains("REG_CONFIRMAR"));
        assertFalse(r.transferirHumano());
    }

        @Test
        void nuevaCedulaCambiaElPacienteAunqueHabiaUnFlujoActivo() {
                Conversacion conv = conversacion();
                conv.setPaciente(paciente());
                conv.setIntencion(Intencion.AGENDAR_CITA);
                conv.setContextoAgente("{\"paso\":\"HORA\",\"fecha\":\"2026-09-15\"}");
                Paciente otro = paciente();
                otro.setId(8L);
                otro.setNombres("Carlos");
                otro.setApellidos("Vega");
                otro.setCedula("1722222222");
                when(pacienteRepository.findByCedulaIgnoreCase("1722222222")).thenReturn(Optional.of(otro));

                AgenteConversacionalService.RespuestaAgente r = servicio.procesar(conv, "1722222222", null);

                assertEquals(otro, conv.getPaciente());
                assertEquals(Intencion.VER_PERFIL, conv.getIntencion());
                assertNull(conv.getContextoAgente());
                assertTrue(r.mensaje().contains("Carlos Vega"));
        }

    @Test
    void registroCompletoPorChatCreaPacienteAdulto() {
        Conversacion conv = conversacion();
        servicio.procesar(conv, "quiero una cita", null);
        when(pacienteRepository.findByCedulaIgnoreCase("0000000000")).thenReturn(Optional.empty());

        AgenteConversacionalService.RespuestaAgente confirmar = servicio.procesar(conv, "0000000000", null);
        assertTrue(confirmar.mensaje().contains("¿Quieres que cree tu ficha"));

        AgenteConversacionalService.RespuestaAgente nombre = servicio.procesar(conv, "si", null);
        assertTrue(nombre.mensaje().contains("¿Cuál es tu nombre"));

        AgenteConversacionalService.RespuestaAgente apellidos = servicio.procesar(conv, "Ana", null);
        assertTrue(apellidos.mensaje().contains("apellidos"));

        AgenteConversacionalService.RespuestaAgente fecha = servicio.procesar(conv, "Gomez", null);
        assertTrue(fecha.mensaje().contains("fecha de nacimiento"));

        AgenteConversacionalService.RespuestaAgente resumen = servicio.procesar(conv, "15/08/1990", null);
        assertTrue(resumen.mensaje().contains("Revisa tus datos"));
        assertTrue(resumen.mensaje().contains("15/08/1990"));
        assertTrue(resumen.mensaje().contains("¿Confirmas tus datos?"));

        Paciente creado = paciente();
        creado.setId(9L);
        creado.setCedula("0000000000");
        creado.setNombres("Ana");
        creado.setApellidos("Gomez");
        PacienteResponse resp = new PacienteResponse(9L, "0000000000", "Ana", "Gomez",
                "593999999999", null, LocalDate.of(1990, 8, 15), null, null,
                "Registrado por WhatsApp", "ACTIVO", Instant.now(), Instant.now(), List.of());
        when(pacienteService.crear(any(PacienteRequest.class))).thenReturn(resp);
        when(pacienteRepository.findById(9L)).thenReturn(Optional.of(creado));

        AgenteConversacionalService.RespuestaAgente listo = servicio.procesar(conv, "si", null);

        assertTrue(listo.mensaje().contains("registré"));
        assertEquals(Intencion.AGENDAR_CITA, conv.getIntencion());
        assertEquals(9L, conv.getPaciente().getId());
        ArgumentCaptor<PacienteRequest> captor = ArgumentCaptor.forClass(PacienteRequest.class);
        verify(pacienteService).crear(captor.capture());
        assertEquals("0000000000", captor.getValue().cedula());
        assertEquals("Ana", captor.getValue().nombres());
        assertEquals("Gomez", captor.getValue().apellidos());
        assertEquals("593999999999", captor.getValue().telefono());
        assertEquals(LocalDate.of(1990, 8, 15), captor.getValue().fechaNacimiento());
        assertTrue(captor.getValue().tutores().isEmpty());
    }

    @Test
    void registroDeMenorIncluyeTutor() {
        Conversacion conv = conversacion();
        conv.setIntencion(Intencion.REGISTRAR_PACIENTE);
        conv.setContextoAgente("{\"paso\":\"REG_NOMBRES\"}");

        servicio.procesar(conv, "Sofia", null);
        servicio.procesar(conv, "Rios", null);

        AgenteConversacionalService.RespuestaAgente tutor = servicio.procesar(conv, "12/03/2016", null);
        assertTrue(tutor.mensaje().contains("menor de edad"));
        assertTrue(tutor.mensaje().contains("padre"));

        AgenteConversacionalService.RespuestaAgente parentesco = servicio.procesar(conv, "madre", null);
        assertTrue(parentesco.mensaje().contains("nombre"));

        AgenteConversacionalService.RespuestaAgente nombreTutor = servicio.procesar(conv, "Laura", null);
        assertTrue(nombreTutor.mensaje().contains("apellidos del tutor"));

        AgenteConversacionalService.RespuestaAgente resumen = servicio.procesar(conv, "Rios", null);
        assertTrue(resumen.mensaje().contains("Tutor"));
        assertTrue(resumen.mensaje().contains("Laura"));

        Paciente creado = paciente();
        creado.setId(9L);
        creado.setNombres("Sofia");
        creado.setApellidos("Rios");
        PacienteResponse resp = new PacienteResponse(9L, null, "Sofia", "Rios",
                "593999999999", null, LocalDate.of(2016, 3, 12), null, null,
                "Registrado por WhatsApp", "ACTIVO", Instant.now(), Instant.now(),
                List.of(new TutorResponse(1L, "MADRE", "Laura", "Rios", null)));
        when(pacienteService.crear(any(PacienteRequest.class))).thenReturn(resp);
        when(pacienteRepository.findById(9L)).thenReturn(Optional.of(creado));

        AgenteConversacionalService.RespuestaAgente listo = servicio.procesar(conv, "si", null);

        assertTrue(listo.mensaje().contains("registré"));
        assertTrue(listo.mensaje().contains("tutor"));
        ArgumentCaptor<PacienteRequest> captor = ArgumentCaptor.forClass(PacienteRequest.class);
        verify(pacienteService).crear(captor.capture());
        assertNull(captor.getValue().cedula());
        assertEquals(LocalDate.of(2016, 3, 12), captor.getValue().fechaNacimiento());
        assertEquals(1, captor.getValue().tutores().size());
        assertEquals("MADRE", captor.getValue().tutores().get(0).parentesco());
        assertEquals("Laura", captor.getValue().tutores().get(0).nombres());
        assertEquals("Rios", captor.getValue().tutores().get(0).apellidos());
    }

    @Test
    void palabraRegistrarmeIniciaRegistroSinCedula() {
        Conversacion conv = conversacion();

        AgenteConversacionalService.RespuestaAgente r = servicio.procesar(conv, "me quiero registrar", null);

        assertTrue(r.mensaje().contains("¿Cuál es tu nombre"));
        assertEquals(Intencion.REGISTRAR_PACIENTE, conv.getIntencion());
        assertTrue(conv.getContextoAgente().contains("REG_NOMBRES"));
    }

    @Test
    void agendamientoNaturalCompletoCreaCitaConConfirmaCion() {
        Conversacion conv = conversacion();
        conv.setPaciente(paciente());
        when(servicioRepository.findByEstadoOrderByNombreAsc("ACTIVO"))
                .thenReturn(List.of(servicioLimpieza()));
        when(odontologoRepository.findTop5ByEstadoOrderByNombresAsc("ACTIVO"))
                .thenReturn(List.of(doctorPerez()));
        when(agendaService.disponibilidad(eq(10L), eq(manana), eq(1L), isNull()))
                .thenReturn(List.of(new SlotResponse(LocalTime.of(10, 0), LocalTime.of(10, 45))));

        AgenteConversacionalService.RespuestaAgente propuesta =
                servicio.procesar(conv, "una cita de limpieza para mañana a las 10 con el doctor perez", null);

        assertTrue(propuesta.mensaje().contains("¿Confirmo tu cita?"));
        assertEquals(Intencion.AGENDAR_CITA, conv.getIntencion());
        assertNotNull(conv.getContextoAgente());

        when(citaService.crear(any(CitaRequest.class))).thenReturn(CitaResponse.from(citaProxima()));
        AgenteConversacionalService.RespuestaAgente confirmada = servicio.procesar(conv, "si", null);

        assertTrue(confirmada.mensaje().contains("quedó agendada"));
        ArgumentCaptor<CitaRequest> captor = ArgumentCaptor.forClass(CitaRequest.class);
        verify(citaService).crear(captor.capture());
        assertEquals(7L, captor.getValue().pacienteId());
        assertEquals(10L, captor.getValue().doctorId());
        assertEquals(1L, captor.getValue().servicioId());
        assertEquals(manana, captor.getValue().fecha());
        assertEquals(LocalTime.of(10, 0), captor.getValue().horaInicio());
        assertNull(conv.getContextoAgente());
    }

    @Test
    void agendamientoPideServicioCuandoFalta() {
        Conversacion conv = conversacion();
        conv.setPaciente(paciente());
        when(servicioRepository.findByEstadoOrderByNombreAsc("ACTIVO"))
                .thenReturn(List.of(servicioLimpieza()));
        when(odontologoRepository.findTop5ByEstadoOrderByNombresAsc("ACTIVO"))
                .thenReturn(List.of(doctorPerez()));

        AgenteConversacionalService.RespuestaAgente r = servicio.procesar(conv, "quiero una cita", null);

        assertTrue(r.mensaje().contains("¿Qué servicio"));
        assertTrue(conv.getContextoAgente().contains("SERVICIO"));
    }

    @Test
    void confirmarCitaConfirmadaViaWhatsApp() {
        Conversacion conv = conversacion();
        conv.setPaciente(paciente());
        conv.setIntencion(Intencion.SALUDO);
        when(citaRepository.proximasDelPaciente(7L, hoy)).thenReturn(List.of(citaProxima()));
        when(citaService.confirmar(42L, "WHATSAPP")).thenReturn(CitaResponse.from(citaProxima()));

        AgenteConversacionalService.RespuestaAgente r = servicio.procesar(conv, "quiero confirmar", null);

        assertTrue(r.mensaje().toLowerCase().contains("confirmé"));
        verify(citaService).confirmar(42L, "WHATSAPP");
    }

    @Test
    void transferenciaAHumanoPorPeticion() {
        Conversacion conv = conversacion();

        AgenteConversacionalService.RespuestaAgente r = servicio.procesar(conv, "quiero hablar con una persona", null);

        assertTrue(r.transferirHumano());
        assertEquals(EstadoConversacion.ATENCION_HUMANA, conv.getEstado());
        assertEquals(Intencion.TRANSFERIR_HUMANO, conv.getIntencion());
    }

    @Test
    void inactividadDeMasDeDosMinutosReiniciaConSaludo() {
        Conversacion conv = conversacion();
        conv.setIntencion(Intencion.AGENDAR_CITA);
        conv.setContextoAgente("{\"paso\":\"FECHA\"}");
        Instant haceTresMinutos = Instant.now().minusSeconds(180);

        AgenteConversacionalService.RespuestaAgente r = servicio.procesar(conv, "mañana a las 10", haceTresMinutos);

        assertTrue(r.mensaje().contains("Reinicié"));
        assertNull(conv.getIntencion());
        assertNull(conv.getContextoAgente());
    }

    @Test
    void inactividadRecienteNoReinicia() {
        Conversacion conv = conversacion();
        conv.setIntencion(Intencion.AGENDAR_CITA);
        Instant haceUnMinuto = Instant.now().minusSeconds(60);

        AgenteConversacionalService.RespuestaAgente r = servicio.procesar(conv, "mañana a las 10", haceUnMinuto);

        assertFalse(r.mensaje().contains("Reinicié"));
        assertEquals(Intencion.REGISTRAR_PACIENTE, conv.getIntencion());
    }

    @Test
    void palabraReiniciarLimpiaContextoYSaluda() {
        Conversacion conv = conversacion();
        conv.setPaciente(paciente());
        conv.setIntencion(Intencion.AGENDAR_CITA);
        conv.setContextoAgente("{\"paso\":\"CREAR\"}");

        AgenteConversacionalService.RespuestaAgente r = servicio.procesar(conv, "reiniciar", null);

        assertTrue(r.mensaje().contains("Reinicié"));
        assertNull(conv.getIntencion());
        assertNull(conv.getContextoAgente());
        assertFalse(r.transferirHumano());
    }

    @Test
    void transferirDesdeElPanelYDesactivarAgente() {
        Conversacion conv = conversacion();
        when(conversacionRepository.findById(1L)).thenReturn(Optional.of(conv));
        when(conversacionRepository.save(any(Conversacion.class))).thenReturn(conv);

        AgenteConversacionResponse transferida = servicio.transferirAHumano(1L);
        assertEquals("ATENCION_HUMANA", transferida.estado());
        assertEquals("TRANSFERIR_HUMANO", transferida.intencion());

        AgenteConversacionResponse desactivada = servicio.activarAgente(1L, false);
        assertFalse(desactivada.agenteActivo());
        assertEquals("ATENCION_HUMANA", desactivada.estado());
    }

    // ------------------------------------------------------------------

    private HorarioOdontologo horario(int dia, LocalTime inicio, LocalTime fin) {
        HorarioOdontologo h = new HorarioOdontologo();
        h.setId((long) dia);
        h.setDiaSemana(dia);
        h.setHoraInicio(inicio);
        h.setHoraFin(fin);
        h.setIntervaloMinutos(60);
        h.setEstado("ACTIVO");
        h.setCreatedAt(Instant.now());
        h.setUpdatedAt(Instant.now());
        return h;
    }

    private Odontologo doctorOrtodoncia() {
        Odontologo o = new Odontologo();
        o.setId(20L);
        o.setNombres("Ana");
        o.setApellidos("Ruiz");
        o.setEspecialidad("Ortodoncia");
        o.setEtiquetas("ortodoncia, ninos");
        o.setEstado("ACTIVO");
        return o;
    }

    @Test
    void consultaGeneralDeDiasYHorariosRespondeSemanaConPdf() {
        Conversacion conv = conversacion();
        conv.setPaciente(paciente());
        when(servicioRepository.findByEstadoOrderByNombreAsc("ACTIVO"))
                .thenReturn(List.of(servicioLimpieza()));
        when(odontologoRepository.findTop5ByEstadoOrderByNombresAsc("ACTIVO"))
                .thenReturn(List.of(doctorPerez()));
        when(odontologoRepository.findById(10L)).thenReturn(Optional.of(doctorPerez()));
        when(horarioRepository.findByOdontologoIdOrderByDiaSemanaAscHoraInicioAsc(10L))
                .thenReturn(List.of(horario(1, LocalTime.of(8, 0), LocalTime.of(17, 0)),
                        horario(2, LocalTime.of(8, 0), LocalTime.of(17, 0)),
                        horario(3, LocalTime.of(8, 0), LocalTime.of(17, 0)),
                        horario(4, LocalTime.of(8, 0), LocalTime.of(17, 0)),
                        horario(5, LocalTime.of(8, 0), LocalTime.of(17, 0))));
        when(agendaService.disponibilidad(eq(10L), any(LocalDate.class), any(), isNull()))
                .thenReturn(List.of(new SlotResponse(LocalTime.of(9, 0), LocalTime.of(9, 45))));
        when(horarioPdfService.generar(any(Odontologo.class), any(), any(), any()))
                .thenReturn(new byte[]{(byte) '%', (byte) 'P', (byte) 'D', (byte) 'F'});

        AgenteConversacionalService.RespuestaAgente r = servicio.procesar(conv, "cuando atiende el doctor perez", null);

        assertTrue(r.mensaje().contains("atiende"));
        assertTrue(r.mensaje().contains("lunes a viernes"));
        assertTrue(r.mensaje().contains("08:00 a 17:00"));
        assertNotNull(r.adjunto());
        assertEquals("application/pdf", r.adjunto().mime());
        assertEquals("horario-perez.pdf", r.adjunto().nombreArchivo());
        assertEquals(Intencion.PREGUNTAR_DISPONIBILIDAD, conv.getIntencion());
    }

    @Test
    void disponibilidadDelDoctorTienePrioridadSobreFlujoPendiente() {
        Conversacion conv = conversacion();
        conv.setPaciente(paciente());
        conv.setIntencion(Intencion.AGENDAR_CITA);
        conv.setContextoAgente("{\"paso\":\"SERVICIO\"}");
        when(servicioRepository.findByEstadoOrderByNombreAsc("ACTIVO"))
                .thenReturn(List.of(servicioLimpieza()));
        when(odontologoRepository.findTop5ByEstadoOrderByNombresAsc("ACTIVO"))
                .thenReturn(List.of(doctorPerez()));
        when(odontologoRepository.findById(10L)).thenReturn(Optional.of(doctorPerez()));
        when(horarioRepository.findByOdontologoIdOrderByDiaSemanaAscHoraInicioAsc(10L))
                .thenReturn(List.of(horario(1, LocalTime.of(8, 0), LocalTime.of(17, 0))));
        when(agendaService.disponibilidad(eq(10L), any(LocalDate.class), any(), isNull()))
                .thenReturn(List.of(new SlotResponse(LocalTime.of(9, 0), LocalTime.of(9, 45))));
        when(horarioPdfService.generar(any(Odontologo.class), any(), any(), any()))
                .thenReturn(new byte[]{(byte) '%', (byte) 'P', (byte) 'D', (byte) 'F'});

        AgenteConversacionalService.RespuestaAgente r = servicio.procesar(
                conv, "disponibilidad del doctor perez", null);

        assertTrue(r.mensaje().contains("Carlos Perez"));
        assertEquals(Intencion.PREGUNTAR_DISPONIBILIDAD, conv.getIntencion());
    }

    @Test
    void disponibilidadConFechaConcretaMuestraCuposDelDia() {
        Conversacion conv = conversacion();
        conv.setPaciente(paciente());
        when(servicioRepository.findByEstadoOrderByNombreAsc("ACTIVO"))
                .thenReturn(List.of(servicioLimpieza()));
        when(odontologoRepository.findTop5ByEstadoOrderByNombresAsc("ACTIVO"))
                .thenReturn(List.of(doctorPerez()));
        when(agendaService.disponibilidad(eq(10L), eq(manana), any(), isNull()))
                .thenReturn(List.of(new SlotResponse(LocalTime.of(10, 0), LocalTime.of(10, 45))));

        AgenteConversacionalService.RespuestaAgente r = servicio.procesar(conv, "cupos para manana con el doctor perez", null);

        assertTrue(r.mensaje().contains("Para el"));
        assertNull(r.adjunto());
        verify(horarioRepository, never()).findByOdontologoIdOrderByDiaSemanaAscHoraInicioAsc(anyLong());
    }

    @Test
    void doctorSeReconocePorEtiquetaEnElChat() {
        Conversacion conv = conversacion();
        conv.setPaciente(paciente());
        when(servicioRepository.findByEstadoOrderByNombreAsc("ACTIVO"))
                .thenReturn(List.of(servicioLimpieza()));
        when(odontologoRepository.findTop5ByEstadoOrderByNombresAsc("ACTIVO"))
                .thenReturn(List.of(doctorPerez(), doctorOrtodoncia()));
        when(odontologoRepository.findById(20L)).thenReturn(Optional.of(doctorOrtodoncia()));
        when(horarioRepository.findByOdontologoIdOrderByDiaSemanaAscHoraInicioAsc(20L))
                .thenReturn(List.of(horario(2, LocalTime.of(9, 0), LocalTime.of(13, 0))));
        when(agendaService.disponibilidad(eq(20L), any(LocalDate.class), any(), isNull()))
                .thenReturn(List.of(new SlotResponse(LocalTime.of(9, 0), LocalTime.of(9, 45))));
        when(horarioPdfService.generar(any(Odontologo.class), any(), any(), any()))
                .thenReturn(new byte[]{(byte) '%', (byte) 'P', (byte) 'D', (byte) 'F'});

        AgenteConversacionalService.RespuestaAgente r = servicio.procesar(conv, "cuando atiende el de ortodoncia", null);

        assertTrue(r.mensaje().contains("Ana"));
        assertNotNull(r.adjunto());
    }

    @Test
    void verCitasRespondeConListaDeCitasProximas() {
        Conversacion conv = conversacion();
        conv.setPaciente(paciente());
        when(citaRepository.proximasDelPaciente(7L, hoy)).thenReturn(List.of(citaProxima()));

        AgenteConversacionalService.RespuestaAgente r = servicio.procesar(conv, "ver citas", null);

        assertTrue(r.mensaje().contains("1."));
        assertTrue(r.mensaje().contains("Limpieza Dental"));
        assertTrue(r.mensaje().contains("Carlos"));
        assertFalse(r.transferirHumano());
    }

    @Test
    void verCitasSinCitasPideAgendar() {
        Conversacion conv = conversacion();
        conv.setPaciente(paciente());
        when(citaRepository.proximasDelPaciente(7L, hoy)).thenReturn(List.of());

        AgenteConversacionalService.RespuestaAgente r = servicio.procesar(conv, "ver citas", null);

        assertTrue(r.mensaje().contains("agendar una nueva"));
        assertFalse(r.transferirHumano());
    }

    @Test
    void verCitasSinPacientePideRegistro() {
        Conversacion conv = conversacion();

        AgenteConversacionalService.RespuestaAgente r = servicio.procesar(conv, "ver citas", null);

        assertTrue(r.mensaje().contains("registrarme"));
        assertFalse(r.transferirHumano());
    }

    @Test
    void verPerfilRespondeConDatosDelPaciente() {
        Conversacion conv = conversacion();
        conv.setPaciente(paciente());

        AgenteConversacionalService.RespuestaAgente r = servicio.procesar(conv, "mis datos", null);

        assertTrue(r.mensaje().contains("Maria"));
        assertTrue(r.mensaje().contains("Lopez"));
        assertTrue(r.mensaje().contains("1711111111"));
        assertFalse(r.transferirHumano());
    }

    @Test
    void verPerfilSinPacientePideRegistro() {
        Conversacion conv = conversacion();

        AgenteConversacionalService.RespuestaAgente r = servicio.procesar(conv, "mis datos", null);

        assertTrue(r.mensaje().contains("registrarme"));
        assertFalse(r.transferirHumano());
    }

    @Test
    void conectarGoogleNoConfigurado() {
        Conversacion conv = conversacion();
        lenient().when(googleService.estaConfigurado()).thenReturn(false);

        AgenteConversacionalService.RespuestaAgente r = servicio.procesar(conv, "conectar google", null);

        assertTrue(r.mensaje().contains("no está configurada"));
        assertFalse(r.transferirHumano());
    }

    // ------------------------------------------------------------------
    // Formato de la lista de horas y validación de las respuestas por paso
    // ------------------------------------------------------------------

    @Test
        void cuposDisponiblesSeListanEnDosColumnas() {
        Conversacion conv = conversacion();
        conv.setPaciente(paciente());
        when(servicioRepository.findByEstadoOrderByNombreAsc("ACTIVO"))
                .thenReturn(List.of(servicioLimpieza()));
        when(odontologoRepository.findTop5ByEstadoOrderByNombresAsc("ACTIVO"))
                .thenReturn(List.of(doctorPerez()));
        when(agendaService.disponibilidad(eq(10L), eq(manana), any(), isNull()))
                .thenReturn(List.of(new SlotResponse(LocalTime.of(10, 0), LocalTime.of(10, 45)),
                        new SlotResponse(LocalTime.of(11, 0), LocalTime.of(11, 45))));

        AgenteConversacionalService.RespuestaAgente r =
                servicio.procesar(conv, "cupos para manana con el doctor perez", null);

        assertTrue(r.mensaje().contains("hay estos cupos:\n\n1. 10:00          2. 11:00"));
    }

    @Test
    void numeroDeServicioFueraDeRangoExplicaElErrorYRepiteLaPregunta() {
        Conversacion conv = conversacion();
        conv.setPaciente(paciente());
        when(servicioRepository.findByEstadoOrderByNombreAsc("ACTIVO"))
                .thenReturn(List.of(servicioLimpieza()));
        when(odontologoRepository.findTop5ByEstadoOrderByNombresAsc("ACTIVO"))
                .thenReturn(List.of(doctorPerez()));

        AgenteConversacionalService.RespuestaAgente pregunta = servicio.procesar(conv, "quiero una cita", null);
        assertTrue(pregunta.mensaje().contains("¿Qué servicio"));
        assertTrue(pregunta.mensaje().contains("1. Limpieza Dental"));

        AgenteConversacionalService.RespuestaAgente error = servicio.procesar(conv, "2", null);
        assertTrue(error.mensaje().contains("No entendí \"2\" como un servicio."));
        assertTrue(error.mensaje().contains("¿Qué servicio"));
        assertTrue(conv.getContextoAgente().contains("SERVICIO"));

        AgenteConversacionalService.RespuestaAgente dia = servicio.procesar(conv, "1", null);
        assertTrue(dia.mensaje().contains("¿Para qué día"));
    }

    @Test
    void numeroDeHoraFueraDeRangoExplicaElErrorYRepiteLaLista() {
        Conversacion conv = conversacion();
        conv.setPaciente(paciente());
        when(servicioRepository.findByEstadoOrderByNombreAsc("ACTIVO"))
                .thenReturn(List.of(servicioLimpieza()));
        when(odontologoRepository.findTop5ByEstadoOrderByNombresAsc("ACTIVO"))
                .thenReturn(List.of(doctorPerez()));
        when(agendaService.disponibilidad(eq(10L), eq(manana), eq(1L), isNull()))
                .thenReturn(List.of(new SlotResponse(LocalTime.of(10, 0), LocalTime.of(10, 45))));

        servicio.procesar(conv, "quiero una cita", null);
        servicio.procesar(conv, "1", null);

        AgenteConversacionalService.RespuestaAgente lista = servicio.procesar(conv, "mañana", null);
        assertTrue(lista.mensaje().contains("¿A qué hora te conviene"));
        assertTrue(lista.mensaje().contains("1. 10:00"));

        AgenteConversacionalService.RespuestaAgente error = servicio.procesar(conv, "9", null);
        assertTrue(error.mensaje().contains("La opción 9 no existe"));
        assertTrue(error.mensaje().contains("1. 10:00"));

        AgenteConversacionalService.RespuestaAgente propuesta = servicio.procesar(conv, "1", null);
        assertTrue(propuesta.mensaje().contains("¿Confirmo tu cita?"));
    }

    @Test
    void horaPuedeResponderseComoHoraNumericaONumeroEscrito() {
        Conversacion conv = conversacion();
        conv.setPaciente(paciente());
        when(servicioRepository.findByEstadoOrderByNombreAsc("ACTIVO"))
                .thenReturn(List.of(servicioLimpieza()));
        when(odontologoRepository.findTop5ByEstadoOrderByNombresAsc("ACTIVO"))
                .thenReturn(List.of(doctorPerez()));
        when(agendaService.disponibilidad(eq(10L), eq(manana), eq(1L), isNull()))
                .thenReturn(List.of(new SlotResponse(LocalTime.of(10, 0), LocalTime.of(10, 45)),
                        new SlotResponse(LocalTime.of(11, 0), LocalTime.of(11, 45))));

        servicio.procesar(conv, "quiero una cita", null);
        servicio.procesar(conv, "1", null);
        servicio.procesar(conv, "mañana", null);

        AgenteConversacionalService.RespuestaAgente porHora = servicio.procesar(conv, "10", null);
        assertTrue(porHora.mensaje().contains("¿Confirmo tu cita?"));
    }

    @Test
    void pdfDeHorarioIncluyeLaDisponibilidadDeLosProximosSieteDias() {
        Conversacion conv = conversacion();
        conv.setPaciente(paciente());
        when(servicioRepository.findByEstadoOrderByNombreAsc("ACTIVO"))
                .thenReturn(List.of(servicioLimpieza()));
        when(odontologoRepository.findTop5ByEstadoOrderByNombresAsc("ACTIVO"))
                .thenReturn(List.of(doctorPerez()));
        when(odontologoRepository.findById(10L)).thenReturn(Optional.of(doctorPerez()));
        when(horarioRepository.findByOdontologoIdOrderByDiaSemanaAscHoraInicioAsc(10L))
                .thenReturn(List.of(horario(1, LocalTime.of(8, 0), LocalTime.of(17, 0))));
        when(agendaService.disponibilidad(eq(10L), any(LocalDate.class), any(), isNull()))
                .thenReturn(List.of(new SlotResponse(LocalTime.of(9, 0), LocalTime.of(9, 45))));
        when(horarioPdfService.generar(any(Odontologo.class), any(), any(), any()))
                .thenReturn(new byte[]{(byte) '%', (byte) 'P', (byte) 'D', (byte) 'F'});

        AgenteConversacionalService.RespuestaAgente r =
                servicio.procesar(conv, "cuando atiende el doctor perez", null);

        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(horarioPdfService).generar(any(Odontologo.class), any(), any(), captor.capture());
        List<HorarioPdfService.DiaDisponibilidad> dias = captor.getValue();

        assertEquals(7, dias.size());
        assertTrue(dias.get(0).libre().contains("09:00"));
        assertEquals("—", dias.get(0).ocupado());
        assertTrue(r.mensaje().contains("disponibilidad de los próximos 7 días en PDF"));
    }
}