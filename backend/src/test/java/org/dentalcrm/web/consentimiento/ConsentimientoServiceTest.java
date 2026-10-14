package org.dentalcrm.web.consentimiento;

import org.dentalcrm.domain.consentimiento.*;
import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.odontologo.OdontologoRepository;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteTutor;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.service.CurrentUserService;
import org.dentalcrm.web.consentimiento.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ConsentimientoServiceTest {

    private ConsentimientoService servicio;

    private ConsentimientoRepository consentimientoRepo;
    private ConsentimientoPlantillaRepository plantillaRepo;
    private PacienteRepository pacienteRepo;
    private OdontologoRepository odontologoRepo;
    private AuditService auditoria;

    private Paciente paciente;
    private Odontologo odontologo;
    private ConsentimientoPlantilla plantilla;

    @BeforeEach
    void setUp() {
        consentimientoRepo = mock(ConsentimientoRepository.class);
        plantillaRepo = mock(ConsentimientoPlantillaRepository.class);
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

        plantilla = new ConsentimientoPlantilla();
        plantilla.setId(3L);
        plantilla.setCodigo("EXTRACCION");
        plantilla.setNombre("Consentimiento para extracción dental");
        plantilla.setCategoria("Cirugía");
        plantilla.setContenido("Riesgos: sangrado, infección y lesión nerviosa.");
        plantilla.setVersion(1);

        when(pacienteRepo.findById(1L)).thenReturn(Optional.of(paciente));
        when(odontologoRepo.findById(7L)).thenReturn(Optional.of(odontologo));
        when(plantillaRepo.findByCodigo("EXTRACCION")).thenReturn(Optional.of(plantilla));
        when(consentimientoRepo.save(any())).thenAnswer(inv -> {
            Consentimiento c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(500L);
            }
            return c;
        });
        when(plantillaRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CurrentUserService usuarioActual = mock(CurrentUserService.class);
        when(usuarioActual.idUsuarioActual()).thenReturn(Optional.of(99L));

        servicio = new ConsentimientoService(consentimientoRepo, plantillaRepo, pacienteRepo,
                odontologoRepo, auditoria, usuarioActual, new ConsentimientoPdfService(null));
    }

    private PacienteTutor tutor() {
        PacienteTutor t = new PacienteTutor();
        t.setId(5L);
        t.setPaciente(paciente);
        t.setParentesco("PADRE");
        t.setNombres("Alexis");
        t.setApellidos("Ortiz");
        t.setTelefono("+593987654321");
        return t;
    }

    private Paciente menorDeEdad() {
        paciente.setFechaNacimiento(LocalDate.now().minusYears(12));
        paciente.getTutores().add(tutor());
        return paciente;
    }

    private Consentimiento borrador() {
        Consentimiento c = new Consentimiento();
        c.setId(10L);
        c.setPaciente(paciente);
        c.setPlantillaCodigo("EXTRACCION");
        c.setPlantillaVersion(1);
        c.setTitulo("Consentimiento para extracción dental");
        c.setCategoria("Cirugía");
        c.setContenido("Riesgos: sangrado, infección y lesión nerviosa.");
        c.setFecha(LocalDate.of(2026, 10, 5));
        c.setOdontologo(odontologo);
        c.setEstado(EstadoConsentimiento.BORRADOR);
        return c;
    }

    private Consentimiento firmado() {
        Consentimiento c = borrador();
        c.setEstado(EstadoConsentimiento.FIRMADO);
        c.setFirmante("Ana Pérez");
        c.setFirmaHash("a".repeat(64));
        c.setFirmadoEn(LocalDateTime.of(2026, 10, 5, 12, 0).toInstant(ZoneOffset.UTC));
        return c;
    }

    // ---------------------------- Plantillas -----------------------------

    @Test
    void listaPlantillasOrdenadas() {
        when(plantillaRepo.findAllByOrderByNombreAsc()).thenReturn(List.of(plantilla));

        List<PlantillaConsentimientoResponse> r = servicio.plantillas();

        assertEquals(1, r.size());
        assertEquals("EXTRACCION", r.get(0).codigo());
        assertEquals(1, r.get(0).version());
        assertTrue(r.get(0).contenido().contains("sangrado"));
    }

    @Test
    void actualizarPlantillaSubeVersionYAudita() {
        ActualizarPlantillaConsentimientoRequest request =
                new ActualizarPlantillaConsentimientoRequest(null, "Texto revisado por la clínica.");

        PlantillaConsentimientoResponse r = servicio.actualizarPlantilla("EXTRACCION", request);

        assertEquals(2, r.version());
        assertEquals("Texto revisado por la clínica.", r.contenido());
        assertEquals("Consentimiento para extracción dental", r.nombre());
        verify(auditoria).registrar(eq("MODIFICAR_PLANTILLA_CONSENTIMIENTO"), eq("HISTORIA_CLINICA"),
                eq("PLANTILLA_CONSENTIMIENTO"), eq(3L), any(), any());
    }

    @Test
    void actualizarPlantillaInexistenteRechaza() {
        BusinessException e = assertThrows(BusinessException.class, () ->
                servicio.actualizarPlantilla("NO_EXISTE",
                        new ActualizarPlantillaConsentimientoRequest(null, "x")));
        assertEquals("PLANTILLA_NO_ENCONTRADA", e.getCode());
        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }

    // ------------------------------ Alta ---------------------------------

    @Test
    void crearCopiaElSnapshotDeLaPlantilla() {
        NuevoConsentimientoRequest request = new NuevoConsentimientoRequest("EXTRACCION",
                LocalDate.of(2026, 10, 5), "María Pérez (tutora)", null, 7L, "Cirugía programada");

        ConsentimientoResponse r = servicio.crear(1L, request);

        assertEquals(500L, r.id());
        assertEquals("BORRADOR", r.estado());
        assertEquals("EXTRACCION", r.plantillaCodigo());
        assertEquals(1, r.plantillaVersion());
        assertEquals("Consentimiento para extracción dental", r.titulo());
        assertEquals("Riesgos: sangrado, infección y lesión nerviosa.", r.contenido());
        assertEquals("María Pérez (tutora)", r.tutorNombre());
        assertEquals("Ana Pérez", r.paciente());
        assertEquals("Luis Ortiz", r.odontologo());
        assertNull(r.firmaHash());
        verify(auditoria).registrar(eq("CREAR_CONSENTIMIENTO"), eq("HISTORIA_CLINICA"),
                eq("CONSENTIMIENTO"), eq(500L), eq(null), any());
    }

    @Test
    void crearConPlantillaInexistenteRechaza() {
        NuevoConsentimientoRequest request = new NuevoConsentimientoRequest("NO_EXISTE",
                LocalDate.now(), null, null, null, null);
        BusinessException e = assertThrows(BusinessException.class, () -> servicio.crear(1L, request));
        assertEquals("PLANTILLA_NO_ENCONTRADA", e.getCode());
        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }

    @Test
    void crearConOdontologoInexistenteRechaza() {
        NuevoConsentimientoRequest request = new NuevoConsentimientoRequest("EXTRACCION",
                LocalDate.now(), null, null, 99L, null);
        BusinessException e = assertThrows(BusinessException.class, () -> servicio.crear(1L, request));
        assertEquals("ODONTOLOGO_NO_ENCONTRADO", e.getCode());
        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }

    // ---------------------------- Consultas ------------------------------

    @Test
    void historialDevuelveResumenes() {
        when(consentimientoRepo.findByPacienteIdOrderByFechaDescIdDesc(1L))
                .thenReturn(List.of(firmado()));

        List<ConsentimientoResumenResponse> r = servicio.historial(1L);

        assertEquals(1, r.size());
        assertEquals("FIRMADO", r.get(0).estado());
        assertEquals("Ana Pérez", r.get(0).firmante());
        assertEquals("Luis Ortiz", r.get(0).odontologo());
        assertEquals(64, r.get(0).firmaHash().length());
    }

    @Test
    void obtenerDevuelveElTextoCongelado() {
        when(consentimientoRepo.findById(10L)).thenReturn(Optional.of(borrador()));

        ConsentimientoResponse r = servicio.obtener(10L);

        assertEquals(10L, r.id());
        assertTrue(r.contenido().contains("infección"));
        assertEquals("BORRADOR", r.estado());
        assertNull(r.firmaImagen());
    }

    @Test
    void obtenerInexistenteRechaza() {
        when(consentimientoRepo.findById(99L)).thenReturn(Optional.empty());
        BusinessException e = assertThrows(BusinessException.class, () -> servicio.obtener(99L));
        assertEquals("NO_ENCONTRADO", e.getCode());
        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }

    // --------------------------- Edición ---------------------------------

    @Test
    void editarBorradorActualizaTextoYCabecera() {
        when(consentimientoRepo.findById(10L)).thenReturn(Optional.of(borrador()));

        ConsentimientoResponse r = servicio.editar(10L, new EditarConsentimientoRequest(
                LocalDate.of(2026, 10, 6), "Extracción de la 48", "Texto nuevo del consentimiento.",
                null, null, 7L, "Actualizado"));

        assertEquals("Extracción de la 48", r.titulo());
        assertEquals("Texto nuevo del consentimiento.", r.contenido());
        assertEquals(LocalDate.of(2026, 10, 6), r.fecha());
        assertEquals("Actualizado", r.observaciones());
        verify(auditoria).registrar(eq("MODIFICAR_CONSENTIMIENTO"), eq("HISTORIA_CLINICA"),
                eq("CONSENTIMIENTO"), eq(10L), any(), any());
    }

    @Test
    void editarConsentimientoFirmadoRechaza() {
        when(consentimientoRepo.findById(10L)).thenReturn(Optional.of(firmado()));
        BusinessException e = assertThrows(BusinessException.class, () ->
                servicio.editar(10L, new EditarConsentimientoRequest(null, null, "nuevo texto",
                        null, null, null, null)));
        assertEquals("CONSENTIMIENTO_FIRMADO", e.getCode());
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
    }

    @Test
    void eliminarBorradorBorraYAudita() {
        Consentimiento c = borrador();
        when(consentimientoRepo.findById(10L)).thenReturn(Optional.of(c));

        servicio.eliminar(10L);

        verify(consentimientoRepo).delete(c);
        verify(auditoria).registrar(eq("ELIMINAR_CONSENTIMIENTO"), eq("HISTORIA_CLINICA"),
                eq("CONSENTIMIENTO"), eq(10L), any(), eq(null));
    }

    @Test
    void eliminarConsentimientoFirmadoRechaza() {
        when(consentimientoRepo.findById(10L)).thenReturn(Optional.of(firmado()));
        BusinessException e = assertThrows(BusinessException.class, () -> servicio.eliminar(10L));
        assertEquals("CONSENTIMIENTO_FIRMADO", e.getCode());
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        verify(consentimientoRepo, never()).delete(any());
    }

    // ----------------------------- Firma ---------------------------------

    @Test
    void firmarCongelaElConsentimientoYCalculaLaHuella() {
        Consentimiento c = borrador();
        when(consentimientoRepo.findById(10L)).thenReturn(Optional.of(c));

        ConsentimientoResponse r = servicio.firmar(10L,
                new FirmarConsentimientoRequest("  Ana Pérez  ", "data:image/png;base64,iVBORw0KGgo="));

        assertEquals("FIRMADO", r.estado());
        assertEquals("Ana Pérez", r.firmante());
        assertNotNull(r.firmadoEn());
        assertEquals(64, r.firmaHash().length());
        assertTrue(r.firmaHash().matches("[0-9a-f]{64}"));
        assertEquals("data:image/png;base64,iVBORw0KGgo=", r.firmaImagen());
        verify(auditoria).registrar(eq("FIRMAR_CONSENTIMIENTO"), eq("HISTORIA_CLINICA"),
                eq("CONSENTIMIENTO"), eq(10L), any(), any());
    }

    @Test
    void laHuellaCambiaSiCambiaElFirmante() {
        when(consentimientoRepo.findById(10L)).thenReturn(Optional.of(borrador()));
        String primera = servicio.firmar(10L, new FirmarConsentimientoRequest("Ana Pérez", null))
                .firmaHash();

        Consentimiento otra = borrador();
        otra.setId(11L);
        when(consentimientoRepo.findById(11L)).thenReturn(Optional.of(otra));
        String segunda = servicio.firmar(11L, new FirmarConsentimientoRequest("Carlos Pérez", null))
                .firmaHash();

        assertNotEquals(primera, segunda);
    }

    @Test
    void firmarSinFirmanteRechaza() {
        when(consentimientoRepo.findById(10L)).thenReturn(Optional.of(borrador()));
        BusinessException e = assertThrows(BusinessException.class,
                () -> servicio.firmar(10L, new FirmarConsentimientoRequest("   ", null)));
        assertEquals("FIRMANTE_REQUERIDO", e.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
    }

    @Test
    void firmarDosVecesRechaza() {
        when(consentimientoRepo.findById(10L)).thenReturn(Optional.of(firmado()));
        BusinessException e = assertThrows(BusinessException.class,
                () -> servicio.firmar(10L, new FirmarConsentimientoRequest("Ana Pérez", null)));
        assertEquals("CONSENTIMIENTO_YA_FIRMADO", e.getCode());
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
    }

    // --------------------- Menores y tutores (§4.14) ------------------------

    @Test
    void crearConTutorDelPacienteCopiaElSnapshot() {
        menorDeEdad();
        NuevoConsentimientoRequest request = new NuevoConsentimientoRequest("EXTRACCION",
                LocalDate.of(2026, 10, 5), null, 5L, null, null);

        ConsentimientoResponse r = servicio.crear(1L, request);

        assertEquals(5L, r.tutorId());
        assertEquals("Alexis Ortiz", r.tutorNombre());
        assertEquals("PADRE", r.tutorParentesco());
        assertEquals("+593987654321", r.tutorTelefono());
    }

    @Test
    void crearConTutorQueNoEsDelPacienteRechaza() {
        menorDeEdad();
        NuevoConsentimientoRequest request = new NuevoConsentimientoRequest("EXTRACCION",
                LocalDate.now(), null, 777L, null, null);

        BusinessException e = assertThrows(BusinessException.class, () -> servicio.crear(1L, request));
        assertEquals("TUTOR_NO_ENCONTRADO", e.getCode());
        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }

    @Test
    void crearMenorSinTutorRechaza() {
        paciente.setFechaNacimiento(LocalDate.now().minusYears(12));
        NuevoConsentimientoRequest request = new NuevoConsentimientoRequest("EXTRACCION",
                LocalDate.now(), "  ", null, null, null);

        BusinessException e = assertThrows(BusinessException.class, () -> servicio.crear(1L, request));
        assertEquals("TUTOR_REQUERIDO", e.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
    }

    @Test
    void editarMenorSinTutorRechaza() {
        menorDeEdad();
        Consentimiento c = borrador();
        c.setTutorNombre(null);
        c.setTutorId(null);
        when(consentimientoRepo.findById(10L)).thenReturn(Optional.of(c));

        BusinessException e = assertThrows(BusinessException.class, () ->
                servicio.editar(10L, new EditarConsentimientoRequest(null, null, null,
                        null, null, null, null)));
        assertEquals("TUTOR_REQUERIDO", e.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
    }

    @Test
    void firmarMenorExigeQueFirmeElTutor() {
        menorDeEdad();
        Consentimiento c = borrador();
        c.setTutorId(5L);
        c.setTutorParentesco("PADRE");
        c.setTutorNombre("Alexis Ortiz");
        when(consentimientoRepo.findById(10L)).thenReturn(Optional.of(c));

        BusinessException e = assertThrows(BusinessException.class, () ->
                servicio.firmar(10L, new FirmarConsentimientoRequest("Juan Pérez", null)));
        assertEquals("FIRMANTE_DEBE_SER_TUTOR", e.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        assertEquals(EstadoConsentimiento.BORRADOR, c.getEstado());
    }

    @Test
    void firmarMenorConElTutorAceptaAunqueCambieElFormato() {
        menorDeEdad();
        Consentimiento c = borrador();
        c.setTutorId(5L);
        c.setTutorNombre("Alexis Ortiz");
        when(consentimientoRepo.findById(10L)).thenReturn(Optional.of(c));

        ConsentimientoResponse r = servicio.firmar(10L,
                new FirmarConsentimientoRequest("  alexis   ortiz ", null));

        assertEquals("FIRMADO", r.estado());
        assertEquals("Alexis Ortiz", r.firmante());
        assertNotNull(r.firmaHash());
    }

    @Test
    void unAdultoFirmaConSuPropioNombre() {
        paciente.setFechaNacimiento(LocalDate.now().minusYears(30));
        when(consentimientoRepo.findById(10L)).thenReturn(Optional.of(borrador()));

        ConsentimientoResponse r = servicio.firmar(10L,
                new FirmarConsentimientoRequest("Ana Pérez", null));

        assertEquals("FIRMADO", r.estado());
        assertEquals("Ana Pérez", r.firmante());
    }

    // ------------------------------- PDF ---------------------------------

    @Test
    void pdfGeneraContenidoYNombre() {
        when(consentimientoRepo.findById(10L)).thenReturn(Optional.of(firmado()));

        byte[] contenido = servicio.pdf(10L);

        assertNotNull(contenido);
        assertTrue(contenido.length > 100);
        assertEquals("consentimiento-10.pdf", servicio.nombreArchivoPdf(10L));
        verify(auditoria, never()).registrar(anyString(), anyString(), anyString(), any(), any(), any());
    }

    @Test
    void cualquierMetodoFallidoNoAudita() {
        when(consentimientoRepo.findById(10L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> servicio.pdf(10L));
        verifyNoInteractions(auditoria);
    }
}
