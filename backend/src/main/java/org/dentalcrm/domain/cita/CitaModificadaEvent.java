package org.dentalcrm.domain.cita;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Una cita cambió de fecha u hora. El paciente necesita saber el horario nuevo,
 * por eso el evento arrastra también el anterior.
 *
 * @param citaId       identificador de la cita
 * @param fechaAnterior día antes del cambio; {@code null} si no había
 * @param horaAnterior  hora antes del cambio; {@code null} si no había
 * @param fechaNueva    día después del cambio
 * @param horaNueva     hora después del cambio
 */
public record CitaModificadaEvent(Long citaId,
                                  LocalDate fechaAnterior,
                                  LocalTime horaAnterior,
                                  LocalDate fechaNueva,
                                  LocalTime horaNueva) {

    public static CitaModificadaEvent de(Long citaId) {
        return new CitaModificadaEvent(citaId, null, null, null, null);
    }

    /** Sólo un cambio de día u hora merece avisar al paciente. */
    public boolean cambioHorario() {
        return fechaNueva != null && horaNueva != null
                && (!fechaNueva.equals(fechaAnterior) || !horaNueva.equals(horaAnterior));
    }
}
