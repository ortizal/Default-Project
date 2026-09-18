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
import org.dentalcrm.multitenant.TenantContext;
import org.dentalcrm.web.reporte.dto.DashboardResponse;
import org.dentalcrm.web.reporte.dto.EstadisticasResponse;
import org.dentalcrm.web.reporte.dto.ReporteDetalle;
import org.dentalcrm.web.reporte.dto.ReporteSerie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class ReporteService {

    private final CitaRepository citaRepository;
    private final PacienteRepository pacienteRepository;
    private final ConversacionRepository conversacionRepository;
    private final MensajeRepository mensajeRepository;
    private final NotificacionRepository notificacionRepository;
    private final ZoneId zona;

    public ReporteService(CitaRepository citaRepository,
                          PacienteRepository pacienteRepository,
                          ConversacionRepository conversacionRepository,
                          MensajeRepository mensajeRepository,
                          NotificacionRepository notificacionRepository,
                          @Value("${app.timezone}") String zona) {
        this.citaRepository = citaRepository;
        this.pacienteRepository = pacienteRepository;
        this.conversacionRepository = conversacionRepository;
        this.mensajeRepository = mensajeRepository;
        this.notificacionRepository = notificacionRepository;
        this.zona = ZoneId.of(zona);
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        LocalDate hoy = LocalDate.now(zona);
        Instant inicioMes = LocalDate.now(zona).withDayOfMonth(1).atStartOfDay(zona).toInstant();
        return new DashboardResponse(
                citaRepository.countByFecha(hoy),
                citaRepository.countByFechaAndEstado(hoy, EstadoCita.PENDIENTE),
                citaRepository.countByFechaAndEstado(hoy, EstadoCita.CONFIRMADA),
                citaRepository.countByFechaAndEstado(hoy, EstadoCita.CANCELADA),
                citaRepository.countByFechaAndEstado(hoy, EstadoCita.NO_ASISTIO),
                pacienteRepository.countNuevosDesde(inicioMes),
                conversacionRepository.countByEstadoIn(List.of(EstadoConversacion.BOT,
                        EstadoConversacion.ATENCION_HUMANA)),
                mensajeRepository.countByEstado(EstadoMensaje.ENVIADO),
                mensajeRepository.countByEstado(EstadoMensaje.ERROR),
                notificacionRepository.countByEstado(EstadoNotificacion.ENVIADA),
                notificacionRepository.countByEstado(EstadoNotificacion.ERROR));
    }

    @Transactional(readOnly = true)
    public EstadisticasResponse estadisticas(LocalDate desde, LocalDate hasta, int limite) {
        LocalDate hoy = LocalDate.now(zona);
        if (desde == null) {
            desde = hoy.minusDays(29);
        }
        if (hasta == null) {
            hasta = hoy;
        }
        if (desde.isAfter(hasta)) {
            throw new BusinessException("RANGO_INVALIDO", "La fecha 'desde' no puede ser posterior a 'hasta'");
        }
        int top = limite <= 0 ? 10 : limite;

        return new EstadisticasResponse(
                citasPorDia(desde, hasta),
                confirmaciones(desde, hasta),
                cancelaciones(desde, hasta),
                noAsistencia(desde, hasta),
                citasPorOdontologo(desde, hasta),
                serviciosMasSolicitados(desde, hasta, top),
                porEstado(desde, hasta),
                confirmacionesPorFuente(desde, hasta));
    }

    private List<ReporteSerie> citasPorDia(LocalDate desde, LocalDate hasta) {
        return citaRepository.citasPorDia(desde, hasta).stream()
                .map(r -> new ReporteSerie(aFecha(r[0]), contar(r[1])))
                .toList();
    }

    private List<ReporteSerie> confirmaciones(LocalDate desde, LocalDate hasta) {
        return citaRepository.confirmacionesPorDia(desde, hasta).stream()
                .map(r -> new ReporteSerie(aFecha(r[0]), contar(r[1])))
                .toList();
    }

    private List<ReporteSerie> cancelaciones(LocalDate desde, LocalDate hasta) {
        return citaRepository.cancelacionesPorDia(desde, hasta).stream()
                .map(r -> new ReporteSerie(aFecha(r[0]), contar(r[1])))
                .toList();
    }

    private List<ReporteSerie> noAsistencia(LocalDate desde, LocalDate hasta) {
        return citaRepository.noAsistioPorDia(desde, hasta).stream()
                .map(r -> new ReporteSerie(aFecha(r[0]), contar(r[1])))
                .toList();
    }

    private List<ReporteDetalle> citasPorOdontologo(LocalDate desde, LocalDate hasta) {
        return citaRepository.citasPorOdontologo(desde, hasta, TenantContext.actualOrDefault()).stream()
                .map(r -> new ReporteDetalle(String.valueOf(r[1]), contar(r[2])))
                .toList();
    }

    private List<ReporteDetalle> serviciosMasSolicitados(LocalDate desde, LocalDate hasta, int top) {
        return citaRepository.serviciosMasSolicitados(desde, hasta, top, TenantContext.actualOrDefault()).stream()
                .map(r -> new ReporteDetalle(String.valueOf(r[1]), contar(r[2])))
                .toList();
    }

    private List<ReporteDetalle> porEstado(LocalDate desde, LocalDate hasta) {
        return citaRepository.citasPorEstado(desde, hasta, TenantContext.actualOrDefault()).stream()
                .map(r -> new ReporteDetalle(String.valueOf(r[0]), contar(r[1])))
                .toList();
    }

    private List<ReporteDetalle> confirmacionesPorFuente(LocalDate desde, LocalDate hasta) {
        return citaRepository.confirmacionesPorFuente(desde, hasta, TenantContext.actualOrDefault()).stream()
                .map(r -> new ReporteDetalle(String.valueOf(r[0]), contar(r[1])))
                .toList();
    }

    private static LocalDate aFecha(Object valor) {
        if (valor instanceof LocalDate d) {
            return d;
        }
        if (valor instanceof Date d) {
            return d.toLocalDate();
        }
        throw new IllegalArgumentException("Tipo de fecha inesperado en reporte: " + valor);
    }

    private static long contar(Object valor) {
        return ((Number) valor).longValue();
    }
}