package org.dentalcrm.web.historia;

import org.dentalcrm.domain.historia.*;
import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.odontologo.OdontologoRepository;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.web.historia.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PeriodontogramaServiceTest {

    private PeriodontogramaService servicio;

    private ExamenPeriodontalRepository examenRepo;
    private PeriodontogramaSitioRepository sitioRepo;
    private PeriodontogramaDienteRepository dienteRepo;
    private HistoriaClinicaRepository historiaRepo;
    private PacienteRepository pacienteRepo;
    private AuditService auditoria;

    private Paciente paciente;
    private HistoriaClinica historia;

    @BeforeEach
    void setUp() {
        examenRepo = mock(ExamenPeriodontalRepository.class);
        sitioRepo = mock(PeriodontogramaSitioRepository.class);
        dienteRepo = mock(PeriodontogramaDienteRepository.class);
        historiaRepo = mock(HistoriaClinicaRepository.class);
        pacienteRepo = mock(PacienteRepository.class);
        auditoria = mock(AuditService.class);

        paciente = new Paciente();
        paciente.setId(1L);
        paciente.setNombres("Ana");
        paciente.setApellidos("Pérez");
        historia = new HistoriaClinica();
        historia.setId(10L);
        historia.setPaciente(paciente);
        historia.setEstado(EstadoHistoria.ABIERTA);

        when(pacienteRepo.findById(1L)).thenReturn(Optional.of(paciente));
        when(historiaRepo.findByPacienteId(1L)).thenReturn(Optional.of(historia));
        when(historiaRepo.save(any())).thenAnswer(inv -> {
            HistoriaClinica h = inv.getArgument(0);
            h.setId(11L);
            return h;
        });
        when(examenRepo.save(any())).thenAnswer(inv -> {
            ExamenPeriodontal e = inv.getArgument(0);
            if (e.getId() == null) {
                e.setId(900L);
            }
            return e;
        });
        when(sitioRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(dienteRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        servicio = new PeriodontogramaService(examenRepo, sitioRepo, dienteRepo,
                historiaRepo, pacienteRepo, mock(OdontologoRepository.class),
                auditoria, new PeriodontogramaPdfService(null));
    }

    private ExamenPeriodontal examen(int version) {
        ExamenPeriodontal e = new ExamenPeriodontal();
        e.setId((long) (900 + version));
        e.setHistoria(historia);
        e.setVersion(version);
        e.setFecha(LocalDate.of(2026, 10, 1));
        return e;
    }

    private PeriodontogramaSitio sitio(ExamenPeriodontal e, int diente, SitioPeriodontal s,
                                       String sondaje, boolean sangrado) {
        PeriodontogramaSitio fila = new PeriodontogramaSitio();
        fila.setId((long) diente);
        fila.setExamen(e);
        fila.setDiente(diente);
        fila.setSitio(s);
        if (sondaje != null) {
            fila.setSondajeMm(new BigDecimal(sondaje));
            fila.setMargenMm(new BigDecimal(sondaje).subtract(BigDecimal.ONE));
            fila.setInsercionMm(new BigDecimal(sondaje));
        }
        fila.setSangrado(sangrado);
        return fila;
    }

    private PeriodontogramaDiente pieza(ExamenPeriodontal e, int diente, Integer movilidad,
                                        Integer furcacion) {
        PeriodontogramaDiente d = new PeriodontogramaDiente();
        d.setId((long) diente);
        d.setExamen(e);
        d.setDiente(diente);
        d.setMovilidad(movilidad);
        d.setFurcacion(furcacion);
        return d;
    }

    // ----------------------------- Consultas --------------------------

    @Test
    void vigente_devuelve_null_cuando_no_hay_examen() {
        when(examenRepo.findFirstByHistoriaIdOrderByVersionDesc(10L)).thenReturn(Optional.empty());

        assertNull(servicio.vigente(1L));
    }

    @Test
    void vigente_devuelve_la_ultima_version_con_sus_mediciones() {
        ExamenPeriodontal e = examen(3);
        when(examenRepo.findFirstByHistoriaIdOrderByVersionDesc(10L)).thenReturn(Optional.of(e));
        when(sitioRepo.findByExamenIdOrderByDienteAsc(903L))
                .thenReturn(List.of(sitio(e, 16, SitioPeriodontal.MESIAL, "4", true)));
        when(dienteRepo.findByExamenIdOrderByDienteAsc(903L))
                .thenReturn(List.of(pieza(e, 16, 1, 0)));

        ExamenPeriodontalResponse r = servicio.vigente(1L);

        assertNotNull(r);
        assertEquals(3, r.version());
        assertEquals(1, r.sitios().size());
        assertEquals(1, r.dientes().size());
        assertEquals("MESIAL", r.sitios().get(0).sitio());
        assertEquals(0, r.sitios().get(0).sondajeMm().compareTo(new BigDecimal("4")));
        verify(auditoria, never()).registrar(any(), any(), any(), any(), any(), any());
    }

    @Test
    void historial_lista_las_versiones_con_su_conteo() {
        ExamenPeriodontal dos = examen(2);
        ExamenPeriodontal uno = examen(1);
        when(examenRepo.findByHistoriaIdOrderByVersionDesc(10L)).thenReturn(List.of(dos, uno));
        when(sitioRepo.countByExamenId(902L)).thenReturn(8L);
        when(dienteRepo.countByExamenId(902L)).thenReturn(2L);
        when(sitioRepo.countByExamenId(901L)).thenReturn(4L);
        when(dienteRepo.countByExamenId(901L)).thenReturn(1L);

        List<ExamenPeriodontalResumenResponse> historial = servicio.historial(1L);

        assertEquals(2, historial.size());
        assertEquals(2, historial.get(0).version());
        assertEquals(8, historial.get(0).sitios());
        assertEquals(2, historial.get(0).dientes());
        assertEquals(4, historial.get(1).sitios());
    }

    @Test
    void historial_devuelve_vacio_si_el_paciente_no_tiene_historia() {
        when(historiaRepo.findByPacienteId(1L)).thenReturn(Optional.empty());

        assertTrue(servicio.historial(1L).isEmpty());
        assertNull(servicio.vigente(1L));
    }

    @Test
    void examen_inexistente_responde_404() {
        when(examenRepo.findById(999L)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class, () -> servicio.obtener(999L));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
        assertEquals("NO_ENCONTRADO", e.getCode());
    }

    @Test
    void comparar_detecta_nuevos_modificados_y_eliminados() {
        ExamenPeriodontal a = examen(1);
        ExamenPeriodontal b = examen(2);
        when(examenRepo.findByHistoriaIdAndVersion(10L, 1)).thenReturn(Optional.of(a));
        when(examenRepo.findByHistoriaIdAndVersion(10L, 2)).thenReturn(Optional.of(b));

        PeriodontogramaSitio conservadoA = sitio(a, 21, SitioPeriodontal.VESTIBULAR, "3", false);
        PeriodontogramaSitio cambiadoA = sitio(a, 22, SitioPeriodontal.MESIAL, "4", false);
        PeriodontogramaSitio borradoA = sitio(a, 23, SitioPeriodontal.DISTAL, "6", true);
        PeriodontogramaSitio conservadoB = sitio(b, 21, SitioPeriodontal.VESTIBULAR, "3", false);
        PeriodontogramaSitio modificadoB = sitio(b, 22, SitioPeriodontal.MESIAL, "5", true);
        PeriodontogramaSitio nuevoB = sitio(b, 24, SitioPeriodontal.LINGUAL, "3", false);

        when(sitioRepo.findByExamenIdOrderByDienteAsc(a.getId()))
                .thenReturn(List.of(conservadoA, cambiadoA, borradoA));
        when(sitioRepo.findByExamenIdOrderByDienteAsc(b.getId()))
                .thenReturn(List.of(conservadoB, modificadoB, nuevoB));

        ComparacionPeriodontalResponse r = servicio.comparar(1L, 1, 2);

        assertEquals(1, r.desde().version());
        assertEquals(2, r.hasta().version());
        assertEquals(1, r.agregados());
        assertEquals(1, r.eliminados());
        assertTrue(r.cambios().stream().anyMatch(c -> c.tipo().equals("NUEVO") && c.diente() == 24));
        assertTrue(r.cambios().stream().anyMatch(c -> c.tipo().equals("MODIFICADO")
                && c.diente() == 22 && "sondajeMm".equals(c.campo())
                && "4".equals(c.anterior()) && "5".equals(c.nuevo())));
        assertTrue(r.cambios().stream().anyMatch(c -> c.tipo().equals("MODIFICADO")
                && c.diente() == 22 && "sangrado".equals(c.campo())));
        assertTrue(r.cambios().stream().anyMatch(c -> c.tipo().equals("ELIMINADO") && c.diente() == 23));
        assertTrue(r.cambios().stream().noneMatch(c -> c.diente() == 21));
    }

    @Test
    void comparar_con_version_inexistente_responde_404() {
        when(examenRepo.findByHistoriaIdAndVersion(10L, 1)).thenReturn(Optional.of(examen(1)));
        when(examenRepo.findByHistoriaIdAndVersion(10L, 9)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class,
                () -> servicio.comparar(1L, 1, 9));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }

    // ----------------------------- Versiones --------------------------

    @Test
    void crear_primera_version_en_blanco() {
        when(examenRepo.findFirstByHistoriaIdOrderByVersionDesc(10L)).thenReturn(Optional.empty());
        when(examenRepo.findByHistoriaIdOrderByVersionDesc(10L)).thenReturn(List.of());

        ExamenPeriodontalResponse r = servicio.crearVersion(1L,
                new NuevaVersionPeriodontalRequest(null, "Primera toma", null));

        assertEquals(1, r.version());
        assertNotNull(r.fecha());
        assertTrue(r.sitios().isEmpty());
        assertTrue(r.dientes().isEmpty());
        verify(auditoria).registrar(eq("CREAR_VERSION_PERIODONTAL"), any(), any(), any(), any(), any());
    }

    @Test
    void crear_version_siguiente_copia_las_mediciones_de_la_anterior() {
        ExamenPeriodontal anterior = examen(1);
        when(examenRepo.findFirstByHistoriaIdOrderByVersionDesc(10L)).thenReturn(Optional.of(anterior));
        when(examenRepo.findByHistoriaIdOrderByVersionDesc(10L)).thenReturn(List.of(anterior));
        when(sitioRepo.findByExamenIdOrderByDienteAsc(anterior.getId()))
                .thenReturn(List.of(sitio(anterior, 36, SitioPeriodontal.MESIAL, "5", true)));
        when(dienteRepo.findByExamenIdOrderByDienteAsc(anterior.getId()))
                .thenReturn(List.of(pieza(anterior, 36, 2, 1)));

        ExamenPeriodontalResponse r = servicio.crearVersion(1L,
                new NuevaVersionPeriodontalRequest(LocalDate.of(2026, 10, 2), null, true));

        assertEquals(2, r.version());
        assertEquals(LocalDate.of(2026, 10, 2), r.fecha());
        verify(sitioRepo).save(argThat(s -> s.getDiente() == 36 && s.getSitio() == SitioPeriodontal.MESIAL));
        verify(dienteRepo).save(argThat(d -> d.getDiente() == 36 && Integer.valueOf(2).equals(d.getMovilidad())));
    }

    @Test
    void crear_version_en_blanco_no_copia() {
        ExamenPeriodontal anterior = examen(1);
        when(examenRepo.findFirstByHistoriaIdOrderByVersionDesc(10L)).thenReturn(Optional.of(anterior));
        when(examenRepo.findByHistoriaIdOrderByVersionDesc(10L)).thenReturn(List.of(anterior));
        when(sitioRepo.findByExamenIdOrderByDienteAsc(anterior.getId()))
                .thenReturn(List.of(sitio(anterior, 41, SitioPeriodontal.LINGUAL, "3", false)));

        servicio.crearVersion(1L, new NuevaVersionPeriodontalRequest(null, null, false));

        verify(sitioRepo, never()).save(any());
        verify(dienteRepo, never()).save(any());
    }

    // ---------------------------- Mediciones --------------------------

    @Test
    void guardar_mediciones_crea_sitios_y_pieza() {
        ExamenPeriodontal e = examen(1);
        when(examenRepo.findById(901L)).thenReturn(Optional.of(e));
        when(sitioRepo.findByExamenIdAndDienteAndSitio(901L, 16, SitioPeriodontal.MESIAL))
                .thenReturn(Optional.empty());
        when(dienteRepo.findByExamenIdAndDiente(901L, 16)).thenReturn(Optional.empty());

        ExamenPeriodontalResponse r = servicio.guardarMediciones(901L, new MedicionesPeriodontalRequest(
                List.of(new SitioPeriodontalRequest(16, SitioPeriodontal.MESIAL,
                        new BigDecimal("4"), new BigDecimal("2"), new BigDecimal("6"),
                        true, false, false)),
                List.of(new DientePeriodontalRequest(16, 1, 0, "honda"))));

        assertNotNull(r);
        assertEquals(901L, r.id());
        verify(sitioRepo).save(argThat(s -> s.isSangrado() && !s.isPlaca()
                && s.getDiente() == 16 && s.getSitio() == SitioPeriodontal.MESIAL
                && s.getSondajeMm().compareTo(new BigDecimal("4")) == 0));
        verify(dienteRepo).save(argThat(d -> d.getDiente() == 16
                && Integer.valueOf(1).equals(d.getMovilidad())
                && Integer.valueOf(0).equals(d.getFurcacion())
                && "honda".equals(d.getNotas())));
        verify(auditoria).registrar(eq("GUARDAR_MEDICIONES_PERIODONTALES"), any(), any(), any(), any(), any());
    }

    @Test
    void guardar_mediciones_actualiza_existente_sin_duplicar() {
        ExamenPeriodontal e = examen(1);
        PeriodontogramaSitio existente = sitio(e, 21, SitioPeriodontal.VESTIBULAR, "3", false);
        when(examenRepo.findById(901L)).thenReturn(Optional.of(e));
        when(sitioRepo.findByExamenIdAndDienteAndSitio(901L, 21, SitioPeriodontal.VESTIBULAR))
                .thenReturn(Optional.of(existente));

        servicio.guardarMediciones(901L, new MedicionesPeriodontalRequest(
                List.of(new SitioPeriodontalRequest(21, SitioPeriodontal.VESTIBULAR,
                        new BigDecimal("7"), null, null, null, true, null)), null));

        verify(sitioRepo).save(existente);
        assertEquals(0, existente.getSondajeMm().compareTo(new BigDecimal("7")));
        assertTrue(existente.isPlaca());
        assertFalse(existente.isSangrado());
    }

    @Test
    void guardar_mediciones_redondea_a_decima() {
        ExamenPeriodontal e = examen(1);
        when(examenRepo.findById(901L)).thenReturn(Optional.of(e));
        when(sitioRepo.findByExamenIdAndDienteAndSitio(901L, 46, SitioPeriodontal.DISTAL))
                .thenReturn(Optional.empty());

        servicio.guardarMediciones(901L, new MedicionesPeriodontalRequest(
                List.of(new SitioPeriodontalRequest(46, SitioPeriodontal.DISTAL,
                        new BigDecimal("3.44"), new BigDecimal("1.26"), null, null, null, null)), null));

        verify(sitioRepo).save(argThat(s ->
                s.getSondajeMm().compareTo(new BigDecimal("3.4")) == 0
                        && s.getMargenMm().compareTo(new BigDecimal("1.3")) == 0));
    }

    @Test
    void guardar_mediciones_rechaza_diente_invalido() {
        ExamenPeriodontal e = examen(1);
        when(examenRepo.findById(901L)).thenReturn(Optional.of(e));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                servicio.guardarMediciones(901L, new MedicionesPeriodontalRequest(
                        List.of(new SitioPeriodontalRequest(19, SitioPeriodontal.MESIAL,
                                BigDecimal.ONE, null, null, null, null, null)), null)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("DIENTE_INVALIDO", ex.getCode());
        verify(sitioRepo, never()).save(any());
    }

    @Test
    void guardar_mediciones_valida_movilidad_y_furacion_fuera_de_rango() {
        ExamenPeriodontal e = examen(1);
        when(examenRepo.findById(901L)).thenReturn(Optional.of(e));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                servicio.guardarMediciones(901L, new MedicionesPeriodontalRequest(null,
                        List.of(new DientePeriodontalRequest(46, 5, null, null)))));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void eliminar_pieza_quita_sus_mediciones() {
        ExamenPeriodontal e = examen(1);
        when(examenRepo.findById(901L)).thenReturn(Optional.of(e));
        when(sitioRepo.countByExamenId(901L)).thenReturn(4L);

        servicio.eliminarPieza(901L, 46);

        verify(sitioRepo).deleteByExamenIdAndDiente(901L, 46);
        verify(dienteRepo).deleteByExamenIdAndDiente(901L, 46);
        verify(auditoria).registrar(eq("ELIMINAR_MEDICION_PERIODONTAL"), any(), any(), any(), any(), any());
    }

    @Test
    void eliminar_pieza_rechaza_numeracion_fdi_invalida() {
        ExamenPeriodontal e = examen(1);
        when(examenRepo.findById(901L)).thenReturn(Optional.of(e));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> servicio.eliminarPieza(901L, 86));

        assertEquals("DIENTE_INVALIDO", ex.getCode());
        verify(sitioRepo, never()).deleteByExamenIdAndDiente(anyLong(), anyInt());
    }

    // ---------------------------- Cabecera ----------------------------

    @Test
    void editar_cabecera_actualiza_fecha_y_observaciones() {
        ExamenPeriodontal e = examen(1);
        when(examenRepo.findById(901L)).thenReturn(Optional.of(e));

        ExamenPeriodontalResponse r = servicio.editarCabecera(901L,
                new EditarExamenPeriodontalRequest(LocalDate.of(2026, 11, 5), "control"));

        assertEquals(LocalDate.of(2026, 11, 5), r.fecha());
        assertEquals("control", r.observaciones());
        verify(auditoria).registrar(eq("MODIFICAR_EXAMEN_PERIODONTAL"), any(), any(), any(), any(), any());
    }

    // -------------------------------- PDF -----------------------------

    @Test
    void pdf_genera_un_documento_conCabeceraYMediciones() {
        ExamenPeriodontal e = examen(1);
        Odontologo doctor = new Odontologo();
        doctor.setNombres("Luis");
        doctor.setApellidos("Mora");
        e.setOdontologo(doctor);
        when(examenRepo.findById(901L)).thenReturn(Optional.of(e));
        when(sitioRepo.findByExamenIdOrderByDienteAsc(901L))
                .thenReturn(List.of(sitio(e, 16, SitioPeriodontal.MESIAL, "4", true)));
        when(dienteRepo.findByExamenIdOrderByDienteAsc(901L))
                .thenReturn(List.of(pieza(e, 16, 2, 1)));

        byte[] contenido = servicio.pdf(901L);

        assertNotNull(contenido);
        assertTrue(contenido.length > 500);
        assertEquals("%PDF", new String(contenido, 0, 4));
        assertEquals("periodontograma-10-v1.pdf", servicio.nombreArchivoPdf(901L));
    }
}
