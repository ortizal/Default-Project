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

    public static boolean corresponden(String a, String b) {
        String na = normalizar(a);
        String nb = normalizar(b);
        if (na.isEmpty() || nb.isEmpty()) {
            return false;
        }
        return na.equals(nb) || na.endsWith(nb) || nb.endsWith(na);
    }
}