package org.dentalcrm.web.archivo;

import org.dentalcrm.domain.archivo.ArchivoClinico;
import org.dentalcrm.domain.archivo.ArchivoClinicoRepository;
import org.dentalcrm.domain.archivo.CategoriaArchivo;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.multitenant.TenantContext;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.service.CurrentUserService;
import org.dentalcrm.storage.FileStorageProvider;
import org.dentalcrm.util.HashUtil;
import org.dentalcrm.web.archivo.dto.ArchivoClinicoResponse;
import org.dentalcrm.web.archivo.dto.ContenidoArchivo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Archivos clínicos — Fase E (plan §4.15): radiografías, fotografías,
 * panorámicas, tomografías, resultados y documentos del paciente.
 *
 * <p>La metadata vive en PostgreSQL y los bytes en el
 * {@link FileStorageProvider}; aquí sólo se persiste la clave relativa,
 * el tamaño y la huella SHA-256 del contenido.
 */
@Service
public class ArchivoClinicoService {

    private static final String MODULO = "HISTORIA_CLINICA";
    private static final String ENTIDAD = "ARCHIVO_CLINICO";

    /** Límite por archivo: 20 MB (el multipart global queda en 25 MB). */
    static final long MAX_BYTES = 20L * 1024 * 1024;

    private static final Set<String> EXTENSIONES =
            Set.of("jpg", "jpeg", "png", "webp", "tif", "tiff", "pdf", "dcm");

