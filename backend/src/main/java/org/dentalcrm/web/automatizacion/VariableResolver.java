package org.dentalcrm.web.automatizacion;

import org.dentalcrm.domain.cita.Cita;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class VariableResolver {

    private final String clinicaNombre;
    private final String clinicaDireccion;
    private final String clinicaTelefono;

    public VariableResolver(@Value("${app.clinica.nombre:Clínica Dental}") String clinicaNombre,
                            @Value("${app.clinica.direccion:}") String clinicaDireccion,
                            @Value("${app.clinica.telefono:}") String clinicaTelefono) {
        this.clinicaNombre = clinicaNombre;
        this.clinicaDireccion = clinicaDireccion;
        this.clinicaTelefono = clinicaTelefono;
    }

    public Map<String, String> variables(Cita cita) {
        Map<String, String> v = new LinkedHashMap<>();
        String nombres = cita.getPaciente().getNombres();
        String apellidos = cita.getPaciente().getApellidos() == null ? "" : cita.getPaciente().getApellidos();
        v.put("nombre", primerNombre(nombres));
        v.put("apellido", primerApellido(apellidos));
        v.put("nombre_completo", (nombres + " " + apellidos).trim());
        v.put("fecha", cita.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        v.put("hora", cita.getHoraInicio().format(DateTimeFormatter.ofPattern("HH:mm")));
        v.put("doctor", cita.getDoctor().getNombres() + " " + cita.getDoctor().getApellidos());
        v.put("servicio", cita.getServicio().getNombre());
        v.put("clinica", clinicaNombre);
        v.put("direccion", clinicaDireccion);
        v.put("telefono", clinicaTelefono);
        v.put("codigo_cita", String.valueOf(cita.getId()));
        return v;
    }

    public String renderizar(String contenido, Map<String, String> variables) {
        if (contenido == null) {
            return null;
        }
        String resultado = contenido;
        for (Map.Entry<String, String> e : variables.entrySet()) {
            if (e.getValue() != null) {
                resultado = resultado.replace("{{" + e.getKey() + "}}", e.getValue());
            }
        }
        return resultado;
    }

    private String primerNombre(String nombres) {
        if (nombres == null || nombres.isBlank()) {
            return "";
        }
        return nombres.trim().split("\\s+")[0];
    }

    private String primerApellido(String apellidos) {
        if (apellidos == null || apellidos.isBlank()) {
            return "";
        }
        return apellidos.trim().split("\\s+")[0];
    }
}