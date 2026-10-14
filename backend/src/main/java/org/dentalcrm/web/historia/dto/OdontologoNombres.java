package org.dentalcrm.web.historia.dto;

import org.dentalcrm.domain.odontologo.Odontologo;

/** Nombre completo de un odontólogo o {@code null}. */
final class OdontologoNombres {

    private OdontologoNombres() {
    }

    static String de(Odontologo o) {
        if (o == null) {
            return null;
        }
        return (o.getNombres() + " " + o.getApellidos()).trim();
    }
}
