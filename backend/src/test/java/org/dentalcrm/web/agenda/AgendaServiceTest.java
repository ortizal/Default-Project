package org.dentalcrm.web.agenda;

import org.dentalcrm.domain.bloqueo.BloqueoAgenda;
import org.dentalcrm.domain.cita.Cita;
import org.dentalcrm.domain.cita.EstadoCita;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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