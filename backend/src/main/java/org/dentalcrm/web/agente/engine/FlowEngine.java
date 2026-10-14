package org.dentalcrm.web.agente.engine;

import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.servicio.Servicio;
import org.dentalcrm.domain.whatsapp.Conversacion;
import org.dentalcrm.web.agente.contexto.ConversacionContexto;
import org.dentalcrm.web.agente.flujo.AgenteEntorno;
import org.dentalcrm.web.agente.flujo.Flujo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ejecuta el flujo activo (§2, prioridad 0): mientras hay flujo no se vuelve
 * al clasificador general, salvo que el flujo devuelva {@code ignorado}.
 *
 * <p>Interpreta la entrada contra el menú activo (§4), la confirma (§11) y las
 * entidades (§10/§12) antes de que el flujo decida; los reenvíos entre flujos
 * (§7, registro → agendamiento) conservan el texto original pero descartan la
 * opción ya consumida.
 */
public class FlowEngine {

    private static final Logger log = LoggerFactory.getLogger(FlowEngine.class);
    private static final int MAX_REENVIOS = 4;

    private final AgenteEntorno e;
    private final Map<String, Flujo> flujos = new HashMap<>();

    public FlowEngine(AgenteEntorno e, List<Flujo> flujos) {
        this.e = e;
        for (Flujo flujo : flujos) {
            this.flujos.put(flujo.codigo(), flujo);
        }
    }

    /** Mensaje del flujo activo: la entrada se resuelve contra su menú (§4). */
    public PasoResultado procesar(Conversacion conversacion, ConversacionContexto ctx,
                                  String texto, String crudo) {
        return ejecutar(conversacion, ctx, interpretar(ctx, texto, crudo, true));
    }

    /** Arranque de un flujo desde una intención: sólo entidades, sin opciones (§2). */
    public PasoResultado arrancar(Conversacion conversacion, ConversacionContexto ctx,
                                  String texto, String crudo) {
        return ejecutar(conversacion, ctx, interpretar(ctx, texto, crudo, false));
    }

    private PasoResultado ejecutar(Conversacion conversacion, ConversacionContexto ctx,
                                   EntradaFlujo entrada) {
        e.entrarEnConversacion(conversacion);
        try {
            return despachar(conversacion, ctx, entrada);
        } finally {
            e.salirDeConversacion();
        }
    }

    private PasoResultado despachar(Conversacion conversacion, ConversacionContexto ctx,
                                    EntradaFlujo entrada) {
        String prefijo = null;
        for (int intento = 0; intento < MAX_REENVIOS; intento++) {
            Flujo flujo = flujos.get(ctx.getFlujo());
            if (flujo == null) {
                return PasoResultado.ignorado();
            }
            PasoResultado resultado;
            try {
                resultado = flujo.responder(conversacion, ctx, entrada);
            } catch (Exception ex) {
                log.warn("Agente: fallo en el flujo {} de la conversación {}: {}",
                        ctx.getFlujo(), conversacion.getId(), ex.getMessage());
                return PasoResultado.ignorado();
            }
            if (resultado == null || resultado.esIgnorado()) {
                return PasoResultado.ignorado();
            }
            if (resultado.esReenviar()) {
                if (resultado.mensaje() != null && !resultado.mensaje().isBlank()) {
                    prefijo = resultado.mensaje();
                }
                entrada = EntradaFlujo.reenviar(entrada);
                continue;
            }
            resultado.anteponer(prefijo);
            return resultado;
        }
        return PasoResultado.ignorado();
    }

    private EntradaFlujo interpretar(ConversacionContexto ctx, String texto, String crudo,
                                     boolean conMenu) {
        List<Servicio> servicios = e.servicios.activos();
        List<Odontologo> odontologos = e.odontologos.activos();
        EntityExtractor.Entidades entidades = e.extractor.extraer(crudo, servicios, odontologos);
        org.dentalcrm.web.agente.contexto.OpcionMenu opcion = null;
        Confirmacion confirmacion = Confirmacion.NINGUNA;
        Integer numero = null;
        if (conMenu) {
            MenuResolver.ResultadoMenu menu = e.menus.resolver(ctx, texto);
            opcion = menu == null ? null : menu.opcion();
            confirmacion = e.confirmaciones.resolver(texto);
            numero = MenuResolver.numeroDe(texto);
        }
        boolean cedula = entidades.cedula() != null || e.soporte.esCedula(texto);
        return new EntradaFlujo(texto, crudo, opcion, numero, confirmacion,
                entidades, entidades.correccion(), cedula);
    }
}
