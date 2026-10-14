package org.dentalcrm.web.agenda.publica;

import org.dentalcrm.domain.cita.Cita;
import org.dentalcrm.domain.cita.CitaRepository;
import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.odontologo.OdontologoRepository;
import org.dentalcrm.domain.paciente.Paciente;
import org.dentalcrm.domain.paciente.PacienteRepository;
import org.dentalcrm.domain.horario.HorarioOdontologoRepository;
import org.dentalcrm.domain.reserva.ReservaWebIdempotencia;
import org.dentalcrm.domain.reserva.ReservaWebIdempotenciaRepository;
import org.dentalcrm.domain.servicio.ServicioRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.web.agenda.AgendaService;
import org.dentalcrm.web.agenda.dto.SlotResponse;
import org.dentalcrm.web.agenda.publica.dto.*;
import org.dentalcrm.web.cita.CitaService;
import org.dentalcrm.web.cita.dto.CitaRequest;
import org.dentalcrm.web.cita.dto.CitaResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Agenda pública por enlace temporal (§10–§15): sólo odontólogos activos
 * verificados en el token, cupos en vivo y reserva con revalidación e
 * idempotencia; nunca expone teléfono, cédula u otros pacientes (§12).
 */
@Service
public class AgendaPublicaService {

    public static final String OBSERVACIONES_WEB = "Reserva web (calendario público)";

    public static final String MODULO = "AGENDA";
    public static final String ACCION_RESERVA_WEB = "RESERVA_WEB";

    private final AgendaPublicaTokenService tokens;
    private final AgendaService agendaService;
    private final CitaService citaService;
    private final OdontologoRepository odontologoRepository;
    private final PacienteRepository pacienteRepository;
    private final ServicioRepository servicioRepository;
    private final CitaRepository citaRepository;
    private final HorarioOdontologoRepository horarioRepository;
    private final ReservaWebIdempotenciaRepository idempotenciaRepository;
    private final AuditService auditService;

    public AgendaPublicaService(AgendaPublicaTokenService tokens,
                                AgendaService agendaService,
                                CitaService citaService,
                                OdontologoRepository odontologoRepository,
                                PacienteRepository pacienteRepository,
                                ServicioRepository servicioRepository,
                                CitaRepository citaRepository,
                                HorarioOdontologoRepository horarioRepository,
                                ReservaWebIdempotenciaRepository idempotenciaRepository,
                                AuditService auditService) {
        this.tokens = tokens;
        this.agendaService = agendaService;
        this.citaService = citaService;
        this.odontologoRepository = odontologoRepository;
        this.pacienteRepository = pacienteRepository;
        this.servicioRepository = servicioRepository;
        this.citaRepository = citaRepository;
        this.horarioRepository = horarioRepository;
        this.idempotenciaRepository = idempotenciaRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public AgendaContextResponse contexto(String token) {
        AgendaPublicaTokenService.Payload p = validar(token);
        Odontologo odontologo = odontologoActivo(p.odontologoId());
        // §13: un enlace de sólo consulta (VIEW_AVAILABILITY) puede ser anónimo.
        Paciente paciente = p.pacienteId() == null ? null : paciente(p.pacienteId());
        return new AgendaContextResponse(odontologo.getId(), nombreCompleto(odontologo),
                odontologo.getEspecialidad(), paciente == null ? "" : primerNombre(paciente.getNombres()),
                p.servicioId(), tokens.ttlMinutos());
    }

    @Transactional(readOnly = true)
    public List<AgendaDiaResponse> dias(String token, int anio, int mes) {
        AgendaPublicaTokenService.Payload p = validar(token);
        Odontologo odontologo = odontologoActivo(p.odontologoId());
        LocalDate hoy = LocalDate.now();
        LocalDate primero = LocalDate.of(anio, mes, 1);
        YearMonth ym = YearMonth.of(anio, mes);
        List<AgendaDiaResponse> dias = new ArrayList<>();
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            LocalDate dia = primero.withDayOfMonth(d);
            if (dia.isBefore(hoy)) {
                dias.add(new AgendaDiaResponse(dia, AgendaDiaResponse.PASADO, 0));
                continue;
            }
            // §14: el calendario público usa la misma fuente de verdad que el
            // chat para separar "no atiende ese día" de "atiende pero no hay cupos".
            if (diaNoLaborable(odontologo.getId(), dia)) {
                dias.add(new AgendaDiaResponse(dia, AgendaDiaResponse.NO_LABORABLE, 0));
                continue;
            }
            int cupos = agendaService.disponibilidad(p.odontologoId(), dia, p.servicioId(), null).size();
            dias.add(new AgendaDiaResponse(dia, estadoDelDia(cupos), cupos));
        }
        return dias;
    }