    private static final Map<String, String> MIME_POR_EXTENSION = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp",
            "tif", "image/tiff",
            "tiff", "image/tiff",
            "pdf", "application/pdf",
            "dcm", "application/dicom");

    private static final Set<String> MIME_ACEPTADOS = Set.copyOf(MIME_POR_EXTENSION.values());

    private final ArchivoClinicoRepository archivoRepository;
    private final PacienteRepository pacienteRepository;
    private final FileStorageProvider storage;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    public ArchivoClinicoService(ArchivoClinicoRepository archivoRepository,
                                 PacienteRepository pacienteRepository,
                                 FileStorageProvider storage,
                                 AuditService auditService,
                                 CurrentUserService currentUserService) {
        this.archivoRepository = archivoRepository;
        this.pacienteRepository = pacienteRepository;
        this.storage = storage;
        this.auditService = auditService;
        this.currentUserService = currentUserService;
    }

    // ------------------------------------------------------------------
    // Altas y consultas
    // ------------------------------------------------------------------

    @Transactional
    public ArchivoClinicoResponse subir(Long pacienteId, String categoria, LocalDate fecha,
                                        String observaciones, MultipartFile archivo) {
        Paciente paciente = pacienteOArrojar(pacienteId);
        CategoriaArchivo categoriaArchivo = categoriaOArrojar(categoria);

        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "ARCHIVO_VACIO",
                    "Selecciona un archivo para subir");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "ARCHIVO_DEMASIADO_GRANDE",
                    "El archivo no puede superar 20 MB");
        }

        String nombre = nombreSeguro(archivo.getOriginalFilename());
        String extension = extension(nombre);
        if (!EXTENSIONES.contains(extension)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "ARCHIVO_FORMATO_INVALIDO",
                    "Formato no permitido: usa JPG, PNG, WEBP, TIFF, PDF o DCM");
        }

        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (IOException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "ARCHIVO_INVALIDO",
                    "No se pudo leer el archivo");
        }
        if (contenido.length == 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "ARCHIVO_VACIO",
                    "El archivo está vacío");
        }

        String carpeta = "tenant-" + TenantContext.actualOrDefault()
                + "/paciente-" + paciente.getId() + "/archivos";
        String clave = storage.guardar(carpeta, UUID.randomUUID() + "-" + nombre, contenido);

        ArchivoClinico entidad = new ArchivoClinico();
        entidad.setPaciente(paciente);
        entidad.setCategoria(categoriaArchivo);
        entidad.setNombre(nombre);
        entidad.setClave(clave);
        entidad.setMime(mime(archivo.getContentType(), extension));
        entidad.setTamano((long) contenido.length);
        entidad.setSha256(HashUtil.sha256Hex(contenido));
        entidad.setFecha(fecha);
        entidad.setObservaciones(observaciones == null || observaciones.isBlank()
                ? null : observaciones.trim());
        entidad.setCreatedBy(currentUserService.idUsuarioActual().orElse(null));

        ArchivoClinico guardado;
        try {
            guardado = archivoRepository.save(entidad);
        } catch (RuntimeException e) {
            // Si la metadata no llega a guardarse, no dejamos huérfanos en disco.
            storage.eliminar(clave);
            throw e;
        }

        auditService.registrar("SUBIR_ARCHIVO", MODULO, ENTIDAD, guardado.getId());
        return ArchivoClinicoResponse.from(guardado);
    }

    @Transactional(readOnly = true)
    public List<ArchivoClinicoResponse> listar(Long pacienteId) {
        pacienteOArrojar(pacienteId);
        return archivoRepository.findByPacienteIdOrderByCreatedAtDescIdDesc(pacienteId).stream()
                .map(ArchivoClinicoResponse::from)
                .toList();
    }

    // ------------------------------------------------------------------
    // Descarga y borrado
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ContenidoArchivo contenido(Long id) {
        ArchivoClinico archivo = archivoOArrojar(id);
        byte[] bytes = storage.leer(archivo.getClave())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "ARCHIVO_FISICO_NO_ENCONTRADO",
                        "El contenido del archivo ya no está disponible"));
        return new ContenidoArchivo(archivo.getNombre(), archivo.getMime(), bytes);
    }

    @Transactional
    public void eliminar(Long id) {
        ArchivoClinico archivo = archivoOArrojar(id);
        storage.eliminar(archivo.getClave());
        archivoRepository.delete(archivo);
        auditService.registrar("ELIMINAR_ARCHIVO", MODULO, ENTIDAD, id);
    }

    // ------------------------------------------------------------------
    // Validaciones y utilidades
    // ------------------------------------------------------------------

    private Paciente pacienteOArrojar(Long pacienteId) {
        return pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO",
                        "Paciente no encontrado"));
    }

    private ArchivoClinico archivoOArrojar(Long id) {
        return archivoRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO",
                        "Archivo no encontrado"));
    }

    private CategoriaArchivo categoriaOArrojar(String categoria) {
        if (categoria == null || categoria.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "CATEGORIA_REQUERIDA",
                    "La categoría del archivo es obligatoria");
        }
        try {
            return CategoriaArchivo.valueOf(categoria.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "CATEGORIA_INVALIDA",
                    "Categoría desconocida: " + categoria);
        }
    }

    /** Sólo el nombre base, sin separadores de ruta ni caracteres de control. */
    private String nombreSeguro(String original) {
        String base = original == null ? "" : original.replace('\\', '/');
        int corte = base.lastIndexOf('/');
        if (corte >= 0) {
            base = base.substring(corte + 1);
        }
        base = base.replaceAll("\\p{Cntrl}", "").replaceAll("[^A-Za-z0-9 ._-]", "_").trim();
        if (base.startsWith(".")) {
            base = "archivo" + base;
        }
        if (base.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "ARCHIVO_NOMBRE_INVALIDO",
                    "El archivo no tiene un nombre válido");
        }
        if (base.length() > 100) {
            String ext = extension(base);
            base = base.substring(0, 100 - ext.length() - 1) + "." + ext;
        }
        return base;
    }

    private String extension(String nombre) {
        int punto = nombre.lastIndexOf('.');
        if (punto < 0 || punto == nombre.length() - 1) {
            return "";
        }
        return nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
    }

    private String mime(String contenido, String extension) {
        if (contenido != null && !contenido.isBlank()) {
            String normalizado = contenido.split(";")[0].trim().toLowerCase(Locale.ROOT);
            if (MIME_ACEPTADOS.contains(normalizado)) {
                return normalizado;
            }
        }
        return MIME_POR_EXTENSION.get(extension);
    }
}
