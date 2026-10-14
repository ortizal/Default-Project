package org.dentalcrm.web.agente.engine;

import org.dentalcrm.domain.agente.Intencion;
import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.servicio.Servicio;
import org.dentalcrm.web.agente.AgenteSoporte;
import org.dentalcrm.web.agente.ParserAgendamiento;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Clasifica la intención general (§6). Se consulta sólo cuando no hubo
 * comandos globales, flujo activo, menú ni dato esperado (§2).
 *
 * <p>El matching normaliza tildes y espacios; el fuzzy matching apoya, pero
 * nunca decide una operación por sí solo.
 */
public class IntentClassifier {

    private static final Set<String> PALABRAS_REINICIAR = Set.of(
            "reiniciar", "reinicia", "reinicio", "reiniciame", "reiniciar chat",
            "reset", "restablecer", "volver a empezar", "volver a comenzar",
            "volver a iniciar", "iniciar de nuevo", "iniciar otra vez",
            "empezar de nuevo", "empezar otra vez", "comenzar de nuevo",
            "empezar desde cero", "desde cero", "nueva conversacion",
            "borrar la conversacion", "olvidar");

    private static final Set<String> PALABRAS_REGISTRO = Set.of(
            "registrarme", "registrarse", "registrar", "registro",
            "registrar paciente", "registrarme como paciente", "registrarse como paciente",
            "crear paciente", "crear cuenta",
            "soy nuevo", "soy nueva", "darme de alta", "darte de alta", "paciente nuevo",
            "obtener ficha", "inscribirme", "inscripcion", "primera vez");

    // "hoal", "holaa" y "ola" son typos frecuentes: el fuzzy no los cubre porque
    // "hola" mide menos de 5 caracteres, así que se listan tal cual (§1 del plan).
    private static final Set<String> PALABRAS_SALUDO = Set.of(
            "hola", "holi", "hoal", "holaa", "ola", "buenos dias", "buen dia", "buenas",
            "buenas tardes", "buenas noches", "saludos", "que tal", "como estas",
            "ayuda", "menu", "opciones", "que puedes hacer", "inicio");

    private static final Set<String> PALABRAS_CONFIRMAR = Set.of(
            "confirmar", "confirma", "confirmacion", "confirmo", "confirmame",
            "confirmar cita", "confirmo la cita");

    private static final Set<String> PALABRAS_CANCELAR = Set.of(
            "cancelar cita", "cancelo la cita", "cancelar mi cita", "eliminar cita",
            "dar de baja", "cancelar turno", "no asistire");

    private static final Set<String> PALABRAS_SERVICIOS = Set.of(
            "servicio", "servicios", "precio", "precios", "costo", "costos", "tarifa",
            "tarifas", "catalogo", "que ofrecen", "que tratan", "lista de servicios");

    private static final Set<String> PALABRAS_DISPONIBILIDAD = Set.of(
            "disponibilidad", "cupo", "cupos", "hay a la", "hay campo", "que horario",
            "que horarios", "horario de atencion", "horarios de atencion",
            "disponibilidad del doctor", "disponibilidad doctor", "horario del doctor",
            "horario doctor", "dias de atencion", "dias y horarios", "dias que atiende",
            "dias que atienden", "cuando atiende", "cuando atienden", "atiende",
            "atienden", "atender", "atencion", "horas de atencion", "turnos",
            "hay espacio", "espacio libre", "tienen citas");

    private static final Set<String> PALABRAS_VER_CITAS = Set.of(
            "ver citas", "mis citas", "ver mis citas", "que citas tengo",
            "citas pendientes", "proximas citas", "consultar citas", "ver agenda",
            "tengo cita", "tengo citas", "verificar citas", "verificar", "mis turnos");

    private static final Set<String> PALABRAS_VER_PERFIL = Set.of(
            "ver perfil", "mi perfil", "perfil", "mis datos", "mi informacion",
            "ver mi informacion", "informacion personal", "datos personales",
            "ver datos", "ver informacion", "quien soy", "como soy", "quien soy yo",
            "como me llamo", "datos");

    private static final Set<String> PALABRAS_CONECTAR_GOOGLE = Set.of(
            "conectar google", "google calendar", "google", "conectar con google",
            "vincular google", "calendario google", "conexion google", "mi google");

    private static final Set<String> PALABRAS_TRANSFERIR = Set.of(
            "humano", "persona", "operador", "operadora", "representante", "transferir",
            "transferencia", "que me atienda", "atenderme", "atencion humana",
            "asistente real", "recepcion", "asesor", "hablar con recepcion");

