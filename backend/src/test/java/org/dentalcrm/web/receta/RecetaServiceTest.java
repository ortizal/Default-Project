package org.dentalcrm.web.receta;

import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.odontologo.OdontologoRepository;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.domain.receta.EstadoReceta;
import org.dentalcrm.domain.receta.Receta;
import org.dentalcrm.domain.receta.RecetaLinea;
import org.dentalcrm.domain.receta.RecetaRepository;
import org.dentalcrm.domain.receta.ViaAdministracion;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.service.CurrentUserService;
import org.dentalcrm.web.receta.dto.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RecetaServiceTest {

    private RecetaService servicio;

    private RecetaRepository recetaRepo;
    private PacienteRepository pacienteRepo;
    private OdontologoRepository odontologoRepo;
    private AuditService auditoria;

    private Paciente paciente;
    private Odontologo odontologo;

    @BeforeEach
    void setUp() {
        recetaRepo = mock(RecetaRepository.class);
        pacienteRepo = mock(PacienteRepository.class);
        odontologoRepo = mock(OdontologoRepository.class);
        auditoria = mock(AuditService.class);

        paciente = new Paciente();
        paciente.setId(1L);
        paciente.setNombres("Ana");
        paciente.setApellidos("Pérez");

        odontologo = new Odontologo();
        odontologo.setId(7L);
        odontologo.setNombres("Luis");
        odontologo.setApellidos("Ortiz");

        when(pacienteRepo.findById(1L)).thenReturn(Optional.of(paciente));
        when(odontologoRepo.findById(7L)).thenReturn(Optional.of(odontologo));
        when(recetaRepo.save(any())).thenAnswer(inv -> {
            Receta r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(500L);
            }
            return r;
        });

        CurrentUserService usuarioActual = mock(CurrentUserService.class);
        when(usuarioActual.idUsuarioActual()).thenReturn(Optional.of(99L));
        servicio = new RecetaService(recetaRepo, pacienteRepo, odontologoRepo, auditoria,
                usuarioActual, new RecetaPdfService(null));
        // El reemplazo de líneas fuerza un flush para respetar la unicidad (receta, número).
        ReflectionTestUtils.setField(servicio, "entityManager", mock(EntityManager.class));
    }

    private RecetaLineaRequest linea(String medicamento, String dosis, ViaAdministracion via) {
        return new RecetaLineaRequest(medicamento, "500 mg", "Comprimido", dosis,
                via, "Cada 8 h", "7 días", "Tomar con alimentos");
    }

    private RecetaRequest request(RecetaLineaRequest... lineas) {
        return new RecetaRequest(LocalDate.of(2026, 10, 5), 7L, "Gingivitis crónica",
                "Control a los 7 días", List.of(lineas));
    }

    private Receta recetaGuardada() {
        Receta r = new Receta();
        r.setId(10L);
        r.setPaciente(paciente);
        r.setOdontologo(odontologo);
        r.setFecha(LocalDate.of(2026, 10, 5));
        r.setDiagnostico("Gingivitis crónica");
        r.setEstado(EstadoReceta.VIGENTE);
        RecetaLinea l = new RecetaLinea();
        l.setId(1L);
        l.setReceta(r);
        l.setNumero(1);
        l.setMedicamento("Clorhexidina");
        l.setDosis("15 ml");
        l.setVia(ViaAdministracion.TOPICA);
        r.getLineas().add(l);
        return r;
    }

    // ------------------------------- Alta --------------------------------

    @Test
    void crearGuardaLineasRenumeradasYAudita() {
        RecetaResponse resp = servicio.crear(1L, request(
                linea("Amoxicilina", "500 mg", ViaAdministracion.ORAL),
                linea("Ibuprofeno", "400 mg", ViaAdministracion.ORAL)));

        assertEquals(500L, resp.id());
        assertEquals("Ana Pérez", resp.paciente());
        assertEquals("Luis Ortiz", resp.odontologo());
        assertEquals("VIGENTE", resp.estado());
        assertEquals(2, resp.lineas().size());
        assertEquals(1, resp.lineas().get(0).numero());
        assertEquals(2, resp.lineas().get(1).numero());
        assertEquals("ORAL", resp.lineas().get(0).via());
        verify(auditoria).registrar(eq("CREAR_RECETA"), eq("HISTORIA_CLINICA"), eq("RECETA"),
                eq(500L), eq(null), any());
    }

    @Test
    void crearSinLineasRechaza() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> servicio.crear(1L, new RecetaRequest(LocalDate.now(), null, null, null,
                        List.of())));
        assertEquals("RECETA_SIN_LINEAS", e.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        verify(recetaRepo, never()).save(any());
    }

    @Test
    void crearLineaSinDosisRechaza() {
        RecetaRequest request = request(new RecetaLineaRequest("Amoxicilina", null, null, null,
                null, null, null, null));
        BusinessException e = assertThrows(BusinessException.class, () -> servicio.crear(1L, request));
        assertEquals("LINEA_INVALIDA", e.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
    }

    @Test
    void crearPacienteInexistenteRechaza() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> servicio.crear(99L, request(linea("Amoxicilina", "500 mg", ViaAdministracion.ORAL))));
        assertEquals("NO_ENCONTRADO", e.getCode());
        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }

    @Test
    void crearOdontologoInexistenteRechaza() {
        RecetaRequest request = new RecetaRequest(LocalDate.now(), 99L, null, null,
                List.of(linea("Amoxicilina", "500 mg", ViaAdministracion.ORAL)));
        BusinessException e = assertThrows(BusinessException.class, () -> servicio.crear(1L, request));
        assertEquals("ODONTOLOGO_NO_ENCONTRADO", e.getCode());
        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }

    // ----------------------------- Consultas -----------------------------

    @Test
    void historialDevuelveResumenesOrdenados() {
        when(recetaRepo.findByPacienteIdOrderByFechaDescIdDesc(1L))
                .thenReturn(List.of(recetaGuardada()));

        List<RecetaResumenResponse> historial = servicio.historial(1L);

        assertEquals(1, historial.size());
        assertEquals("VIGENTE", historial.get(0).estado());
        assertEquals("Gingivitis crónica", historial.get(0).diagnostico());
        assertEquals(1, historial.get(0).lineas());
        assertEquals("Luis Ortiz", historial.get(0).odontologo());
    }

    @Test
    void obtenerDevuelveDetalleConLineas() {
        when(recetaRepo.findById(10L)).thenReturn(Optional.of(recetaGuardada()));

        RecetaResponse resp = servicio.obtener(10L);

        assertEquals(10L, resp.id());
        assertEquals(1, resp.lineas().size());
        assertEquals("Clorhexidina", resp.lineas().get(0).medicamento());
        assertEquals("TOPICA", resp.lineas().get(0).via());
        assertEquals("15 ml", resp.lineas().get(0).dosis());
    }

    @Test
    void obtenerInexistenteRechaza() {
        when(recetaRepo.findById(99L)).thenReturn(Optional.empty());
        BusinessException e = assertThrows(BusinessException.class, () -> servicio.obtener(99L));
        assertEquals("NO_ENCONTRADO", e.getCode());
        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }

    // ------------------------------ Edición ------------------------------

    @Test
    void editarReemplazaLineasYAudita() {
        Receta receta = recetaGuardada();
        when(recetaRepo.findById(10L)).thenReturn(Optional.of(receta));

        RecetaResponse resp = servicio.editar(10L, request(linea("Metronidazol", "250 mg",
                ViaAdministracion.ORAL)));

        assertEquals(1, resp.lineas().size());
        assertEquals("Metronidazol", resp.lineas().get(0).medicamento());
        assertEquals(1, resp.lineas().get(0).numero());
        verify(auditoria).registrar(eq("MODIFICAR_RECETA"), eq("HISTORIA_CLINICA"), eq("RECETA"),
                eq(10L), any(), any());
    }

    @Test
    void editarRecetaAnuladaRechaza() {
        Receta receta = recetaGuardada();
        receta.setEstado(EstadoReceta.ANULADA);
        when(recetaRepo.findById(10L)).thenReturn(Optional.of(receta));

        BusinessException e = assertThrows(BusinessException.class,
                () -> servicio.editar(10L, request(linea("X", "1 g", ViaAdministracion.ORAL))));
        assertEquals("RECETA_ANULADA", e.getCode());
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
    }

    @Test
    void editarSinLineasRechaza() {
        when(recetaRepo.findById(10L)).thenReturn(Optional.of(recetaGuardada()));
        BusinessException e = assertThrows(BusinessException.class, () -> servicio.editar(10L,
                new RecetaRequest(LocalDate.now(), null, null, null, null)));
        assertEquals("RECETA_SIN_LINEAS", e.getCode());
    }

    // ----------------------------- Anulación -----------------------------

    @Test
    void anularCambiaEstadoYAudita() {
        Receta receta = recetaGuardada();
        when(recetaRepo.findById(10L)).thenReturn(Optional.of(receta));

        RecetaResponse resp = servicio.cambiarEstado(10L,
                new CambioEstadoRecetaRequest(EstadoReceta.ANULADA, "Alergia al principio activo"));

        assertEquals("ANULADA", resp.estado());
        assertEquals("Alergia al principio activo", resp.motivoAnulacion());
        verify(auditoria).registrar(eq("ANULAR_RECETA"), eq("HISTORIA_CLINICA"), eq("RECETA"),
                eq(10L), eq("VIGENTE"), any());
    }

    @Test
    void anularSinMotivoRechaza() {
        when(recetaRepo.findById(10L)).thenReturn(Optional.of(recetaGuardada()));
        BusinessException e = assertThrows(BusinessException.class,
                () -> servicio.cambiarEstado(10L, new CambioEstadoRecetaRequest(EstadoReceta.ANULADA, "  ")));
        assertEquals("MOTIVO_REQUERIDO", e.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
    }

    @Test
    void anularYaAnuladaRechaza() {
        Receta receta = recetaGuardada();
        receta.setEstado(EstadoReceta.ANULADA);
        when(recetaRepo.findById(10L)).thenReturn(Optional.of(receta));

        BusinessException e = assertThrows(BusinessException.class,
                () -> servicio.cambiarEstado(10L, new CambioEstadoRecetaRequest(EstadoReceta.ANULADA, "x")));
        assertEquals("RECETA_YA_ANULADA", e.getCode());
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
    }

    @Test
    void otroEstadoRechaza() {
        when(recetaRepo.findById(10L)).thenReturn(Optional.of(recetaGuardada()));
        BusinessException e = assertThrows(BusinessException.class,
                () -> servicio.cambiarEstado(10L, new CambioEstadoRecetaRequest(EstadoReceta.VIGENTE, "x")));
        assertEquals("ESTADO_INVALIDO", e.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
    }

    // -------------------------------- PDF --------------------------------

    @Test
    void pdfGeneraContenidoYNombre() {
        when(recetaRepo.findById(10L)).thenReturn(Optional.of(recetaGuardada()));

        byte[] contenido = servicio.pdf(10L);

        assertNotNull(contenido);
        assertTrue(contenido.length > 100);
        assertEquals("receta-10.pdf", servicio.nombreArchivoPdf(10L));
        verify(auditoria, never()).registrar(anyString(), anyString(), anyString(), any(), any(), any());
    }
}
