package org.dentalcrm.web.consentimiento;

import org.dentalcrm.domain.consentimiento.*;
import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.odontologo.OdontologoRepository;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.domain.paciente.PacienteTutor;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.service.CurrentUserService;
import org.dentalcrm.util.HashUtil;
import org.dentalcrm.web.consentimiento.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Locale;

/**
 * Consentimientos informados — Fase E (plan §4.13): plantillas por
 * categoría, snapshot inmutable del texto al emitir, firma con hash
 * SHA-256 y PDF. Un consentimiento sólo se edita mientras está en
 * BORRADOR; al firmarse queda congelado y sólo admite lectura.
 */
@Service
public class ConsentimientoService {

    private static final String MODULO = "HISTORIA_CLINICA";
    private static final String ENTIDAD = "CONSENTIMIENTO";
    private static final String ENTIDAD_PLANTILLA = "PLANTILLA_CONSENTIMIENTO";

    private final ConsentimientoRepository consentimientoRepository;
    private final ConsentimientoPlantillaRepository plantillaRepository;
    private final PacienteRepository pacienteRepository;
    private final OdontologoRepository odontologoRepository;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;
    private final ConsentimientoPdfService pdfService;

    public ConsentimientoService(ConsentimientoRepository consentimientoRepository,
                                 ConsentimientoPlantillaRepository plantillaRepository,
                                 PacienteRepository pacienteRepository,
                                 OdontologoRepository odontologoRepository,
                                 AuditService auditService,
                                 CurrentUserService currentUserService,
                                 ConsentimientoPdfService pdfService) {
        this.consentimientoRepository = consentimientoRepository;
        this.plantillaRepository = plantillaRepository;
        this.pacienteRepository = pacienteRepository;
        this.odontologoRepository = odontologoRepository;
        this.auditService = auditService;
        this.currentUserService = currentUserService;
        this.pdfService = pdfService;
    }

    // ------------------------------------------------------------------
    // Plantillas
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<PlantillaConsentimientoResponse> plantillas() {
        return plantillaRepository.findAllByOrderByNombreAsc().stream()
                .map(PlantillaConsentimientoResponse::from)
                .toList();
    }

