package org.dentalcrm.web.agente.engine;

import org.dentalcrm.web.agente.AgenteSoporte;

/**
 * Interpreta sí/no antes de cualquier otra lectura del paso (§11).
 */
public class ConfirmationResolver {

    private final AgenteSoporte soporte;

    public ConfirmationResolver(AgenteSoporte soporte) {
        this.soporte = soporte;
    }

    public Confirmacion resolver(String texto) {
        String t = AgenteSoporte.compactar(texto);
        if (t.isEmpty()) {
            return Confirmacion.NINGUNA;
        }
        if (soporte.esConfirmacion(t)) {
            return Confirmacion.SI;
        }
        if (soporte.esRechazo(t)) {
            return Confirmacion.NO;
        }
        return Confirmacion.NINGUNA;
    }
}
