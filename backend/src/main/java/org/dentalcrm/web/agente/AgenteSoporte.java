package org.dentalcrm.web.agente;

import org.dentalcrm.domain.horario.HorarioOdontologo;
import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.servicio.Servicio;
import org.dentalcrm.web.agenda.dto.SlotResponse;
import org.dentalcrm.web.agente.contexto.MenuActual;
import org.dentalcrm.web.agente.contexto.OpcionMenu;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilidades compartidas por el motor y los flujos: formato de mensajes,
 * parsing auxiliar y fuzzy matching (§6 de las instrucciones).
 *
 * <p>El fuzzy matching "apoya, no decide operaciones": sólo se usa para
 * emparejar un texto contra las opciones ya mostradas o contra el menú.
 */
public class AgenteSoporte {

    /** Marca mostrada por el asistente. */
    public static final String MARCA = "Little Smile";

    public static final String MENSAJE_MENU = """
            ¿En qué puedo ayudarte?

            1. 📅 Agendar una cita
            2. 📋 Ver mis citas
            3. 🕐 Consultar disponibilidad
            4. ✅ Confirmar o cancelar una cita
            5. 👤 Ver mi información
            6. 💬 Hablar con recepción

            Puedes responder con el número o escribir lo que necesitas.
            """;

    public static final String MENSAJE_SIN_PACIENTE = """
            ¡Hola! 😊 Soy el asistente de %s. Para ayudarte necesito encontrarte en nuestro sistema.\n\
            Envía tu número de cédula para localizarte, o escribe "registrarme" si eres nuevo.
            """.formatted(MARCA);

    public static final String MENSAJE_PREGUNTA_CEDULA = """
            No te encuentro en nuestro sistema.\n\
            Envía tu número de cédula o escribe "registrarme" para crear tu ficha de paciente.
            """;

    public static final String MENSAJE_PREGUNTA_NOMBRE = """
            Perfecto, voy a crear tu ficha de paciente 📝\n¿Cuál es tu nombre?
            """;

    public static final String MENSAJE_PREGUNTA_APELLIDOS = "¿Y tus apellidos?";
    public static final String MENSAJE_PREGUNTA_FECHA =
            "¿Cuál es tu fecha de nacimiento?\nEscríbela así: 15/08/1990";
    public static final String MENSAJE_PREGUNTA_TUTOR = """
            Como eres menor de edad, necesito registrar a tu tutor legal (padre o madre).\n\
            ¿Quién será tu tutor? Responde "padre" o "madre".
            """;

    /** Retomar una conversación interrumpida (§15): continúa o empieza de nuevo. */
    public static String mensajeReanudacion(String pendiente) {
        return "Hola nuevamente 👋. Estábamos en medio de este proceso:\n"
                + (pendiente == null || pendiente.isBlank() ? "—" : pendiente)
                + "\n\n1. Continuar\n2. Empezar de nuevo";
    }

    public static final String[] ETIQUETAS_MENU = {
            "Agendar una cita", "Ver mis citas",
            "Consultar disponibilidad", "Confirmar o cancelar una cita",
            "Ver mi información", "Hablar con recepción"
    };

    private static final Pattern P_PALABRA_AFIRM =
            Pattern.compile("(^|[^a-z])(si|sí|claro|seguro|ok|de acuerdo|confirmo|dale|correcto|exacto|perfecto|s|ya)([^a-z]|$)");
    private static final Pattern P_PALABRA_NEG =
            Pattern.compile("(^|[^a-z])(no|nop|nel|mejor no|no gracias|cancelar|cancela|olvidalo|ninguno)([^a-z]|$)");

    private final ZoneId zona;

    private final String agendaPublicaBaseUrl;

    public AgenteSoporte(String timezone) {
        this(timezone, "http://localhost:4200/dental_crm/agenda-publica");
    }

    public AgenteSoporte(String timezone, String agendaPublicaBaseUrl) {
        this.zona = ZoneId.of(timezone);
        this.agendaPublicaBaseUrl = agendaPublicaBaseUrl;
    }

    /** Enlace público del calendario con el token firmado (§10). */
    public String urlAgendaPublica(String token) {
        String base = agendaPublicaBaseUrl == null || agendaPublicaBaseUrl.isBlank()
                ? "http://localhost:4200/dental_crm/agenda-publica" : agendaPublicaBaseUrl;
        return base + "?t=" + token;
    }

    public LocalDate hoy() {
        return LocalDate.now(zona);
    }

    // ------------------------------------------------------------------
    // Menú principal (§5)
    // ------------------------------------------------------------------

    public List<OpcionMenu> opcionesPrincipales() {
        return List.of(
                OpcionMenu.menu(1, ETIQUETAS_MENU[0], "AGENDAR_CITA"),
                OpcionMenu.menu(2, ETIQUETAS_MENU[1], "VER_CITAS"),
                OpcionMenu.menu(3, ETIQUETAS_MENU[2], "PREGUNTAR_DISPONIBILIDAD"),
                OpcionMenu.menu(4, ETIQUETAS_MENU[3], "GESTIONAR_CITA"),
                OpcionMenu.menu(5, ETIQUETAS_MENU[4], "VER_PERFIL"),
                OpcionMenu.menu(6, ETIQUETAS_MENU[5], "TRANSFERIR_HUMANO"));
    }

