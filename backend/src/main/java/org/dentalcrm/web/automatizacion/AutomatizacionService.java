package org.dentalcrm.web.automatizacion;

import org.dentalcrm.domain.automatizacion.*;
import org.dentalcrm.domain.cita.Cita;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.web.automatizacion.dto.AutomatizacionRequest;
import org.dentalcrm.web.automatizacion.dto.AutomatizacionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
public class AutomatizacionService {

    private static final Logger log = LoggerFactory.getLogger(AutomatizacionService.class);
    private static final String MODULO = "AUTOMATIZACIONES";

    private final AutomatizacionRepository automatizacionRepository;
    private final PlantillaMensajeRepository plantillaRepository;
    private final NotificacionRepository notificacionRepository;
    private final VariableResolver variableResolver;
    private final AuditService auditService;
    private final ZoneId zoneId;

    public AutomatizacionService(AutomatizacionRepository automatizacionRepository,
                                 PlantillaMensajeRepository plantillaRepository,
                                 NotificacionRepository notificacionRepository,
                                 VariableResolver variableResolver,
                                 AuditService auditService,
                                 @org.springframework.beans.factory.annotation.Value("${app.timezone}") String timezone) {
        this.automatizacionRepository = automatizacionRepository;
        this.plantillaRepository = plantillaRepository;
        this.notificacionRepository = notificacionRepository;
        this.variableResolver = variableResolver;
        this.auditService = auditService;
        this.zoneId = ZoneId.of(timezone);
    }

    // ------------------------------------------------------------------
    // CRUD
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AutomatizacionResponse> listar() {
        return automatizacionRepository.findAllByOrderByEventoAscNombreAsc().stream()
                .map(AutomatizacionResponse::from).toList();
    }

    @Transactional
    public AutomatizacionResponse crear(AutomatizacionRequest request) {
        EventoAutomatizacion evento = validarEvento(request.evento());
        PlantillaMensaje plantilla = plantillaOError(request.plantillaId());
        Automatizacion a = new Automatizacion();
        a.setNombre(request.nombre().trim());
        a.setEvento(evento);
        a.setMinutosAntes(request.minutosAntes());
        a.setPlantilla(plantilla);
        a.setCondicion(request.condicion());
        a.setActiva(request.activa() == null || request.activa());
        automatizacionRepository.save(a);
        auditService.registrar("CREAR_AUTOMATIZACION", MODULO, "AUTOMATIZACION", a.getId());
        return AutomatizacionResponse.from(a);
    }

    @Transactional
    public AutomatizacionResponse actualizar(Long id, AutomatizacionRequest request) {
        Automatizacion a = automatizacionOError(id);
        a.setNombre(request.nombre().trim());
        a.setEvento(validarEvento(request.evento()));
        a.setMinutosAntes(request.minutosAntes());
        a.setPlantilla(plantillaOError(request.plantillaId()));
        a.setCondicion(request.condicion());
        a.setActiva(request.activa() == null || request.activa());
        automatizacionRepository.save(a);
        auditService.registrar("ACTUALIZAR_AUTOMATIZACION", MODULO, "AUTOMATIZACION", id);
        return AutomatizacionResponse.from(a);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!automatizacionRepository.existsById(id)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Automatización no encontrada");
        }
        automatizacionRepository.deleteById(id);
        auditService.registrar("ELIMINAR_AUTOMATIZACION", MODULO, "AUTOMATIZACION", id);
    }

    @Transactional
    public AutomatizacionResponse activar(Long id) {
        Automatizacion a = automatizacionOError(id);
        a.setActiva(true);
        automatizacionRepository.save(a);
        auditService.registrar("ACTIVAR_AUTOMATIZACION", MODULO, "AUTOMATIZACION", id);
        return AutomatizacionResponse.from(a);
    }

    @Transactional
    public AutomatizacionResponse desactivar(Long id) {
        Automatizacion a = automatizacionOError(id);
        a.setActiva(false);
        automatizacionRepository.save(a);
        auditService.registrar("DESACTIVAR_AUTOMATIZACION", MODULO, "AUTOMATIZACION", id);
        return AutomatizacionResponse.from(a);
    }

    // ------------------------------------------------------------------
    // Generación de notificaciones a partir de eventos de cita
    // ------------------------------------------------------------------

    @Transactional
    public int generar(Cita cita, EventoAutomatizacion... eventos) {
        if (eventos == null || eventos.length == 0) {
            return 0;
        }
        var variables = variableResolver.variables(cita);
        int generadas = 0;
        for (EventoAutomatizacion evento : eventos) {
            generadas += generarPara(cita, evento, variables);
        }
        return generadas;
    }

    private int generarPara(Cita cita, EventoAutomatizacion evento, Map<String, String> variables) {
        String telefono = cita.getPaciente().getTelefono();
        int creadas = 0;
        Instant ahora = Instant.now();
        for (Automatizacion a : automatizacionRepository.findByActivaTrueAndEvento(evento)) {
            Notificacion n = new Notificacion();
            n.setCita(cita);
            n.setAutomatizacion(a);
            n.setPaciente(cita.getPaciente());
            n.setTelefono(telefono);
            n.setPlantilla(a.getPlantilla());
            n.setMensajeGenerado(variableResolver.renderizar(a.getPlantilla().getContenido(), variables));
            n.setProgramadaAt(programadaPara(a, cita, ahora));
            n.setEstado(EstadoNotificacion.PENDIENTE);
            notificacionRepository.save(n);
            creadas++;
        }
        return creadas;
    }

    private Instant programadaPara(Automatizacion a, Cita cita, Instant ahora) {
        Instant programada = ahora;
        if (a.getEvento() == EventoAutomatizacion.CITA_PROXIMA
                && a.getMinutosAntes() != null && a.getMinutosAntes() > 0) {
            Instant inicioCita = cita.getFecha().atTime(cita.getHoraInicio()).atZone(zoneId).toInstant();
            programada = inicioCita.minus(a.getMinutosAntes(), ChronoUnit.MINUTES);
        }
        return programada.isBefore(ahora) ? ahora : programada;
    }

    private EventoAutomatizacion validarEvento(String evento) {
        try {
            return EventoAutomatizacion.valueOf(evento);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("EVENTO_INVALIDO", "Evento inválido: debe ser uno de " + List.of(EventoAutomatizacion.values()));
        }
    }

    private PlantillaMensaje plantillaOError(Long id) {
        return plantillaRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Plantilla no encontrada"));
    }

    private Automatizacion automatizacionOError(Long id) {
        return automatizacionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Automatización no encontrada"));
    }
}