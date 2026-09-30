package org.dentalcrm.web.configuracion;

import org.dentalcrm.domain.configuracion.Consultorio;
import org.dentalcrm.domain.configuracion.ConsultorioRepository;
import org.dentalcrm.multitenant.TenantContext;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.service.PrivateFileCipher;
import org.dentalcrm.web.configuracion.dto.ConsultorioRequest;
import org.dentalcrm.web.configuracion.dto.ConsultorioResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class ConsultorioService {

    private static final String MODULO = "CONFIGURACION";
    private static final long MAX_FIRMA_BYTES = 5L * 1024 * 1024;

    private final ConsultorioRepository repository;
    private final AuditService auditService;
    private final PrivateFileCipher privateFileCipher;
    private final String nombreInicial;
    private final String direccionInicial;
    private final String telefonoInicial;

    public ConsultorioService(ConsultorioRepository repository,
                              AuditService auditService,
                              PrivateFileCipher privateFileCipher,
                              @Value("${app.clinica.nombre:Clínica Dental}") String nombreInicial,
                              @Value("${app.clinica.direccion:}") String direccionInicial,
                              @Value("${app.clinica.telefono:}") String telefonoInicial) {
        this.repository = repository;
        this.auditService = auditService;
        this.privateFileCipher = privateFileCipher;
        this.nombreInicial = nombreInicial;
        this.direccionInicial = direccionInicial;
        this.telefonoInicial = telefonoInicial;
    }

    @Transactional
    public ConsultorioResponse obtener() {
        return ConsultorioResponse.from(obtenerOInicializar());
    }

    @Transactional
    public ConsultorioResponse actualizar(ConsultorioRequest request) {
        Consultorio consultorio = obtenerOInicializar();
        consultorio.setNombre(request.nombre().trim());
        consultorio.setRazonSocial(limpiar(request.razonSocial()));
        consultorio.setRuc(limpiar(request.ruc()));
        consultorio.setCorreoElectronico(limpiar(request.correoElectronico()));
        consultorio.setDireccion(limpiar(request.direccion()));
        consultorio.setTelefono(limpiar(request.telefono()));
        consultorio.setEnlaceUbicacion(limpiar(request.enlaceUbicacion()));
        consultorio.setHorarioAtencion(limpiar(request.horarioAtencion()));
        repository.save(consultorio);
        auditService.registrar("ACTUALIZAR_CONSULTORIO", MODULO, "CONSULTORIO", consultorio.getId());
        return ConsultorioResponse.from(consultorio);
    }

    @Transactional
    public ConsultorioResponse cargarFirmaDigital(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new org.dentalcrm.exception.BusinessException(HttpStatus.BAD_REQUEST,
                    "FIRMA_VACIA", "Selecciona un archivo .p12");
        }
        if (archivo.getSize() > MAX_FIRMA_BYTES) {
            throw new org.dentalcrm.exception.BusinessException(HttpStatus.BAD_REQUEST,
                    "FIRMA_DEMASIADO_GRANDE", "El archivo .p12 no puede superar 5 MB");
        }

        String nombre = nombreArchivo(archivo.getOriginalFilename());
        if (!nombre.toLowerCase(java.util.Locale.ROOT).endsWith(".p12")) {
            throw new org.dentalcrm.exception.BusinessException(HttpStatus.BAD_REQUEST,
                    "FORMATO_FIRMA_INVALIDO", "La firma digital debe estar en formato .p12");
        }
        if (nombre.length() > 255) {
            throw new org.dentalcrm.exception.BusinessException(HttpStatus.BAD_REQUEST,
                    "NOMBRE_FIRMA_INVALIDO", "El nombre del archivo no puede superar 255 caracteres");
        }

        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (IOException e) {
            throw new org.dentalcrm.exception.BusinessException(HttpStatus.BAD_REQUEST,
                    "ARCHIVO_INVALIDO", "No se pudo leer el archivo .p12");
        }
        if (contenido.length < 16 || (contenido[0] & 0xff) != 0x30) {
            throw new org.dentalcrm.exception.BusinessException(HttpStatus.BAD_REQUEST,
                    "ARCHIVO_INVALIDO", "El archivo no tiene una estructura PKCS#12 reconocible");
        }

        Consultorio consultorio = obtenerOInicializar();
        consultorio.setFirmaDigital(privateFileCipher.encrypt(contenido));
        consultorio.setFirmaDigitalNombre(nombre);
        repository.save(consultorio);
        auditService.registrar("CARGAR_FIRMA_DIGITAL", MODULO, "CONSULTORIO", consultorio.getId());
        return ConsultorioResponse.from(consultorio);
    }

    @Transactional
    public ConsultorioResponse eliminarFirmaDigital() {
        Consultorio consultorio = obtenerOInicializar();
        consultorio.setFirmaDigital(null);
        consultorio.setFirmaDigitalNombre(null);
        repository.save(consultorio);
        auditService.registrar("ELIMINAR_FIRMA_DIGITAL", MODULO, "CONSULTORIO", consultorio.getId());
        return ConsultorioResponse.from(consultorio);
    }

    private Consultorio obtenerOInicializar() {
        return repository.findFirstByOrderByIdAsc().orElseGet(() -> {
            Consultorio consultorio = new Consultorio();
            consultorio.setTenantId(TenantContext.actualOrDefault());
            consultorio.setNombre(nombreInicial);
            consultorio.setDireccion(direccionInicial);
            consultorio.setTelefono(telefonoInicial);
            return repository.save(consultorio);
        });
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private String nombreArchivo(String nombreOriginal) {
        if (nombreOriginal == null || nombreOriginal.isBlank()) {
            return "firma.p12";
        }
        String normalizado = nombreOriginal.replace('\\', '/');
        return normalizado.substring(normalizado.lastIndexOf('/') + 1);
    }
}