    /** Texto del menú principal para reenviarlo cuando el usuario lo pide. */
    public String menuTexto() {
        return MENSAJE_MENU;
    }

    public String mensajeSaludo() {
        return "¡Hola! 👋 Soy el asistente virtual de " + MARCA + " 🦷.\n" + MENSAJE_MENU;
    }

    public MenuActual menuPrincipal() {
        return MenuActual.PRINCIPAL;
    }

    // ------------------------------------------------------------------
    // Confirmaciones (§11)
    // ------------------------------------------------------------------

    public boolean esConfirmacion(String t) {
        return P_PALABRA_AFIRM.matcher(" " + (t == null ? "" : t) + " ").find();
    }

    public boolean esRechazo(String t) {
        return P_PALABRA_NEG.matcher(" " + (t == null ? "" : t) + " ").find();
    }

    public boolean esCedula(String t) {
        String digitos = t == null ? "" : t.replaceAll("\\D", "");
        return digitos.length() >= 7 && digitos.length() <= 10;
    }

    public boolean contiene(String t, Set<String> palabras) {
        for (String p : palabras) {
            if (ParserAgendamiento.contienePalabra(t, p)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Normalización y fuzzy matching (§6)
    // ------------------------------------------------------------------

    public static String normalizar(String texto) {
        return ParserAgendamiento.normalizar(texto);
    }

    /** Compacta el texto: minúsculas, sin tildes y con un solo espacio. */
    public static String compactar(String texto) {
        String n = normalizar(texto);
        return n.replaceAll("\\s+", " ").trim();
    }

    /**
     * Coincidencia tolerante a errores de tipeo: igualdad exacta, igualdad por
     * prefijo razonable o distancia de edición corta.
     */
    public static boolean coincide(String a, String b) {
        String x = compactar(a);
        String y = compactar(b);
        if (x.isEmpty() || y.isEmpty()) {
            return false;
        }
        if (x.equals(y)) {
            return true;
        }
        if (x.contains(y) || y.contains(x)) {
            int menor = Math.min(x.length(), y.length());
            if (menor >= 4) {
                return true;
            }
        }
        int tolerancia = y.length() <= 6 ? 1 : 2;
        return distancia(x, y) <= tolerancia;
    }

    public static int distancia(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] act = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            act[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int costo = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                act[j] = Math.min(Math.min(act[j - 1] + 1, prev[j] + 1), prev[j - 1] + costo);
            }
            int[] tmp = prev;
            prev = act;
            act = tmp;
        }
        return prev[b.length()];
    }

    /** Distancia de edición entre dos etiquetas de opción (sin espacios extra). */
    public static int distanciaEtiquetas(String a, String b) {
        return distancia(compactar(a), compactar(b));
    }

    // ------------------------------------------------------------------
    // Formato
    // ------------------------------------------------------------------

    public String formatearFecha(LocalDate fecha) {
        if (fecha == null) {
            return "-";
        }
        DayOfWeek dia = fecha.getDayOfWeek();
        String nombreDia = dia.getDisplayName(TextStyle.FULL, new Locale("es", "EC"));
        return nombreDia + " " + fecha.getDayOfMonth() + "/" + fecha.getMonthValue();
    }

    public String formatoFechaLatina(LocalDate f) {
        if (f == null) {
            return "-";
        }
        return String.format("%02d/%02d/%04d", f.getDayOfMonth(), f.getMonthValue(), f.getYear());
    }

    public String nombreDiaSemana(int diaSemana) {
        DayOfWeek d = DayOfWeek.of(diaSemana);
        return d.getDisplayName(TextStyle.FULL, new Locale("es", "EC"));
    }

    public String recortar(String t) {
        String texto = t == null ? "" : t.trim().replaceAll("\\s+", " ");
        if (texto.isEmpty()) {
            texto = "…";
        }
        return texto.length() > 40 ? texto.substring(0, 40) + "…" : texto;
    }

    public String listarServicios(List<Servicio> servicios) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < servicios.size(); i++) {
            Servicio s = servicios.get(i);
            sb.append(i + 1).append(". ").append(s.getNombre()).append("\n");
        }
        return sb.toString();
    }

    public String listarOdontologos(List<Odontologo> odontologos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < odontologos.size(); i++) {
            Odontologo o = odontologos.get(i);
            sb.append(i + 1).append(". ").append(nombreOdontologo(o));
            if (o.getEspecialidad() != null && !o.getEspecialidad().isBlank()) {
                sb.append(" — ").append(o.getEspecialidad());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public String nombreOdontologo(Odontologo o) {
        if (o == null) {
            return "?";
        }
        String iniciales = o.getEtiquetas() != null && o.getEtiquetas().toUpperCase(Locale.ROOT).startsWith("DRA")
                ? "Dra. "
                : "Dr. ";
        return iniciales + o.getNombres() + " " + o.getApellidos();
    }

    public String nombreServicio(Servicio s) {
        return s == null ? "?" : s.getNombre();
    }

    public String horasDeSlots(List<SlotResponse> slots) {
        return dosColumnas(slots.stream().map(slot -> slot.horaInicio().toString()).toList());
    }

    public String dosColumnas(List<String> opciones) {
        StringBuilder sb = new StringBuilder();
        int anchoColumna = 18;
        for (int i = 0; i < opciones.size(); i += 2) {
            if (i > 0) {
                sb.append("\n");
            }
            String izquierda = (i + 1) + ". " + opciones.get(i);
            sb.append(String.format("%-" + anchoColumna + "s", izquierda));
            if (i + 1 < opciones.size()) {
                sb.append((i + 2) + ". ").append(opciones.get(i + 1));
            }
        }
        return sb.toString();
    }

    public String resumenHorario(List<HorarioOdontologo> horarios) {
        Map<String, List<HorarioOdontologo>> porRango = new LinkedHashMap<>();
        for (HorarioOdontologo h : horarios) {
            porRango.computeIfAbsent(h.getHoraInicio() + "-" + h.getHoraFin(), k -> new ArrayList<>()).add(h);
        }
        List<String> filas = new ArrayList<>();
        for (var e : porRango.entrySet()) {
            List<HorarioOdontologo> hs = e.getValue().stream()
                    .sorted(Comparator.comparing(HorarioOdontologo::getDiaSemana))
                    .toList();
            HorarioOdontologo h = hs.get(0);
            filas.add("• " + resumirDias(hs) + " de " + h.getHoraInicio() + " a " + h.getHoraFin());
        }
        return String.join("\n", filas);
    }

    public String resumirDias(List<HorarioOdontologo> hs) {
        List<Integer> dias = hs.stream().map(HorarioOdontologo::getDiaSemana).toList();
        StringBuilder sb = new StringBuilder();
        int inicio = 0;
        while (inicio < dias.size()) {
            int fin = inicio;
            while (fin + 1 < dias.size() && dias.get(fin + 1) == dias.get(fin) + 1) {
                fin++;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(nombreDiaSemana(dias.get(inicio)));
            if (fin > inicio) {
                sb.append(" a ").append(nombreDiaSemana(dias.get(fin)));
            }
            inicio = fin + 1;
        }
        return sb.toString();
    }

    public String preguntaServicio(List<Servicio> servicios, String error) {
        return (error == null ? "" : error + "\n\n")
                + "¿Qué servicio te gustaría?\n" + listarServicios(servicios)
                + "Envía el número o el nombre del servicio.";
    }

    public String preguntaOdontologo(List<Odontologo> odontologos, String error) {
        return (error == null ? "" : error + "\n\n")
                + "¿Con qué odontólogo prefieres tu cita?\n" + listarOdontologos(odontologos)
                + "Envía el número o el nombre del odontólogo.";
    }

    public String preguntaFecha(String error) {
        return (error == null ? "" : error + "\n\n")
                + "¿Para qué día te gustaría tu cita?\n"
                + "Puede ser 'mañana', 'viernes' o '20/09' (a partir de hoy).";
    }

    // ------------------------------------------------------------------
    // Fechas y registro
    // ------------------------------------------------------------------

    public LocalDate parsearFechaLibre(String t) {
        if (t == null || t.isBlank()) {
            return null;
        }
        String[] formas = {
                "(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})",
                "(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2})",
                "(\\d{4})[/-](\\d{1,2})[/-](\\d{1,2})"
        };
        for (String forma : formas) {
            Matcher m = Pattern.compile(forma).matcher(t.trim());
            if (m.find()) {
                try {
                    int anio = Integer.parseInt(m.group(3));
                    int mes = Integer.parseInt(m.group(2));
                    int dia = Integer.parseInt(m.group(1));
                    if (anio < 100) {
                        anio += anio >= 50 ? 1900 : 2000;
                    }
                    return LocalDate.of(anio, mes, dia);
                } catch (Exception ex) {
                    return null;
                }
            }
        }
        return null;
    }

    public String parsearParentesco(String t) {
        String n = ParserAgendamiento.normalizar(t);
        if (n.contains("padre") || n.contains("papa") || n.contains("papi")) {
            return "PADRE";
        }
        if (n.contains("madre") || n.contains("mama") || n.contains("mami")) {
            return "MADRE";
        }
        return null;
    }

    public boolean esMenor(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null) {
            return false;
        }
        return Period.between(fechaNacimiento, hoy()).getYears() < 18;
    }

    public long diasDesde(LocalDate fecha) {
        return ChronoUnit.DAYS.between(fecha, hoy());
    }
}
