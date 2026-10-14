package org.dentalcrm.web.agente.contexto;

/**
 * Opción numérica de un menú enviado al usuario (§4 de las instrucciones).
 *
 * <p>Cada lista que se muestra en el chat persiste su mapa para que un
 * posterior {@code 2} se resuelva contra las opciones reales y nunca vuelva
 * al clasificador general.
 *
 * @param numero  número que ve el usuario (1-based)
 * @param id      entidad asociada (puede ser {@code null})
 * @param tipo    qué representa la opción
 * @param label   texto mostrado en el chat
 * @param valor   valor adicional: intención, franja, hora, acción, etc.
 */
public record OpcionMenu(int numero, Long id, TipoOpcion tipo, String label, String valor) {

    public static OpcionMenu menu(int numero, String label, String intencion) {
        return new OpcionMenu(numero, null, TipoOpcion.MENU, label, intencion);
    }

    public static OpcionMenu servicio(int numero, Long id, String label) {
        return new OpcionMenu(numero, id, TipoOpcion.SERVICIO, label, null);
    }

    public static OpcionMenu servicio(int numero, Long id, String label, String valor) {
        return new OpcionMenu(numero, id, TipoOpcion.SERVICIO, label, valor);
    }

    public static OpcionMenu odontologo(int numero, Long id, String label) {
        return new OpcionMenu(numero, id, TipoOpcion.ODONTOLOGO, label, null);
    }

    public static OpcionMenu odontologo(int numero, Long id, String label, String valor) {
        return new OpcionMenu(numero, id, TipoOpcion.ODONTOLOGO, label, valor);
    }

    public static OpcionMenu franja(int numero, String valor, String label) {
        return new OpcionMenu(numero, null, TipoOpcion.FRANJA, label, valor);
    }

    public static OpcionMenu hora(int numero, String hora, String label) {
        return new OpcionMenu(numero, null, TipoOpcion.HORA, label, hora);
    }

    public static OpcionMenu paciente(int numero, Long id, String label, String valor) {
        return new OpcionMenu(numero, id, TipoOpcion.PACIENTE, label, valor);
    }

    public static OpcionMenu accion(int numero, String valor, String label) {
        return new OpcionMenu(numero, null, TipoOpcion.ACCION, label, valor);
    }

    public static OpcionMenu decision(int numero, String valor, String label) {
        return new OpcionMenu(numero, null, TipoOpcion.DECISION, label, valor);
    }
}
