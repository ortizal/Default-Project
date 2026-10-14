package org.dentalcrm.web.archivo;

import org.dentalcrm.domain.archivo.ArchivoClinico;
import org.dentalcrm.domain.archivo.ArchivoClinicoRepository;
import org.dentalcrm.domain.archivo.CategoriaArchivo;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.service.CurrentUserService;
import org.dentalcrm.storage.FileStorageProvider;
import org.dentalcrm.web.archivo.dto.ArchivoClinicoResponse;
import org.dentalcrm.web.archivo.dto.ContenidoArchivo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.Mockito.*;

class ArchivoClinicoServiceTest {

    private ArchivoClinicoService servicio;

    private ArchivoClinicoRepository archivoRepo;
    private PacienteRepository pacienteRepo;
    private FileStorageProvider storage;
    private AuditService auditoria;

    private Paciente paciente;

    @BeforeEach
    void setUp() {
        archivoRepo = mock(ArchivoClinicoRepository.class);
        pacienteRepo = mock(PacienteRepository.class);
        storage = mock(FileStorageProvider.class);
        auditoria = mock(AuditService.class);

        paciente = new Paciente();
        paciente.setId(1L);
        paciente.setNombres("Ana");
        paciente.setApellidos("Pérez");

        when(pacienteRepo.findById(1L)).thenReturn(Optional.of(paciente));
        when(storage.guardar(anyString(), anyString(), any(byte[].class)))
                .thenReturn("tenant-1/paciente-1/archivos/abc-rx.png");
        when(archivoRepo.save(any())).thenAnswer(inv -> {
            ArchivoClinico a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId(77L);
            }
            return a;
        });

        CurrentUserService usuarioActual = mock(CurrentUserService.class);
        when(usuarioActual.idUsuarioActual()).thenReturn(Optional.of(99L));

