package org.dentalcrm.web.historia;

import org.dentalcrm.domain.historia.*;
import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.odontologo.OdontologoRepository;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.service.CurrentUserService;
import org.dentalcrm.web.historia.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Odontograma interactivo — Fase C (plan §4.5): versiones fechadas con
 * condiciones por diente FDI y superficie, estados clínicos, log de
 * cambios y comparación temporal. Sólo datos estructurados: el dibujo
 * (SVG) lo arma el cliente.
 */
@Service
public class OdontogramaService {

    private static final String MODULO = "HISTORIA_CLINICA";
    private static final String ENTIDAD = "ODONTOGRAMA";
    private static final String ENTIDAD_DIENTE = "ODONTOGRAMA_DIENTE";

    private final OdontogramaRepository odontogramaRepository;
    private final OdontogramaDienteRepository dienteRepository;
    private final OdontogramaEventoRepository eventoRepository;
    private final HistoriaClinicaRepository historiaRepository;
    private final PacienteRepository pacienteRepository;
    private final OdontologoRepository odontologoRepository;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    public OdontogramaService(OdontogramaRepository odontogramaRepository,
                              OdontogramaDienteRepository dienteRepository,
                              OdontogramaEventoRepository eventoRepository,
                              HistoriaClinicaRepository historiaRepository,
                              PacienteRepository pacienteRepository,
                              OdontologoRepository odontologoRepository,
                              AuditService auditService,
                              CurrentUserService currentUserService) {
        this.odontogramaRepository = odontogramaRepository;
        this.dienteRepository = dienteRepository;
        this.eventoRepository = eventoRepository;
        this.historiaRepository = historiaRepository;
        this.pacienteRepository = pacienteRepository;
        this.odontologoRepository = odontologoRepository;
        this.auditService = auditService;
        this.currentUserService = currentUserService;
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public OdontogramaResponse vigente(Long pacienteId) {
        HistoriaClinica historia = historiaOArrojarLectura(pacienteId);
        if (historia == null) {
            return null;
        }
        return odontogramaRepository.findFirstByHistoriaIdOrderByVersionDesc(historia.getId())
                .map(this::completo)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<OdontogramaResumenResponse> historial(Long pacienteId) {
        HistoriaClinica historia = historiaOArrojarLectura(pacienteId);
        if (historia == null) {
            return List.of();
        }
        return odontogramaRepository.findByHistoriaIdOrderByVersionDesc(historia.getId()).stream()
                .map(o -> OdontogramaResumenResponse.from(o, dienteRepository.countByOdontogramaId(o.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public OdontogramaResponse version(Long pacienteId, int version) {
        HistoriaClinica historia = historiaOArrojarLectura(pacienteId);
        if (historia == null) {
            throw noEncontrado("Odontograma");
        }
        Odontograma odontograma = odontogramaRepository
                .findByHistoriaIdAndVersion(historia.getId(), version)
                .orElseThrow(() -> noEncontrado("Versión de odontograma"));
        return completo(odontograma);
    }

    @Transactional(readOnly = true)
    public List<EventoResponse> eventos(Long odontogramaId) {
        Odontograma odontograma = odontogramaOArrojar(odontogramaId);
        return eventoRepository.findTop100ByOdontogramaIdOrderByFechaDesc(odontograma.getId()).stream()
                .map(EventoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ComparacionResponse comparar(Long pacienteId, int desde, int hasta) {
        HistoriaClinica historia = historiaOArrojarLectura(pacienteId);
        if (historia == null) {
            throw noEncontrado("Odontograma");
        }
        Odontograma a = odontogramaRepository.findByHistoriaIdAndVersion(historia.getId(), desde)
                .orElseThrow(() -> noEncontrado("Versión de odontograma"));
        Odontograma b = odontogramaRepository.findByHistoriaIdAndVersion(historia.getId(), hasta)
                .orElseThrow(() -> noEncontrado("Versión de odontograma"));

        List<OdontogramaDiente> dientesA = dienteRepository.findByOdontogramaIdOrderByDienteAsc(a.getId());
        List<OdontogramaDiente> dientesB = dienteRepository.findByOdontogramaIdOrderByDienteAsc(b.getId());

        Map<String, OdontogramaDiente> mapaA = new LinkedHashMap<>();
        for (OdontogramaDiente d : dientesA) {
            mapaA.put(clave(d), d);
        }

        List<CambioResponse> cambios = new ArrayList<>();
        for (OdontogramaDiente d : dientesB) {
            OdontogramaDiente anterior = mapaA.remove(clave(d));
            if (anterior == null) {
                cambios.add(new CambioResponse(d.getDiente(), d.getSuperficie(), d.getCondicion(),
                        null, d.getEstado(), "NUEVO"));
            } else if (anterior.getEstado() != d.getEstado()
                    || !Objects.equals(anterior.getNotas(), d.getNotas())) {
                cambios.add(new CambioResponse(d.getDiente(), d.getSuperficie(), d.getCondicion(),
                        anterior.getEstado(), d.getEstado(), "MODIFICADO"));
            }
        }
        for (OdontogramaDiente d : mapaA.values()) {
            cambios.add(new CambioResponse(d.getDiente(), d.getSuperficie(), d.getCondicion(),
                    d.getEstado(), null, "ELIMINADO"));
        }

        return new ComparacionResponse(
                OdontogramaResumenResponse.from(a, dientesA.size()),
                OdontogramaResumenResponse.from(b, dientesB.size()),
                cambios);
    }

    // ------------------------------------------------------------------
    // Versiones
    // ------------------------------------------------------------------

    @Transactional
    public OdontogramaResponse crearVersion(Long pacienteId, NuevaVersionRequest request) {
        HistoriaClinica historia = historiaOcrear(pacienteId);
        List<Odontograma> versiones = odontogramaRepository.findByHistoriaIdOrderByVersionDesc(historia.getId());
        int siguiente = versiones.isEmpty() ? 1 : versiones.get(0).getVersion() + 1;

        Odontograma odontograma = new Odontograma();
        odontograma.setHistoria(historia);
        odontograma.setVersion(siguiente);
        odontograma.setFecha(request.fecha() != null ? request.fecha() : LocalDate.now());
        odontograma.setDenticion(request.denticion() != null ? request.denticion() : Denticion.PERMANENTE);
        odontograma.setObservaciones(request.observaciones());
        odontograma.setCreatedBy(currentUserService.idUsuarioActual().orElse(null));
        Odontograma guardado = odontogramaRepository.save(odontograma);

        boolean copiar = request.copiarUltima() == null || request.copiarUltima();
        int copiadas = 0;
        if (copiar && !versiones.isEmpty()) {
            copiadas = copiarDientes(versiones.get(0), guardado);
        }

        registrarEvento(guardado, AccionOdontograma.CREAR_VERSION, null, null, null, null,
                "V" + siguiente,
                copiadas > 0 ? "Copia de la versión " + versiones.get(0).getVersion()
                        + " (" + copiadas + " registros)" : "Versión en blanco");
        OdontogramaResponse respuesta = completo(guardado);
        auditService.registrar("CREAR_ODONTOGRAMA", MODULO, ENTIDAD,
                guardado.getId(), null, respuesta);
        return respuesta;
    }

    // ------------------------------------------------------------------
    // Dientes y superficies
    // ------------------------------------------------------------------

    @Transactional
    public DienteResponse guardarDiente(Long odontogramaId, DienteRequest request) {
        Odontograma odontograma = odontogramaOArrojar(odontogramaId);
        validarDiente(request.diente());
        String condicion = request.condicion().trim();
        EstadoOdontograma estado = request.estado() != null ? request.estado() : EstadoOdontograma.EXISTENTE;

        OdontogramaDiente existente = dienteRepository.findByOdontogramaIdOrderByDienteAsc(odontogramaId).stream()
                .filter(d -> d.getDiente() == request.diente()
                        && d.getSuperficie() == request.superficie()
                        && d.getCondicion().equalsIgnoreCase(condicion))
                .findFirst()
                .orElse(null);

        if (existente == null) {
            OdontogramaDiente diente = new OdontogramaDiente();
            diente.setOdontograma(odontograma);
            diente.setDiente(request.diente());
            diente.setSuperficie(request.superficie());
            diente.setCondicion(condicion);
            diente.setEstado(estado);
            diente.setNotas(request.notas());
            OdontogramaDiente guardado = dienteRepository.save(diente);
            registrarEvento(odontograma, AccionOdontograma.AGREGAR, guardado.getDiente(),
                    guardado.getSuperficie(), guardado.getCondicion(), null, estado.name(), null);
            auditService.registrar("AGREGAR_CONDICION", MODULO, ENTIDAD_DIENTE,
                    guardado.getId(), null, DienteResponse.from(guardado));
            return DienteResponse.from(guardado);
        }

        EstadoOdontograma anterior = existente.getEstado();
        existente.setEstado(estado);
        existente.setNotas(request.notas());
        OdontogramaDiente guardado = dienteRepository.save(existente);
        if (anterior != estado) {
            registrarEvento(odontograma, AccionOdontograma.MODIFICAR, guardado.getDiente(),
                    guardado.getSuperficie(), guardado.getCondicion(),
                    anterior.name(), estado.name(), null);
        }
        auditService.registrar("MODIFICAR_CONDICION", MODULO, ENTIDAD_DIENTE,
                guardado.getId(), anterior == null ? null : anterior.name(), DienteResponse.from(guardado));
        return DienteResponse.from(guardado);
    }

    @Transactional
    public void eliminarDiente(Long odontogramaId, Long dienteId) {
        Odontograma odontograma = odontogramaOArrojar(odontogramaId);
        OdontogramaDiente diente = dienteRepository.findById(dienteId)
                .orElseThrow(() -> noEncontrado("Condición"));
        if (diente.getOdontograma().getId() == null
                || !Objects.equals(diente.getOdontograma().getId(), odontograma.getId())) {
            throw noEncontrado("Condición");
        }
        DienteResponse anterior = DienteResponse.from(diente);
        registrarEvento(odontograma, AccionOdontograma.ELIMINAR, diente.getDiente(),
                diente.getSuperficie(), diente.getCondicion(), diente.getEstado().name(), null, null);
        dienteRepository.delete(diente);
        auditService.registrar("ELIMINAR_CONDICION", MODULO, ENTIDAD_DIENTE, dienteId, anterior, null);
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private int copiarDientes(Odontograma origen, Odontograma destino) {
        int copiadas = 0;
        for (OdontogramaDiente d : dienteRepository.findByOdontogramaIdOrderByDienteAsc(origen.getId())) {
            OdontogramaDiente copia = new OdontogramaDiente();
            copia.setOdontograma(destino);
            copia.setDiente(d.getDiente());
            copia.setSuperficie(d.getSuperficie());
            copia.setCondicion(d.getCondicion());
            copia.setEstado(d.getEstado());
            copia.setNotas(d.getNotas());
            copia.setOdontologo(d.getOdontologo());
            dienteRepository.save(copia);
            copiadas++;
        }
        return copiadas;
    }

    private OdontogramaResponse completo(Odontograma odontograma) {
        List<OdontogramaDiente> dientes = dienteRepository.findByOdontogramaIdOrderByDienteAsc(odontograma.getId());
        return OdontogramaResponse.from(odontograma, dientes, nombreOdontologo(odontograma.getOdontologo()));
    }

    private String nombreOdontologo(Odontologo odontologo) {
        if (odontologo == null) {
            return null;
        }
        String nombres = odontologo.getNombres() == null ? "" : odontologo.getNombres().trim();
        String apellidos = odontologo.getApellidos() == null ? "" : odontologo.getApellidos().trim();
        return (nombres + " " + apellidos).trim();
    }

    private void validarDiente(int diente) {
        int cuadrante = diente / 10;
        int pieza = diente % 10;
        boolean valido = switch (cuadrante) {
            case 1, 2, 3, 4 -> pieza >= 1 && pieza <= 8;
            case 5, 6, 7, 8 -> pieza >= 1 && pieza <= 5;
            default -> false;
        };
        if (!valido) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "DIENTE_INVALIDO",
                    "Número FDI inválido: " + diente);
        }
    }

    private String clave(OdontogramaDiente d) {
        return d.getDiente() + "|" + (d.getSuperficie() == null ? "-" : d.getSuperficie().name())
                + "|" + d.getCondicion().toLowerCase();
    }

    private void registrarEvento(Odontograma odontograma, AccionOdontograma accion, Integer diente,
                                 SuperficieDental superficie, String condicion,
                                 String estadoAnterior, String estadoNuevo, String detalle) {
        OdontogramaEvento evento = new OdontogramaEvento();
        evento.setOdontograma(odontograma);
        evento.setAccion(accion);
        evento.setDiente(diente);
        evento.setSuperficie(superficie);
        evento.setCondicion(condicion);
        evento.setEstadoAnterior(estadoAnterior);
        evento.setEstadoNuevo(estadoNuevo);
        evento.setDetalle(detalle);
        evento.setAutorId(currentUserService.idUsuarioActual().orElse(null));
        eventoRepository.save(evento);
    }

    private Odontograma odontogramaOArrojar(Long odontogramaId) {
        return odontogramaRepository.findById(odontogramaId)
                .orElseThrow(() -> noEncontrado("Odontograma"));
    }

    private Paciente pacienteOArrojar(Long pacienteId) {
        return pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> noEncontrado("Paciente"));
    }

    private HistoriaClinica historiaOArrojarLectura(Long pacienteId) {
        Paciente paciente = pacienteOArrojar(pacienteId);
        return historiaRepository.findByPacienteId(paciente.getId()).orElse(null);
    }

    private HistoriaClinica historiaOcrear(Long pacienteId) {
        Paciente paciente = pacienteOArrojar(pacienteId);
        HistoriaClinica existente = historiaRepository.findByPacienteId(paciente.getId()).orElse(null);
        if (existente != null) {
            return existente;
        }
        HistoriaClinica historia = new HistoriaClinica();
        historia.setPaciente(paciente);
        historia.setEstado(EstadoHistoria.ABIERTA);
        HistoriaClinica creada = historiaRepository.save(historia);
        auditService.registrar("CREAR_HISTORIA", MODULO, "HISTORIA_CLINICA",
                creada.getId(), null, creada.getId());
        return creada;
    }

    private BusinessException noEncontrado(String entidad) {
        return new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", entidad + " no encontrado");
    }
}
