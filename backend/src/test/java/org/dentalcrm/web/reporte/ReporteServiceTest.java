package org.dentalcrm.web.reporte;

import org.dentalcrm.domain.automatizacion.EstadoNotificacion;
import org.dentalcrm.domain.automatizacion.NotificacionRepository;
import org.dentalcrm.domain.cita.CitaRepository;
import org.dentalcrm.domain.cita.EstadoCita;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.domain.whatsapp.ConversacionRepository;
import org.dentalcrm.domain.whatsapp.EstadoConversacion;
import org.dentalcrm.domain.whatsapp.EstadoMensaje;
import org.dentalcrm.domain.whatsapp.MensajeRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.web.reporte.dto.DashboardResponse;
import org.dentalcrm.web.reporte.dto.EstadisticasResponse;
import org.dentalcrm.web.reporte.dto.ReporteDetalle;
import org.dentalcrm.web.reporte.dto.ReporteSerie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock private CitaRepository citaRepository;
    @Mock private PacienteRepository pacienteRepository;
    @Mock private ConversacionRepository conversacionRepository;
    @Mock private MensajeRepository mensajeRepository;
    @Mock private NotificacionRepository notificacionRepository;

    private ReporteService servicio;
    private final ZoneId zona = ZoneId.of("America/Guayaquil");

    @BeforeEach
    void setUp() {
        servicio = new ReporteService(citaRepository, pacienteRepository, conversacionRepository,
                mensajeRepository, notificacionRepository, "America/Guayaquil");
    }

    @Test
    void dashboardDevuelveTodosLosConteosDelDia() {
        LocalDate hoy = LocalDate.now(zona);
        when(citaRepository.countByFecha(hoy)).thenReturn(5L);
        when(citaRepository.countByFechaAndEstado(hoy, EstadoCita.PENDIENTE)).thenReturn(2L);
        when(citaRepository.countByFechaAndEstado(hoy, EstadoCita.CONFIRMADA)).thenReturn(1L);
        when(citaRepository.countByFechaAndEstado(hoy, EstadoCita.CANCELADA)).thenReturn(1L);
        when(citaRepository.countByFechaAndEstado(hoy, EstadoCita.NO_ASISTIO)).thenReturn(1L);
        when(pacienteRepository.countNuevosDesde(any(Instant.class))).thenReturn(4L);
        when(conversacionRepository.countByEstadoIn(eq(List.of(EstadoConversacion.BOT,
                EstadoConversacion.ATENCION_HUMANA)))).thenReturn(3L);
        when(mensajeRepository.countByEstado(EstadoMensaje.ENVIADO)).thenReturn(10L);
        when(mensajeRepository.countByEstado(EstadoMensaje.ERROR)).thenReturn(2L);
        when(notificacionRepository.countByEstado(EstadoNotificacion.ENVIADA)).thenReturn(8L);
        when(notificacionRepository.countByEstado(EstadoNotificacion.ERROR)).thenReturn(0L);

        DashboardResponse r = servicio.dashboard();

        assertEquals(5, r.citasDeHoy());
        assertEquals(2, r.citasPendientesHoy());
        assertEquals(1, r.citasConfirmadasHoy());
        assertEquals(1, r.citasCanceladasHoy());
        assertEquals(1, r.noAsistieronHoy());
        assertEquals(4, r.pacientesNuevosMes());
        assertEquals(3, r.conversacionesAbiertas());
        assertEquals(10, r.mensajesEnviados());
        assertEquals(2, r.mensajesConError());
        assertEquals(8, r.notificacionesEnviadas());
        assertEquals(0, r.notificacionesConError());
    }

    @Test
    void dashboardCuentaPacientesNuevosDesdeInicioDeMes() {
        Instant inicioMes = LocalDate.now(zona).withDayOfMonth(1).atStartOfDay(zona).toInstant();
        when(citaRepository.countByFecha(any(LocalDate.class))).thenReturn(0L);
        when(citaRepository.countByFechaAndEstado(any(LocalDate.class), any(EstadoCita.class))).thenReturn(0L);
        when(pacienteRepository.countNuevosDesde(inicioMes)).thenReturn(7L);
        when(conversacionRepository.countByEstadoIn(any(List.class))).thenReturn(0L);
        when(mensajeRepository.countByEstado(any(EstadoMensaje.class))).thenReturn(0L);
        when(notificacionRepository.countByEstado(any(EstadoNotificacion.class))).thenReturn(0L);

        servicio.dashboard();

        verify(pacienteRepository).countNuevosDesde(inicioMes);
    }

    @Test
    void estadisticasUsaRangoPorDefectoDe30Dias() {
        LocalDate hoy = LocalDate.now(zona);
        when(citaRepository.citasPorDia(hoy.minusDays(29), hoy)).thenReturn(java.util.List.<Object[]>of());
        when(citaRepository.confirmacionesPorDia(hoy.minusDays(29), hoy)).thenReturn(java.util.List.<Object[]>of());
        when(citaRepository.cancelacionesPorDia(hoy.minusDays(29), hoy)).thenReturn(java.util.List.<Object[]>of());
        when(citaRepository.noAsistioPorDia(hoy.minusDays(29), hoy)).thenReturn(java.util.List.<Object[]>of());
        when(citaRepository.citasPorOdontologo(hoy.minusDays(29), hoy, 1L)).thenReturn(java.util.List.<Object[]>of());
        when(citaRepository.serviciosMasSolicitados(hoy.minusDays(29), hoy, 10, 1L)).thenReturn(java.util.List.<Object[]>of());
        when(citaRepository.citasPorEstado(hoy.minusDays(29), hoy, 1L)).thenReturn(java.util.List.<Object[]>of());
        when(citaRepository.confirmacionesPorFuente(hoy.minusDays(29), hoy, 1L)).thenReturn(java.util.List.<Object[]>of());

        EstadisticasResponse r = servicio.estadisticas(null, null, 0);

        assertTrue(r.citasPorDia().isEmpty());
        assertTrue(r.citasPorOdontologo().isEmpty());
    }

    @Test
    void estadisticasMapeaSeriesYDetalles() {
        LocalDate d1 = LocalDate.of(2026, 9, 10);
        LocalDate d2 = LocalDate.of(2026, 9, 11);
        List<Object[]> citasPorDia = java.util.List.<Object[]>of(new Object[]{d1, 3L}, new Object[]{d2, 1L});
        List<Object[]> confirmaciones = java.util.List.<Object[]>of(new Object[]{d1, 1L});
        List<Object[]> cancelaciones = java.util.List.<Object[]>of(new Object[]{d2, 1L});
        List<Object[]> porOdontologo = java.util.List.<Object[]>of(new Object[]{1L, "Carlos Ruiz", 2L});
        List<Object[]> porServicio = java.util.List.<Object[]>of(new Object[]{11L, "Limpieza", 4L});
        List<Object[]> porEstado = java.util.List.<Object[]>of(new Object[]{"CONFIRMADA", 1L}, new Object[]{"PENDIENTE", 3L});
        List<Object[]> porFuente = java.util.List.<Object[]>of(new Object[]{"WHATSAPP", 1L});
        when(citaRepository.citasPorDia(d1, d2)).thenReturn(citasPorDia);
        when(citaRepository.confirmacionesPorDia(d1, d2)).thenReturn(confirmaciones);
        when(citaRepository.cancelacionesPorDia(d1, d2)).thenReturn(cancelaciones);
        when(citaRepository.noAsistioPorDia(d1, d2)).thenReturn(java.util.List.<Object[]>of());
        when(citaRepository.citasPorOdontologo(d1, d2, 1L)).thenReturn(porOdontologo);
        when(citaRepository.serviciosMasSolicitados(d1, d2, 5, 1L)).thenReturn(porServicio);
        when(citaRepository.citasPorEstado(d1, d2, 1L)).thenReturn(porEstado);
        when(citaRepository.confirmacionesPorFuente(d1, d2, 1L)).thenReturn(porFuente);

        EstadisticasResponse r = servicio.estadisticas(d1, d2, 5);

        List<ReporteSerie> series = r.citasPorDia();
        assertEquals(2, series.size());
        assertEquals(d1, series.get(0).fecha());
        assertEquals(3, series.get(0).total());
        assertEquals(new ReporteSerie(d1, 1L), r.confirmaciones().get(0));
        assertEquals(new ReporteSerie(d2, 1L), r.cancelaciones().get(0));
        assertEquals(new ReporteDetalle("Carlos Ruiz", 2L), r.citasPorOdontologo().get(0));
        assertEquals(new ReporteDetalle("Limpieza", 4L), r.serviciosMasSolicitados().get(0));
        assertEquals(new ReporteDetalle("CONFIRMADA", 1L), r.porEstado().get(0));
        assertEquals(new ReporteDetalle("WHATSAPP", 1L), r.confirmacionesPorFuente().get(0));
        verify(citaRepository).serviciosMasSolicitados(d1, d2, 5, 1L);
    }

    @Test
    void estadisticasRechazaRangoInvalido() {
        LocalDate desde = LocalDate.of(2026, 9, 20);
        LocalDate hasta = LocalDate.of(2026, 9, 10);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> servicio.estadisticas(desde, hasta, 10));

        assertEquals("RANGO_INVALIDO", ex.getCode());
        verify(citaRepository, never()).citasPorDia(any(LocalDate.class), any(LocalDate.class));
        verify(citaRepository, never()).serviciosMasSolicitados(any(LocalDate.class), any(LocalDate.class), anyInt(), anyLong());
    }
}