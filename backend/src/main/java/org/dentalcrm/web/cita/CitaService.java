package org.dentalcrm.web.cita;

import org.dentalcrm.domain.cita.Cita;
import org.dentalcrm.domain.cita.CitaAtendidaEvent;
import org.dentalcrm.domain.cita.CitaCanceladaEvent;
import org.dentalcrm.domain.cita.CitaConfirmadaEvent;
import org.dentalcrm.domain.cita.CitaCreadaEvent;
import org.dentalcrm.domain.cita.CitaModificadaEvent;
import org.dentalcrm.domain.cita.CitaNoAsistioEvent;
import org.dentalcrm.domain.cita.CitaRepository;
import org.dentalcrm.domain.cita.EstadoCita;
import org.dentalcrm.domain.horario.HorarioOdontologo;
import org.dentalcrm.domain.horario.HorarioOdontologoRepository;
import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.odontologo.OdontologoRepository;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.domain.servicio.Servicio;
import org.dentalcrm.domain.servicio.ServicioRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.service.CurrentUserService;
import org.dentalcrm.web.agenda.AgendaService;
import org.dentalcrm.web.cita.dto.CancelarRequest;
import org.dentalcrm.web.cita.dto.CitaRequest;
import org.dentalcrm.web.cita.dto.CitaResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class CitaService {

    private static final String MODULO = "CITAS";

    private final CitaRepository citaRepository;
    private final PacienteRepository pacienteRepository;
    private final OdontologoRepository odontologoRepository;
    private final ServicioRepository servicioRepository;
    private final HorarioOdontologoRepository horarioRepository;
    private final AgendaService agendaService;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;
    private final ApplicationEventPublisher eventPublisher;
    private final ZoneId zonaCita;

    public CitaService(CitaRepository citaRepository,
                       PacienteRepository pacienteRepository,
                       OdontologoRepository odontologoRepository,
                       ServicioRepository servicioRepository,
                       HorarioOdontologoRepository horarioRepository,
                       AgendaService agendaService,
                       AuditService auditService,
                       CurrentUserService currentUserService,
                       ApplicationEventPublisher eventPublisher,
                       @Value("${app.timezone}") String timezone) {
        this.citaRepository = citaRepository;
        this.pacienteRepository = pacienteRepository;
        this.odontologoRepository = odontologoRepository;
        this.servicioRepository = servicioRepository;
        this.horarioRepository = horarioRepository;
        this.agendaService = agendaService;
        this.auditService = auditService;
        this.currentUserService = currentUserService;
        this.eventPublisher = eventPublisher;
        this.zonaCita = ZoneId.of(timezone);
    }

    @Transactional(readOnly = true)
    public Page<CitaResponse> listar(EstadoCita estado, Long doctorId, Long pacienteId, String paciente,
                                     LocalDate fecha, LocalDate desde, LocalDate hasta, int page, int size) {
        Specification<Cita> spec = (root, query, cb) -> {
            if (Long.class != query.getResultType()) {
                root.fetch("paciente", jakarta.persistence.criteria.JoinType.LEFT);
                root.fetch("doctor", jakarta.persistence.criteria.JoinType.LEFT);
                root.fetch("servicio", jakarta.persistence.criteria.JoinType.LEFT);
            }
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (estado != null) {
                predicates.add(cb.equal(root.get("estado"), estado));
            }
            if (doctorId != null) {
                predicates.add(cb.equal(root.get("doctor").get("id"), doctorId));
            }
            if (pacienteId != null) {
                predicates.add(cb.equal(root.get("paciente").get("id"), pacienteId));
            }
            if (paciente != null && !paciente.isBlank()) {
                String like = "%" + paciente.trim().toLowerCase() + "%";
                var pac = root.join("paciente", jakarta.persistence.criteria.JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(pac.get("nombres")), like),
                        cb.like(cb.lower(pac.get("apellidos")), like),
                        cb.like(cb.concat(cb.lower(pac.get("nombres")), cb.concat(cb.literal(" "), cb.lower(pac.get("apellidos")))), like)));
            }
            if (fecha != null) {
                predicates.add(cb.equal(root.get("fecha"), fecha));
            }
            if (desde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fecha"), desde));
            }
            if (hasta != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fecha"), hasta));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        Sort sort = Sort.by(Sort.Direction.ASC, "fecha").and(Sort.by(Sort.Direction.ASC, "horaInicio"));
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 500), sort);
        return citaRepository.findAll(spec, pageRequest).map(CitaResponse::from);
    }

    @Transactional(readOnly = true)
    public CitaResponse obtener(Long id) {
        return CitaResponse.from(buscarOArrojar(id));
    }

    @Transactional
    public CitaResponse crear(CitaRequest request) {
        Paciente paciente = pacienteRepository.findById(request.pacienteId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Paciente no encontrado"));
        Odontologo doctor = odontologoRepository.findById(request.doctorId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Odontólogo no encontrado"));
        Servicio servicio = servicioRepository.findById(request.servicioId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Servicio no encontrado"));
        if (!"ACTIVO".equals(servicio.getEstado())) {
            throw new BusinessException("SERVICIO_INACTIVO", "El servicio seleccionado está inactivo");
        }

        LocalTime horaFin = request.horaInicio().plusMinutes(servicio.getDuracionMinutos());
        validarDisponibilidad(doctor.getId(), request.fecha(), request.horaInicio(), horaFin);

        Cita cita = new Cita();
        cita.setPaciente(paciente);
        cita.setDoctor(doctor);
        cita.setServicio(servicio);
        cita.setFecha(request.fecha());
        cita.setHoraInicio(request.horaInicio());
        cita.setHoraFin(horaFin);
        cita.setObservaciones(request.observaciones());
        currentUserService.idUsuarioActual().ifPresent(cita::setCreatedBy);

        try {
            Cita creada = citaRepository.save(cita);
            auditService.registrar("CREAR_CITA", MODULO, "CITA", creada.getId(), null, CitaResponse.from(creada));
            eventPublisher.publishEvent(new CitaCreadaEvent(creada.getId()));
            return CitaResponse.from(creada);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BusinessException("CITA_CAMBIO_CONCURRENTE", "La cita fue modificada por otra persona, reintente");
        }
    }

    @Transactional
    public CitaResponse actualizar(Long id, CitaRequest request) {
        Cita cita = buscarOArrojar(id);
        if (!cita.getEstado().esActiva() || cita.getEstado() == EstadoCita.ATENDIDA) {
            throw new BusinessException("CITA_NO_MODIFICABLE", "Solo se pueden modificar citas pendientes o confirmadas");
        }

        Paciente paciente = pacienteRepository.findById(request.pacienteId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Paciente no encontrado"));
        Odontologo doctor = odontologoRepository.findById(request.doctorId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Odontólogo no encontrado"));
        Servicio servicio = servicioRepository.findById(request.servicioId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Servicio no encontrado"));

        LocalTime horaFin = request.horaInicio().plusMinutes(servicio.getDuracionMinutos());
        validarDisponibilidad(doctor.getId(), request.fecha(), request.horaInicio(), horaFin, id);

        CitaResponse anterior = CitaResponse.from(cita);
        cita.setPaciente(paciente);
        cita.setDoctor(doctor);
        cita.setServicio(servicio);
        cita.setFecha(request.fecha());
        cita.setHoraInicio(request.horaInicio());
        cita.setHoraFin(horaFin);
        cita.setObservaciones(request.observaciones());

        try {
            Cita actualizada = citaRepository.save(cita);
            auditService.registrar("MODIFICAR_CITA", MODULO, "CITA", id, anterior, CitaResponse.from(actualizada));
            eventPublisher.publishEvent(new CitaModificadaEvent(id,
                    anterior.fecha(), anterior.horaInicio(),
                    actualizada.getFecha(), actualizada.getHoraInicio()));
            return CitaResponse.from(actualizada);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BusinessException("CITA_CAMBIO_CONCURRENTE", "La cita fue modificada por otra persona, reintente");
        }
    }

    @Transactional
    public CitaResponse confirmar(Long id) {
        return confirmar(id, null);
    }

    /**
     * Confirma una cita pendiente. {@code source} indica el origen de la
     * confirmación: null/"PANEL" desde el sistema o "WHATSAPP" desde WhatsApp.
     */
    @Transactional
    public CitaResponse confirmar(Long id, String source) {
        Cita cita = buscarOArrojar(id);
        if (cita.getEstado() != EstadoCita.PENDIENTE) {
            throw new BusinessException("CITA_NO_CONFIRMABLE", "Solo se confirman citas pendientes");
        }
        CitaResponse anterior = CitaResponse.from(cita);
        cita.setEstado(EstadoCita.CONFIRMADA);
        cita.setConfirmada(true);
        cita.setConfirmadaAt(java.time.Instant.now());
        cita.setConfirmationSource(source);
        Cita confirmada = citaRepository.save(cita);
        auditService.registrar("CONFIRMAR_CITA", MODULO, "CITA", id, anterior, CitaResponse.from(confirmada));
        eventPublisher.publishEvent(new CitaConfirmadaEvent(id));
        return CitaResponse.from(confirmada);
    }

    @Transactional
    public CitaResponse cancelar(Long id, CancelarRequest request) {
        Cita cita = buscarOArrojar(id);
        if (cita.getEstado() != EstadoCita.PENDIENTE && cita.getEstado() != EstadoCita.CONFIRMADA) {
            throw new BusinessException("CITA_NO_CANCELABLE", "Solo se cancelan citas pendientes o confirmadas");
        }
        CitaResponse anterior = CitaResponse.from(cita);
        cita.setEstado(EstadoCita.CANCELADA);
        cita.setCanceladaAt(java.time.Instant.now());
        cita.setCanceladaMotivo(request == null ? null : request.motivo());
        cita.setConfirmada(false);
        Cita cancelada = citaRepository.save(cita);
        auditService.registrar("CANCELAR_CITA", MODULO, "CITA", id, anterior, CitaResponse.from(cancelada));
        eventPublisher.publishEvent(new CitaCanceladaEvent(id));
        return CitaResponse.from(cancelada);
    }

    @Transactional
    public CitaResponse atender(Long id) {
        Cita cita = buscarOArrojar(id);
        if (cita.getEstado() != EstadoCita.PENDIENTE && cita.getEstado() != EstadoCita.CONFIRMADA) {
            throw new BusinessException("CITA_NO_ATENDIBLE", "Solo se atienden citas pendientes o confirmadas");
        }
        CitaResponse anterior = CitaResponse.from(cita);
        cita.setEstado(EstadoCita.ATENDIDA);
        Cita atendida = citaRepository.save(cita);
        auditService.registrar("ATENDER_CITA", MODULO, "CITA", id, anterior, CitaResponse.from(atendida));
        eventPublisher.publishEvent(new CitaAtendidaEvent(id));
        return CitaResponse.from(atendida);
    }

    @Transactional
    public CitaResponse noAsistio(Long id) {
        Cita cita = buscarOArrojar(id);
        if (cita.getEstado() != EstadoCita.PENDIENTE && cita.getEstado() != EstadoCita.CONFIRMADA) {
            throw new BusinessException("CITA_NO_MARCA", "Solo se marca no asistió en citas pendientes o confirmadas");
        }
        CitaResponse anterior = CitaResponse.from(cita);
        cita.setEstado(EstadoCita.NO_ASISTIO);
        Cita resultado = citaRepository.save(cita);
        auditService.registrar("NO_ASISTIO_CITA", MODULO, "CITA", id, anterior, CitaResponse.from(resultado));
        eventPublisher.publishEvent(new CitaNoAsistioEvent(id));
        return CitaResponse.from(resultado);
    }

    private void validarDisponibilidad(Long doctorId, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        validarDisponibilidad(doctorId, fecha, horaInicio, horaFin, null);
    }

    private void validarDisponibilidad(Long doctorId, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, Long citaActual) {
        LocalDate hoy = LocalDate.now(zonaCita);
        if (fecha.isBefore(hoy)) {
            throw new BusinessException("FECHA_PASADA", "No se pueden agendar citas en fechas pasadas");
        }
        if (fecha.equals(hoy) && !horaInicio.isAfter(LocalTime.now(zonaCita))) {
            throw new BusinessException("HORA_PASADA", "La hora seleccionada ya pasó");
        }
        if (!existeHorario(doctorId, fecha, horaInicio, horaFin)) {
            throw new BusinessException("HORARIO_NO_DISPONIBLE", "El horario del odontólogo no admite la cita en ese lapso");
        }
        List<?> solapadas = citaRepository.findOverlap(doctorId, fecha, horaInicio, horaFin).stream()
                .filter(c -> !c.getId().equals(citaActual))
                .toList();
        if (!solapadas.isEmpty()) {
            throw new BusinessException("APPOINTMENT_NOT_AVAILABLE", "El horario seleccionado ya no está disponible");
        }
        if (!agendaService.estaLibre(doctorId, fecha, horaInicio, horaFin)) {
            throw new BusinessException("HORARIO_BLOQUEADO", "El horario seleccionado está bloqueado");
        }
    }

    private boolean existeHorario(Long doctorId, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        // Un día con excepción manda sobre el horario semanal: si está cerrado no
        // se admite nada y, si tiene horario especial, la cita debe caber entera.
        var excepcion = agendaService.excepcion(doctorId, fecha);
        if (excepcion.isPresent()) {
            return excepcion.get().admite(horaInicio, horaFin);
        }
        return horarioRepository.findByOdontologoIdAndDiaSemanaAndEstado(doctorId, fecha.getDayOfWeek().getValue(), "ACTIVO")
                .stream()
                .anyMatch(h -> !h.getHoraInicio().isAfter(horaInicio) && !h.getHoraFin().isBefore(horaFin));
    }

    private Cita buscarOArrojar(Long id) {
        return citaRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Cita no encontrada"));
    }
}