        servicio = new ArchivoClinicoService(archivoRepo, pacienteRepo, storage, auditoria, usuarioActual);
    }

    private MockMultipartFile archivo(String nombre, String contentType, byte[] contenido) {
        return new MockMultipartFile("archivo", nombre, contentType, contenido);
    }

    private MockMultipartFile png() {
        return archivo("panoramica.png", "image/png", "imagen-de-prueba".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void subirGuardaMetadataEnLaBdYElContenidoEnElAlmacenamiento() {
        ArchivoClinicoResponse r = servicio.subir(1L, "PANORAMICA", LocalDate.of(2026, 10, 1),
                "Control anual", png());

        assertNotNull(r.id());
        assertEquals(1L, r.pacienteId());
        assertEquals("PANORAMICA", r.categoria());
        assertEquals("panoramica.png", r.nombre());
        assertEquals("image/png", r.mime());
        assertEquals(16L, r.tamano());
        assertEquals(64, r.sha256().length());
        assertEquals(LocalDate.of(2026, 10, 1), r.fecha());
        assertEquals("Control anual", r.observaciones());

        verify(storage).guardar(contains("paciente-1/archivos"), endsWith("panoramica.png"),
                any(byte[].class));
        verify(auditoria).registrar("SUBIR_ARCHIVO", "HISTORIA_CLINICA", "ARCHIVO_CLINICO", 77L);
        verify(archivoRepo).save(any());
    }

    @Test
    void subirConCategoriaInvalidaRechaza() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> servicio.subir(1L, "MEME", null, null, png()));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        assertEquals("CATEGORIA_INVALIDA", e.getCode());
        verify(storage, never()).guardar(anyString(), anyString(), any(byte[].class));
    }

    @Test
    void subirSinCategoriaRechaza() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> servicio.subir(1L, "  ", null, null, png()));
        assertEquals("CATEGORIA_REQUERIDA", e.getCode());
    }

    @Test
    void subirSinArchivoOConArchivoVacioRechaza() {
        BusinessException sinArchivo = assertThrows(BusinessException.class,
                () -> servicio.subir(1L, "RADIOGRAFIA", null, null, null));
        assertEquals("ARCHIVO_VACIO", sinArchivo.getCode());

        MockMultipartFile vacio = archivo("rx.png", "image/png", new byte[0]);
        BusinessException vacio2 = assertThrows(BusinessException.class,
                () -> servicio.subir(1L, "RADIOGRAFIA", null, null, vacio));
        assertEquals("ARCHIVO_VACIO", vacio2.getCode());
    }

    @Test
    void subirArchivoMayorA20MbRechaza() {
        org.springframework.web.multipart.MultipartFile grande =
                mock(org.springframework.web.multipart.MultipartFile.class);
        when(grande.getOriginalFilename()).thenReturn("rx.png");
        when(grande.getSize()).thenReturn(21L * 1024 * 1024);

        BusinessException e = assertThrows(BusinessException.class,
                () -> servicio.subir(1L, "RADIOGRAFIA", null, null, grande));
        assertEquals("ARCHIVO_DEMASIADO_GRANDE", e.getCode());
        verify(storage, never()).guardar(anyString(), anyString(), any(byte[].class));
    }

    @Test
    void subirFormatoNoPermitidoRechaza() {
        MockMultipartFile exe = archivo("malware.exe", "application/x-msdownload", "MZ".getBytes(StandardCharsets.UTF_8));
        BusinessException e = assertThrows(BusinessException.class,
                () -> servicio.subir(1L, "DOCUMENTO", null, null, exe));
        assertEquals("ARCHIVO_FORMATO_INVALIDO", e.getCode());
        verify(storage, never()).guardar(anyString(), anyString(), any(byte[].class));
    }

    @Test
    void subirUsaElNombreBaseYNoRutasDelNavegador() {
        MockMultipartFile relativo = archivo("../../etc/panoramica.png", "image/png",
                "contenido".getBytes(StandardCharsets.UTF_8));

        ArchivoClinicoResponse r = servicio.subir(1L, "PANORAMICA", null, null, relativo);

        assertEquals("panoramica.png", r.nombre());
        verify(storage).guardar(anyString(), argThat(n -> n.endsWith("panoramica.png") && !n.contains("/")),
                any(byte[].class));
    }

    @Test
    void mimeNoConfiableSeDeduDeLaExtension() {
        MockMultipartFile sinTipo = archivo("rx.jpg", null, "contenido".getBytes(StandardCharsets.UTF_8));
        assertEquals("image/jpeg", servicio.subir(1L, "RADIOGRAFIA", null, null, sinTipo).mime());

        MockMultipartFile texto = archivo("estudio.pdf", "text/plain", "contenido".getBytes(StandardCharsets.UTF_8));
        assertEquals("application/pdf", servicio.subir(1L, "RESULTADO", null, null, texto).mime());
    }

    @Test
    void pacienteInexistenteResponde404() {
        when(pacienteRepo.findById(9L)).thenReturn(Optional.empty());
        BusinessException e = assertThrows(BusinessException.class,
                () -> servicio.listar(9L));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
        assertEquals("NO_ENCONTRADO", e.getCode());
    }

    @Test
    void listarDevuelveLosArchivosDelPaciente() {
        ArchivoClinico a = new ArchivoClinico();
        a.setId(5L);
        a.setPaciente(paciente);
        a.setCategoria(CategoriaArchivo.DOCUMENTO);
        a.setNombre("resultado.pdf");
        a.setClave("k");
        a.setMime("application/pdf");
        a.setTamano(1200L);
        when(archivoRepo.findByPacienteIdOrderByCreatedAtDescIdDesc(1L)).thenReturn(List.of(a));

        List<ArchivoClinicoResponse> lista = servicio.listar(1L);

        assertEquals(1, lista.size());
        assertEquals("resultado.pdf", lista.get(0).nombre());
        assertEquals("DOCUMENTO", lista.get(0).categoria());
    }

    @Test
    void contenidoLeeElAlmacenamientoYDevuelveNombreYMime() {
        ArchivoClinico a = new ArchivoClinico();
        a.setId(6L);
        a.setPaciente(paciente);
        a.setCategoria(CategoriaArchivo.FOTOGRAFIA);
        a.setNombre("frente.png");
        a.setClave("tenant-1/paciente-1/archivos/x.png");
        a.setMime("image/png");
        a.setTamano(9L);
        when(archivoRepo.findById(6L)).thenReturn(Optional.of(a));
        when(storage.leer(a.getClave())).thenReturn(Optional.of("bytes".getBytes(StandardCharsets.UTF_8)));

        ContenidoArchivo c = servicio.contenido(6L);

        assertEquals("frente.png", c.nombre());
        assertEquals("image/png", c.mime());
        assertEquals("bytes", new String(c.contenido(), StandardCharsets.UTF_8));
    }

    @Test
    void contenidoFaltanteEnDiscoResponde404() {
        ArchivoClinico a = new ArchivoClinico();
        a.setId(7L);
        a.setPaciente(paciente);
        a.setCategoria(CategoriaArchivo.FOTOGRAFIA);
        a.setNombre("borrada.png");
        a.setClave("k");
        when(archivoRepo.findById(7L)).thenReturn(Optional.of(a));
        when(storage.leer("k")).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class, () -> servicio.contenido(7L));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
        assertEquals("ARCHIVO_FISICO_NO_ENCONTRADO", e.getCode());
    }

    @Test
    void eliminarBorraElContenidoLaMetadataYAudita() {
        ArchivoClinico a = new ArchivoClinico();
        a.setId(8L);
        a.setPaciente(paciente);
        a.setCategoria(CategoriaArchivo.RADIOGRAFIA);
        a.setNombre("rx.png");
        a.setClave("tenant-1/paciente-1/archivos/x.png");
        when(archivoRepo.findById(8L)).thenReturn(Optional.of(a));

        servicio.eliminar(8L);

        verify(storage).eliminar("tenant-1/paciente-1/archivos/x.png");
        verify(archivoRepo).delete(a);
        verify(auditoria).registrar("ELIMINAR_ARCHIVO", "HISTORIA_CLINICA", "ARCHIVO_CLINICO", 8L);
    }

    @Test
    void archivoInexistenteResponde404() {
        when(archivoRepo.findById(404L)).thenReturn(Optional.empty());
        BusinessException e = assertThrows(BusinessException.class, () -> servicio.eliminar(404L));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
        assertEquals("NO_ENCONTRADO", e.getCode());
        verify(storage, never()).eliminar(anyString());
    }
}
