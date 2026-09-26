package org.dentalcrm.web.agenda;

import org.dentalcrm.domain.cita.Cita;
import org.dentalcrm.domain.horario.ExcepcionHorario;
import org.dentalcrm.domain.horario.ExcepcionHorarioRepository;
import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.odontologo.OdontologoRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.service.CurrentUserService;
import org.dentalcrm.web.agenda.dto.ExcepcionHorarioRequest;
import org.dentalcrm.web.agenda.dto.ExcepcionHorarioResponse;
import org.dentalcrm.web.cita.CitaService;
import org.dentalcrm.web.cita.dto.CancelarRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

/**
 * Días de excepción de atención de un odontólogo.
 *
 * <p>Al crear una, las citas activas que queden fuera del día permitido se
 * cancelan; la cancelación publica su evento y el pipeline de notificaciones
 * avisa a cada paciente por WhatsApp.
 */
@Service
public class ExcepcionHorarioService {

    private static final String MODULO = "AGENDA";
    private static final Set<String> TIPOS_VALIDOS =
            Set.of(ExcepcionHorario.TIPO_CERRADO, ExcepcionHorario.TIPO_HORARIO_ESPECIAL);

    private final ExcepcionHorarioRepository excepcionRepository;
    private final OdontologoRepository odontologoRepository;
    private final AgendaService agendaService;
    private final CitaService citaService;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;
    private final ZoneId zona;

    public ExcepcionHorarioService(ExcepcionHorarioRepository excepcionRepository,
                                   OdontologoRepository odontologoRepository,
                                   AgendaService agendaService,
                                   CitaService citaService,
                                   AuditService auditService,
                                   CurrentUserService currentUserService,
                                   @org.springframework.beans.factory.annotation.Value("${app.timezone}") String timezone) {
        this.excepcionRepository = excepcionRepository;
        this.odontologoRepository = odontologoRepository;
        this.agendaService = agendaService;
        this.citaService = citaService;
        this.auditService = auditService;
        this.currentUserService = currentUserService;
        this.zona = ZoneId.of(timezone);
    }

    @Transactional(readOnly = true)
    public List<ExcepcionHorarioResponse> listar(Long odontologoId, LocalDate desde, LocalDate hasta) {
        return excepcionRepository.findByOdontologoIdAndFechaBetween(odontologoId, desde, hasta).stream()
                .map(ExcepcionHorarioResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExcepcionHorarioResponse obtener(Long id) {
        return ExcepcionHorarioResponse.from(buscarOArrojar(id));
    }

    @Transactional
    public ExcepcionHorarioResponse crear(ExcepcionHorarioRequest request) {
        validar(request);
        Odontologo odontologo = odontologoRepository.findById(request.odontologoId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Odontólogo no encontrado"));
        excepcionRepository.findByOdontologoIdAndFecha(request.odontologoId(), request.fecha())
                .ifPresent(e -> {
                    throw new BusinessException("EXCEPCION_DUPLICADA",
                            "Ya existe una excepción de atención para ese odontólogo en esa fecha");
                });

        ExcepcionHorario excepcion = new ExcepcionHorario();
        excepcion.setOdontologo(odontologo);
        excepcion.setFecha(request.fecha());
        excepcion.setTipo(ExcepcionHorario.TIPO_CERRADO.equals(request.tipo())
                ? ExcepcionHorario.TIPO_CERRADO : ExcepcionHorario.TIPO_HORARIO_ESPECIAL);
        excepcion.setHoraInicio(request.horaInicio());
        excepcion.setHoraFin(request.horaFin());
        excepcion.setMotivo(request.motivo());
        currentUserService.idUsuarioActual().ifPresent(excepcion::setCreatedBy);

        ExcepcionHorario guardada = excepcionRepository.save(excepcion);
        int canceladas = cancelarAfectadas(guardada);
        auditService.registrar("CREAR_EXCEPCION_HORARIO", MODULO, "EXCEPCION_HORARIO",
                guardada.getId(), null, ExcepcionHorarioResponse.from(guardada, canceladas));
        return ExcepcionHorarioResponse.from(guardada, canceladas);
    }

    @Transactional
    public void eliminar(Long id) {
        ExcepcionHorario excepcion = buscarOArrojar(id);
        excepcionRepository.delete(excepcion);
        auditService.registrar("ELIMINAR_EXCEPCION_HORARIO", MODULO, "EXCEPCION_HORARIO",
                id, ExcepcionHorarioResponse.from(excepcion), null);
    }

    /**
     * Cancela las citas pendientes o confirmadas que el nuevo día no admite.
     * Cada cancelación dispara la notificación al paciente, así que no hace falta
     * avisar aquí: el pipeline de WhatsApp se encarga.
     *
     * @return número de citas canceladas
     */
    private int cancelarAfectadas(ExcepcionHorario excepcion) {
        String motivo = excepcion.esCerrado()
                ? "La clínica no atiende el " + excepcion.getFecha()
                : "Ese día la atención es de " + excepcion.getHoraInicio() + " a " + excepcion.getHoraFin();
        int canceladas = 0;
        for (Cita cita : agendaService.citasActivasDelDia(excepcion.getOdontologo().getId(), excepcion.getFecha())) {
            if (!excepcion.afectaA(cita.getHoraInicio(), cita.getHoraFin())) {
                continue;
            }
            citaService.cancelar(cita.getId(), new CancelarRequest(motivo));
            canceladas++;
        }
        return canceladas;
    }

    private void validar(ExcepcionHorarioRequest request) {
        if (!TIPOS_VALIDOS.contains(request.tipo())) {
            throw new BusinessException("EXCEPCION_TIPO_INVALIDO",
                    "El tipo debe ser CERRADO u HORARIO_ESPECIAL");
        }
        if (request.fecha().isBefore(LocalDate.now(zona))) {
            throw new BusinessException("EXCEPCION_FECHA_PASADA",
                    "No se pueden crear excepciones en fechas pasadas");
        }
        if (ExcepcionHorario.TIPO_CERRADO.equals(request.tipo())) {
            if (request.horaInicio() != null || request.horaFin() != null) {
                throw new BusinessException("EXCEPCION_HORAS_INVALIDAS",
                        "Un día cerrado no admite horas: déjalas vacías");
            }
            return;
        }
        if (request.horaInicio() == null || request.horaFin() == null) {
            throw new BusinessException("EXCEPCION_HORAS_INVALIDAS",
                    "El horario especial necesita hora de inicio y de fin");
        }
        if (!request.horaFin().isAfter(request.horaInicio())) {
            throw new BusinessException("EXCEPCION_HORAS_INVALIDAS",
                    "La hora de fin debe ser posterior a la de inicio");
        }
    }

    private ExcepcionHorario buscarOArrojar(Long id) {
        return excepcionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Excepción no encontrada"));
    }
}
