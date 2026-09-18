package org.dentalcrm.web.paciente;

import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PacienteServiceTest {

    private PacienteService servicio;
    private PacienteRepository repo;

    @BeforeEach
    void setUp() {
        repo = mock(PacienteRepository.class);
        servicio = new PacienteService(repo, mock(AuditService.class));
    }

    @Test
    void rechazaCedulaDuplicada() {
        Paciente existente = new Paciente();
        existente.setId(1L);
        when(repo.existeGlobalCedula("1712345678")).thenReturn(true);

        assertThrows(BusinessException.class, () -> servicio.crear(pacienteConCedula("1712345678")));
    }

    @Test
    void noValidaCedulaCuandoEsNula() {
        when(repo.save(any(Paciente.class))).thenAnswer(inv -> inv.getArgument(0));
        servicio.crear(pacienteConCedula(null));
    }

    @Test
    void rechazaPacienteInexistente() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> servicio.obtener(99L));
    }

    private static org.dentalcrm.web.paciente.dto.PacienteRequest pacienteConCedula(String cedula) {
        return new org.dentalcrm.web.paciente.dto.PacienteRequest(
                cedula, "Juan", "Pérez", "0991234567", "juan@mail.com", null, null, null, null, List.of());
    }

    @Test
    void rechazaMenorSinTutor() {
        assertThrows(BusinessException.class,
                () -> servicio.crear(pacienteConCedulaYFecha(java.time.LocalDate.now().minusYears(10))));
    }

    private static org.dentalcrm.web.paciente.dto.PacienteRequest pacienteConCedulaYFecha(java.time.LocalDate nacimiento) {
        return new org.dentalcrm.web.paciente.dto.PacienteRequest(
                null, "Juan", "Pérez", null, null, nacimiento, null, null, null, List.of());
    }
}