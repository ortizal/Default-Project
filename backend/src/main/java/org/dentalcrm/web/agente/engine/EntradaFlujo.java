package org.dentalcrm.web.agente.engine;

import org.dentalcrm.web.agente.contexto.OpcionMenu;

/**
 * Entrada ya interpretada contra el contexto: lo que el usuario escribió,
 * resuelto como opción de menú, confirmación y/o entidades (§22).
 *
 * @param texto         texto normalizado (minúsculas y sin tildes)
 * @param crudo         texto tal cual lo escribió el usuario
 * @param opcion        opción del menú activo que corresponde, si la hay
 * @param numero        número escrito, si lo hay (aunque esté fuera de rango)
 * @param confirmacion  SÍ/NO si el texto es una confirmación
 * @param entidades     fechas, horas, nombres detectados
 * @param correccion    corrección explícita del dato actual (§12)
 * @param esCedula      el texto parece una cédula
 */
public record EntradaFlujo(
        String texto,
        String crudo,
        OpcionMenu opcion,
        Integer numero,
        Confirmacion confirmacion,
        EntityExtractor.Entidades entidades,
        EntityExtractor.Correccion correccion,
        boolean esCedula
) {
    /** Entrada sin contenido: se usa al reenviar el control entre flujos (§2). */
    public static EntradaFlujo neutral() {
        return new EntradaFlujo("", "", null, null, Confirmacion.NINGUNA,
                EntityExtractor.Entidades.vacias(), null, false);
    }

    /** Reenvío entre flujos: conserva el texto y las entidades, no la opción (§2). */
    public static EntradaFlujo reenviar(EntradaFlujo anterior) {
        return new EntradaFlujo(anterior == null ? "" : anterior.texto(),
                anterior == null ? "" : anterior.crudo(),
                null, null, Confirmacion.NINGUNA,
                anterior == null ? EntityExtractor.Entidades.vacias() : anterior.entidades(),
                anterior == null ? null : anterior.correccion(),
                false);
    }

    public boolean tieneOpcion() {
        return opcion != null;
    }

    public boolean esConfirmacion() {
        return confirmacion == Confirmacion.SI;
    }

    public boolean esRechazo() {
        return confirmacion == Confirmacion.NO;
    }
}
