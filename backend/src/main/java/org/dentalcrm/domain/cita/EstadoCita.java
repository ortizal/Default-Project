package org.dentalcrm.domain.cita;

public enum EstadoCita {
    PENDIENTE,
    CONFIRMADA,
    ATENDIDA,
    CANCELADA,
    NO_ASISTIO;

    public boolean esActiva() {
        return this == PENDIENTE || this == CONFIRMADA || this == ATENDIDA;
    }
}