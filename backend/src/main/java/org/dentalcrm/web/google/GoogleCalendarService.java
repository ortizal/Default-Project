package org.dentalcrm.web.google;

import org.dentalcrm.domain.cita.Cita;
import org.dentalcrm.domain.cita.CitaCanceladaEvent;
import org.dentalcrm.domain.cita.CitaConfirmadaEvent;
import org.dentalcrm.domain.cita.CitaCreadaEvent;
import org.dentalcrm.domain.cita.CitaModificadaEvent;
import org.dentalcrm.domain.cita.CitaRepository;
import org.dentalcrm.domain.cita.EstadoCita;
import org.dentalcrm.domain.cita.SyncEstado;
import org.dentalcrm.domain.google.GoogleAccount;
import org.dentalcrm.domain.google.GoogleAccountRepository;
import org.dentalcrm.domain.google.GoogleCalendario;
import org.dentalcrm.domain.google.GoogleCalendarioRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.web.google.dto.GoogleCalendarioResponse;
import org.dentalcrm.web.google.dto.GoogleStatusResponse;
import org.dentalcrm.web.google.dto.GoogleSyncResponse;
import org.dentalcrm.web.google.GoogleService.CalendarioRemoto;
import org.dentalcrm.web.google.GoogleService.EventoGoogle;
import org.dentalcrm.web.google.GoogleService.GoogleTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.springframework.scheduling.annotation.Scheduled;

