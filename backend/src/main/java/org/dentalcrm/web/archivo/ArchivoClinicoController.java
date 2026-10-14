package org.dentalcrm.web.archivo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.dentalcrm.web.archivo.dto.ArchivoClinicoResponse;
import org.dentalcrm.web.archivo.dto.ContenidoArchivo;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

/**
 * Archivos clínicos — Fase E (plan §4.15): subida con clasificación,
 * listado, descarga del contenido y borrado, con metadata en PostgreSQL
 * y bytes en el almacenamiento del servidor.
 */
@RestController
@RequestMapping("/api/v1/historia-clinica")
@Tag(name = "Archivos clínicos", description = "Radiografías, fotografías, resultados y "
        + "documentos del paciente (metadata en BD, contenido en FileStorageProvider)")
public class ArchivoClinicoController {

    private final ArchivoClinicoService servicio;

    public ArchivoClinicoController(ArchivoClinicoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/pacientes/{pacienteId}/archivos")
    @PreAuthorize("hasAuthority('PERMISO_ARCHIVOS_CLINICOS_READ')")
    @Operation(summary = "Listado de archivos clínicos del paciente")
    public ResponseEntity<List<ArchivoClinicoResponse>> listar(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(servicio.listar(pacienteId));
    }

    @PostMapping(value = "/pacientes/{pacienteId}/archivos",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERMISO_ARCHIVOS_CLINICOS_WRITE')")
    @Operation(summary = "Subir un archivo clínico (multipart: archivo, categoria, fecha?, observaciones?)")
    public ResponseEntity<ArchivoClinicoResponse> subir(
            @PathVariable Long pacienteId,
            @RequestParam("categoria") String categoria,
            @RequestParam(value = "fecha", required = false) LocalDate fecha,
            @RequestParam(value = "observaciones", required = false) String observaciones,
            @RequestPart("archivo") MultipartFile archivo) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicio.subir(pacienteId, categoria, fecha, observaciones, archivo));
    }

    @GetMapping("/archivos/{id}/contenido")
    @PreAuthorize("hasAuthority('PERMISO_ARCHIVOS_CLINICOS_READ')")
    @Operation(summary = "Descargar el contenido binario del archivo")
    public ResponseEntity<byte[]> contenido(@PathVariable Long id) {
        ContenidoArchivo archivo = servicio.contenido(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + ascii(archivo.nombre()) + "\"")
                .contentType(MediaType.parseMediaType(archivo.mime()))
                .contentLength(archivo.contenido().length)
                .body(archivo.contenido());
    }

    @DeleteMapping("/archivos/{id}")
    @PreAuthorize("hasAuthority('PERMISO_ARCHIVOS_CLINICOS_WRITE')")
    @Operation(summary = "Eliminar el archivo (metadata y contenido)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private String ascii(String nombre) {
        return nombre.replaceAll("[^A-Za-z0-9 ._-]", "_");
    }
}
