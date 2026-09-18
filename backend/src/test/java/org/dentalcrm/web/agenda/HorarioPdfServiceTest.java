package org.dentalcrm.web.agenda;

import org.dentalcrm.domain.horario.HorarioOdontologo;
import org.dentalcrm.domain.odontologo.Odontologo;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HorarioPdfServiceTest {

    @Test
    void generarProduceUnPdfValido() {
        HorarioPdfService servicio = new HorarioPdfService("Clínica Test", "Av. Principal 123", "0999999999");

        Odontologo o = new Odontologo();
        o.setNombres("Carlos");
        o.setApellidos("Perez");
        o.setEspecialidad("Odontología General");

        HorarioOdontologo h = new HorarioOdontologo();
        h.setDiaSemana(DayOfWeek.MONDAY.getValue());
        h.setHoraInicio(LocalTime.of(8, 0));
        h.setHoraFin(LocalTime.of(17, 0));
        h.setEstado("ACTIVO");

        byte[] pdf = servicio.generar(o, List.of(h), List.of("• jueves 18/09 a las 09:00"));

        assertNotNull(pdf);
        assertTrue(pdf.length > 100);
        String inicio = new String(pdf, 0, Math.min(8, pdf.length), StandardCharsets.ISO_8859_1);
        assertTrue(inicio.startsWith("%PDF"));
        assertEquals("horario-perez.pdf", servicio.nombreArchivo(o));
    }

    @Test
    void nombreArchivoSaneaLasTildes() {
        HorarioPdfService servicio = new HorarioPdfService("Clínica", null, null);
        Odontologo o = new Odontologo();
        o.setNombres("Ana");
        o.setApellidos("Gómez");

        assertEquals("horario-gomez.pdf", servicio.nombreArchivo(o));
    }
}