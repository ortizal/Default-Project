package org.dentalcrm.web.agente.tool;

import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.domain.paciente.PacienteTutor;
import org.dentalcrm.util.TelefonoUtil;
import org.dentalcrm.web.agente.AgenteSoporte;
import org.dentalcrm.web.paciente.PacienteService;
import org.dentalcrm.web.paciente.dto.PacienteRequest;
import org.dentalcrm.web.paciente.dto.TutorRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Acceso a pacientes: identificación por teléfono/cédula y alta desde el chat (§7).
 *
 * <p>El teléfono de WhatsApp es la vía principal; la cédula sólo se pide cuando
 * el teléfono no alcanza para identificar al paciente.
 */
public class PatientTool {

    private static final Logger log = LoggerFactory.getLogger(PatientTool.class);

    private final PacienteRepository pacienteRepository;
    private final PacienteService pacienteService;
    private final AgenteSoporte soporte;

    public PatientTool(PacienteRepository pacienteRepository, PacienteService pacienteService,
                       AgenteSoporte soporte) {
        this.pacienteRepository = pacienteRepository;
        this.pacienteService = pacienteService;
        this.soporte = soporte;
    }

    public Optional<Paciente> porCedula(String cedula) {
        if (cedula == null || cedula.isBlank()) {
            return Optional.empty();
        }
        return pacienteRepository.findByCedulaIgnoreCase(cedula.trim());
    }

    public Optional<Paciente> porId(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return pacienteRepository.findById(id);
    }

    /** Paciente cuyo teléfono propio coincide con el número que escribe (§7). */
    public Optional<Paciente> porTelefono(String telefono) {
        return pacienteRepository.activosConTelefono().stream()
                .filter(p -> p.getTelefono() != null && TelefonoUtil.corresponden(p.getTelefono(), telefono))
                .findFirst();
    }

    /** Pacientes cuyo tutor registró este teléfono (la persona que escribe es tutor). */
    public List<Paciente> dependientesDe(String telefono) {
        return pacienteRepository.activosConTelefono().stream()
                .filter(p -> p.getTutores().stream()
                        .anyMatch(t -> t.getTelefono() != null
                                && TelefonoUtil.corresponden(t.getTelefono(), telefono)))
                .toList();
    }

    /** Opciones a mostrar cuando el teléfono no identifica a una sola persona. */
    public List<Paciente> candidatos(String telefono) {
        Set<Paciente> candidatos = new LinkedHashSet<>(dependientesDe(telefono));
        porTelefono(telefono).ifPresent(candidatos::add);
        return new ArrayList<>(candidatos);
    }

    /** Pacientes cuyo nombre coincide con el mencionado en el mensaje (§10). */
    public List<Paciente> porNombre(String texto) {
        String t = org.dentalcrm.web.agente.AgenteSoporte.compactar(texto);
        if (t.isEmpty()) {
            return List.of();
        }
        return pacienteRepository.findAll().stream()
                .filter(p -> p.getEstado() == null || "ACTIVO".equals(p.getEstado()))
                .filter(p -> org.dentalcrm.web.agente.AgenteSoporte.coincide(nombre(p), t)
                        || org.dentalcrm.web.agente.AgenteSoporte.coincide(p.getNombres(), t)
                        || org.dentalcrm.web.agente.AgenteSoporte.coincide(p.getApellidos(), t))
                .toList();
    }

    public String nombre(Paciente p) {
        return p == null ? "" : (p.getNombres() + " " + (p.getApellidos() == null ? "" : p.getApellidos())).strip();
    }

    public String nombreCorto(Paciente p) {
        return p == null ? "" : p.getNombres();
    }

    public boolean esMenor(Paciente p) {
        return p != null && soporte.esMenor(p.getFechaNacimiento());
    }

    /** Tutor registrado en la ficha del paciente (si lo hay). */
    public String tutorDe(Paciente p) {
        if (p == null || p.getTutores() == null || p.getTutores().isEmpty()) {
            return null;
        }
        PacienteTutor t = p.getTutores().get(0);
        return t.getParentesco() + " " + t.getNombres()
                + (t.getApellidos() == null ? "" : " " + t.getApellidos());
    }

    /**
     * Crea la ficha desde el chat (incluye tutor si es menor, §7).
     *
     * @return el paciente creado
     * @throws Exception si el backend rechaza el alta (cédula duplicada, etc.)
     */
    public Paciente crearDesdeChat(String telefono, String cedula, String nombres, String apellidos,
                                   java.time.LocalDate fechaNacimiento, String parentesco,
                                   String tutorNombres, String tutorApellidos) throws Exception {
        boolean menor = soporte.esMenor(fechaNacimiento);
        List<TutorRequest> tutores = new ArrayList<>();
        if (menor) {
            tutores.add(new TutorRequest(parentesco, tutorNombres, tutorApellidos, null));
        }
        var respuesta = pacienteService.crear(new PacienteRequest(
                cedula, nombres, apellidos, telefono, null, fechaNacimiento, null, null,
                "Registrado por WhatsApp", "ACTIVO", tutores));
        Paciente nuevo = pacienteRepository.findById(respuesta.id())
                .orElseThrow(() -> new IllegalStateException("Paciente creado pero no recuperable"));
        log.info("Agente: paciente {} creado desde el chat (menor={})", nuevo.getId(), menor);
        return nuevo;
    }
}