@Service
public class GoogleCalendarService {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarService.class);
    private static final String MODULO = "GOOGLE_CALENDAR";
    private static final int MARGEN_TOKEN_SEG = 300;

    private final GoogleAccountRepository accountRepository;
    private final GoogleCalendarioRepository calendarioRepository;
    private final CitaRepository citaRepository;
    private final GoogleService googleService;
    private final AuditService auditService;
    private final String timezone;

    public GoogleCalendarService(GoogleAccountRepository accountRepository,
                                 GoogleCalendarioRepository calendarioRepository,
                                 CitaRepository citaRepository,
                                 GoogleService googleService,
                                 AuditService auditService,
                                 @Value("${app.timezone}") String timezone) {
        this.accountRepository = accountRepository;
        this.calendarioRepository = calendarioRepository;
        this.citaRepository = citaRepository;
        this.googleService = googleService;
        this.auditService = auditService;
        this.timezone = timezone;
    }

    @Transactional
    public String iniciarConexion() {
        googleService.arrojarSiNoConfigurado();
        return googleService.urlAutenticacion();
    }

    @Transactional
    public GoogleStatusResponse completarConexion(String code) {
        googleService.arrojarSiNoConfigurado();
        if (code == null || code.isBlank()) {
            throw new BusinessException("GOOGLE_AUTH_ERROR", "No se recibió el código de autorización de Google");
        }

        GoogleTokenResponse tok = googleService.intercambiarCodigo(code);
        if (tok.accessToken() == null || tok.accessToken().isBlank()) {
            throw new BusinessException("GOOGLE_AUTH_ERROR", "Google no devolvió un access token válido");
        }

        String email = googleService.obtenerEmail(tok.accessToken());
        if (email == null || email.isBlank()) {
            throw new BusinessException("GOOGLE_AUTH_ERROR", "No se pudo obtener el correo de la cuenta Google");
        }

        GoogleAccount cuenta = accountRepository.findByEmailIgnoreCase(email).orElseGet(GoogleAccount::new);
        cuenta.setEmail(email);
        cuenta.setAccessToken(tok.accessToken());
        if (tok.refreshToken() != null) {
            cuenta.setRefreshToken(tok.refreshToken());
        }
        cuenta.setScope(tok.scope() != null ? tok.scope() : googleService.scopeCompleto());
        cuenta.setExpiresAt(tok.expiresIn() != null
                ? Instant.now().plus(tok.expiresIn(), ChronoUnit.SECONDS)
                : null);
        accountRepository.save(cuenta);

        List<CalendarioRemoto> remotos = googleService.listarCalendarios(cuenta.getAccessToken());
        calendarioRepository.deleteAllByCuentaId(cuenta.getId());

        String seleccionPrevia = calendarioSeleccionadoPrevio(cuenta.getId());
        String seleccionar = resolverSeleccionInicial(seleccionPrevia, remotos);

        for (CalendarioRemoto remoto : remotos) {
            GoogleCalendario c = new GoogleCalendario();
            c.setCuenta(cuenta);
            c.setCalendarId(remoto.calendarId());
            c.setSummary(remoto.summary());
            c.setTimeZone(remoto.timeZone() != null ? remoto.timeZone() : timezone);
            c.setSeleccionado(remoto.calendarId().equals(seleccionar));
            calendarioRepository.save(c);
        }

        auditService.registrar("CONECTAR_GOOGLE", MODULO, "GOOGLE_ACCOUNT", cuenta.getId());
        return status();
    }

    private String resolverSeleccionInicial(String previa, List<CalendarioRemoto> remotos) {
        if (previa != null && remotos.stream().anyMatch(r -> r.calendarId().equals(previa))) {
            return previa;
        }
        if (remotos.stream().anyMatch(r -> "primary".equals(r.calendarId()))) {
            return "primary";
        }
        return remotos.isEmpty() ? null : remotos.get(0).calendarId();
    }

    private String calendarioSeleccionadoPrevio(Long cuentaId) {
        return calendarioRepository.findByCuentaIdAndSeleccionadoTrue(cuentaId)
                .map(GoogleCalendario::getCalendarId)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public GoogleStatusResponse status() {
        boolean configurada = googleService.estaConfigurado();
        Optional<GoogleAccount> cuenta = cuentaActiva();
        GoogleCalendario seleccionado = cuenta.flatMap(c ->
                        calendarioRepository.findByCuentaIdAndSeleccionadoTrue(c.getId()))
                .orElse(null);

        return new GoogleStatusResponse(
                configurada,
                cuenta.isPresent(),
                cuenta.map(GoogleAccount::getEmail).orElse(null),
                cuenta.map(c -> calendarioRepository.findByCuentaIdOrderBySummaryAsc(c.getId())
                                .stream().map(GoogleCalendarioResponse::from).toList())
                        .orElse(List.of()),
                seleccionado == null ? null : GoogleCalendarioResponse.from(seleccionado),
                citaRepository.countBySyncStatus(SyncEstado.SYNCED),
                citaRepository.countBySyncStatus(SyncEstado.PENDING),
                citaRepository.countBySyncStatus(SyncEstado.ERROR));
    }

    @Transactional(readOnly = true)
    public List<GoogleCalendarioResponse> listarCalendarios() {
        GoogleAccount cuenta = cuentaActivaOError();
        return calendarioRepository.findByCuentaIdOrderBySummaryAsc(cuenta.getId())
                .stream().map(GoogleCalendarioResponse::from).toList();
    }

    @Transactional
    public void seleccionarCalendario(Long id) {
        GoogleAccount cuenta = cuentaActivaOError();
        GoogleCalendario objetivo = calendarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("GOOGLE_CALENDARIO_NO_ENCONTRADO", "Calendario no encontrado"));
        if (!objetivo.getCuenta().getId().equals(cuenta.getId())) {
            throw new BusinessException("GOOGLE_CALENDARIO_NO_ENCONTRADO", "El calendario no pertenece a la cuenta conectada");
        }

        calendarioRepository.desmarcarTodos(cuenta.getId());
        objetivo.setSeleccionado(true);
        calendarioRepository.save(objetivo);
        auditService.registrar("SELECCIONAR_CALENDARIO", MODULO, "GOOGLE_CALENDAR", objetivo.getId());
    }

    @Transactional
    public void desconectar() {
        Optional<GoogleAccount> cuenta = cuentaActiva();
        if (cuenta.isPresent()) {
            List<Cita> conEvento = citaRepository.findAll().stream()
                    .filter(c -> c.getGoogleEventId() != null
                            || c.getGoogleEventIdDoctor() != null
                            || c.getSyncStatus() != SyncEstado.NO_SYNC)
                    .toList();
            for (Cita cita : conEvento) {
                cita.setGoogleEventId(null);
                cita.setGoogleCalendarId(null);
                cita.setGoogleEventIdDoctor(null);
                cita.setGoogleCalendarIdDoctor(null);
                cita.setSyncStatus(SyncEstado.NO_SYNC);
                cita.setSyncError(null);
                cita.setSyncAttempts(0);
                citaRepository.save(cita);
            }
            accountRepository.delete(cuenta.get());
            auditService.registrar("DESCONECTAR_GOOGLE", MODULO, "GOOGLE_ACCOUNT", cuenta.get().getId());
        }
    }

    @Transactional
    public GoogleSyncResponse sincronizar() {
        cuentaActivaOError();
        int creados = 0;
        int actualizados = 0;
        int cancelados = 0;
        int errores = 0;
        int sincronizadas = 0;

        for (Cita cita : citaRepository.findAll()) {
            try {
                if (cita.getEstado().esActiva() && cita.getEstado() != EstadoCita.NO_ASISTIO) {
                    if (cita.getGoogleEventId() == null) {
                        crearEvento(cita);
                        creados++;
                    } else {
                        actualizarEvento(cita);
                        actualizados++;
                    }
                    if (cita.getSyncStatus() == SyncEstado.SYNCED) {
                        sincronizadas++;
                    }
                } else if ((cita.getEstado() == EstadoCita.CANCELADA || cita.getEstado() == EstadoCita.NO_ASISTIO)
                        && cita.getGoogleEventId() != null) {
                    eliminarEvento(cita);
                    cancelados++;
                }
            } catch (Exception e) {
                errores++;
                log.warn("Error sincronizando cita {} con Google Calendar: {}", cita.getId(), e.getMessage());
            }
        }

        auditService.registrar("SINCRONIZAR_GOOGLE", MODULO, null, null);
        return new GoogleSyncResponse(creados, actualizados, cancelados, errores, sincronizadas);
    }

    // ------------------------------------------------------------------
    // Listeners: se ejecutan tras el COMMIT de crear/mover/cancelar una cita,
    // para no llamar a la API de Google dentro de la transacción (que podría
    // hacer rollback y dejar el evento externo sin compensación, o bloquear
    // la conexión de BD durante la llamada HTTP).
    // ------------------------------------------------------------------

    @TransactionalEventListener
    public void onCitaCreada(CitaCreadaEvent evento) {
        sincronizarPorEvento(evento.citaId(), false);
    }

    @TransactionalEventListener
    public void onCitaModificada(CitaModificadaEvent evento) {
        sincronizarPorEvento(evento.citaId(), true);
    }

    @TransactionalEventListener
    public void onCitaCancelada(CitaCanceladaEvent evento) {
        sincronizarPorEvento(evento.citaId(), false);
    }

    @TransactionalEventListener
    public void onCitaConfirmada(CitaConfirmadaEvent evento) {
        sincronizarPorEvento(evento.citaId(), true);
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    public void recordarCitasProximas() {
        LocalDate hoy = LocalDate.now();
        LocalDate fin = hoy.plusDays(2);
        List<Cita> citas = citaRepository.findByEstadoAndFechaBetween(
                EstadoCita.CONFIRMADA, hoy, fin);
        citas.forEach(cita -> {
            try {
                sincronizarPorEvento(cita.getId(), true);
            } catch (Exception e) {
                log.warn("No se pudo recordar cita {}", cita.getId(), e);
            }
        });
    }

    private void sincronizarPorEvento(Long citaId, boolean yaTieneEvento) {
        if (!googleService.estaConfigurado() || cuentaActiva().isEmpty()) {
            return;
        }
        Cita cita = citaRepository.findById(citaId).orElse(null);
        if (cita == null) {
            return;
        }
        try {
            if (cita.getEstado().esActiva()) {
                if (cita.getGoogleEventId() != null || yaTieneEvento) {
                    actualizarEvento(cita);
                } else {
                    crearEvento(cita);
                }
            } else if (cita.getEstado() == EstadoCita.CANCELADA || cita.getEstado() == EstadoCita.NO_ASISTIO) {
                if (cita.getGoogleEventId() != null) {
                    eliminarEvento(cita);
                }
            }
        } catch (Exception e) {
            log.warn("Sincronización automática falló para cita {}: {}", citaId, e.getMessage());
            marcarError(cita, e);
        }
    }

    // ------------------------------------------------------------------
    // Operaciones de evento (crear / actualizar / eliminar)
    // ------------------------------------------------------------------

    private void crearEvento(Cita cita) {
        GoogleAccount cuenta = cuentaActivaOError();
        GoogleCalendario cal = calendarioSeleccionado(cuenta);
        String accessToken = accesoValido(cuenta);
        String eventId = googleService.crearEvento(accessToken, cal.getCalendarId(), construirEvento(cita, cal));
        if (eventId == null || eventId.isBlank()) {
            throw new BusinessException("GOOGLE_API_ERROR", "Google no devolvió un id de evento");
        }
        cita.setGoogleEventId(eventId);
        cita.setGoogleCalendarId(cal.getCalendarId());
        marcarSincronizada(cita);
        sincronizarPersonal(cita);
    }

    private void actualizarEvento(Cita cita) {
        if (cita.getGoogleEventId() == null) {
            crearEvento(cita);
            return;
        }
        GoogleAccount cuenta = cuentaActivaOError();
        GoogleCalendario cal = calendarioSeleccionado(cuenta);
        String accessToken = accesoValido(cuenta);
        googleService.actualizarEvento(accessToken, cal.getCalendarId(), cita.getGoogleEventId(),
                construirEvento(cita, cal));
        cita.setGoogleCalendarId(cal.getCalendarId());
        marcarSincronizada(cita);
        sincronizarPersonal(cita);
    }

    private void eliminarEvento(Cita cita) {
        if (cita.getGoogleEventId() == null) {
            return;
        }
        GoogleAccount cuenta = cuentaActivaOError();
        GoogleCalendario cal = calendarioSeleccionado(cuenta);
        String accessToken = accesoValido(cuenta);
        googleService.eliminarEvento(accessToken, cal.getCalendarId(), cita.getGoogleEventId());
        cita.setGoogleEventId(null);
        cita.setGoogleCalendarId(null);
        cita.setSyncStatus(SyncEstado.NO_SYNC);
        cita.setSyncError(null);
        cita.setSyncAttempts(0);
        cita.setLastSyncAt(Instant.now());
        citaRepository.save(cita);
        eliminarPersonal(cita);
    }

    // ------------------------------------------------------------------
    // Calendario personal del odontólogo (además del principal del consultorio)
    // ------------------------------------------------------------------

    private void sincronizarPersonal(Cita cita) {
        if (cita.getDoctor() == null
                || cita.getDoctor().getGoogleCalendarId() == null
                || cita.getDoctor().getGoogleCalendarId().isBlank()) {
            return;
        }
        String calId = cita.getDoctor().getGoogleCalendarId();
        // El mismo calendario ya aloja el evento principal: no duplicar.
        if (calId.equals(cita.getGoogleCalendarId())) {
            return;
        }
        try {
            GoogleAccount cuenta = cuentaActivaOError();
            String accessToken = accesoValido(cuenta);
            EventoGoogle evento = construirEventoPersonal(cita);
            if (cita.getGoogleEventIdDoctor() == null) {
                String eventId = googleService.crearEvento(accessToken, calId, evento);
                if (eventId != null && !eventId.isBlank()) {
                    cita.setGoogleEventIdDoctor(eventId);
                    cita.setGoogleCalendarIdDoctor(calId);
                    citaRepository.save(cita);
                }
            } else {
                googleService.actualizarEvento(accessToken, cita.getGoogleCalendarIdDoctor(), cita.getGoogleEventIdDoctor(), evento);
                cita.setGoogleCalendarIdDoctor(calId);
                citaRepository.save(cita);
            }
        } catch (Exception e) {
            log.warn("No se pudo sincronizar el calendario personal '{}' del doctor de la cita {}: {}",
                    calId, cita.getId(), e.getMessage());
        }
    }

    private void eliminarPersonal(Cita cita) {
        if (cita.getGoogleEventIdDoctor() == null || cita.getGoogleCalendarIdDoctor() == null) {
            return;
        }
        try {
            GoogleAccount cuenta = cuentaActivaOError();
            String accessToken = accesoValido(cuenta);
            googleService.eliminarEvento(accessToken, cita.getGoogleCalendarIdDoctor(), cita.getGoogleEventIdDoctor());
            cita.setGoogleEventIdDoctor(null);
            cita.setGoogleCalendarIdDoctor(null);
            citaRepository.save(cita);
        } catch (Exception e) {
            log.warn("No se pudo eliminar el evento del calendario personal de la cita {}: {}", cita.getId(), e.getMessage());
        }
    }

    private EventoGoogle construirEventoPersonal(Cita cita) {
        String descripcion = "Servicio: " + cita.getServicio().getNombre();
        if (cita.getPaciente().getTelefono() != null && !cita.getPaciente().getTelefono().isBlank()) {
            descripcion += "\nTeléfono: " + cita.getPaciente().getTelefono();
        }
        return new EventoGoogle(
                "Cita - " + cita.getPaciente().getNombres() + " " + cita.getPaciente().getApellidos()
                        + " (" + cita.getServicio().getNombre() + ")",
                descripcion,
                cita.getFecha(), cita.getHoraInicio(), cita.getHoraFin(), timezone);
    }

    private void marcarSincronizada(Cita cita) {
        cita.setSyncStatus(SyncEstado.SYNCED);
        cita.setSyncError(null);
        cita.setSyncAttempts(0);
        cita.setLastSyncAt(Instant.now());
        citaRepository.save(cita);
    }

    private void marcarError(Cita cita, Exception e) {
        String msg = e.getMessage();
        if (msg != null && msg.length() > 500) {
            msg = msg.substring(0, 500);
        }
        cita.setSyncStatus(SyncEstado.ERROR);
        cita.setSyncError(msg);
        cita.setSyncAttempts((cita.getSyncAttempts() == null ? 0 : cita.getSyncAttempts()) + 1);
        cita.setLastSyncAt(Instant.now());
        citaRepository.save(cita);
    }

    private EventoGoogle construirEvento(Cita cita, GoogleCalendario cal) {
        String descripcion = "Servicio: " + cita.getServicio().getNombre()
                + "\nOdontólogo: " + cita.getDoctor().getNombres() + " " + cita.getDoctor().getApellidos();
        if (cita.getPaciente().getTelefono() != null && !cita.getPaciente().getTelefono().isBlank()) {
            descripcion += "\nTeléfono: " + cita.getPaciente().getTelefono();
        }
        String zona = cal.getTimeZone() != null ? cal.getTimeZone() : timezone;
        return new EventoGoogle(
                "Cita odontológica - " + cita.getPaciente().getNombres() + " " + cita.getPaciente().getApellidos(),
                descripcion,
                cita.getFecha(), cita.getHoraInicio(), cita.getHoraFin(), zona);
    }

    private String accesoValido(GoogleAccount cuenta) {
        if (cuenta.tokenExpirado(MARGEN_TOKEN_SEG)) {
            if (cuenta.getRefreshToken() == null || cuenta.getRefreshToken().isBlank()) {
                throw new BusinessException("GOOGLE_TOKEN_NO_REFRESCADO",
                        "El token de acceso expiró y no hay refresh token (reconecte la cuenta)");
            }
            GoogleTokenResponse tok = googleService.refrescarToken(cuenta.getRefreshToken());
            if (tok.accessToken() == null || tok.accessToken().isBlank()) {
                throw new BusinessException("GOOGLE_TOKEN_REFRESH_FAILED",
                        "Google rechazó el refresh token (reconecte la cuenta)");
            }
            cuenta.setAccessToken(tok.accessToken());
            if (tok.refreshToken() != null) {
                cuenta.setRefreshToken(tok.refreshToken());
            }
            cuenta.setExpiresAt(tok.expiresIn() != null
                    ? Instant.now().plus(tok.expiresIn(), ChronoUnit.SECONDS)
                    : null);
            accountRepository.save(cuenta);
        }
        return cuenta.getAccessToken();
    }

    private GoogleCalendario calendarioSeleccionado(GoogleAccount cuenta) {
        return calendarioRepository.findByCuentaIdAndSeleccionadoTrue(cuenta.getId())
                .orElseThrow(() -> new BusinessException("GOOGLE_SIN_CALENDARIO",
                        "No hay un calendario seleccionado para sincronizar las citas"));
    }

    private Optional<GoogleAccount> cuentaActiva() {
        return accountRepository.findTopByOrderByIdAsc();
    }

    private GoogleAccount cuentaActivaOError() {
        return cuentaActiva()
                .orElseThrow(() -> new BusinessException("GOOGLE_SIN_CUENTA",
                        "No hay una cuenta de Google conectada"));
    }
}