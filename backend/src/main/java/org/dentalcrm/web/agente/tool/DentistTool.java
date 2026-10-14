package org.dentalcrm.web.agente.tool;

import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.odontologo.OdontologoRepository;
import org.dentalcrm.web.agente.AgenteSoporte;

import java.util.List;
import java.util.Optional;

/**
 * Catálogo de odontólogos (§16).
 */
public class DentistTool {

    private final OdontologoRepository odontologoRepository;

    public DentistTool(OdontologoRepository odontologoRepository) {
        this.odontologoRepository = odontologoRepository;
    }

    public List<Odontologo> activos() {
        return odontologoRepository.findTop5ByEstadoOrderByNombresAsc("ACTIVO");
    }

    public Optional<Odontologo> porId(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return odontologoRepository.findById(id)
                .or(() -> activos().stream().filter(o -> id.equals(o.getId())).findFirst());
    }

    public String nombre(Long id) {
        return porId(id).map(o -> "Dr. " + o.getNombres() + " " + o.getApellidos()).orElse("?");
    }

    public String nombreCompleto(Odontologo o) {
        return "Dr. " + o.getNombres() + " " + o.getApellidos();
    }

    /** Odontólogo elegido por nombre, apellido o etiqueta (§6). */
    public Optional<Odontologo> porNombre(String texto) {
        String t = AgenteSoporte.compactar(texto);
        if (t.isEmpty()) {
            return Optional.empty();
        }
        return activos().stream()
                .filter(o -> AgenteSoporte.coincide(o.getNombres(), t)
                        || AgenteSoporte.coincide(o.getApellidos(), t)
                        || AgenteSoporte.coincide(nombreCompleto(o), t)
                        || etiqueta(o, t))
                .findFirst();
    }

    private boolean etiqueta(Odontologo o, String t) {
        if (o.getEtiquetas() == null || o.getEtiquetas().isBlank()) {
            return false;
        }
        for (String et : AgenteSoporte.compactar(o.getEtiquetas()).split("[^a-z0-9]+")) {
            if (et.length() >= 4 && (t.contains(et) || AgenteSoporte.distancia(t, et) <= 2)) {
                return true;
            }
        }
        return false;
    }
}
