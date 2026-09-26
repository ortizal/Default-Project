package org.dentalcrm.web.agenda;

import org.dentalcrm.domain.cita.Cita;
import org.dentalcrm.domain.cita.EstadoCita;
import org.dentalcrm.domain.horario.ExcepcionHorario;
import org.dentalcrm.domain.horario.ExcepcionHorarioRepository;
import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.odontologo.OdontologoRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.service.CurrentUserService;
import org.dentalcrm.web.agenda.dto.ExcepcionHorarioRequest;
import org.dentalcrm.web.agenda.dto.ExcepcionHorarioResponse;
import org.dentalcrm.web.cita.CitaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExcepcionHorarioServiceTest {

    private static final LocalDate FUTURO = LocalDate.of(2099, 1, 15);

    private ExcepcionHorarioRepository excepcionRepository;
    private OdontologoRepository odontologoRepository;
    private AgendaService agendaService;
    private CitaService citaService;

    private ExcepcionHorarioService servicio;

    @BeforeEach
    void setUp() {
        excepcionRepository = mock(ExcepcionHorarioRepository.class);
        odontologoRepository = mock(OdontologoRepository.class);
        agendaService = mock(AgendaService.class);
        citaService = mock(CitaService.class);
        AuditService auditService = mock(AuditService.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);

        when(odontologoRepository.findById(1L)).thenReturn(Optional.of(odontologo()));
        when(excepcionRepository.findByOdontologoIdAndFecha(anyLong(), any())).thenReturn(Optional.empty());
        when(excepcionRepository.save(any(ExcepcionHorario.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        servicio = new ExcepcionHorarioService(excepcionRepository, odontologoRepository,
                agendaService, citaService, auditService, currentUserService, "America/Guayaquil");
    }

    @Test
    void crearDiaCerradoCancelaTodasLasCitasActivasDelDia() {
        Cita a = cita(LocalTime.of(9, 0));
        Cita b = cita(LocalTime.of(11, 30));
        Cita c = cita(LocalTime.of(16, 0));
        when(agendaService.citasActivasDelDia(1L, FUTURO)).thenReturn(List.of(a, b, c));

        ExcepcionHorarioResponse r = servicio.crear(cerrado(null, null));

        assertEquals(3, r.citasCanceladas());
        verify(citaService, times(3)).cancelar(anyLong(), any());
        verify(citaService, times(3)).cancelar(anyLong(),
                org.mockito.ArgumentMatchers.argThat(m -> m.motivo() != null && m.motivo().contains("2099-01-15")));
    }

    @Test
    void crearHorarioEspecialNoCancelaLasCitasDentroDeLaVentana() {
        Cita dentroDeLaVentana = cita(LocalTime.of(10, 0));
        Cita fueraDeLaVentana = cita(LocalTime.of(15, 0));
        when(agendaService.citasActivasDelDia(1L, FUTURO)).thenReturn(List.of(dentroDeLaVentana, fueraDeLaVentana));

        ExcepcionHorarioResponse r = servicio.crear(
                new ExcepcionHorarioRequest(1L, FUTURO, ExcepcionHorario.TIPO_HORARIO_ESPECIAL,
                        LocalTime.of(9, 0), LocalTime.of(13, 0), "Jornada corta"));

        assertEquals(1, r.citasCanceladas());
        verify(citaService, times(1)).cancelar(anyLong(), any());
    }

    @Test
    void crearSinCitasDevuelveCeroCanceladas() {
        when(agendaService.citasActivasDelDia(1L, FUTURO)).thenReturn(List.of());

        ExcepcionHorarioResponse r = servicio.crear(cerrado(null, null));

        assertEquals(0, r.citasCanceladas());
        verify(citaService, never()).cancelar(anyLong(), any());
    }

    @Test
    void crearRechazaFechasPasadas() {
        ExcepcionHorarioRequest req = new ExcepcionHorarioRequest(1L,
                LocalDate.of(2000, 1, 1), ExcepcionHorario.TIPO_CERRADO, null, null, null);

        BusinessException e = assertThrows(BusinessException.class, () -> servicio.crear(req));

        assertEquals("EXCEPCION_FECHA_PASADA", e.getCode());
        verify(excepcionRepository, never()).save(any());
    }

    @Test
    void crearRechazaDiaCerradoQueTraigaHoras() {
        ExcepcionHorarioRequest req = new ExcepcionHorarioRequest(1L, FUTURO,
                ExcepcionHorario.TIPO_CERRADO, LocalTime.of(9, 0), LocalTime.of(13, 0), null);

        BusinessException e = assertThrows(BusinessException.class, () -> servicio.crear(req));

        assertEquals("EXCEPCION_HORAS_INVALIDAS", e.getCode());
    }

    @Test
    void crearRechazaHorarioEspecialSinHoras() {
        ExcepcionHorarioRequest req = new ExcepcionHorarioRequest(1L, FUTURO,
                ExcepcionHorario.TIPO_HORARIO_ESPECIAL, null, null, null);

        BusinessException e = assertThrows(BusinessException.class, () -> servicio.crear(req));

        assertEquals("EXCEPCION_HORAS_INVALIDAS", e.getCode());
    }

    @Test
    void crearRechazaHorarioEspecialCuyoFinSeaAntesDelInicio() {
        ExcepcionHorarioRequest req = new ExcepcionHorarioRequest(1L, FUTURO,
                ExcepcionHorario.TIPO_HORARIO_ESPECIAL, LocalTime.of(13, 0), LocalTime.of(9, 0), null);

        BusinessException e = assertThrows(BusinessException.class, () -> servicio.crear(req));

        assertEquals("EXCEPCION_HORAS_INVALIDAS", e.getCode());
    }

    @Test
    void crearRechazaTiposQueNoSeanCerradoNiEspecial() {
        ExcepcionHorarioRequest req = new ExcepcionHorarioRequest(1L, FUTURO, "VACACIONES", null, null, null);

        BusinessException e = assertThrows(BusinessException.class, () -> servicio.crear(req));

        assertEquals("EXCEPCION_TIPO_INVALIDO", e.getCode());
    }

    @Test
    void crearRechazaExcepcionDuplicadaEnLaMismaFecha() {
        when(excepcionRepository.findByOdontologoIdAndFecha(1L, FUTURO))
                .thenReturn(Optional.of(new ExcepcionHorario()));

        BusinessException e = assertThrows(BusinessException.class, () -> servicio.crear(cerrado(null, null)));

        assertEquals("EXCEPCION_DUPLICADA", e.getCode());
    }

    private ExcepcionHorarioRequest cerrado(LocalTime inicio, LocalTime fin) {
        return new ExcepcionHorarioRequest(1L, FUTURO, ExcepcionHorario.TIPO_CERRADO, inicio, fin, "Mantenimiento");
    }

    private static Odontologo odontologo() {
        Odontologo o = new Odontologo();
        o.setId(1L);
        o.setNombres("Ana");
        o.setApellidos("Paz");
        return o;
    }

    private static Cita cita(LocalTime inicio) {
        Cita c = new Cita();
        c.setId(77L);
        c.setEstado(EstadoCita.CONFIRMADA);
        c.setHoraInicio(inicio);
        c.setHoraFin(inicio.plusMinutes(30));
        return c;
    }
}
