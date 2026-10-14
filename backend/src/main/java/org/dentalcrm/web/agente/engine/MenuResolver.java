package org.dentalcrm.web.agente.engine;

import org.dentalcrm.web.agente.AgenteSoporte;
import org.dentalcrm.web.agente.contexto.ConversacionContexto;
import org.dentalcrm.web.agente.contexto.OpcionMenu;

/**
 * Resuelve un número (o el texto de una etiqueta) contra el menú activo (§4).
 *
 * <p>Es la corrección central del bug original: {@code MENU_PRINCIPAL + 4}
 * debe devolver {@code CONSULTAR_DISPONIBILIDAD} sin volver a clasificar.
 */
public class MenuResolver {

    private final AgenteSoporte soporte;

    public MenuResolver(AgenteSoporte soporte) {
        this.soporte = soporte;
    }

    /**
     * @param opcion  opción elegida, o {@code null} si el texto no es una opción
     * @param fueraDeRango número escrito que no existe en el menú activo (§14)
     */
    public record ResultadoMenu(OpcionMenu opcion, Integer fueraDeRango) {
        public boolean encontrado() {
            return opcion != null;
        }
    }

    public ResultadoMenu resolver(ConversacionContexto ctx, String texto) {
        String t = AgenteSoporte.compactar(texto);
        Integer numero = numeroDe(t);

        if (ctx.tieneOpciones()) {
            if (numero != null) {
                for (OpcionMenu opcion : ctx.getOpciones()) {
                    if (opcion.numero() == numero) {
                        return new ResultadoMenu(opcion, null);
                    }
                }
                return new ResultadoMenu(null, numero);
            }
            OpcionMenu porEtiqueta = porEtiqueta(ctx, t);
            if (porEtiqueta != null) {
                return new ResultadoMenu(porEtiqueta, null);
            }
            return new ResultadoMenu(null, null);
        }

        if (ctx.getMenuActual() == org.dentalcrm.web.agente.contexto.MenuActual.PRINCIPAL && numero != null) {
            if (numero >= 1 && numero <= soporte.opcionesPrincipales().size()) {
                return new ResultadoMenu(soporte.opcionesPrincipales().get(numero - 1), null);
            }
            return new ResultadoMenu(null, numero);
        }
        return new ResultadoMenu(null, null);
    }

    /** La etiqueta puede escribirse con errores: fuzzy matching auxiliar (§6). */
    private OpcionMenu porEtiqueta(ConversacionContexto ctx, String t) {
        if (t.isEmpty() || t.matches("\\d+")) {
            return null;
        }
        OpcionMenu mejor = null;
        int mejorDistancia = Integer.MAX_VALUE;
        for (OpcionMenu opcion : ctx.getOpciones()) {
            if (!soporte.coincide(opcion.label(), t)) {
                continue;
            }
            int d = AgenteSoporte.distanciaEtiquetas(opcion.label(), t);
            if (d < mejorDistancia) {
                mejorDistancia = d;
                mejor = opcion;
            }
        }
        return mejor;
    }

    public static Integer numeroDe(String t) {
        String v = AgenteSoporte.compactar(t).replaceAll("^(opcion|opcion|el|la|numero|#)\\s*", "");
        if (v.matches("\\d{1,2}")) {
            return Integer.parseInt(v);
        }
        return null;
    }
}
