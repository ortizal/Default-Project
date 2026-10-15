package org.dentalcrm.domain.automatizacion;

public enum EventoAutomatizacion {
    CITA_CREADA, CITA_PROXIMA, CITA_CONFIRMADA, CITA_CANCELADA, CITA_ATENDIDA, NO_ASISTIO, CITA_MODIFICADA,
    /** Pago recibido en la cuenta del paciente (abono o anticipo), plan §3.3/§3.4. */
    COBRO_REALIZADO
}
