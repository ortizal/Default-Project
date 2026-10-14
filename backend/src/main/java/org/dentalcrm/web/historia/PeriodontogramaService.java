package org.dentalcrm.web.historia;

import org.dentalcrm.domain.historia.*;
import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.odontologo.OdontologoRepository;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.web.historia.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Periodontograma — Fase E (plan §4.11): examenes por versión con
 * mediciones por diente y sitio (sondaje, margen, inserción, sangrado,
 * placa y supuración), movilidad y furcación por pieza, comparación
 * temporal entre exámenes y reporte. Todo datos estructurados.
 */
@Service
public class PeriodontogramaService {

    private static final String MODULO = "HISTORIA_CLINICA";
    private static final String ENTIDAD = "EXAMEN_PERIODONTAL";
    private static final String ENTIDAD_SITIO = "PERIODONTOGRAMA_SITIO";
    private static final String ENTIDAD_DIENTE = "PERIODONTOGRAMA_DIENTE";

    private final ExamenPeriodontalRepository examenRepository;
    private final PeriodontogramaSitioRepository sitioRepository;
    private final PeriodontogramaDienteRepository dienteRepository;
    private final HistoriaClinicaRepository historiaRepository;
    private final PacienteRepository pacienteRepository;
    private final OdontologoRepository odontologoRepository;
    private final AuditService auditService;
    private final PeriodontogramaPdfService pdfService;