    @Transactional(readOnly = true)
    public List<AgendaSlotResponse> slots(String token, LocalDate fecha) {
        AgendaPublicaTokenService.Payload p = validar(token);
        Odontologo odontologo = odontologoActivo(p.odontologoId());
        return agendaService.disponibilidad(p.odontologoId(), fecha, p.servicioId(), null).stream()
                .map(s -> new AgendaSlotResponse(s.horaInicio().toString(), s.horaFin().toString(),
                        odontologo.getId(), nombreCompleto(odontologo),
                        tokens.firmarSlot(odontologo.getId(), fecha, s.horaInicio())))
                .toList();
    }

    @Transactional
    public AgendaReservarResponse reservar(AgendaReservarRequest req) {
        var payload = tokens.validar(req.token());
        if (payload.isEmpty()) {
            return error("El enlace caducó o no es válido. Pide uno nuevo por WhatsApp.");
        }
        AgendaPublicaTokenService.Payload p = payload.get();
        // §13: sólo el enlace de reserva (BOOK_APPOINTMENT) crea citas; el de
        // consulta (VIEW_AVAILABILITY) es anónimo y se queda en lectura.
        if (!AgendaPublicaTokenService.PROPOSITO.equals(p.proposito())) {
            return error("Este enlace es sólo para consultar. Pide uno de reserva por WhatsApp.");
        }
        if (p.pacienteId() == null) {
            return error("Para reservar hay que identificar al paciente primero.");
        }
        // §15: el cupo se resuelve desde el slotToken firmado en /slots; la
        // fecha/hora del navegador sólo quedan como respaldo de compatibilidad.
        final LocalDate fecha;
        final LocalTime horaInicio;
        if (req.slotToken() != null && !req.slotToken().isBlank()) {
            var slot = tokens.validarSlot(req.slotToken());
            if (slot.isEmpty()) {
                return error("Ese cupo ya no es válido. Actualiza la página y elige otro horario.");
            }
            AgendaPublicaTokenService.SlotPayload s = slot.get();
            if (!p.odontologoId().equals(s.odontologoId())) {
                return error("El cupo elegido no corresponde a este odontólogo.");
            }
            try {
                fecha = LocalDate.parse(s.fecha());
                horaInicio = LocalTime.parse(s.hora());
            } catch (Exception ex) {
                return error("La hora elegida no es válida.");
            }
        } else {
            if (req.fecha() == null || req.horaInicio() == null) {
                return error("Falta la fecha o la hora elegida.");
            }
            try {
                fecha = req.fecha();
                horaInicio = LocalTime.parse(req.horaInicio());
            } catch (Exception ex) {
                return error("La hora elegida no es válida.");
            }
        }
        Long odontologoId = req.odontologoId() == null ? p.odontologoId() : req.odontologoId();
        if (!odontologoId.equals(p.odontologoId())) {
            return error("El enlace no corresponde a ese odontólogo.");
        }
        Long servicioId = req.servicioId() == null ? p.servicioId() : req.servicioId();
        if (servicioId == null || servicioRepository.findById(servicioId)
                .filter(s -> "ACTIVO".equals(s.getEstado())).isEmpty()) {
            return error("No hay un servicio válido para la reserva.");
        }

        // Idempotencia explícita (§15): la misma clave de reintento devuelve la
        // cita ya creada sin pasar por la agenda ni crear otra. La garantía
        // fuerte contra dos envíos concurrentes es el índice único
        // (tenant_id, idempotency_key); si el INSERT pierde la carrera, el
        // DataIntegrityViolationException (relanzado aquí como OCUPADA) manda
        // al cliente a reintentar, y ese reintento cae en este mismo camino.
        if (req.idempotencyKey() != null && !req.idempotencyKey().isBlank()) {
            var previa = idempotenciaRepository
                    .findByTenantIdAndIdempotencyKey(p.tenant(), req.idempotencyKey());
            if (previa.isPresent()) {
                return citaRepetida(previa.get().getCitaId(),
                        "Ya reservaste con este enlace: no se creó otra cita.");
            }
        }

        // Idempotencia por clave natural (respaldo): la misma persona/hora dos
        // veces = la misma cita, aunque el cliente no mande idempotencyKey.
        Cita existente = citaRepository.findAll().stream()
                .filter(c -> c.getEstado().esActiva() && p.pacienteId().equals(c.getPaciente().getId())
                        && odontologoId.equals(c.getDoctor().getId())
                        && fecha.equals(c.getFecha()) && horaInicio.equals(c.getHoraInicio()))
                .findFirst().orElse(null);
        if (existente != null) {
            return new AgendaReservarResponse(AgendaReservarResponse.REPETIDA, CitaResponse.from(existente),
                    List.of(), "Ya tienes una cita en ese horario: no se creó otra.");
        }

        // Revalidación en vivo: el cupo mostrado puede haberse tomado mientras tanto (§15).
        List<SlotResponse> libres = agendaService.disponibilidad(odontologoId, fecha, servicioId, null);
        if (libres.stream().noneMatch(s -> s.horaInicio().equals(horaInicio))) {
            return ocupada(odontologoId, fecha, servicioId, libres);
        }
        try {
            CitaResponse creada = citaService.crear(new CitaRequest(p.pacienteId(), odontologoId,
                    servicioId, fecha, horaInicio, OBSERVACIONES_WEB));
            if (req.idempotencyKey() != null && !req.idempotencyKey().isBlank()) {
                ReservaWebIdempotencia enlace = new ReservaWebIdempotencia();
                enlace.setIdempotencyKey(req.idempotencyKey());
                enlace.setCitaId(creada.id());
                enlace.setOdontologoId(odontologoId);
                enlace.setPacienteId(p.pacienteId());
                enlace.setFecha(fecha);
                enlace.setHoraInicio(horaInicio);
                idempotenciaRepository.save(enlace);
            }
            // Auditoría §17: la reserva web es una alta de cita sin usuario de
            // panel, así que queda trazada con su propia acción.
            auditService.registrar(ACCION_RESERVA_WEB, MODULO, "CITA", creada.id(),
                    null, creada);
            return new AgendaReservarResponse(AgendaReservarResponse.CREADA, creada, List.of(),
                    "¡Listo! Tu cita quedó reservada. Recibirás la confirmación por WhatsApp.");
        } catch (BusinessException ex) {
            return ocupada(odontologoId, fecha, servicioId,
                    agendaService.disponibilidad(odontologoId, fecha, servicioId, null));
        }
    }

