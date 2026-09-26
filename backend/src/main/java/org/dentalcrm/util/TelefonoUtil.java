package org.dentalcrm.util;

public final class TelefonoUtil {

    private TelefonoUtil() {
    }

    public static String digitos(String telefono) {
        if (telefono == null) {
            return "";
        }
        return telefono.replaceAll("\\D", "");
    }

    public static String normalizar(String telefono) {
        String d = digitos(telefono);
        while (d.startsWith("0")) {
            d = d.substring(1);
        }
        return d;
    }

    /**
     * Devuelve el teléfono en formato E.164 ({@code +593991112233}).
     *
     * <p>Sólo se normaliza lo que se reconoce: un número internacional escrito con
     * {@code +} o un número ecuatoriano en formato local ({@code 09…}, {@code 9…},
     * {@code 593…}). Cualquier otra cosa se devuelve tal cual, sin romperla.
     */
    public static String aE164(String telefono) {
        if (telefono == null) {
            return null;
        }
        String texto = telefono.trim();
        if (texto.isEmpty()) {
            return null;
        }
        String d = digitos(texto);
        if (d.isEmpty()) {
            return null;
        }
        if (texto.startsWith("+")) {
            return "+" + d;
        }
        if (d.startsWith("593") && d.length() >= 11) {
            return "+" + d;
        }
        // Móvil local: 09XXXXXXXX (10 dígitos)
        if (d.length() == 10 && d.startsWith("09")) {
            return "+593" + d.substring(1);
        }
        // Móvil sin cero inicial: 9XXXXXXXX (9 dígitos)
        if (d.length() == 9 && d.startsWith("9")) {
            return "+593" + d;
        }
        // Fijo local: 0NXXXXXXX (9 dígitos)
        if (d.length() == 9 && d.startsWith("0")) {
            return "+593" + d.substring(1);
        }
        return texto;
    }

    /** Forma legible para mostrar al usuario: {@code +593 99 111 2233}. */
    public static String mostrar(String telefono) {
        String e164 = aE164(telefono);
        if (e164 == null || !e164.startsWith("+593") || e164.length() != 13) {
            return telefono == null ? null : telefono.trim();
        }
        String n = e164.substring(4);
        return "+593 " + n.substring(0, 2) + " " + n.substring(2, 5) + " " + n.substring(5);
    }

    public static boolean corresponden(String a, String b) {
        String na = normalizar(a);
        String nb = normalizar(b);
        if (na.isEmpty() || nb.isEmpty()) {
            return false;
        }
        return na.equals(nb) || na.endsWith(nb) || nb.endsWith(na);
    }
}