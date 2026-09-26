package org.dentalcrm.web.agenda;

import org.dentalcrm.domain.bloqueo.BloqueoAgenda;
import org.dentalcrm.domain.bloqueo.BloqueoAgendaRepository;
import org.dentalcrm.domain.cita.Cita;
import org.dentalcrm.domain.cita.CitaRepository;
import org.dentalcrm.domain.cita.EstadoCita;
import org.dentalcrm.domain.horario.ExcepcionHorario;
import org.dentalcrm.domain.horario.ExcepcionHorarioRepository;
import org.dentalcrm.domain.horario.HorarioOdontologo;
import org.dentalcrm.domain.horario.HorarioOdontologoRepository;
import org.dentalcrm.domain.servicio.ServicioRepository;
import org.dentalcrm.web.agenda.dto.SlotResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgendaServiceTest {

    // Tests de la regla de bloqueo (secciones 10 y 11 del plan)
    @Test
    void bloqueoDiaCompletoBloqueaTodo() {
        assertTrue(AgendaService.bloquea(new BloqueoAgenda(), LocalTime.of(8, 0), LocalTime.of(9, 0)));
    }

    @Test
    void bloqueoParcialSolapa() {
        BloqueoAgenda b = bloqueo(LocalTime.of(9, 0), LocalTime.of(10, 0));
        assertTrue(AgendaService.bloquea(b, LocalTime.of(9, 30), LocalTime.of(10, 30)));
    }

    @Test
    void bloqueoParcialNoSolapa() {
        BloqueoAgenda b = bloqueo(LocalTime.of(9, 0), LocalTime.of(10, 0));
        assertFalse(AgendaService.bloquea(b, LocalTime.of(11, 0), LocalTime.of(12, 0)));
    }

    @Test
    void bloqueoLimitesExactosNoCuentanComoSolapeSiNoCruzan() {
        BloqueoAgenda b = bloqueo(LocalTime.of(9, 0), LocalTime.of(10, 0));
        assertFalse(AgendaService.bloquea(b, LocalTime.of(10, 0), LocalTime.of(11, 0)));
    }

    @Test
    void citaCanceladaNoCuentaParaDobleReserva() {
        Cita cancelada = cita(EstadoCita.CANCELADA, LocalTime.of(9, 0), LocalTime.of(10, 0));
        assertFalse(cancelada.getEstado().esActiva());
    }

    // ------------------------------------------------------------------
    // Días de excepción de atención
    // ------------------------------------------------------------------

    @Test
    void diaCerradoNoOfreceNingunCupoNiConsultaElHorarioSemanal() {
        ExcepcionHorario cerrada = new ExcepcionHorario();
        cerrada.setTipo(ExcepcionHorario.TIPO_CERRADO);
        ExcepcionHorarioRepository excepcionRepo = mock(ExcepcionHorarioRepository.class);
        HorarioOdontologoRepository horarioRepo = mock(HorarioOdontologoRepository.class);
        when(excepcionRepo.findByOdontologoIdAndFecha(anyLong(), any())).thenReturn(Optional.of(cerrada));

        AgendaService svc = servicio(horarioRepo, excepcionRepo);

        LocalDate fecha = LocalDate.now().plusDays(30);
        assertTrue(svc.disponibilidad(1L, fecha, null, 30).isEmpty());
        verify(horarioRepo, never()).findByOdontologoIdAndDiaSemanaAndEstado(anyLong(), any(), any());
        assertFalse(svc.estaLibre(1L, fecha, LocalTime.of(9, 0), LocalTime.of(9, 30)));
    }

    @Test
    void horarioEspecialAcotaLosCuposALaVentanaDelDia() {
        ExcepcionHorario especial = new ExcepcionHorario();
        especial.setTipo(ExcepcionHorario.TIPO_HORARIO_ESPECIAL);
        especial.setHoraInicio(LocalTime.of(9, 0));
        especial.setHoraFin(LocalTime.of(13, 0));
        ExcepcionHorarioRepository excepcionRepo = mock(ExcepcionHorarioRepository.class);
        when(excepcionRepo.findByOdontologoIdAndFecha(anyLong(), any())).thenReturn(Optional.of(especial));

        AgendaService svc = servicio(mock(HorarioOdontologoRepository.class), excepcionRepo);

        List<SlotResponse> slots = svc.disponibilidad(1L, LocalDate.now().plusDays(30), null, 60);

        assertFalse(slots.isEmpty());
        assertTrue(slots.stream().allMatch(s -> !s.horaInicio().isBefore(LocalTime.of(9, 0))
                && !s.horaInicio().isAfter(LocalTime.of(12, 0))));
        assertTrue(svc.estaLibre(1L, LocalDate.now().plusDays(30), LocalTime.of(10, 0), LocalTime.of(11, 0)));
        assertFalse(svc.estaLibre(1L, LocalDate.now().plusDays(30), LocalTime.of(14, 0), LocalTime.of(15, 0)));
    }

    @Test
    void sinExcepcionSeUsaElHorarioSemanalDeSiempre() {
        HorarioOdontologoRepository horarioRepo = mock(HorarioOdontologoRepository.class);
        LocalDate fecha = LocalDate.now().plusDays(30);
        when(horarioRepo.findByOdontologoIdAndDiaSemanaAndEstado(anyLong(), any(), any()))
                .thenReturn(List.of(horario(LocalTime.of(8, 0), LocalTime.of(10, 0))));

        AgendaService svc = servicio(horarioRepo, mock(ExcepcionHorarioRepository.class));

        List<SlotResponse> slots = svc.disponibilidad(1L, fecha, null, 60);

        assertEquals(3, slots.size());
        assertEquals(LocalTime.of(8, 0), slots.get(0).horaInicio());
    }

    @Test
    void elDiaCerradoApareceComoOcupadoEnLaDisponibilidad() {
        ExcepcionHorario cerrada = new ExcepcionHorario();
        cerrada.setTipo(ExcepcionHorario.TIPO_CERRADO);
        cerrada.setMotivo("Feriado nacional");
        ExcepcionHorarioRepository excepcionRepo = mock(ExcepcionHorarioRepository.class);
        when(excepcionRepo.findByOdontologoIdAndFecha(anyLong(), any())).thenReturn(Optional.of(cerrada));

        AgendaService svc = servicio(mock(HorarioOdontologoRepository.class), excepcionRepo);

        List<AgendaService.Ocupado> tramos = svc.ocupados(1L, LocalDate.now().plusDays(30));

        assertEquals(1, tramos.size());
        assertEquals("Todo el día Feriado nacional", tramos.get(0).descripcion());
    }

    private static AgendaService servicio(HorarioOdontologoRepository horarioRepo,
                                          ExcepcionHorarioRepository excepcionRepo) {
        CitaRepository citaRepo = mock(CitaRepository.class);
        when(citaRepo.findByFechaAndDoctorIdOrderByHoraInicioAsc(any(), anyLong())).thenReturn(List.of());
        BloqueoAgendaRepository bloqueoRepo = mock(BloqueoAgendaRepository.class);
        when(bloqueoRepo.findByOdontologoIdAndFecha(anyLong(), any())).thenReturn(List.of());
        return new AgendaService(horarioRepo, citaRepo, bloqueoRepo,
                mock(ServicioRepository.class), excepcionRepo, "America/Guayaquil");
    }

    private static HorarioOdontologo horario(LocalTime inicio, LocalTime fin) {
        HorarioOdontologo h = new HorarioOdontologo();
        h.setHoraInicio(inicio);
        h.setHoraFin(fin);
        h.setIntervaloMinutos(30);
        h.setEstado("ACTIVO");
        return h;
    }

    private static BloqueoAgenda bloqueo(LocalTime inicio, LocalTime fin) {
        BloqueoAgenda b = new BloqueoAgenda();
        b.setHoraInicio(inicio);
        b.setHoraFin(fin);
        return b;
    }

    private static Cita cita(EstadoCita estado, LocalTime inicio, LocalTime fin) {
        Cita c = new Cita();
        c.setEstado(estado);
        c.setHoraInicio(inicio);
        c.setHoraFin(fin);
        return c;
    }
}