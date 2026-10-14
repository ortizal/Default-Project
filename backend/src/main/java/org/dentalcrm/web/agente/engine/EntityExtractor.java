package org.dentalcrm.web.agente.engine;

import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.servicio.Servicio;
import org.dentalcrm.web.agente.AgenteSoporte;
import org.dentalcrm.web.agente.ParserAgendamiento;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrae entidades de un mensaje libre (§10) y detecta correcciones
 * explícitas de un dato ya elegido (§12).
 *
 * <p>La extracción nunca inventa: todo valor proviene del texto del usuario
 * o de catálogos reales del backend (§16).
 */
public class EntityExtractor {

    /** Entidades reconocidas en el mensaje. */
    public record Entidades(
            LocalDate fecha,
            LocalTime hora,
            String franja,
            Long servicioId,
            Long odontologoId,
            String textoServicio,
            String textoOdontologo,
            String textoPaciente,
            String cedula,
            Correccion correccion
    ) {
        public static Entidades vacias() {
            return new Entidades(null, null, null, null, null, null, null, null, null, null);
        }

        public boolean tieneAlgo() {
            return fecha != null || hora != null || franja != null || servicioId != null
                    || odontologoId != null || textoServicio != null || textoOdontologo != null
                    || textoPaciente != null || cedula != null;
        }
    }

    /** Dato que el usuario pide cambiar (§12). */
    public record Correccion(TipoCorreccion tipo, String texto) {
    }

    public enum TipoCorreccion {
        FECHA, FRANJA, ODONTOLOGO, SERVICIO, PACIENTE, HORA, DATO
    }

    private static final Pattern P_MEJOR = Pattern.compile(
            "\\bmejor\\s+(hoy|manana|pasado\\s+manana|lunes|martes|miercoles|jueves|viernes|sabado|domingo)\\b");
    private static final Pattern P_PREFIERO_FRANJA = Pattern.compile(
            "\\bprefiero\\b.{0,20}\\b(manana|tarde|mediodia|medio dia)\\b");
    private static final Pattern P_CAMBIA_DOCTOR = Pattern.compile(
            "\\b(cambia(r)?|cambiemos|otro|otra)\\s+(al\\s+)?(doctor|dr|dra|odontologo|profesional)\\b");
    private static final Pattern P_CAMBIA_FECHA = Pattern.compile(
            "\\b(cambia(r)?|cambiemos|otro|otra)\\s+(la\\s+)?(fecha|dia)\\b");
    private static final Pattern P_CAMBIA_HORA = Pattern.compile(
            "\\b(cambia(r)?|cambiemos|otro|otra)\\s+(la\\s+)?(hora|cita|turno)\\b");
    private static final Pattern P_PARA_OTRO = Pattern.compile(
            "\\bno\\s*(,|,)?\\s*para\\s+(?<nombre>[a-záéíóúñü ]{3,40})$");
    private static final Pattern P_NO_SERVICIO = Pattern.compile(
            "^(?<si>[a-záéíóúñü ]{3,40})\\s*,?\\s*no\\s+(?<no>[a-záéíóúñü ]{3,40})$");
    private static final Pattern P_CEDULA = Pattern.compile("\\b(\\d{7,10})\\b");
    private static final Pattern P_PARA_NOMBRE = Pattern.compile(
            "\\bpara\\s+(?<nombre>[a-záéíóúñü ]{3,40})$");

    private final AgenteSoporte soporte;

    public EntityExtractor(AgenteSoporte soporte) {
        this.soporte = soporte;
    }

    public Entidades extraer(String texto, List<Servicio> servicios, List<Odontologo> odontologos) {
        String t = AgenteSoporte.compactar(texto);
        if (t.isEmpty()) {
            return Entidades.vacias();
        }
        ParserAgendamiento.EntradaAgendamiento entrada = ParserAgendamiento.parsear(
                t, soporte.hoy(), servicios, odontologos);

        Correccion correccion = correccion(t);
        String franja = franja(t);
        String cedula = cedula(t);
        String paciente = paciente(t, correccion);

        return new Entidades(
                entrada.fecha(),
                entrada.horaInicio(),
                franja,
                entrada.servicioId(),
                entrada.doctorId(),
                entrada.servicioId() != null ? null : textoServicio(t, servicios),
                entrada.doctorId() != null ? null : textoOdontologo(t, odontologos),
                paciente,
                cedula,
                correccion);
    }

