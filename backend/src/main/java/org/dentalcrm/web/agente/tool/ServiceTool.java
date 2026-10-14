package org.dentalcrm.web.agente.tool;

import org.dentalcrm.domain.servicio.Servicio;
import org.dentalcrm.domain.servicio.ServicioRepository;
import org.dentalcrm.web.agente.AgenteSoporte;

import java.util.List;
import java.util.Optional;

/**
 * Catálogo de servicios (§16: precios y existencias siempre los decide el backend).
 */
public class ServiceTool {

    private final ServicioRepository servicioRepository;

    public ServiceTool(ServicioRepository servicioRepository) {
        this.servicioRepository = servicioRepository;
    }

    public List<Servicio> activos() {
        return servicioRepository.findByEstadoOrderByNombreAsc("ACTIVO");
    }

    public Optional<Servicio> porId(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return servicioRepository.findById(id)
                .or(() -> activos().stream().filter(s -> id.equals(s.getId())).findFirst());
    }

    public String nombre(Long id) {
        return porId(id).map(Servicio::getNombre).orElse("?");
    }

    /** Servicio elegido por nombre escrito (tolerante a errores, §6). */
    public Optional<Servicio> porNombre(String texto) {
        String t = AgenteSoporte.compactar(texto);
        if (t.isEmpty()) {
            return Optional.empty();
        }
        return activos().stream()
                .filter(s -> AgenteSoporte.coincide(s.getNombre(), t))
                .findFirst();
    }
}
