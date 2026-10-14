package org.dentalcrm.web.agente.flujo;

import org.dentalcrm.domain.whatsapp.Conversacion;
import org.dentalcrm.web.agente.contexto.ConversacionContexto;
import org.dentalcrm.web.agente.engine.EntradaFlujo;
import org.dentalcrm.web.agente.engine.PasoResultado;

/**
 * Un flujo conversacional: interpreta la entrada contra el paso activo y
 * devuelve el resultado estructurado del §18.
 */
public interface Flujo {

    /** Código del flujo; coincide con {@link ConversacionContexto#getFlujo()}. */
    String codigo();

    /**
     * @param conversacion conversación en curso (paciente vinculado, teléfono)
     * @param ctx          contexto mutable; el flujo actualiza paso/opciones/etc.
     * @param entrada      entrada ya interpretada contra el contexto
     */
    PasoResultado responder(Conversacion conversacion, ConversacionContexto ctx, EntradaFlujo entrada);
}
