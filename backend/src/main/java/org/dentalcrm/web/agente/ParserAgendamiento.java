package org.dentalcrm.web.agente;

import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.servicio.Servicio;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser de agendamiento natural en español: extrae fecha, hora, servicio y
 * odontólogo a partir de un mensaje libre ("cita con el dr perez manana a las 10",
 * "viernes 19", "10:30", "limpieza", ...).
 */
final class ParserAgendamiento {

    record EntradaAgendamiento(LocalDate fecha, LocalTime horaInicio, Long servicioId, Long doctorId) {
        static EntradaAgendamiento vacia() {
            return new EntradaAgendamiento(null, null, null, null);
        }
    }

    private static final Map<String, Integer> DIAS = Map.ofEntries(
            Map.entry("lunes", 1), Map.entry("martes", 2), Map.entry("miercoles", 3),
            Map.entry("jueves", 4), Map.entry("viernes", 5), Map.entry("sabado", 6),
            Map.entry("domingo", 7));

    private static final Set<String> PALABRAS_SIN_PESO = Set.of(
            "alguna", "alguna vez", "usted", "tengo", "tener", "tienes", "tiene",
            "quiero", "queria", "quisiera", "necesito", "podria", "podrias", "puedes",
            "puedo", "para", "favor", "con", "del", "una", "unas", "por", "realizar",
            "pedir", "hacer", "manera", "saber", "darte", "darle", "avisa", "avisanos");

    private static final Pattern P_PASADO_MANANA = Pattern.compile("\\bpasad[oa]\\s+manana\\b");
    private static final Pattern P_MANANA = Pattern.compile("\\bmanana\\b");
    private static final Pattern P_HOY = Pattern.compile("\\bhoy\\b");
    private static final Pattern P_ISO = Pattern.compile("(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})");
    private static final Pattern P_DIA_MES = Pattern.compile("\\b(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{2,4}))?\\b");
    private static final Pattern P_HORA_LA = Pattern.compile("(?:a\\s*la|a\\s*las|las)\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?");
    private static final Pattern P_HORA_DECIMAL = Pattern.compile("\\b(\\d{1,2})[:.](\\d{2})\\b");
    private static final Pattern P_HORA_AMPM = Pattern.compile("\\b(\\d{1,2})\\s*(am|pm)\\b");
    private static final Pattern P_INDICE = Pattern.compile("(?:opcion|opcion|el|la|numero|#)?\\s*\\b(\\d{1,2})\\b\\s*$");

    private ParserAgendamiento() {
    }

    static EntradaAgendamiento parsear(String texto, LocalDate hoy,
                                       List<Servicio> servicios, List<Odontologo> odontologos) {
        String t = normalizar(texto);
        return new EntradaAgendamiento(
                fechaDelTexto(t, hoy).orElse(null),
                horaDelTexto(t).orElse(null),
                servicioDelTexto(t, servicios).orElse(null),
                doctorDelTexto(t, odontologos).orElse(null));
    }

