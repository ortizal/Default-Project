package org.dentalcrm.web.agente.flujo;

import org.dentalcrm.domain.whatsapp.Conversacion;
import org.dentalcrm.domain.whatsapp.EstadoConversacion;
import org.dentalcrm.web.agente.contexto.ConversacionContexto;
import org.dentalcrm.web.agente.contexto.MenuActual;
import org.dentalcrm.web.agente.engine.EntradaFlujo;
import org.dentalcrm.web.agente.engine.PasoResultado;

/**
 * Transfiere a una persona de recepción (§13): conserva el contexto para que
 * el equipo sepa en qué punto quedó la conversación.
 */
public class HumanTransferFlow extends FlujoBase {

    public static final String CODIGO = "TRANSFERIR_HUMANO";

    public HumanTransferFlow(AgenteEntorno e) {
        super(e);
    }

    @Override
    public String codigo() {
        return CODIGO;
    }

    @Override
    public PasoResultado responder(Conversacion conversacion, ConversacionContexto ctx, EntradaFlujo entrada) {
        conversacion.setEstado(EstadoConversacion.ATENCION_HUMANA);
        ctx.setFlujo(CODIGO);
        ctx.setPaso(null);
        ctx.setMenuActual(MenuActual.PRINCIPAL);
        ctx.setOpciones(soporte().opcionesPrincipales());
        String texto = "Claro 👍 Te voy a comunicar con una persona de nuestro equipo. ¡Un momento por favor!";
        ctx.setUltimaPregunta(texto);
        return PasoResultado.continuar(texto).transferir();
    }
}
