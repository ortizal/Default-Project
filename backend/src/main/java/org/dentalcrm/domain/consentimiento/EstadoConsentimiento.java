package org.dentalcrm.domain.consentimiento;

/**
 * Estado de un consentimiento: BORRADOR mientras se redacta y FIRMADO en
 * cuanto el paciente/tutor lo suscribe (a partir de ahí es inmutable).
 */
public enum EstadoConsentimiento {
    BORRADOR,
    FIRMADO
}