    public PeriodontogramaService(ExamenPeriodontalRepository examenRepository,
                                  PeriodontogramaSitioRepository sitioRepository,
                                  PeriodontogramaDienteRepository dienteRepository,
                                  HistoriaClinicaRepository historiaRepository,
                                  PacienteRepository pacienteRepository,
                                  OdontologoRepository odontologoRepository,
                                  AuditService auditService,
                                  PeriodontogramaPdfService pdfService) {
        this.examenRepository = examenRepository;
        this.sitioRepository = sitioRepository;
        this.dienteRepository = dienteRepository;
        this.historiaRepository = historiaRepository;
        this.pacienteRepository = pacienteRepository;
        this.odontologoRepository = odontologoRepository;
        this.auditService = auditService;
        this.pdfService = pdfService;
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ExamenPeriodontalResponse vigente(Long pacienteId) {
        HistoriaClinica historia = historiaLectura(pacienteId);
        if (historia == null) {
            return null;
        }
        return examenRepository.findFirstByHistoriaIdOrderByVersionDesc(historia.getId())
                .map(this::completo)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ExamenPeriodontalResumenResponse> historial(Long pacienteId) {
        HistoriaClinica historia = historiaLectura(pacienteId);
        if (historia == null) {
            return List.of();
        }
        return examenRepository.findByHistoriaIdOrderByVersionDesc(historia.getId()).stream()
                .map(e -> ExamenPeriodontalResumenResponse.from(e,
                        (int) sitioRepository.countByExamenId(e.getId()),
                        (int) dienteRepository.countByExamenId(e.getId()),
                        nombreOdontologo(e.getOdontologo())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ExamenPeriodontalResponse obtener(Long examenId) {
        return completo(examenOArrojar(examenId));
    }

    // ------------------------------------------------------------------
    // Versiones
    // ------------------------------------------------------------------

    @Transactional
    public ExamenPeriodontalResponse crearVersion(Long pacienteId,
                                                  NuevaVersionPeriodontalRequest request) {
        HistoriaClinica historia = historiaCrear(pacienteId);
        ExamenPeriodontal anterior = examenRepository
                .findFirstByHistoriaIdOrderByVersionDesc(historia.getId()).orElse(null);
        int version = examenRepository.findByHistoriaIdOrderByVersionDesc(historia.getId()).stream()
                .mapToInt(ExamenPeriodontal::getVersion).max().orElse(0) + 1;

        ExamenPeriodontal examen = new ExamenPeriodontal();
        examen.setHistoria(historia);
        examen.setVersion(version);
        examen.setFecha(request == null || request.fecha() == null ? LocalDate.now() : request.fecha());
        examen.setObservaciones(request == null ? null : request.observaciones());
        if (anterior != null) {
            examen.setOdontologo(anterior.getOdontologo());
        }
        ExamenPeriodontal guardado = examenRepository.save(examen);

        boolean copiar = request == null || request.copiarUltima() == null || request.copiarUltima();
        int copiados = copiar && anterior != null ? copiarMediciones(anterior, guardado) : 0;

        auditService.registrar("CREAR_VERSION_PERIODONTAL", MODULO, ENTIDAD, guardado.getId(),
                null, Map.of("version", version, "copiadas", copiados));
        return completo(guardado);
    }

    @Transactional
    public ExamenPeriodontalResponse editarCabecera(Long examenId,
                                                    EditarExamenPeriodontalRequest request) {
        ExamenPeriodontal examen = examenOArrojar(examenId);
        ExamenPeriodontalResumenResponse antes = resumen(examen);
        if (request != null) {
            if (request.fecha() != null) {
                examen.setFecha(request.fecha());
            }
            if (request.observaciones() != null) {
                examen.setObservaciones(request.observaciones());
            }
        }
        ExamenPeriodontal guardado = examenRepository.save(examen);
        auditService.registrar("MODIFICAR_EXAMEN_PERIODONTAL", MODULO, ENTIDAD, examenId,
                antes, resumen(guardado));
        return completo(guardado);
    }

    // ------------------------------------------------------------------
    // Mediciones
    // ------------------------------------------------------------------

    @Transactional
    public ExamenPeriodontalResponse guardarMediciones(Long examenId,
                                                       MedicionesPeriodontalRequest request) {
        ExamenPeriodontal examen = examenOArrojar(examenId);
        int sitios = 0;
        int piezas = 0;

        if (request != null && request.sitios() != null) {
            for (SitioPeriodontalRequest s : request.sitios()) {
                validarDiente(s.diente());
                PeriodontogramaSitio fila = sitioRepository
                        .findByExamenIdAndDienteAndSitio(examen.getId(), s.diente(), s.sitio())
                        .orElseGet(() -> {
                            PeriodontogramaSitio nueva = new PeriodontogramaSitio();
                            nueva.setExamen(examen);
                            nueva.setDiente(s.diente());
                            nueva.setSitio(s.sitio());
                            return nueva;
                        });
                fila.setSondajeMm(decima(s.sondajeMm()));
                fila.setMargenMm(decima(s.margenMm()));
                fila.setInsercionMm(decima(s.insercionMm()));
                fila.setSangrado(Boolean.TRUE.equals(s.sangrado()));
                fila.setPlaca(Boolean.TRUE.equals(s.placa()));
                fila.setSupuracion(Boolean.TRUE.equals(s.supuracion()));
                sitioRepository.save(fila);
                sitios++;
            }
        }

        if (request != null && request.dientes() != null) {
            for (DientePeriodontalRequest d : request.dientes()) {
                validarDiente(d.diente());
                if (d.movilidad() == null && d.furcacion() == null
                        && (d.notas() == null || d.notas().isBlank())) {
                    continue;
                }
                validarGrado(d.movilidad(), "MOVILIDAD_INVALIDA", "movilidad");
                validarGrado(d.furcacion(), "FURCACION_INVALIDA", "furcación");
                PeriodontogramaDiente fila = dienteRepository
                        .findByExamenIdAndDiente(examen.getId(), d.diente())
                        .orElseGet(() -> {
                            PeriodontogramaDiente nueva = new PeriodontogramaDiente();
                            nueva.setExamen(examen);
                            nueva.setDiente(d.diente());
                            return nueva;
                        });
                fila.setMovilidad(d.movilidad());
                fila.setFurcacion(d.furcacion());
                fila.setNotas(d.notas());
                dienteRepository.save(fila);
                piezas++;
            }
        }

        auditService.registrar("GUARDAR_MEDICIONES_PERIODONTALES", MODULO, ENTIDAD, examenId,
                null, Map.of("sitios", sitios, "piezas", piezas));
        return completo(examen);
    }

    @Transactional
    public void eliminarPieza(Long examenId, int diente) {
        ExamenPeriodontal examen = examenOArrojar(examenId);
        validarDiente(diente);
        int previos = (int) sitioRepository.countByExamenId(examen.getId());
        sitioRepository.deleteByExamenIdAndDiente(examen.getId(), diente);
        dienteRepository.deleteByExamenIdAndDiente(examen.getId(), diente);
        auditService.registrar("ELIMINAR_MEDICION_PERIODONTAL", MODULO, ENTIDAD_DIENTE, examenId,
                Map.of("diente", diente), Map.of("sitiosAnteriores", previos));
    }

    // ------------------------------------------------------------------
    // Comparación
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ComparacionPeriodontalResponse comparar(Long pacienteId, int desde, int hasta) {
        HistoriaClinica historia = historiaLectura(pacienteId);
        if (historia == null) {
            throw noEncontrado("Historia clínica");
        }
        ExamenPeriodontal eDesde = examenRepository.findByHistoriaIdAndVersion(historia.getId(), desde)
                .orElseThrow(() -> noEncontrado("Examen periodontal (desde)"));
        ExamenPeriodontal eHasta = examenRepository.findByHistoriaIdAndVersion(historia.getId(), hasta)
                .orElseThrow(() -> noEncontrado("Examen periodontal (hasta)"));

        Map<String, PeriodontogramaSitio> a = mapaSitios(eDesde);
        Map<String, PeriodontogramaSitio> b = mapaSitios(eHasta);
        Map<Integer, PeriodontogramaDiente> da = mapaDientes(eDesde);
        Map<Integer, PeriodontogramaDiente> db = mapaDientes(eHasta);

        List<CambioPeriodontalResponse> cambios = new ArrayList<>();
        int agregados = 0;
        int eliminados = 0;

        for (Map.Entry<String, PeriodontogramaSitio> e : b.entrySet()) {
            PeriodontogramaSitio nueva = e.getValue();
            PeriodontogramaSitio vieja = a.get(e.getKey());
            if (vieja == null) {
                agregados++;
                cambios.add(new CambioPeriodontalResponse(nueva.getDiente(), nueva.getSitio().name(),
                        "NUEVO", "medicion", null, resumenSitio(nueva)));
                continue;
            }
            compararSitio(vieja, nueva, cambios);
        }
        for (Map.Entry<String, PeriodontogramaSitio> e : a.entrySet()) {
            if (!b.containsKey(e.getKey())) {
                eliminados++;
                PeriodontogramaSitio vieja = e.getValue();
                cambios.add(new CambioPeriodontalResponse(vieja.getDiente(), vieja.getSitio().name(),
                        "ELIMINADO", "medicion", resumenSitio(vieja), null));
            }
        }

        for (Map.Entry<Integer, PeriodontogramaDiente> e : db.entrySet()) {
            PeriodontogramaDiente nueva = e.getValue();
            PeriodontogramaDiente vieja = da.get(e.getKey());
            if (vieja == null) {
                agregados++;
                cambios.add(new CambioPeriodontalResponse(nueva.getDiente(), null, "NUEVO",
                        "pieza", null, resumenPieza(nueva)));
                continue;
            }
            compararDiente(vieja, nueva, cambios);
        }
        for (Map.Entry<Integer, PeriodontogramaDiente> e : da.entrySet()) {
            if (!db.containsKey(e.getKey())) {
                eliminados++;
                PeriodontogramaDiente vieja = e.getValue();
                cambios.add(new CambioPeriodontalResponse(vieja.getDiente(), null, "ELIMINADO",
                        "pieza", resumenPieza(vieja), null));
            }
        }

        cambios.sort(Comparator.comparingInt(CambioPeriodontalResponse::diente)
                .thenComparing(c -> c.sitio() == null ? "" : c.sitio()));

        return new ComparacionPeriodontalResponse(resumen(eDesde), resumen(eHasta), cambios,
                agregados, eliminados);
    }

    // ------------------------------------------------------------------
    // PDF
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public byte[] pdf(Long examenId) {
        ExamenPeriodontal examen = examenOArrojar(examenId);
        Paciente paciente = examen.getHistoria().getPaciente();
        String nombre = ((paciente.getNombres() == null ? "" : paciente.getNombres()) + " "
                + (paciente.getApellidos() == null ? "" : paciente.getApellidos())).trim();
        return pdfService.generar(examen, nombre,
                sitioRepository.findByExamenIdOrderByDienteAsc(examen.getId()),
                dienteRepository.findByExamenIdOrderByDienteAsc(examen.getId()));
    }

    @Transactional(readOnly = true)
    public String nombreArchivoPdf(Long examenId) {
        return pdfService.nombreArchivo(examenOArrojar(examenId));
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private int copiarMediciones(ExamenPeriodontal origen, ExamenPeriodontal destino) {
        int copiadas = 0;
        for (PeriodontogramaSitio s : sitioRepository.findByExamenIdOrderByDienteAsc(origen.getId())) {
            PeriodontogramaSitio copia = new PeriodontogramaSitio();
            copia.setExamen(destino);
            copia.setDiente(s.getDiente());
            copia.setSitio(s.getSitio());
            copia.setSondajeMm(s.getSondajeMm());
            copia.setMargenMm(s.getMargenMm());
            copia.setInsercionMm(s.getInsercionMm());
            copia.setSangrado(s.isSangrado());
            copia.setPlaca(s.isPlaca());
            copia.setSupuracion(s.isSupuracion());
            sitioRepository.save(copia);
            copiadas++;
        }
        for (PeriodontogramaDiente d : dienteRepository.findByExamenIdOrderByDienteAsc(origen.getId())) {
            PeriodontogramaDiente copia = new PeriodontogramaDiente();
            copia.setExamen(destino);
            copia.setDiente(d.getDiente());
            copia.setMovilidad(d.getMovilidad());
            copia.setFurcacion(d.getFurcacion());
            copia.setNotas(d.getNotas());
            dienteRepository.save(copia);
            copiadas++;
        }
        return copiadas;
    }

    private void compararSitio(PeriodontogramaSitio vieja, PeriodontogramaSitio nueva,
                               List<CambioPeriodontalResponse> cambios) {
        compararCampo(vieja.getDiente(), nueva.getSitio().name(), "sondajeMm",
                texto(vieja.getSondajeMm()), texto(nueva.getSondajeMm()), cambios);
        compararCampo(vieja.getDiente(), nueva.getSitio().name(), "margenMm",
                texto(vieja.getMargenMm()), texto(nueva.getMargenMm()), cambios);
        compararCampo(vieja.getDiente(), nueva.getSitio().name(), "insercionMm",
                texto(vieja.getInsercionMm()), texto(nueva.getInsercionMm()), cambios);
        compararCampo(vieja.getDiente(), nueva.getSitio().name(), "sangrado",
                texto(vieja.isSangrado()), texto(nueva.isSangrado()), cambios);
        compararCampo(vieja.getDiente(), nueva.getSitio().name(), "placa",
                texto(vieja.isPlaca()), texto(nueva.isPlaca()), cambios);
        compararCampo(vieja.getDiente(), nueva.getSitio().name(), "supuracion",
                texto(vieja.isSupuracion()), texto(nueva.isSupuracion()), cambios);
    }

    private void compararDiente(PeriodontogramaDiente vieja, PeriodontogramaDiente nueva,
                                List<CambioPeriodontalResponse> cambios) {
        compararCampo(vieja.getDiente(), null, "movilidad",
                texto(vieja.getMovilidad()), texto(nueva.getMovilidad()), cambios);
        compararCampo(vieja.getDiente(), null, "furcacion",
                texto(vieja.getFurcacion()), texto(nueva.getFurcacion()), cambios);
        compararCampo(vieja.getDiente(), null, "notas",
                texto(vieja.getNotas()), texto(nueva.getNotas()), cambios);
    }

    private void compararCampo(int diente, String sitio, String campo,
                               String anterior, String nuevo,
                               List<CambioPeriodontalResponse> cambios) {
        if (Objects.equals(anterior, nuevo)) {
            return;
        }
        cambios.add(new CambioPeriodontalResponse(diente, sitio, "MODIFICADO", campo, anterior, nuevo));
    }

    private Map<String, PeriodontogramaSitio> mapaSitios(ExamenPeriodontal examen) {
        Map<String, PeriodontogramaSitio> mapa = new LinkedHashMap<>();
        for (PeriodontogramaSitio s : sitioRepository.findByExamenIdOrderByDienteAsc(examen.getId())) {
            mapa.put(s.getDiente() + "|" + s.getSitio().name(), s);
        }
        return mapa;
    }

    private Map<Integer, PeriodontogramaDiente> mapaDientes(ExamenPeriodontal examen) {
        Map<Integer, PeriodontogramaDiente> mapa = new LinkedHashMap<>();
        for (PeriodontogramaDiente d : dienteRepository.findByExamenIdOrderByDienteAsc(examen.getId())) {
            mapa.put(d.getDiente(), d);
        }
        return mapa;
    }

    private ExamenPeriodontalResponse completo(ExamenPeriodontal examen) {
        List<SitioPeriodontalResponse> sitios =
                sitioRepository.findByExamenIdOrderByDienteAsc(examen.getId()).stream()
                        .sorted(Comparator.comparingInt(PeriodontogramaSitio::getDiente)
                                .thenComparingInt(s -> s.getSitio().ordinal()))
                        .map(SitioPeriodontalResponse::from)
                        .toList();
        List<DientePeriodontalResponse> dientes =
                dienteRepository.findByExamenIdOrderByDienteAsc(examen.getId()).stream()
                        .map(DientePeriodontalResponse::from)
                        .toList();
        return ExamenPeriodontalResponse.from(examen, sitios, dientes,
                nombreOdontologo(examen.getOdontologo()));
    }

    private ExamenPeriodontalResumenResponse resumen(ExamenPeriodontal examen) {
        return ExamenPeriodontalResumenResponse.from(examen,
                (int) sitioRepository.countByExamenId(examen.getId()),
                (int) dienteRepository.countByExamenId(examen.getId()),
                nombreOdontologo(examen.getOdontologo()));
    }

    private String resumenSitio(PeriodontogramaSitio s) {
        return "sondaje " + texto(s.getSondajeMm()) + ", sangrado " + texto(s.isSangrado());
    }

    private String resumenPieza(PeriodontogramaDiente d) {
        return "movilidad " + texto(d.getMovilidad()) + ", furcación " + texto(d.getFurcacion());
    }

    private String texto(BigDecimal v) {
        return v == null ? null : v.stripTrailingZeros().toPlainString();
    }

    private String texto(Integer v) {
        return v == null ? null : String.valueOf(v);
    }

    private String texto(boolean v) {
        return v ? "sí" : "no";
    }

    private String texto(String v) {
        return v == null || v.isBlank() ? null : v.trim();
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

    private String nombreOdontologo(Odontologo odontologo) {
        if (odontologo == null) {
            return null;
        }
        String nombres = odontologo.getNombres() == null ? "" : odontologo.getNombres().trim();
        String apellidos = odontologo.getApellidos() == null ? "" : odontologo.getApellidos().trim();
        return (nombres + " " + apellidos).trim();
    }

    private ExamenPeriodontal examenOArrojar(Long examenId) {
        return examenRepository.findById(examenId)
                .orElseThrow(() -> noEncontrado("Examen periodontal"));
    }

    private HistoriaClinica historiaLectura(Long pacienteId) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> noEncontrado("Paciente"));
        return historiaRepository.findByPacienteId(paciente.getId()).orElse(null);
    }

    private HistoriaClinica historiaCrear(Long pacienteId) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> noEncontrado("Paciente"));
        HistoriaClinica existente = historiaRepository.findByPacienteId(paciente.getId()).orElse(null);
        if (existente != null) {
            return existente;
        }
        HistoriaClinica historia = new HistoriaClinica();
        historia.setPaciente(paciente);
        historia.setEstado(EstadoHistoria.ABIERTA);
        return historiaRepository.save(historia);
    }

    private void validarGrado(Integer valor, String codigo, String campo) {
        if (valor != null && (valor < 0 || valor > 3)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, codigo,
                    "Grado de " + campo + " inválido: " + valor + " (debe ser 0 a 3)");
        }
    }

    private BusinessException noEncontrado(String entidad) {
        return new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", entidad + " no encontrado");
    }

    /** Redondea a una décima como hace la columna NUMERIC(4,1). */
    public static BigDecimal decima(BigDecimal valor) {
        return valor == null ? null : valor.setScale(1, RoundingMode.HALF_UP);
    }
}