    private static final Set<String> PALABRAS_GESTIONAR = Set.of(
            "confirmar o cancelar", "confirmar o cancelar una cita", "gestionar cita",
            "cambiar cita", "reprogramar", "reprogramar cita", "mover cita", "agendar otra vez");

    private static final Set<String> PALABRAS_AGENDAR = Set.of(
            "cita", "citas", "agendar", "agenda", "reservar", "reserva", "turno", "turnos",
            "queria", "necesito una", "sacar una", "quiero una cita", "pedir cita");

    private static final Pattern P_PATTERN_CEDULA = Pattern.compile("^\\d{6,13}$");

    private final AgenteSoporte soporte;

    public IntentClassifier(AgenteSoporte soporte) {
        this.soporte = soporte;
    }

    public boolean esReiniciar(String t) {
        return contieneFuzzy(t, PALABRAS_REINICIAR);
    }

    public Intencion clasificar(String t, List<Servicio> servicios, List<Odontologo> odontologos) {
        if (t == null || t.isBlank()) {
            return Intencion.DESCONOCIDO;
        }
        if (P_PATTERN_CEDULA.matcher(t).matches()) {
            return Intencion.VER_PERFIL;
        }
        Intencion directa = directa(t);
        if (directa != null) {
            return directa;
        }
        if (contieneFuzzy(t, PALABRAS_TRANSFERIR)) {
            return Intencion.TRANSFERIR_HUMANO;
        }
        if (contieneFuzzy(t, PALABRAS_REGISTRO)) {
            return Intencion.REGISTRAR_PACIENTE;
        }
        if (contieneFuzzy(t, PALABRAS_SERVICIOS)) {
            return Intencion.LISTAR_SERVICIOS;
        }
        if (contieneFuzzy(t, PALABRAS_DISPONIBILIDAD)) {
            return Intencion.PREGUNTAR_DISPONIBILIDAD;
        }
        if (contieneFuzzy(t, PALABRAS_GESTIONAR)) {
            return Intencion.GESTIONAR_CITA;
        }
        if (contieneFuzzy(t, PALABRAS_CONFIRMAR)) {
            return Intencion.CONFIRMAR_CITA;
        }
        if (contieneFuzzy(t, PALABRAS_CANCELAR)) {
            return Intencion.CANCELAR_CITA;
        }
        if (contieneFuzzy(t, PALABRAS_VER_PERFIL)) {
            return Intencion.VER_PERFIL;
        }
        if (contieneFuzzy(t, PALABRAS_CONECTAR_GOOGLE)) {
            return Intencion.CONECTAR_GOOGLE;
        }
        if (contieneFuzzy(t, PALABRAS_VER_CITAS)) {
            return Intencion.VER_CITAS;
        }
        if (contieneFuzzy(t, PALABRAS_AGENDAR)) {
            return Intencion.AGENDAR_CITA;
        }
        ParserAgendamiento.EntradaAgendamiento entrada = ParserAgendamiento.parsear(
                t, soporte.hoy(), servicios, odontologos);
        if (entrada.fecha() != null || entrada.horaInicio() != null
                || entrada.servicioId() != null || entrada.doctorId() != null) {
            return Intencion.AGENDAR_CITA;
        }
        if (contieneFuzzy(t, PALABRAS_SALUDO)) {
            return Intencion.SALUDO;
        }
        return Intencion.DESCONOCIDO;
    }

    private Intencion directa(String t) {
        return switch (t) {
            case "menu", "inicio", "ayuda" -> Intencion.SALUDO;
            case "cancelar" -> Intencion.CANCELAR_CITA;
            case "confirmar" -> Intencion.CONFIRMAR_CITA;
            default -> null;
        };
    }

    private boolean contieneFuzzy(String t, Set<String> palabras) {
        if (soporte.contiene(t, palabras)) {
            return true;
        }
        for (String palabra : palabras) {
            if (prefijoRazonable(t, palabra)) {
                return true;
            }
        }
        return false;
    }

    /** "consultar disponi" ~ "disponibilidad" (§6). */
    private boolean prefijoRazonable(String t, String palabra) {
        String a = AgenteSoporte.compactar(t);
        String b = AgenteSoporte.compactar(palabra);
        if (a.length() < 5 || b.length() < 5) {
            return false;
        }
        if (a.startsWith(b) || b.startsWith(a) || AgenteSoporte.distancia(a, b) <= 2) {
            return true;
        }
        // "consultar disponi" ~ "disponibilidad" (§6): se compara token a token,
        // sin dejar que la primera palabra decida por sí sola.
        for (String token : a.split(" ")) {
            if (token.length() < 5) {
                continue;
            }
            if (b.startsWith(token) || token.startsWith(b)
                    || AgenteSoporte.distancia(token, b) <= 2) {
                return true;
            }
        }
        return false;
    }
}
