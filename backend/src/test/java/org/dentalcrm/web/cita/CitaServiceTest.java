package org.dentalcrm.web.cita;

import org.dentalcrm.domain.cita.Cita;
import org.dentalcrm.domain.cita.CitaRepository;
import org.dentalcrm.domain.cita.EstadoCita;
import org.dentalcrm.domain.horario.HorarioOdontologoRepository;
import org.dentalcrm.domain.odontologo.OdontologoRepository;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.domain.servicio.ServicioRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.service.CurrentUserService;
import org.dentalcrm.web.agenda.AgendaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CitaServiceTest {

    private CitaService servicio;
    private CitaRepository citaRepo;
    private HorarioOdontologoRepository horarioRepo;
    private AgendaService agendaService;

    @BeforeEach
    void setUp() {
        citaRepo = mock(CitaRepository.class);
        horarioRepo = mock(HorarioOdontologoRepository.class);
        agendaService = mock(AgendaService.class);
        PacienteRepository pacRepo = mock(PacienteRepository.class);
        OdontologoRepository docRepo = mock(OdontologoRepository.class);
        ServicioRepository servRepo = mock(ServicioRepository.class);
        servicio = new CitaService(citaRepo, pacRepo, docRepo, servRepo, horarioRepo,
                agendaService, mock(AuditService.class), mock(CurrentUserService.class),
                mock(org.springframework.context.ApplicationEventPublisher.class),
                "America/Guayaquil");
    }

    @Test
    void rechazaDobleReserva() {
        when(citaRepo.findOverlap(1L, LocalDate.of(2026, 9, 15), LocalTime.of(9, 0), LocalTime.of(10, 0)))
                .thenReturn(java.util.List.of(cita(99L)));
        when(horarioRepo.findByOdontologoIdAndDiaSemanaAndEstado(1L, 2, "ACTIVO"))
                .thenReturn(horarioDisponible());
        when(agendaService.estaLibre(1L, LocalDate.of(2026, 9, 15), LocalTime.of(9, 0), LocalTime.of(10, 0)))
                .thenReturn(true);

        assertThrows(BusinessException.class, () -> servicio.crear(request()));
    }

    @Test
    void horarioFueraDeRangoRechaza() {
        when(citaRepo.findOverlap(any(), any(), any(), any())).thenReturn(java.util.List.of());
        when(horarioRepo.findByOdontologoIdAndDiaSemanaAndEstado(1L, 2, "ACTIVO"))
                .thenReturn(java.util.List.of());
        when(agendaService.estaLibre(1L, LocalDate.of(2026, 9, 15), LocalTime.of(9, 0), LocalTime.of(10, 0)))
                .thenReturn(true);
        assertThrows(BusinessException.class, () -> servicio.crear(request()));
    }

    @Test
    void confirmarSoloDesdePendiente() {
        Cita c = new Cita();
        c.setId(1L);
        c.setEstado(EstadoCita.CONFIRMADA);
        when(citaRepo.findById(1L)).thenReturn(Optional.of(c));
        assertThrows(BusinessException.class, () -> servicio.confirmar(1L));
    }

    @Test
    void confirmarDesdePendienteFunciona() {
        Cita c = citaCompleta(1L, EstadoCita.PENDIENTE);
        when(citaRepo.findById(1L)).thenReturn(Optional.of(c));
        when(citaRepo.save(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));
        var r = servicio.confirmar(1L);
        assertEquals(EstadoCita.CONFIRMADA, r.estado());
        assertTrue(r.confirmada());
    }

    @Test
    void cancelarRechazaCitaYaCancelada() {
        Cita c = citaCompleta(1L, EstadoCita.CANCELADA);
        when(citaRepo.findById(1L)).thenReturn(Optional.of(c));
        assertThrows(BusinessException.class, () -> servicio.cancelar(1L, null));
    }

    @Test
    void cancelarDesdeConfirmadaFunciona() {
        Cita c = citaCompleta(1L, EstadoCita.CONFIRMADA);
        when(citaRepo.findById(1L)).thenReturn(Optional.of(c));
        when(citaRepo.save(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));
        var r = servicio.cancelar(1L, null);
        assertEquals(EstadoCita.CANCELADA, r.estado());
    }

    private static Cita citaCompleta(Long id, EstadoCita estado) {
        Cita c = new Cita();
        c.setId(id);
        c.setEstado(estado);

        org.dentalcrm.domain.paciente.Paciente p = new org.dentalcrm.domain.paciente.Paciente();
        p.setId(1L);
        p.setNombres("Juan");
        p.setApellidos("Pérez");
        c.setPaciente(p);

        org.dentalcrm.domain.odontologo.Odontologo d = new org.dentalcrm.domain.odontologo.Odontologo();
        d.setId(1L);
        d.setNombres("María");
        d.setApellidos("López");
        c.setDoctor(d);

        org.dentalcrm.domain.servicio.Servicio s = new org.dentalcrm.domain.servicio.Servicio();
        s.setId(1L);
        s.setNombre("Limpieza");
        s.setDuracionMinutos(45);
        c.setServicio(s);

        c.setFecha(LocalDate.of(2026, 9, 15));
        c.setHoraInicio(LocalTime.of(9, 0));
        c.setHoraFin(LocalTime.of(9, 45));
        return c;
    }

    private static Cita cita(Long id) {
        Cita c = new Cita();
        c.setId(id);
        c.setEstado(EstadoCita.CONFIRMADA);
        c.setHoraInicio(LocalTime.of(9, 0));
        c.setHoraFin(LocalTime.of(10, 0));
        return c;
    }

    private static java.util.List<org.dentalcrm.domain.horario.HorarioOdontologo> horarioDisponible() {
        org.dentalcrm.domain.horario.HorarioOdontologo h = new org.dentalcrm.domain.horario.HorarioOdontologo();
        h.setHoraInicio(LocalTime.of(8, 0));
        h.setHoraFin(LocalTime.of(12, 0));
        return java.util.List.of(h);
    }

    private static org.dentalcrm.web.cita.dto.CitaRequest request() {
        return new org.dentalcrm.web.cita.dto.CitaRequest(
                1L, 1L, 1L, LocalDate.of(2026, 9, 15), LocalTime.of(9, 0), null);
    }
}