    /** "mejor mañana", "prefiero por la tarde", "cambia al doctor", "no, para Dulce"... */
    private Correccion correccion(String t) {
        Matcher mejor = P_MEJOR.matcher(t);
        if (mejor.find()) {
            return new Correccion(TipoCorreccion.FECHA, mejor.group(1));
        }
        Matcher prefiero = P_PREFIERO_FRANJA.matcher(t);
        if (prefiero.find()) {
            return new Correccion(TipoCorreccion.FRANJA, prefiero.group(1).replace("medio dia", "mediodia"));
        }
        if (P_CAMBIA_DOCTOR.matcher(t).find()) {
            return new Correccion(TipoCorreccion.ODONTOLOGO, t);
        }
        if (P_CAMBIA_FECHA.matcher(t).find()) {
            return new Correccion(TipoCorreccion.FECHA, t);
        }
        if (P_CAMBIA_HORA.matcher(t).find()) {
            return new Correccion(TipoCorreccion.HORA, t);
        }
        Matcher paraOtro = P_PARA_OTRO.matcher(t);
        if (paraOtro.find()) {
            return new Correccion(TipoCorreccion.PACIENTE, paraOtro.group("nombre").trim());
        }
        Matcher noServicio = P_NO_SERVICIO.matcher(t);
        if (noServicio.find()) {
            return new Correccion(TipoCorreccion.SERVICIO, noServicio.group("si").trim());
        }
        return null;
    }

    private String franja(String t) {
        if (t.contains("medio dia") || t.contains("mediodia")) {
            return "MEDIODIA";
        }
        if (ParserAgendamiento.contienePalabra(t, "manana")) {
            return "MANANA";
        }
        if (ParserAgendamiento.contienePalabra(t, "tarde")) {
            return "TARDE";
        }
        return null;
    }

    private String cedula(String t) {
        if (t.matches(".*\\b\\d{7,10}\\b.*")) {
            Matcher m = P_CEDULA.matcher(t);
            if (m.find()) {
                return m.group(1);
            }
        }
        return null;
    }

    private String paciente(String t, Correccion correccion) {
        if (correccion != null && correccion.tipo() == TipoCorreccion.PACIENTE) {
            return capitalizar(correccion.texto());
        }
        Matcher m = P_PARA_NOMBRE.matcher(t);
        if (m.find()) {
            String nombre = m.group("nombre").trim();
            if (!SERVICIOS_COMUNES.contains(nombre)) {
                return capitalizar(nombre);
            }
        }
        return null;
    }

    private static final List<String> SERVICIOS_COMUNES = List.of(
            "una limpieza", "una valoracion", "una consulta", "una revision", "una endodoncia");

    private String textoServicio(String t, List<Servicio> servicios) {
        for (Servicio s : servicios) {
            String nombre = AgenteSoporte.compactar(s.getNombre());
            for (String palabra : nombre.split(" ")) {
                if (palabra.length() >= 5 && t.contains(palabra)) {
                    return s.getNombre();
                }
            }
        }
        return null;
    }

    private String textoOdontologo(String t, List<Odontologo> odontologos) {
        for (Odontologo o : odontologos) {
            String apellido = AgenteSoporte.compactar(o.getApellidos());
            String nombre = AgenteSoporte.compactar(o.getNombres());
            if (!apellido.isEmpty() && t.contains(apellido)) {
                return o.getNombres() + " " + o.getApellidos();
            }
            if (nombre.length() >= 4 && t.contains(nombre)) {
                return o.getNombres() + " " + o.getApellidos();
            }
        }
        return null;
    }

    private String capitalizar(String texto) {
        String limpio = AgenteSoporte.compactar(texto);
        if (limpio.isEmpty()) {
            return null;
        }
        return limpio.substring(0, 1).toUpperCase(Locale.ROOT) + limpio.substring(1);
    }
}
