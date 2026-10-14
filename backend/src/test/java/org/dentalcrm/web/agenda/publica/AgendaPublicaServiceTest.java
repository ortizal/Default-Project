package org.dentalcrm.web.agenda.publica;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dentalcrm.domain.cita.Cita;
import org.dentalcrm.domain.cita.CitaRepository;
import org.dentalcrm.domain.cita.EstadoCita;
import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.odontologo.OdontologoRepository;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.domain.servicio.Servicio;
import org.dentalcrm.domain.servicio.ServicioRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.web.agenda.AgendaService;
import org.dentalcrm.web.agenda.dto.SlotResponse;
import org.dentalcrm.web.agenda.publica.dto.*;
import org.dentalcrm.web.cita.CitaService;
import org.dentalcrm.web.cita.dto.CitaRequest;
import org.dentalcrm.web.cita.dto.CitaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgendaPublicaServiceTest {

    @Mock private AgendaService agendaService;
    @Mock private CitaService citaService;
    @Mock private OdontologoRepository odontologoRepository;
    @Mock private PacienteRepository pacienteRepository;
    @Mock private ServicioRepository servicioRepository;
    @Mock private CitaRepository citaRepository;
    @Mock private org.dentalcrm.domain.horario.HorarioOdontologoRepository horarioRepository;
    @Mock private org.dentalcrm.domain.reserva.ReservaWebIdempotenciaRepository idempotenciaRepository;
    @Mock private org.dentalcrm.service.AuditService auditService;

    private AgendaPublicaTokenService tokens;
    private AgendaPublicaService service;

    private final LocalDate manana = LocalDate.now().plusDays(1);

    @BeforeEach
    void setUp() {
        tokens = new AgendaPublicaTokenService(new ObjectMapper(), "", 30, "test");
        tokens.derivarSecreto();
        service = new AgendaPublicaService(tokens, agendaService, citaService,
                odontologoRepository, pacienteRepository, servicioRepository, citaRepository,
                horarioRepository, idempotenciaRepository, auditService);

        Odontologo doctor = new Odontologo();
        doctor.setId(10L);
        doctor.setNombres("Ana");
        doctor.setApellidos("Paz");
        doctor.setEspecialidad("Endodoncia");
        doctor.setEstado("ACTIVO");
        lenient().when(odontologoRepository.findById(10L)).thenReturn(Optional.of(doctor));

        Paciente paciente = new Paciente();
        paciente.setId(7L);
        paciente.setNombres("Alexis Rodrigo");
        paciente.setApellidos("Sanchez");
        paciente.setCedula("1711111111");
        lenient().when(pacienteRepository.findById(7L)).thenReturn(Optional.of(paciente));

        Servicio servicio = new Servicio();
        servicio.setId(1L);
        servicio.setNombre("Limpieza");
        servicio.setDuracionMinutos(45);
        servicio.setPrecio(new BigDecimal("40.00"));
        servicio.setEstado("ACTIVO");
        lenient().when(servicioRepository.findById(1L)).thenReturn(Optional.of(servicio));
    }

    @Test
    void tokenFirmadoEsValidoYElCorruptoNo() {
        String token = tokens.firmar(7L, 10L, 1L);
        assertTrue(tokens.validar(token).isPresent());
        assertFalse(tokens.validar(token + "x").isPresent());
        assertFalse(tokens.validar("basura").isPresent());
        assertFalse(tokens.validar(null).isPresent());
    }

    @Test
    void tokenVencidoNoSirve() throws Exception {
        String cuerpo = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                new ObjectMapper().writeValueAsBytes(new AgendaPublicaTokenService.Payload(
                        1L, 7L, 10L, 1L, AgendaPublicaTokenService.PROPOSITO,
                        java.time.Instant.now().getEpochSecond() - 60)));
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(
                java.security.MessageDigest.getInstance("SHA-256")
                        .digest("agenda-publica:test".getBytes()), "HmacSHA256"));
        String token = cuerpo + "." + java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(cuerpo.getBytes()));
        assertTrue(tokens.validar(token).isEmpty());
    }

    @Test
    void contextoExponeSoloNombreDePacienteNoCedulaNiTelefono() {
        AgendaContextResponse ctx = service.contexto(tokens.firmar(7L, 10L, 1L));
        assertEquals(10L, ctx.odontologoId());
        assertEquals("Ana Paz", ctx.odontologo());
        assertEquals("Endodoncia", ctx.especialidad());
        assertEquals("Alexis", ctx.paciente());
        assertFalse(ctx.toString().contains("1711111111"));
    }

    @Test
    void diasDelMesMarcanPasadoSinCuposYDisponibles() {
        laborableTodosLosDias();
        when(agendaService.disponibilidad(eq(10L), any(LocalDate.class), eq(1L), isNull()))
                .thenReturn(List.of(new SlotResponse(LocalTime.of(10, 0), LocalTime.of(10, 45)),
                        new SlotResponse(LocalTime.of(11, 0), LocalTime.of(11, 45)),
                        new SlotResponse(LocalTime.of(12, 0), LocalTime.of(12, 45))));
        LocalDate hoy = LocalDate.now();
        List<AgendaDiaResponse> dias = service.dias(tokens.firmar(7L, 10L, 1L), hoy.getYear(), hoy.getMonthValue());
        assertEquals(hoy.lengthOfMonth(), dias.size());
        assertEquals(AgendaDiaResponse.PASADO, dias.get(0).estado());
        AgendaDiaResponse disponible = dias.stream().filter(d -> d.fecha().isAfter(hoy)).findFirst().orElseThrow();
        assertEquals(AgendaDiaResponse.DISPONIBLE, disponible.estado());
        assertEquals(3, disponible.cupos());
    }

    @Test
    void diasSinCuposLaborablesQuedanSinDisponibilidad() {
        laborableTodosLosDias();
        when(agendaService.disponibilidad(eq(10L), any(LocalDate.class), eq(1L), isNull()))
                .thenReturn(List.of());
        LocalDate hoy = LocalDate.now();
        List<AgendaDiaResponse> dias = service.dias(tokens.firmar(7L, 10L, 1L), hoy.getYear(), hoy.getMonthValue());
        assertTrue(dias.stream().filter(d -> !d.fecha().isBefore(hoy))
                .allMatch(d -> AgendaDiaResponse.SIN_DISPONIBILIDAD.equals(d.estado())));
    }

    @Test
    void diasSinHorarioNiExcepcionQuedanNoLaborables() {
        lenient().when(agendaService.excepcion(eq(10L), any(LocalDate.class)))
                .thenReturn(java.util.Optional.empty());
        lenient().when(horarioRepository.findByOdontologoIdAndDiaSemanaAndEstado(eq(10L), anyInt(), eq("ACTIVO")))
                .thenReturn(List.of());
        LocalDate hoy = LocalDate.now();
        List<AgendaDiaResponse> dias = service.dias(tokens.firmar(7L, 10L, 1L), hoy.getYear(), hoy.getMonthValue());
        assertTrue(dias.stream().filter(d -> d.fecha().isAfter(hoy))
                .allMatch(d -> AgendaDiaResponse.NO_LABORABLE.equals(d.estado())));
        // No laborable no debe llamar a la agenda (no es un problema de cupos).
        verify(agendaService, never()).disponibilidad(eq(10L), any(LocalDate.class), eq(1L), isNull());
    }

    @Test
    void diaConExcepcionCerradoEsNoLaborableAunqueTengaHorario() {
        LocalDate futuro = LocalDate.now().plusDays(14);
        laborableTodosLosDias();
        lenient().when(agendaService.excepcion(eq(10L), eq(futuro)))
                .thenReturn(java.util.Optional.of(excepcionCerrada()));
        List<AgendaDiaResponse> dias = service.dias(tokens.firmar(7L, 10L, 1L),
                futuro.getYear(), futuro.getMonthValue());
        AgendaDiaResponse dia = dias.stream().filter(d -> d.fecha().equals(futuro)).findFirst().orElseThrow();
        assertEquals(AgendaDiaResponse.NO_LABORABLE, dia.estado());
        assertEquals(0, dia.cupos());
    }

    @Test
    void reservarConIdempotenciaClaveNuevaCreaYRegistraAuditoria() {
        String clave = "reintento-nav-1";
        when(agendaService.disponibilidad(eq(10L), eq(manana), eq(1L), isNull()))
                .thenReturn(List.of(new SlotResponse(LocalTime.of(10, 0), LocalTime.of(10, 45))));
        when(citaRepository.findAll()).thenReturn(List.of());
        when(idempotenciaRepository.findByTenantIdAndIdempotencyKey(any(), eq(clave)))
                .thenReturn(java.util.Optional.empty());
        CitaResponse creada = CitaResponse.from(cita());
        when(citaService.crear(any(CitaRequest.class))).thenReturn(creada);

        AgendaReservarResponse r = service.reservar(new AgendaReservarRequest(
                tokens.firmar(7L, 10L, 1L), manana, "10:00", 10L, 1L, clave));

        assertEquals(AgendaReservarResponse.CREADA, r.estado());
        verify(idempotenciaRepository).save(any(org.dentalcrm.domain.reserva.ReservaWebIdempotencia.class));
        verify(auditService).registrar(eq(AgendaPublicaService.ACCION_RESERVA_WEB),
                eq(AgendaPublicaService.MODULO), eq("CITA"), eq(creada.id()), isNull(), eq(creada));
    }

    @Test
    void reservarConMismaIdempotenciaClaveDevuelveLaCitaSinCrearOtra() {
        String clave = "reintento-nav-2";
        Cita previa = cita();
        when(idempotenciaRepository.findByTenantIdAndIdempotencyKey(any(), eq(clave)))
                .thenReturn(java.util.Optional.of(enlace(previa.getId(), clave)));
        when(citaRepository.findById(previa.getId())).thenReturn(java.util.Optional.of(previa));

        AgendaReservarResponse r = service.reservar(new AgendaReservarRequest(
                tokens.firmar(7L, 10L, 1L), manana, "10:00", 10L, 1L, clave));

        assertEquals(AgendaReservarResponse.REPETIDA, r.estado());
        assertEquals(previa.getId(), r.cita().id());
        verify(citaService, never()).crear(any(CitaRequest.class));
        verify(auditService, never()).registrar(eq(AgendaPublicaService.ACCION_RESERVA_WEB),
                any(), any(), any(), any(), any());
    }

    @Test
    void slotsDevuelveSoloHorasDelToken() {
        when(agendaService.disponibilidad(10L, manana, 1L, null))
                .thenReturn(List.of(new SlotResponse(LocalTime.of(10, 0), LocalTime.of(10, 45))));
        List<AgendaSlotResponse> slots = service.slots(tokens.firmar(7L, 10L, 1L), manana);
        assertEquals(1, slots.size());
        assertEquals("10:00", slots.get(0).horaInicio());
        assertEquals(10L, slots.get(0).odontologoId());
    }

    @Test
    void reservarCreaLaCitaCuandoElCupoEstaLibre() {
        when(agendaService.disponibilidad(eq(10L), eq(manana), eq(1L), isNull()))
                .thenReturn(List.of(new SlotResponse(LocalTime.of(10, 0), LocalTime.of(10, 45))));
        CitaResponse creada = CitaResponse.from(cita());
        when(citaService.crear(any(CitaRequest.class))).thenReturn(creada);

        AgendaReservarResponse r = service.reservar(new AgendaReservarRequest(
                tokens.firmar(7L, 10L, 1L), manana, "10:00", 10L, 1L, null));

        assertEquals(AgendaReservarResponse.CREADA, r.estado());
        assertNotNull(r.cita());
        assertTrue(r.mensaje().contains("reservada"));
    }

    @Test
    void reservarConCupoOcupadoOfreceAlternativas() {
        when(agendaService.disponibilidad(eq(10L), eq(manana), eq(1L), isNull()))
                .thenReturn(List.of(new SlotResponse(LocalTime.of(11, 0), LocalTime.of(11, 45))));
        AgendaReservarResponse r = service.reservar(new AgendaReservarRequest(
                tokens.firmar(7L, 10L, 1L), manana, "10:00", 10L, 1L, null));
        assertEquals(AgendaReservarResponse.OCUPADA, r.estado());
        assertEquals(1, r.alternativas().size());
        assertEquals("11:00", r.alternativas().get(0).horaInicio());
    }

    @Test
    void reservarSeRepiteSiYaExisteLaMismaCita() {
        when(agendaService.disponibilidad(eq(10L), eq(manana), eq(1L), isNull()))
                .thenReturn(List.of(new SlotResponse(LocalTime.of(10, 0), LocalTime.of(10, 45))));
        when(citaRepository.findAll()).thenReturn(List.of(cita()));
        AgendaReservarResponse r = service.reservar(new AgendaReservarRequest(
                tokens.firmar(7L, 10L, 1L), manana, "10:00", 10L, 1L, null));
        assertEquals(AgendaReservarResponse.REPETIDA, r.estado());
        assertTrue(r.mensaje().contains("no se creó otra"));
    }

    @Test
    void reservarConTokenInvalidoNoTocaLaAgenda() {
        AgendaReservarResponse r = service.reservar(new AgendaReservarRequest(
                "token-falso", manana, "10:00", 10L, 1L, null));
        assertEquals(AgendaReservarResponse.ERROR, r.estado());
    }

    @Test
    void reservarDeOtroOdontologoDelTokenSeRechaza() {
        AgendaReservarResponse r = service.reservar(new AgendaReservarRequest(
                tokens.firmar(7L, 10L, 1L), manana, "10:00", 11L, 1L, null));
        assertEquals(AgendaReservarResponse.ERROR, r.estado());
        assertTrue(r.mensaje().contains("odontólogo"));
    }

    @Test
    void generarEnlaceSeparaElPropositoSegunElPaciente() {
        assertThrows(BusinessException.class, () -> service.generarEnlace(7L, null, 1L));

        String conPaciente = service.generarEnlace(7L, 10L, 1L);
        assertEquals(AgendaPublicaTokenService.PROPOSITO,
                tokens.validar(conPaciente).orElseThrow().proposito());

        // Sin paciente el enlace es de sólo consulta (VIEW_AVAILABILITY, §13).
        String anonimo = service.generarEnlace(null, 10L, 1L);
        var payload = tokens.validar(anonimo).orElseThrow();
        assertEquals(AgendaPublicaTokenService.PROPOSITO_CONSULTA, payload.proposito());
        assertNull(payload.pacienteId());
    }

    @Test
    void contextoDeSoloConsultaNoExigePaciente() {
        String consulta = tokens.firmar(null, 10L, 1L, AgendaPublicaTokenService.PROPOSITO_CONSULTA);

        AgendaContextResponse ctx = service.contexto(consulta);

        assertEquals(10L, ctx.odontologoId());
        assertEquals("", ctx.paciente());
    }

    @Test
    void slotsEntregaUnSlotTokenFirmado() {
        when(agendaService.disponibilidad(10L, manana, 1L, null))
                .thenReturn(List.of(new SlotResponse(LocalTime.of(10, 0), LocalTime.of(10, 45))));

        List<AgendaSlotResponse> slots = service.slots(tokens.firmar(7L, 10L, 1L), manana);

        assertNotNull(slots.get(0).slotToken());
        var slot = tokens.validarSlot(slots.get(0).slotToken()).orElseThrow();
        assertEquals(10L, slot.odontologoId());
        assertEquals(manana.toString(), slot.fecha());
        assertEquals("10:00", slot.hora());
    }

    @Test
    void reservarConSlotTokenUsaLaFechaYLaHoraFirmadas() {
        when(agendaService.disponibilidad(eq(10L), eq(manana), eq(1L), isNull()))
                .thenReturn(List.of(new SlotResponse(LocalTime.of(10, 0), LocalTime.of(10, 45))));
        when(citaRepository.findAll()).thenReturn(List.of());
        when(citaService.crear(any(CitaRequest.class))).thenReturn(CitaResponse.from(cita()));
        String slotToken = tokens.firmarSlot(10L, manana, LocalTime.of(10, 0));

        AgendaReservarResponse r = service.reservar(new AgendaReservarRequest(
                tokens.firmar(7L, 10L, 1L), null, null, null, null, null, slotToken));

        assertEquals(AgendaReservarResponse.CREADA, r.estado());
        verify(citaService).crear(any(CitaRequest.class));
    }

    @Test
    void reservarConSlotTokenDeOtroDoctorOBasuroSeRechaza() {
        String deOtro = tokens.firmarSlot(11L, manana, LocalTime.of(10, 0));
        AgendaReservarResponse otro = service.reservar(new AgendaReservarRequest(
                tokens.firmar(7L, 10L, 1L), null, null, null, null, null, deOtro));
        assertEquals(AgendaReservarResponse.ERROR, otro.estado());
        assertTrue(otro.mensaje().contains("odontólogo"));

        AgendaReservarResponse basura = service.reservar(new AgendaReservarRequest(
                tokens.firmar(7L, 10L, 1L), null, null, null, null, null, "slot-basura"));
        assertEquals(AgendaReservarResponse.ERROR, basura.estado());
        assertTrue(basura.mensaje().contains("ya no es válido"));
        verify(citaService, never()).crear(any(CitaRequest.class));
    }

    @Test
    void reservarConEnlaceDeSoloConsultaSeRechaza() {
        String consulta = tokens.firmar(null, 10L, 1L, AgendaPublicaTokenService.PROPOSITO_CONSULTA);

        AgendaReservarResponse r = service.reservar(new AgendaReservarRequest(
                consulta, manana, "10:00", 10L, 1L, null));

        assertEquals(AgendaReservarResponse.ERROR, r.estado());
        assertTrue(r.mensaje().contains("consultar"));
        verify(citaService, never()).crear(any(CitaRequest.class));
    }

    private Cita cita() {
        Paciente paciente = new Paciente();
        paciente.setId(7L);
        Odontologo doctor = new Odontologo();
        doctor.setId(10L);
        doctor.setNombres("Ana");
        doctor.setApellidos("Paz");
        Servicio servicio = new Servicio();
        servicio.setId(1L);
        servicio.setNombre("Limpieza");
        servicio.setDuracionMinutos(45);
        Cita c = new Cita();
        c.setId(42L);
        c.setPaciente(paciente);
        c.setDoctor(doctor);
        c.setServicio(servicio);
        c.setFecha(manana);
        c.setHoraInicio(LocalTime.of(10, 0));
        c.setHoraFin(LocalTime.of(10, 45));
        c.setEstado(EstadoCita.PENDIENTE);
        return c;
    }

    private void laborableTodosLosDias() {
        lenient().when(agendaService.excepcion(eq(10L), any(LocalDate.class)))
                .thenReturn(java.util.Optional.empty());
        lenient().when(horarioRepository.findByOdontologoIdAndDiaSemanaAndEstado(eq(10L), anyInt(), eq("ACTIVO")))
                .thenReturn(List.of(horarioActivo()));
    }

    private org.dentalcrm.domain.horario.HorarioOdontologo horarioActivo() {
        var h = new org.dentalcrm.domain.horario.HorarioOdontologo();
        h.setDiaSemana(LocalDate.now().getDayOfWeek().getValue());
        h.setHoraInicio(LocalTime.of(8, 0));
        h.setHoraFin(LocalTime.of(18, 0));
        h.setEstado("ACTIVO");
        return h;
    }

    private org.dentalcrm.domain.horario.ExcepcionHorario excepcionCerrada() {
        var ex = new org.dentalcrm.domain.horario.ExcepcionHorario();
        ex.setTipo(org.dentalcrm.domain.horario.ExcepcionHorario.TIPO_CERRADO);
        return ex;
    }

    private org.dentalcrm.domain.reserva.ReservaWebIdempotencia enlace(Long citaId, String clave) {
        var e = new org.dentalcrm.domain.reserva.ReservaWebIdempotencia();
        e.setIdempotencyKey(clave);
        e.setCitaId(citaId);
        e.setOdontologoId(10L);
        e.setPacienteId(7L);
        e.setFecha(manana);
        e.setHoraInicio(LocalTime.of(10, 0));
        return e;
    }
}
