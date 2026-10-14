package org.dentalcrm.web.historia;

import org.dentalcrm.domain.historia.*;
import org.dentalcrm.domain.odontologo.OdontologoRepository;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.service.CurrentUserService;
import org.dentalcrm.web.historia.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OdontogramaServiceTest {

    private OdontogramaService servicio;

    private OdontogramaRepository odontogramaRepo;
    private OdontogramaDienteRepository dienteRepo;
    private OdontogramaEventoRepository eventoRepo;
    private HistoriaClinicaRepository historiaRepo;
    private PacienteRepository pacienteRepo;
    private AuditService auditoria;
    private CurrentUserService usuarioActual;

    private Paciente paciente;
    private HistoriaClinica historia;

    @BeforeEach
    void setUp() {
        odontogramaRepo = mock(OdontogramaRepository.class);
        dienteRepo = mock(OdontogramaDienteRepository.class);
        eventoRepo = mock(OdontogramaEventoRepository.class);
        historiaRepo = mock(HistoriaClinicaRepository.class);
        pacienteRepo = mock(PacienteRepository.class);
        auditoria = mock(AuditService.class);
        usuarioActual = mock(CurrentUserService.class);
        when(usuarioActual.idUsuarioActual()).thenReturn(Optional.of(77L));

        paciente = new Paciente();
        paciente.setId(1L);
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
        when(odontogramaRepo.save(any())).thenAnswer(inv -> {
            Odontograma o = inv.getArgument(0);
            if (o.getId() == null) {
                o.setId(5L);
            }
            return o;
        });
        when(dienteRepo.save(any())).thenAnswer(inv -> {
            OdontogramaDiente d = inv.getArgument(0);
            if (d.getId() == null) {
                d.setId(100L);
            }
            return d;
        });
        when(eventoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        servicio = new OdontogramaService(odontogramaRepo, dienteRepo, eventoRepo,
                historiaRepo, pacienteRepo, mock(OdontologoRepository.class),
                auditoria, usuarioActual);
    }

    private Odontograma version(int version) {
        Odontograma o = new Odontograma();
        o.setId((long) (100 + version));
        o.setHistoria(historia);
        o.setVersion(version);
        o.setFecha(LocalDate.of(2026, 10, 1));
        o.setDenticion(Denticion.PERMANENTE);
        return o;
    }

    private OdontogramaDiente condicion(Odontograma o, int diente, SuperficieDental superficie,
                                        String condicion, EstadoOdontograma estado) {
        OdontogramaDiente d = new OdontogramaDiente();
        d.setId((long) diente);
        d.setOdontograma(o);
        d.setDiente(diente);
        d.setSuperficie(superficie);
        d.setCondicion(condicion);
        d.setEstado(estado);
        return d;
    }

    // ----------------------------- Consultas --------------------------

    @Test
    void vigente_devuelve_null_cuando_no_hay_odontograma() {
        when(odontogramaRepo.findFirstByHistoriaIdOrderByVersionDesc(10L)).thenReturn(Optional.empty());

        assertNull(servicio.vigente(1L));
    }

    @Test
    void vigente_devuelve_la_ultima_version_con_sus_dientes() {
        Odontograma o = version(2);
        when(odontogramaRepo.findFirstByHistoriaIdOrderByVersionDesc(10L)).thenReturn(Optional.of(o));
        when(dienteRepo.findByOdontogramaIdOrderByDienteAsc(o.getId()))
                .thenReturn(List.of(condicion(o, 16, SuperficieDental.OCLUSAL, "CARIES", EstadoOdontograma.DIAGNOSTICO)));

        OdontogramaResponse respuesta = servicio.vigente(1L);

        assertNotNull(respuesta);
        assertEquals(2, respuesta.version());
        assertEquals(1, respuesta.dientes().size());
        assertEquals("CARIES", respuesta.dientes().get(0).condicion());
        verify(auditoria, never()).registrar(any(), any(), any(), any(), any(), any());
    }

    @Test
    void historial_lista_las_versiones_con_su_conteo() {
        Odontograma dos = version(2);
        Odontograma uno = version(1);
        when(odontogramaRepo.findByHistoriaIdOrderByVersionDesc(10L)).thenReturn(List.of(dos, uno));
        when(dienteRepo.countByOdontogramaId(dos.getId())).thenReturn(3L);
        when(dienteRepo.countByOdontogramaId(uno.getId())).thenReturn(2L);

        List<OdontogramaResumenResponse> historial = servicio.historial(1L);

        assertEquals(2, historial.size());
        assertEquals(2, historial.get(0).version());
        assertEquals(3, historial.get(0).registros());
        assertEquals(1, historial.get(1).version());
    }

    @Test
    void version_inexistente_responde_404() {
        when(odontogramaRepo.findByHistoriaIdAndVersion(10L, 9)).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class, () -> servicio.version(1L, 9));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
        assertEquals("NO_ENCONTRADO", e.getCode());
    }

    @Test
    void comparar_detecta_nuevos_modificados_y_eliminados() {
        Odontograma a = version(1);
        Odontograma b = version(2);
        when(odontogramaRepo.findByHistoriaIdAndVersion(10L, 1)).thenReturn(Optional.of(a));
        when(odontogramaRepo.findByHistoriaIdAndVersion(10L, 2)).thenReturn(Optional.of(b));

        OdontogramaDiente conservado = condicion(a, 21, SuperficieDental.OCLUSAL, "CARIES", EstadoOdontograma.DIAGNOSTICO);
        OdontogramaDiente cambiado = condicion(a, 22, SuperficieDental.MESIAL, "ENDODONCIA", EstadoOdontograma.PLANIFICADO);
        OdontogramaDiente borrado = condicion(a, 23, null, "AUSENTE", EstadoOdontograma.EXISTENTE);
        OdontogramaDiente otroCambiado = condicion(b, 21, SuperficieDental.OCLUSAL, "CARIES", EstadoOdontograma.DIAGNOSTICO);
        OdontogramaDiente otroModificado = condicion(b, 22, SuperficieDental.MESIAL, "ENDODONCIA", EstadoOdontograma.COMPLETADO);
        OdontogramaDiente nuevo = condicion(b, 24, SuperficieDental.INCISAL, "FRACTURA", EstadoOdontograma.DIAGNOSTICO);

        when(dienteRepo.findByOdontogramaIdOrderByDienteAsc(a.getId())).thenReturn(List.of(conservado, cambiado, borrado));
        when(dienteRepo.findByOdontogramaIdOrderByDienteAsc(b.getId())).thenReturn(List.of(otroCambiado, otroModificado, nuevo));

        ComparacionResponse r = servicio.comparar(1L, 1, 2);

        assertEquals(1, r.desde().version());
        assertEquals(2, r.hasta().version());
        assertEquals(3, r.cambios().size());
        assertTrue(r.cambios().stream().anyMatch(c -> c.tipo().equals("NUEVO") && c.diente() == 24));
        assertTrue(r.cambios().stream().anyMatch(c -> c.tipo().equals("MODIFICADO") && c.diente() == 22));
        assertTrue(r.cambios().stream().anyMatch(c -> c.tipo().equals("ELIMINADO") && c.diente() == 23));
        assertTrue(r.cambios().stream().noneMatch(c -> c.diente() == 21));
    }

    // ----------------------------- Versiones --------------------------

    @Test
    void crear_primera_version_en_blanco_y_registra_evento() {
        when(odontogramaRepo.findByHistoriaIdOrderByVersionDesc(10L)).thenReturn(List.of());
        when(dienteRepo.findByOdontogramaIdOrderByDienteAsc(any())).thenReturn(List.of());

        OdontogramaResponse respuesta = servicio.crearVersion(1L,
                new NuevaVersionRequest(null, Denticion.PERMANENTE, "Primera toma", null));

        assertEquals(1, respuesta.version());
        assertTrue(respuesta.dientes().isEmpty());
        assertEquals(Denticion.PERMANENTE.name(), respuesta.denticion());
        verify(eventoRepo).save(any());
        verify(auditoria).registrar(eq("CREAR_ODONTOGRAMA"), any(), any(), any(), any(), any());
    }

    @Test
    void crear_version_siguiente_copia_los_dientes_de_la_anterior() {
        Odontograma anterior = version(1);
        when(odontogramaRepo.findByHistoriaIdOrderByVersionDesc(10L)).thenReturn(List.of(anterior));
        when(dienteRepo.findByOdontogramaIdOrderByDienteAsc(anterior.getId()))
                .thenReturn(List.of(condicion(anterior, 36, SuperficieDental.OCLUSAL, "RESTAURACION", EstadoOdontograma.COMPLETADO)));
        when(dienteRepo.findByOdontogramaIdOrderByDienteAsc(5L)).thenReturn(List.of());

        OdontogramaResponse respuesta = servicio.crearVersion(1L,
                new NuevaVersionRequest(LocalDate.of(2026, 10, 2), Denticion.MIXTA, null, true));

        assertEquals(2, respuesta.version());
        assertEquals(Denticion.MIXTA.name(), respuesta.denticion());
        verify(dienteRepo, times(1)).save(any());
        verify(eventoRepo).save(argThat(e -> e.getAccion() == AccionOdontograma.CREAR_VERSION));
    }

    @Test
    void crear_version_en_blanco_no_copia() {
        Odontograma anterior = version(1);
        when(odontogramaRepo.findByHistoriaIdOrderByVersionDesc(10L)).thenReturn(List.of(anterior));
        when(dienteRepo.findByOdontogramaIdOrderByDienteAsc(anterior.getId()))
                .thenReturn(List.of(condicion(anterior, 36, null, "AUSENTE", EstadoOdontograma.EXISTENTE)));

        servicio.crearVersion(1L, new NuevaVersionRequest(null, null, null, false));

        verify(dienteRepo, never()).save(any());
    }

    // ------------------------------ Dientes ---------------------------

    @Test
    void guardar_diente_nuevo_crea_registro_y_evento_agregar() {
        Odontograma o = version(1);
        when(odontogramaRepo.findById(101L)).thenReturn(Optional.of(o));
        when(dienteRepo.findByOdontogramaIdOrderByDienteAsc(101L)).thenReturn(List.of());

        DienteResponse r = servicio.guardarDiente(101L, new DienteRequest(16,
                SuperficieDental.OCLUSAL, "CARIES", null, "honda"));

        assertEquals(16, r.diente());
        assertEquals(EstadoOdontograma.EXISTENTE, r.estado());
        verify(dienteRepo).save(any());
        verify(eventoRepo).save(argThat(e -> e.getAccion() == AccionOdontograma.AGREGAR));
        verify(auditoria).registrar(eq("AGREGAR_CONDICION"), any(), any(), any(), any(), any());
    }

    @Test
    void guardar_diente_existente_actualiza_el_estado_sin_duplicar() {
        Odontograma o = version(1);
        OdontogramaDiente existente = condicion(o, 16, SuperficieDental.OCLUSAL, "CARIES",
                EstadoOdontograma.DIAGNOSTICO);
        when(odontogramaRepo.findById(101L)).thenReturn(Optional.of(o));
        when(dienteRepo.findByOdontogramaIdOrderByDienteAsc(101L)).thenReturn(List.of(existente));

        DienteResponse r = servicio.guardarDiente(101L, new DienteRequest(16,
                SuperficieDental.OCLUSAL, "caries", EstadoOdontograma.PROCESO, null));

        assertEquals(EstadoOdontograma.PROCESO, r.estado());
        verify(dienteRepo).save(existente);
        verify(eventoRepo).save(argThat(e -> e.getAccion() == AccionOdontograma.MODIFICAR
                && "DIAGNOSTICO".equals(e.getEstadoAnterior())
                && "PROCESO".equals(e.getEstadoNuevo())));
    }

    @Test
    void guardar_diente_rechaza_numeracion_fdi_invalida() {
        Odontograma o = version(1);
        when(odontogramaRepo.findById(101L)).thenReturn(Optional.of(o));

        BusinessException e = assertThrows(BusinessException.class, () ->
                servicio.guardarDiente(101L, new DienteRequest(19, null, "CARIES", null, null)));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        assertEquals("DIENTE_INVALIDO", e.getCode());
        verify(dienteRepo, never()).save(any());
    }

    @Test
    void guardar_diente_rechaza_piezas_temporales_fuera_de_rango() {
        Odontograma o = version(1);
        when(odontogramaRepo.findById(101L)).thenReturn(Optional.of(o));

        assertThrows(BusinessException.class, () ->
                servicio.guardarDiente(101L, new DienteRequest(86, null, "CARIES", null, null)));
    }

    @Test
    void eliminar_diente_quita_la_condicion_y_registra_evento() {
        Odontograma o = version(1);
        OdontogramaDiente existente = condicion(o, 41, SuperficieDental.LINGUAL, "FRACTURA",
                EstadoOdontograma.REFERIDO);
        existente.setId(777L);
        when(odontogramaRepo.findById(101L)).thenReturn(Optional.of(o));
        when(dienteRepo.findById(777L)).thenReturn(Optional.of(existente));

        servicio.eliminarDiente(101L, 777L);

        verify(dienteRepo).delete(existente);
        verify(eventoRepo).save(argThat(e -> e.getAccion() == AccionOdontograma.ELIMINAR));
        verify(auditoria).registrar(eq("ELIMINAR_CONDICION"), any(), any(), any(), any(), any());
    }

    @Test
    void eliminar_diente_de_otro_odontograma_responde_404() {
        Odontograma propio = version(1);
        Odontograma ajeno = version(2);
        ajeno.setId(200L);
        OdontogramaDiente diente = condicion(ajeno, 41, null, "AUSENTE", EstadoOdontograma.EXISTENTE);
        diente.setId(777L);
        when(odontogramaRepo.findById(101L)).thenReturn(Optional.of(propio));
        when(dienteRepo.findById(777L)).thenReturn(Optional.of(diente));

        BusinessException e = assertThrows(BusinessException.class, () -> servicio.eliminarDiente(101L, 777L));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
        verify(dienteRepo, never()).delete(any());
    }

    // ------------------------------ Eventos ---------------------------

    @Test
    void eventos_devuelve_el_log_de_la_version() {
        Odontograma o = version(1);
        when(odontogramaRepo.findById(101L)).thenReturn(Optional.of(o));
        OdontogramaEvento evento = new OdontogramaEvento();
        evento.setId(3L);
        evento.setOdontograma(o);
        evento.setAccion(AccionOdontograma.AGREGAR);
        evento.setDiente(16);
        evento.setSuperficie(SuperficieDental.OCLUSAL);
        evento.setCondicion("CARIES");
        evento.setEstadoNuevo(EstadoOdontograma.DIAGNOSTICO.name());
        evento.setFecha(java.time.Instant.now());
        when(eventoRepo.findTop100ByOdontogramaIdOrderByFechaDesc(101L)).thenReturn(List.of(evento));

        List<EventoResponse> eventos = servicio.eventos(101L);

        assertEquals(1, eventos.size());
        assertEquals("AGREGAR", eventos.get(0).accion());
        assertEquals(16, eventos.get(0).diente());
    }
}
