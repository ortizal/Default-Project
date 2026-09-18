package org.dentalcrm.web.paciente;

import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.domain.paciente.PacienteTutor;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.web.paciente.dto.PacienteRequest;
import org.dentalcrm.web.paciente.dto.PacienteResponse;
import org.dentalcrm.web.paciente.dto.TutorRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Service
public class PacienteService {

    private static final String MODULO = "PACIENTES";
    private static final int MAYORIA_EDAD = 18;

    private final PacienteRepository pacienteRepository;
    private final AuditService auditService;

    public PacienteService(PacienteRepository pacienteRepository, AuditService auditService) {
        this.pacienteRepository = pacienteRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<PacienteResponse> listar(String q, String estado, int page, int size) {
        String query = q == null ? "" : q.trim();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return pacienteRepository.buscar(query, estado, pageable).map(PacienteResponse::from);
    }

    @Transactional(readOnly = true)
    public PacienteResponse obtener(Long id) {
        return PacienteResponse.from(buscarOArrojar(id));
    }

    @Transactional
    public PacienteResponse crear(PacienteRequest request) {
        validarCedulaUnica(request.cedula(), null);
        validarTutores(request);
        Paciente p = new Paciente();
        aplicar(p, request);
        aplicarTutores(p, request.tutores());
        Paciente creado = pacienteRepository.save(p);
        auditService.registrar("CREAR_PACIENTE", MODULO, "PACIENTE", creado.getId(), null, PacienteResponse.from(creado));
        return PacienteResponse.from(creado);
    }

    @Transactional
    public PacienteResponse actualizar(Long id, PacienteRequest request) {
        Paciente p = buscarOArrojar(id);
        validarCedulaUnica(request.cedula(), id);
        validarTutores(request);
        PacienteResponse anterior = PacienteResponse.from(p);
        p.getTutores().clear();
        aplicar(p, request);
        aplicarTutores(p, request.tutores());
        Paciente actualizado = pacienteRepository.save(p);
        auditService.registrar("MODIFICAR_PACIENTE", MODULO, "PACIENTE", id, anterior, PacienteResponse.from(actualizado));
        return PacienteResponse.from(actualizado);
    }

    @Transactional
    public void desactivar(Long id) {
        Paciente p = buscarOArrojar(id);
        if ("INACTIVO".equals(p.getEstado())) {
            throw new BusinessException("PACIENTE_INACTIVO", "El paciente ya está inactivo");
        }
        PacienteResponse anterior = PacienteResponse.from(p);
        p.setEstado("INACTIVO");
        pacienteRepository.save(p);
        auditService.registrar("DESACTIVAR_PACIENTE", MODULO, "PACIENTE", id, anterior, PacienteResponse.from(p));
    }

    private Paciente buscarOArrojar(Long id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Paciente no encontrado"));
    }

    private void validarCedulaUnica(String cedula, Long idActual) {
        if (cedula == null || cedula.isBlank()) {
            return;
        }
        String c = cedula.trim();
        boolean existe = idActual == null
                ? pacienteRepository.existeGlobalCedula(c)
                : pacienteRepository.existeGlobalCedulaExcepto(c, idActual);
        if (existe) {
            throw new BusinessException("CEDULA_DUPLICADA", "Ya existe un paciente con la cédula " + c);
        }
    }

    private void aplicar(Paciente p, PacienteRequest r) {
        p.setCedula(r.cedula() == null ? null : r.cedula().trim());
        p.setNombres(r.nombres().trim());
        p.setApellidos(r.apellidos().trim());
        p.setTelefono(r.telefono() == null ? null : r.telefono().trim());
        p.setEmail(r.email() == null ? null : r.email().trim());
        p.setFechaNacimiento(r.fechaNacimiento());
        p.setDireccion(r.direccion());
        p.setObservaciones(r.observaciones());
        if (r.estado() != null) {
            p.setEstado(r.estado());
        }
    }

    private void validarTutores(PacienteRequest r) {
        List<TutorRequest> tutores = r.tutores() == null ? List.of() : r.tutores();
        if (esMenor(r.fechaNacimiento())) {
            if (tutores.isEmpty()) {
                throw new BusinessException("TUTOR_REQUERIDO",
                        "El paciente es menor de edad: debe registrar al padre y/o madre como tutor");
            }
        }
        long padres = tutores.stream().filter(t -> "PADRE".equals(t.parentesco())).count();
        long madres = tutores.stream().filter(t -> "MADRE".equals(t.parentesco())).count();
        if (padres > 1 || madres > 1) {
            throw new BusinessException("TUTOR_DUPLICADO", "Solo se puede registrar un padre y una madre");
        }
        boolean nombreVacio = tutores.stream()
                .anyMatch(t -> t.nombres() == null || t.nombres().isBlank());
        if (nombreVacio) {
            throw new BusinessException("TUTOR_NOMBRE", "Los nombres del tutor son obligatorios");
        }
    }

    private void aplicarTutores(Paciente p, List<TutorRequest> tutores) {
        if (tutores == null) {
            return;
        }
        for (TutorRequest t : tutores) {
            PacienteTutor tutor = new PacienteTutor();
            tutor.setPaciente(p);
            tutor.setParentesco(t.parentesco());
            tutor.setNombres(t.nombres().trim());
            tutor.setApellidos(t.apellidos() == null ? null : t.apellidos().trim());
            tutor.setTelefono(t.telefono() == null ? null : t.telefono().trim());
            p.getTutores().add(tutor);
        }
    }

    private boolean esMenor(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null) {
            return false;
        }
        return Period.between(fechaNacimiento, LocalDate.now()).getYears() < MAYORIA_EDAD;
    }
}