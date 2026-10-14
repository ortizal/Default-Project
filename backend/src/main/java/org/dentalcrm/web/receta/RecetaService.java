package org.dentalcrm.web.receta;

import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.odontologo.OdontologoRepository;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.domain.receta.*;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.service.CurrentUserService;
import org.dentalcrm.web.receta.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Recetas médicas — Fase E (plan §4.12): receta por paciente con pauta
 * completa por medicamento, historial, anulación motivada y PDF. Las
 * líneas se guardan de forma atómica (alta o reemplazo completo).
 */
@Service
public class RecetaService {

    private static final String MODULO = "HISTORIA_CLINICA";
    private static final String ENTIDAD = "RECETA";

    private final RecetaRepository recetaRepository;
    private final PacienteRepository pacienteRepository;
    private final OdontologoRepository odontologoRepository;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;
    private final RecetaPdfService pdfService;

    @PersistenceContext
    private EntityManager entityManager;

    public RecetaService(RecetaRepository recetaRepository,
                         PacienteRepository pacienteRepository,
                         OdontologoRepository odontologoRepository,
                         AuditService auditService,
                         CurrentUserService currentUserService,
                         RecetaPdfService pdfService) {
        this.recetaRepository = recetaRepository;
        this.pacienteRepository = pacienteRepository;
        this.odontologoRepository = odontologoRepository;
        this.auditService = auditService;
        this.currentUserService = currentUserService;
        this.pdfService = pdfService;
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<RecetaResumenResponse> historial(Long pacienteId) {
        Paciente paciente = pacienteOArrojar(pacienteId);
        return recetaRepository.findByPacienteIdOrderByFechaDescIdDesc(paciente.getId()).stream()
                .map(r -> RecetaResumenResponse.from(r, nombreOdontologo(r.getOdontologo())))
                .toList();
    }

    @Transactional(readOnly = true)
    public RecetaResponse obtener(Long recetaId) {
        return completa(recetaOArrojar(recetaId));
    }

    // ------------------------------------------------------------------
    // Alta y edición
    // ------------------------------------------------------------------

    @Transactional
    public RecetaResponse crear(Long pacienteId, RecetaRequest request) {
        Paciente paciente = pacienteOArrojar(pacienteId);

        Receta receta = new Receta();
        receta.setPaciente(paciente);
        receta.setEstado(EstadoReceta.VIGENTE);
        receta.setCreatedBy(currentUserService.idUsuarioActual().orElse(null));
        aplicarCabecera(receta, request);
        Receta guardada = recetaRepository.save(receta);

        auditService.registrar("CREAR_RECETA", MODULO, ENTIDAD, guardada.getId(),
                null, Map.of("pacienteId", paciente.getId(), "lineas", guardada.getLineas().size()));
        return completa(guardada);
    }

    @Transactional
    public RecetaResponse editar(Long recetaId, RecetaRequest request) {
        Receta receta = recetaOArrojar(recetaId);
        if (receta.getEstado() == EstadoReceta.ANULADA) {
            throw new BusinessException(HttpStatus.CONFLICT, "RECETA_ANULADA",
                    "La receta está anulada y no se puede editar");
        }
        RecetaResumenResponse antes = RecetaResumenResponse.from(receta, nombreOdontologo(receta.getOdontologo()));
        aplicarCabecera(receta, request);
        Receta guardada = recetaRepository.save(receta);

        auditService.registrar("MODIFICAR_RECETA", MODULO, ENTIDAD, recetaId, antes,
                RecetaResumenResponse.from(guardada, nombreOdontologo(guardada.getOdontologo())));
        return completa(guardada);
    }

    // ------------------------------------------------------------------
    // Anulación
    // ------------------------------------------------------------------

    @Transactional
    public RecetaResponse cambiarEstado(Long recetaId, CambioEstadoRecetaRequest request) {
        Receta receta = recetaOArrojar(recetaId);
        if (request == null || request.estado() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "ESTADO_REQUERIDO",
                    "El estado de la receta es obligatorio");
        }
        if (request.estado() != EstadoReceta.ANULADA) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "ESTADO_INVALIDO",
                    "Sólo se puede anular una receta");
        }
        if (request.motivo() == null || request.motivo().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "MOTIVO_REQUERIDO",
                    "La anulación exige un motivo");
        }
        if (receta.getEstado() == EstadoReceta.ANULADA) {
            throw new BusinessException(HttpStatus.CONFLICT, "RECETA_YA_ANULADA",
                    "La receta ya estaba anulada");
        }

        String antes = receta.getEstado().name();
        receta.setEstado(EstadoReceta.ANULADA);
        receta.setMotivoAnulacion(request.motivo().trim());
        Receta guardada = recetaRepository.save(receta);

        auditService.registrar("ANULAR_RECETA", MODULO, ENTIDAD, recetaId, antes,
                Map.of("estado", EstadoReceta.ANULADA.name(), "motivo", guardada.getMotivoAnulacion()));
        return completa(guardada);
    }

    // ------------------------------------------------------------------
    // PDF
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public byte[] pdf(Long recetaId) {
        Receta receta = recetaOArrojar(recetaId);
        return pdfService.generar(receta, nombrePaciente(receta.getPaciente()));
    }

    @Transactional(readOnly = true)
    public String nombreArchivoPdf(Long recetaId) {
        return pdfService.nombreArchivo(recetaOArrojar(recetaId));
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private void aplicarCabecera(Receta receta, RecetaRequest request) {
        if (request != null) {
            receta.setFecha(request.fecha() == null ? LocalDate.now() : request.fecha());
            receta.setDiagnostico(vacioANull(request.diagnostico()));
            receta.setObservaciones(vacioANull(request.observaciones()));
            receta.setOdontologo(request.odontologoId() == null
                    ? null
                    : odontologoRepository.findById(request.odontologoId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ODONTOLOGO_NO_ENCONTRADO",
                            "Odontólogo no encontrado")));
        } else if (receta.getFecha() == null) {
            receta.setFecha(LocalDate.now());
        }
        aplicarLineas(receta, request == null ? null : request.lineas());
    }

    private void aplicarLineas(Receta receta, List<RecetaLineaRequest> lineas) {
        if (lineas == null || lineas.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "RECETA_SIN_LINEAS",
                    "La receta debe tener al menos un medicamento");
        }
        for (RecetaLineaRequest req : lineas) {
            if (req == null || req.medicamento() == null || req.medicamento().isBlank()
                    || req.dosis() == null || req.dosis().isBlank()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "LINEA_INVALIDA",
                        "Cada medicamento requiere nombre y dosis");
            }
        }
        receta.getLineas().clear();
        // Hibernate inserta antes de borrar: se vacía la colección y se fija el
        // borrado de las líneas anteriores (unicidad receta_id, número) antes de
        // montar las nuevas.
        if (receta.getId() != null) {
            entityManager.flush();
        }
        int numero = 1;
        for (RecetaLineaRequest req : lineas) {
            RecetaLinea linea = new RecetaLinea();
            linea.setReceta(receta);
            linea.setNumero(numero++);
            linea.setMedicamento(req.medicamento().trim());
            linea.setConcentracion(vacioANull(req.concentracion()));
            linea.setPresentacion(vacioANull(req.presentacion()));
            linea.setDosis(req.dosis().trim());
            linea.setVia(req.via());
            linea.setFrecuencia(vacioANull(req.frecuencia()));
            linea.setDuracion(vacioANull(req.duracion()));
            linea.setInstrucciones(vacioANull(req.instrucciones()));
            receta.getLineas().add(linea);
        }
    }

    private RecetaResponse completa(Receta receta) {
        List<RecetaLineaResponse> lineas = receta.getLineas().stream()
                .map(RecetaLineaResponse::from)
                .toList();
        return RecetaResponse.from(receta, nombrePaciente(receta.getPaciente()),
                nombreOdontologo(receta.getOdontologo()), lineas);
    }

    private String vacioANull(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private String nombrePaciente(Paciente paciente) {
        return ((paciente.getNombres() == null ? "" : paciente.getNombres()) + " "
                + (paciente.getApellidos() == null ? "" : paciente.getApellidos())).trim();
    }

    private String nombreOdontologo(Odontologo odontologo) {
        if (odontologo == null) {
            return null;
        }
        return ((odontologo.getNombres() == null ? "" : odontologo.getNombres()) + " "
                + (odontologo.getApellidos() == null ? "" : odontologo.getApellidos())).trim();
    }

    private Receta recetaOArrojar(Long recetaId) {
        return recetaRepository.findById(recetaId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO",
                        "Receta no encontrada"));
    }

    private Paciente pacienteOArrojar(Long pacienteId) {
        return pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO",
                        "Paciente no encontrado"));
    }
}