    /** Cupo tomado entre la lista y la confirmación: devuelve alternativas reales (§15). */
    private AgendaReservarResponse ocupada(Long odontologoId, LocalDate fecha, Long servicioId,
                                           List<SlotResponse> libres) {
        return new AgendaReservarResponse(AgendaReservarResponse.OCUPADA, null,
                libres.stream().limit(3)
                        .map(s -> new AgendaSlotResponse(s.horaInicio().toString(), s.horaFin().toString(),
                                odontologoId, "", tokens.firmarSlot(odontologoId, fecha, s.horaInicio())))
                        .toList(),
                "Ese horario acaba de ocuparse. Elige otro de las opciones.");
    }

    /**
     * Genera el enlace temporal para el chat (§10): odontólogo verificado y,
     * si hay paciente identificado, propósito de reserva; sin paciente, sólo
     * consulta de disponibilidad (VIEW_AVAILABILITY, §13).
     */
    public String generarEnlace(Long pacienteId, Long odontologoId, Long servicioId) {
        if (odontologoId == null) {
            throw new BusinessException("ENLACE_INCOMPLETO", "Falta el odontólogo para el enlace");
        }
        return tokens.firmar(pacienteId, odontologoId, servicioId);
    }

    public long ttlMinutos() {
        return tokens.ttlMinutos();
    }

    private AgendaReservarResponse citaRepetida(Long citaId, String mensaje) {
        CitaResponse cita = citaRepository.findById(citaId)
                .map(CitaResponse::from)
                .orElse(null);
        return new AgendaReservarResponse(AgendaReservarResponse.REPETIDA, cita, List.of(), mensaje);
    }

    /**
     * ¿No atiende ese día? Clínica cerrada por excepción, o sin horario
     * semanal activo ese weekday. Es la misma fuente que usa el chat en
     * {@link AgendaService#disponibilidad}, para que ambos canales digan lo
     * mismo (§14). Un día laborable sin cupos libres cae en
     * {@code SIN_DISPONIBILIDAD}, no aquí.
     */
    private boolean diaNoLaborable(Long odontologoId, LocalDate dia) {
        var excepcion = agendaService.excepcion(odontologoId, dia);
        if (excepcion.isPresent()) {
            return excepcion.get().esCerrado();
        }
        return horarioRepository
                .findByOdontologoIdAndDiaSemanaAndEstado(odontologoId, dia.getDayOfWeek().getValue(), "ACTIVO")
                .isEmpty();
    }

    private static AgendaReservarResponse error(String mensaje) {
        return new AgendaReservarResponse(AgendaReservarResponse.ERROR, null, List.of(), mensaje);
    }

    private AgendaPublicaTokenService.Payload validar(String token) {
        return tokens.validar(token).orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                "ENLACE_INVALIDO", "El enlace caducó o no es válido. Pide uno nuevo por WhatsApp."));
    }

    private Odontologo odontologoActivo(Long id) {
        return odontologoRepository.findById(id)
                .filter(o -> "ACTIVO".equals(o.getEstado()))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO",
                        "El odontólogo ya no está disponible."));
    }

    private Paciente paciente(Long id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO",
                        "Paciente no encontrado."));
    }

    private static String estadoDelDia(int cupos) {
        if (cupos <= 0) {
            return AgendaDiaResponse.SIN_DISPONIBILIDAD;
        }
        return cupos < 3 ? AgendaDiaResponse.POCA : AgendaDiaResponse.DISPONIBLE;
    }

    private static String nombreCompleto(Odontologo o) {
        return (o.getNombres() + " " + o.getApellidos()).trim();
    }

    private static String primerNombre(String nombres) {
        if (nombres == null || nombres.isBlank()) {
            return "";
        }
        return nombres.trim().split("\\s+")[0];
    }
}