    static Optional<LocalDate> fechaDelTexto(String t, LocalDate hoy) {
        t = normalizar(t);
        if (t.isBlank()) {
            return Optional.empty();
        }
        if (P_PASADO_MANANA.matcher(t).find()) {
            return Optional.of(hoy.plusDays(2));
        }
        if (P_MANANA.matcher(t).find()) {
            return Optional.of(hoy.plusDays(1));
        }
        if (P_HOY.matcher(t).find()) {
            return Optional.of(hoy);
        }
        for (Map.Entry<String, Integer> e : DIAS.entrySet()) {
            if (contienePalabra(t, e.getKey())) {
                int offset = (e.getValue() - hoy.getDayOfWeek().getValue() + 7) % 7;
                boolean proximo = contienePalabra(t, "proximo") || contienePalabra(t, "proxima");
                if (offset == 0 && proximo) {
                    offset = 7;
                }
                return Optional.of(hoy.plusDays(offset));
            }
        }
        Matcher iso = P_ISO.matcher(t);
        if (iso.find()) {
            try {
                return Optional.of(LocalDate.of(entero(iso, 1), entero(iso, 2), entero(iso, 3)));
            } catch (Exception ex) {
                return Optional.empty();
            }
        }
        Matcher dm = P_DIA_MES.matcher(t);
        if (dm.find()) {
            int dia = entero(dm, 1);
            int mes = entero(dm, 2);
            int anio = dm.group(3) != null ? anioCompleto(entero(dm, 3)) : hoy.getYear();
            try {
                LocalDate fecha = LocalDate.of(anio, mes, dia);
                if (!fecha.isBefore(hoy)) {
                    return Optional.of(fecha);
                }
            } catch (Exception ex) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    static Optional<LocalTime> horaDelTexto(String t) {
        t = normalizar(t);
        if (contienePalabra(t, "mediodia") || t.contains("medio dia")) {
            return Optional.of(LocalTime.NOON);
        }
        Matcher la = P_HORA_LA.matcher(t);
        if (la.find()) {
            int min = la.group(2) != null ? entero(la, 2) : 0;
            return horaConMeridiano(entero(la, 1), min, la.group(3));
        }
        Matcher dec = P_HORA_DECIMAL.matcher(t);
        if (dec.find()) {
            return horaConMeridiano(entero(dec, 1), entero(dec, 2), null);
        }
        Matcher ap = P_HORA_AMPM.matcher(t);
        if (ap.find()) {
            return horaConMeridiano(entero(ap, 1), 0, ap.group(2));
        }
        return Optional.empty();
    }

    static Optional<Long> servicioDelTexto(String t, List<Servicio> servicios) {
        if (servicios.isEmpty()) {
            return Optional.empty();
        }
        Optional<Integer> indice = indiceDeLista(t, servicios.size());
        if (indice.isPresent()) {
            return Optional.of(servicios.get(indice.get() - 1).getId());
        }
        for (Servicio s : servicios) {
            if (contienePalabra(t, normalizar(s.getNombre()))) {
                return Optional.of(s.getId());
            }
        }
        for (Servicio s : servicios) {
            String nombre = normalizar(s.getNombre());
            for (String pal : tokensSignificativos(t)) {
                if (pal.length() >= 4 && contienePalabra(nombre, pal)) {
                    return Optional.of(s.getId());
                }
            }
        }
        return Optional.empty();
    }

    static Optional<Long> doctorDelTexto(String t, List<Odontologo> odontologos) {
        if (odontologos.isEmpty()) {
            return Optional.empty();
        }
        Optional<Integer> indice = indiceDeLista(t, odontologos.size());
        if (indice.isPresent()) {
            return Optional.of(odontologos.get(indice.get() - 1).getId());
        }
        for (Odontologo o : odontologos) {
            String nombreCompleto = normalizar(o.getNombres() + " " + o.getApellidos());
            if (contienePalabra(t, nombreCompleto)) {
                return Optional.of(o.getId());
            }
        }
        for (Odontologo o : odontologos) {
            String nombreCompleto = normalizar(o.getNombres() + " " + o.getApellidos());
            for (String pal : tokensSignificativos(t)) {
                if (pal.startsWith("dr") || pal.equals(normalizar(o.getApellidos()))) {
                    if (contienePalabra(nombreCompleto, pal)) {
                        return Optional.of(o.getId());
                    }
                }
            }
        }
        for (Odontologo o : odontologos) {
            if (coincideEtiqueta(t, o.getEtiquetas())) {
                return Optional.of(o.getId());
            }
        }
        return Optional.empty();
    }

    private static boolean coincideEtiqueta(String t, String etiquetas) {
        if (etiquetas == null || etiquetas.isBlank() || t == null || t.isBlank()) {
            return false;
        }
        for (String et : normalizar(etiquetas).split("[^a-z0-9]+")) {
            if (et.length() >= 4) {
                for (String pal : tokensSignificativos(t)) {
                    if (pal.equals(et)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.trim().toLowerCase(Locale.ROOT);
    }

    static boolean contienePalabra(String texto, String palabra) {
        if (texto == null || palabra == null || palabra.isBlank()) {
            return false;
        }
        return texto.matches("(?s).*\\b" + Pattern.quote(palabra) + "\\b.*");
    }

    private static Optional<LocalTime> horaConMeridiano(int hora, int minuto, String meridiano) {
        if (minuto < 0 || minuto > 59) {
            return Optional.empty();
        }
        int h = hora;
        if (meridiano != null) {
            if (meridiano.equals("pm")) {
                h = hora == 12 ? 12 : hora + 12;
            } else {
                h = hora == 12 ? 0 : hora;
            }
        }
        if (h < 0 || h > 23) {
            return Optional.empty();
        }
        return Optional.of(LocalTime.of(h, minuto));
    }

    private static Optional<Integer> indiceDeLista(String t, int tamano) {
        String t2 = t.trim();
        if (t2.matches("\\d{1,2}")) {
            try {
                int v = Integer.parseInt(t2);
                if (v >= 1 && v <= tamano) {
                    return Optional.of(v);
                }
            } catch (NumberFormatException ignored) {
                // no-op
            }
            return Optional.empty();
        }
        Matcher m = P_INDICE.matcher(t2);
        if (m.find() && !m.group(1).isEmpty() && t2.length() <= 12) {
            int v = Integer.parseInt(m.group(1));
            if (v >= 1 && v <= tamano) {
                return Optional.of(v);
            }
        }
        return Optional.empty();
    }

    private static Set<String> tokensSignificativos(String t) {
        Set<String> tokens = new java.util.LinkedHashSet<>();
        for (String pal : t.split("[^a-z0-9]+")) {
            if (pal.length() >= 4 && !PALABRAS_SIN_PESO.contains(pal)) {
                tokens.add(pal);
            }
        }
        return tokens;
    }

    private static int entero(Matcher m, int grupo) {
        return Integer.parseInt(m.group(grupo));
    }

    private static int anioCompleto(int anio) {
        return anio < 100 ? 2000 + anio : anio;
    }
}