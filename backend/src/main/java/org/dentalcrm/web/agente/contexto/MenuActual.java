package org.dentalcrm.web.agente.contexto;

/**
 * Menú visible en pantalla; junto con {@link ConversacionContexto#getOpciones()}
 * determina el significado de un número (§4 y §11).
 */
public enum MenuActual {
    PRINCIPAL,
    SERVICIOS,
    ODONTOLOGOS,
    FECHAS,
    FRANJAS,
    HORAS,
    POST_DISPO,
    CONFIRMACION,
    GESTION_CITA,
    PACIENTES,
    PARA_QUIEN,
    REANUDACION,
    REGISTRO
}