    @Transactional
    public PlantillaConsentimientoResponse actualizarPlantilla(String codigo,
                                                               ActualizarPlantillaConsentimientoRequest request) {
        ConsentimientoPlantilla plantilla = plantillaOArrojar(codigo);
        String nombreAntes = plantilla.getNombre();
        String contenidoAntes = plantilla.getContenido();

        if (request.nombre() != null && !request.nombre().isBlank()) {
            plantilla.setNombre(request.nombre().trim());
        }
        plantilla.setContenido(request.contenido().trim());
        plantilla.setVersion(plantilla.getVersion() + 1);
        ConsentimientoPlantilla guardada = plantillaRepository.save(plantilla);

        auditService.registrar("MODIFICAR_PLANTILLA_CONSENTIMIENTO", MODULO, ENTIDAD_PLANTILLA,
                guardada.getId(),
                java.util.Map.of("nombre", nombreAntes, "contenido", contenidoAntes),
                java.util.Map.of("nombre", guardada.getNombre(), "contenido", guardada.getContenido(),
                        "version", guardada.getVersion()));
        return PlantillaConsentimientoResponse.from(guardada);
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ConsentimientoResumenResponse> historial(Long pacienteId) {
        Paciente paciente = pacienteOArrojar(pacienteId);
        return consentimientoRepository.findByPacienteIdOrderByFechaDescIdDesc(paciente.getId()).stream()
                .map(c -> ConsentimientoResumenResponse.from(c, nombreOdontologo(c.getOdontologo())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ConsentimientoResponse obtener(Long id) {
        return completa(consentimientoOArrojar(id));
    }

    // ------------------------------------------------------------------
    // Alta y edición (sólo BORRADOR)
    // ------------------------------------------------------------------

    @Transactional
    public ConsentimientoResponse crear(Long pacienteId, NuevoConsentimientoRequest request) {
        Paciente paciente = pacienteOArrojar(pacienteId);
        ConsentimientoPlantilla plantilla = plantillaOArrojar(request.plantillaCodigo());

        Consentimiento c = new Consentimiento();
        c.setPaciente(paciente);
        c.setEstado(EstadoConsentimiento.BORRADOR);
        c.setFecha(request.fecha() == null ? LocalDate.now() : request.fecha());
        aplicarTutor(c, paciente, request.tutorId(), request.tutorNombre());
        exigirTutorSiMenor(paciente, c);
        c.setObservaciones(vacioANull(request.observaciones()));
        c.setOdontologo(odontologoDe(request.odontologoId()));

        // Snapshot: el consentimiento conserva el texto y la versión de la
        // plantilla con los que se emitió, aunque la plantilla cambie luego.
        c.setPlantillaCodigo(plantilla.getCodigo());
        c.setPlantillaVersion(plantilla.getVersion());
        c.setTitulo(plantilla.getNombre());
        c.setCategoria(plantilla.getCategoria());
        c.setContenido(plantilla.getContenido());
        c.setCreatedBy(currentUserService.idUsuarioActual().orElse(null));

        Consentimiento guardado = consentimientoRepository.save(c);
        auditService.registrar("CREAR_CONSENTIMIENTO", MODULO, ENTIDAD, guardado.getId(), null,
                java.util.Map.of("plantilla", guardado.getPlantillaCodigo(),
                        "version", guardado.getPlantillaVersion(),
                        "pacienteId", guardado.getPaciente().getId()));
        return completa(guardado);
    }

    @Transactional
    public ConsentimientoResponse editar(Long id, EditarConsentimientoRequest request) {
        Consentimiento c = consentimientoOArrojar(id);
        exigirBorrador(c, "editar");
        Paciente paciente = c.getPaciente();

        String antes = resumenTexto(c);
        if (request != null) {
            if (request.fecha() != null) {
                c.setFecha(request.fecha());
            }
            if (request.titulo() != null && !request.titulo().isBlank()) {
                c.setTitulo(request.titulo().trim());
            }
            if (request.contenido() != null && !request.contenido().isBlank()) {
                c.setContenido(request.contenido().trim());
            }
            aplicarTutor(c, paciente, request.tutorId(), request.tutorNombre());
            c.setObservaciones(vacioANull(request.observaciones()));
            if (request.odontologoId() != null) {
                c.setOdontologo(odontologoDe(request.odontologoId()));
            }
        }
        exigirTutorSiMenor(paciente, c);
        Consentimiento guardado = consentimientoRepository.save(c);
        auditService.registrar("MODIFICAR_CONSENTIMIENTO", MODULO, ENTIDAD, id, antes,
                resumenTexto(guardado));
        return completa(guardado);
    }

    @Transactional
    public void eliminar(Long id) {
        Consentimiento c = consentimientoOArrojar(id);
        exigirBorrador(c, "eliminar");
        String antes = resumenTexto(c);
        consentimientoRepository.delete(c);
        auditService.registrar("ELIMINAR_CONSENTIMIENTO", MODULO, ENTIDAD, id, antes, null);
    }

    // ------------------------------------------------------------------
    // Firma (congela el consentimiento)
    // ------------------------------------------------------------------

    @Transactional
    public ConsentimientoResponse firmar(Long id, FirmarConsentimientoRequest request) {
        Consentimiento c = consentimientoOArrojar(id);
        if (c.getEstado() == EstadoConsentimiento.FIRMADO) {
            throw new BusinessException(HttpStatus.CONFLICT, "CONSENTIMIENTO_YA_FIRMADO",
                    "El consentimiento ya estaba firmado");
        }
        if (request == null || request.firmante() == null || request.firmante().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "FIRMANTE_REQUERIDO",
                    "El nombre del firmante es obligatorio");
        }

        exigirFirmanteTutor(c, request.firmante());

        String antes = resumenTexto(c);
        // En un menor se guarda el nombre canónico del tutor registrado.
        c.setFirmante(esMenor(c.getPaciente()) ? c.getTutorNombre() : request.firmante().trim());
        c.setFirmaImagen(vacioANull(request.firmaImagen()));
        c.setEstado(EstadoConsentimiento.FIRMADO);
        c.setFirmadoEn(Instant.now());
        c.setFirmaHash(huella(c));
        Consentimiento guardado = consentimientoRepository.save(c);

        auditService.registrar("FIRMAR_CONSENTIMIENTO", MODULO, ENTIDAD, id, antes,
                java.util.Map.of("firmante", guardado.getFirmante(), "firmaHash", guardado.getFirmaHash()));
        return completa(guardado);
    }

    // ------------------------------------------------------------------
    // PDF
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public byte[] pdf(Long id) {
        Consentimiento c = consentimientoOArrojar(id);
        return pdfService.generar(c, nombrePaciente(c.getPaciente()));
    }

    @Transactional(readOnly = true)
    public String nombreArchivoPdf(Long id) {
        Consentimiento c = consentimientoOArrojar(id);
        return "consentimiento-" + c.getId() + ".pdf";
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    /** Huella SHA-256 del texto + identidad del documento + firma. */
    private String huella(Consentimiento c) {
        String datos = String.join("|",
                "CONSENTIMIENTO", String.valueOf(c.getId()),
                "PACIENTE", String.valueOf(c.getPaciente().getId()),
                "PLANTILLA", c.getPlantillaCodigo(),
                "VERSION", String.valueOf(c.getPlantillaVersion()),
                "TITULO", c.getTitulo(),
                "CONTENIDO", c.getContenido(),
                "FECHA", c.getFecha().toString(),
                "FIRMANTE", c.getFirmante() == null ? "" : c.getFirmante(),
                "IMAGEN", c.getFirmaImagen() == null ? "" : c.getFirmaImagen());
        return HashUtil.sha256Hex(datos);
    }

    /**
     * §4.14: el consentimiento reutiliza el tutor ya registrado en el
     * paciente. Si no se elige tutor se conserva el nombre tecleado.
     */
    private void aplicarTutor(Consentimiento c, Paciente paciente, Long tutorId, String tutorNombre) {
        if (tutorId == null) {
            c.setTutorId(null);
            c.setTutorParentesco(null);
            c.setTutorTelefono(null);
            c.setTutorNombre(vacioANull(tutorNombre));
            return;
        }
        PacienteTutor tutor = paciente.getTutores().stream()
                .filter(t -> tutorId.equals(t.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "TUTOR_NO_ENCONTRADO",
                        "El tutor indicado no pertenece a este paciente"));
        c.setTutorId(tutor.getId());
        c.setTutorParentesco(tutor.getParentesco());
        c.setTutorTelefono(tutor.getTelefono());
        c.setTutorNombre(completo(tutor.getNombres(), tutor.getApellidos()));
    }

    /** Un menor de edad sólo puede llevar consentimiento con su tutor (§4.14). */
    private void exigirTutorSiMenor(Paciente paciente, Consentimiento c) {
        if (esMenor(paciente) && (c.getTutorNombre() == null || c.getTutorNombre().isBlank())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TUTOR_REQUERIDO",
                    "El paciente es menor de edad: elija el tutor que autoriza");
        }
    }

    /** La firma de un consentimiento de menor corresponde a su tutor (§4.14). */
    private void exigirFirmanteTutor(Consentimiento c, String firmante) {
        if (!esMenor(c.getPaciente())) {
            return;
        }
        String tutor = c.getTutorNombre();
        if (tutor == null || tutor.isBlank()
                || firmante == null || !normalizar(firmante).equals(normalizar(tutor))) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "FIRMANTE_DEBE_SER_TUTOR",
                    "El paciente es menor de edad: quien firma debe ser su tutor"
                            + (tutor == null || tutor.isBlank() ? "" : " (" + tutor + ")"));
        }
    }

    private boolean esMenor(Paciente paciente) {
        LocalDate nacimiento = paciente.getFechaNacimiento();
        return nacimiento != null && Period.between(nacimiento, LocalDate.now()).getYears() < 18;
    }

    private String normalizar(String valor) {
        return valor.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private String completo(String nombres, String apellidos) {
        return ((nombres == null ? "" : nombres.trim()) + " "
                + (apellidos == null ? "" : apellidos.trim())).trim();
    }

    private void exigirBorrador(Consentimiento c, String accion) {
        if (c.getEstado() != EstadoConsentimiento.BORRADOR) {
            throw new BusinessException(HttpStatus.CONFLICT, "CONSENTIMIENTO_FIRMADO",
                    "El consentimiento está firmado y no se puede " + accion);
        }
    }

    private ConsentimientoResponse completa(Consentimiento c) {
        return ConsentimientoResponse.from(c, nombrePaciente(c.getPaciente()),
                nombreOdontologo(c.getOdontologo()));
    }

    private String resumenTexto(Consentimiento c) {
        return c.getTitulo() + " · " + c.getFecha() + " · v" + c.getPlantillaVersion()
                + (c.getContenido() == null ? "" : " · " + c.getContenido().hashCode());
    }

    private Odontologo odontologoDe(Long odontologoId) {
        return odontologoId == null
                ? null
                : odontologoRepository.findById(odontologoId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ODONTOLOGO_NO_ENCONTRADO",
                        "Odontólogo no encontrado"));
    }

    private String vacioANull(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private String nombrePaciente(Paciente paciente) {
        return completo(paciente.getNombres(), paciente.getApellidos());
    }

    private String nombreOdontologo(Odontologo odontologo) {
        if (odontologo == null) {
            return null;
        }
        return completo(odontologo.getNombres(), odontologo.getApellidos());
    }

    private Consentimiento consentimientoOArrojar(Long id) {
        return consentimientoRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO",
                        "Consentimiento no encontrado"));
    }

    private ConsentimientoPlantilla plantillaOArrojar(String codigo) {
        return plantillaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PLANTILLA_NO_ENCONTRADA",
                        "Plantilla de consentimiento no encontrada"));
    }

    private Paciente pacienteOArrojar(Long pacienteId) {
        return pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO",
                        "Paciente no encontrado"));
    }
}
