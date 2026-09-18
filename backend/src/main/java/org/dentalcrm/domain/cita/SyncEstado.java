package org.dentalcrm.domain.cita;

public enum SyncEstado {
    NO_SYNC,
    PENDING,
    SYNCED,
    ERROR;

    public boolean requiereReintento() {
        return this == PENDING || this == ERROR;
    }
}