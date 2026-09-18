package org.dentalcrm.web.agente;

import org.dentalcrm.web.agente.ParserAgendamiento.EntradaAgendamiento;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParserAgendamientoTest {

    private final LocalDate hoy = LocalDate.of(2026, 9, 10); // jueves

    @Test
    void parsesRelativeDates() {
        assertEquals(hoy, ParserAgendamiento.fechaDelTexto("quiero una cita hoy", hoy).orElse(null));
        assertEquals(hoy.plusDays(1), ParserAgendamiento.fechaDelTexto("para mañana", hoy).orElse(null));
        assertEquals(hoy.plusDays(2), ParserAgendamiento.fechaDelTexto("pasado mañana", hoy).orElse(null));
    }

    @Test
    void parsesWeekdayAndExplicitDate() {
        assertEquals(hoy.plusDays(1), ParserAgendamiento.fechaDelTexto("el viernes", hoy).orElse(null)); // viernes
        assertEquals(hoy.plusDays(7), ParserAgendamiento.fechaDelTexto("el próximo jueves", hoy).orElse(null));
        assertEquals(LocalDate.of(2026, 9, 20), ParserAgendamiento.fechaDelTexto("el 20/09", hoy).orElse(null));
    }

    @Test
    void parsesTimes() {
        assertEquals(LocalTime.of(10, 0), ParserAgendamiento.horaDelTexto("a las 10").orElse(null));
        assertEquals(LocalTime.of(14, 30), ParserAgendamiento.horaDelTexto("a las 14:30").orElse(null));
        assertEquals(LocalTime.of(9, 45), ParserAgendamiento.horaDelTexto("9:45").orElse(null));
        assertEquals(LocalTime.of(17, 0), ParserAgendamiento.horaDelTexto("5 pm").orElse(null));
        assertEquals(LocalTime.of(12, 0), ParserAgendamiento.horaDelTexto("12 del medio día").orElse(null));
        assertTrue(ParserAgendamiento.horaDelTexto("hola que tal").isEmpty());
    }

    @Test
    void emptyInputYieldsEmptyEntry() {
        EntradaAgendamiento e = ParserAgendamiento.parsear("", hoy, List.of(), List.of());
        assertNull(e.fecha());
        assertNull(e.horaInicio());
        assertNull(e.servicioId());
        assertNull(e.doctorId());
    }
}