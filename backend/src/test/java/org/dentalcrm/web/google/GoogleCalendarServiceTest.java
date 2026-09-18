package org.dentalcrm.web.google;

import org.dentalcrm.domain.cita.Cita;
import org.dentalcrm.domain.cita.CitaCanceladaEvent;
import org.dentalcrm.domain.cita.CitaCreadaEvent;
import org.dentalcrm.domain.cita.CitaRepository;
import org.dentalcrm.domain.cita.EstadoCita;
import org.dentalcrm.domain.cita.SyncEstado;
import org.dentalcrm.domain.google.GoogleAccount;
import org.dentalcrm.domain.google.GoogleAccountRepository;
import org.dentalcrm.domain.google.GoogleCalendario;
import org.dentalcrm.domain.google.GoogleCalendarioRepository;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.web.google.GoogleService.CalendarioRemoto;
import org.dentalcrm.web.google.GoogleService.GoogleTokenResponse;
import org.dentalcrm.web.google.dto.GoogleStatusResponse;
import org.dentalcrm.web.google.dto.GoogleSyncResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoogleCalendarServiceTest {

    private GoogleAccountRepository accountRepo;
    private GoogleCalendarioRepository calendarioRepo;
    private CitaRepository citaRepo;
    private GoogleService googleService;
    private GoogleCalendarService servicio;

    @BeforeEach
    void setUp() {
        accountRepo = mock(GoogleAccountRepository.class);
        calendarioRepo = mock(GoogleCalendarioRepository.class);
        citaRepo = mock(CitaRepository.class);
        googleService = mock(GoogleService.class);
        servicio = new GoogleCalendarService(accountRepo, calendarioRepo, citaRepo,
                googleService, mock(AuditService.class), "America/Guayaquil");
    }

    @Test
    void completarConexionGuardaCuentaYSeleccionaPrimary() {
        when(googleService.estaConfigurado()).thenReturn(true);
        when(googleService.intercambiarCodigo("codigo")).thenReturn(new GoogleTokenResponse("tok", "ref", 3600, "scope"));
        when(googleService.obtenerEmail("tok")).thenReturn("doctor@clinica.com");
        when(googleService.listarCalendarios("tok")).thenReturn(List.of(
                new CalendarioRemoto("primary", "Dr. López", "America/Guayaquil"),
                new CalendarioRemoto("personal", "Personal", "America/Guayaquil")));
        when(accountRepo.findByEmailIgnoreCase("doctor@clinica.com")).thenReturn(Optional.empty());
        when(accountRepo.findTopByOrderByIdAsc()).thenReturn(Optional.of(cuentaCon(1L)));
        when(calendarioRepo.findByCuentaIdAndSeleccionadoTrue(anyLong())).thenReturn(Optional.empty());
        when(calendarioRepo.findByCuentaIdOrderBySummaryAsc(anyLong())).thenReturn(List.of());
        when(calendarioRepo.save(any(GoogleCalendario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(citaRepo.countBySyncStatus(any(SyncEstado.class))).thenReturn(0L);

        GoogleStatusResponse resp = servicio.completarConexion("codigo");

        ArgumentCaptor<GoogleCalendario> captor = ArgumentCaptor.forClass(GoogleCalendario.class);
        verify(calendarioRepo, org.mockito.Mockito.atLeast(2)).save(captor.capture());
        GoogleCalendario seleccionado = captor.getAllValues().stream()
                .filter(c -> Boolean.TRUE.equals(c.getSeleccionado()))
                .findFirst().orElseThrow();
        assertEquals("primary", seleccionado.getCalendarId());

        assertTrue(resp.conectada());
        assertEquals("doctor@clinica.com", resp.email());
    }

    @Test
    void completarConexionSinPrimarySeleccionaPrimero() {
        when(googleService.estaConfigurado()).thenReturn(true);
        when(googleService.intercambiarCodigo("codigo")).thenReturn(new GoogleTokenResponse("tok", "ref", 3600, "s"));
        when(googleService.obtenerEmail("tok")).thenReturn("a@b.com");
        when(googleService.listarCalendarios("tok")).thenReturn(List.of(
                new CalendarioRemoto("vacaciones", "Vacaciones", "UTC")));
        when(accountRepo.findByEmailIgnoreCase("a@b.com")).thenReturn(Optional.empty());
        when(accountRepo.findTopByOrderByIdAsc()).thenReturn(Optional.of(cuentaCon(1L)));
        when(calendarioRepo.findByCuentaIdAndSeleccionadoTrue(anyLong())).thenReturn(Optional.empty());
        when(calendarioRepo.findByCuentaIdOrderBySummaryAsc(anyLong())).thenReturn(List.of());
        when(calendarioRepo.save(any(GoogleCalendario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(citaRepo.countBySyncStatus(any(SyncEstado.class))).thenReturn(0L);

        servicio.completarConexion("codigo");

        ArgumentCaptor<GoogleCalendario> captor = ArgumentCaptor.forClass(GoogleCalendario.class);
        verify(calendarioRepo, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        assertTrue(captor.getAllValues().stream().allMatch(c -> Boolean.TRUE.equals(c.getSeleccionado())));
    }

    @Test
    void seleccionarCalendarioDesmarcaElAnterior() {
        GoogleAccount cuenta = cuentaCon(1L);
        when(accountRepo.findTopByOrderByIdAsc()).thenReturn(Optional.of(cuenta));

        GoogleCalendario nuevo = calendario(2L, cuenta, "personal", false);
        when(calendarioRepo.findById(2L)).thenReturn(Optional.of(nuevo));

        servicio.seleccionarCalendario(2L);

        verify(calendarioRepo).desmarcarTodos(1L);
        assertTrue(nuevo.getSeleccionado());
    }

    @Test
    void citaCreadaSincronizaYQuedaSYNCED() {
        GoogleAccount cuenta = cuentaCon(1L);
        GoogleCalendario cal = calendario(1L, cuenta, "primary", true);
        cal.setTimeZone("America/Guayaquil");
        when(googleService.estaConfigurado()).thenReturn(true);
        when(accountRepo.findTopByOrderByIdAsc()).thenReturn(Optional.of(cuenta));
        when(calendarioRepo.findByCuentaIdAndSeleccionadoTrue(1L)).thenReturn(Optional.of(cal));
        when(googleService.crearEvento(anyString(), anyString(), any(GoogleService.EventoGoogle.class)))
                .thenReturn("evt-123");
        when(citaRepo.findById(1L)).thenReturn(Optional.of(citaCompleta(1L, EstadoCita.PENDIENTE)));
        when(citaRepo.save(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));

        servicio.onCitaCreada(new CitaCreadaEvent(1L));

        ArgumentCaptor<Cita> captor = ArgumentCaptor.forClass(Cita.class);
        verify(citaRepo).save(captor.capture());
        assertEquals("evt-123", captor.getValue().getGoogleEventId());
        assertEquals("primary", captor.getValue().getGoogleCalendarId());
        assertEquals(SyncEstado.SYNCED, captor.getValue().getSyncStatus());
    }

    @Test
    void errorEnSyncMarcaCitaERROR() {
        GoogleAccount cuenta = cuentaCon(1L);
        GoogleCalendario cal = calendario(1L, cuenta, "primary", true);
        when(googleService.estaConfigurado()).thenReturn(true);
        when(accountRepo.findTopByOrderByIdAsc()).thenReturn(Optional.of(cuenta));
        when(calendarioRepo.findByCuentaIdAndSeleccionadoTrue(1L)).thenReturn(Optional.of(cal));
        when(googleService.crearEvento(anyString(), anyString(), any(GoogleService.EventoGoogle.class)))
                .thenThrow(new org.dentalcrm.exception.BusinessException("GOOGLE_API_ERROR", "falla simulada"));
        when(citaRepo.findById(1L)).thenReturn(Optional.of(citaCompleta(1L, EstadoCita.PENDIENTE)));
        when(citaRepo.save(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));

        servicio.onCitaCreada(new CitaCreadaEvent(1L));

        ArgumentCaptor<Cita> captor = ArgumentCaptor.forClass(Cita.class);
        verify(citaRepo).save(captor.capture());
        assertEquals(SyncEstado.ERROR, captor.getValue().getSyncStatus());
        assertTrue(captor.getValue().getSyncError().contains("falla simulada"));
    }

    @Test
    void citaCanceladaEliminaEvento() {
        GoogleAccount cuenta = cuentaCon(1L);
        GoogleCalendario cal = calendario(1L, cuenta, "primary", true);
        Cita cita = citaCompleta(1L, EstadoCita.CANCELADA);
        cita.setGoogleEventId("evt-999");
        when(googleService.estaConfigurado()).thenReturn(true);
        when(accountRepo.findTopByOrderByIdAsc()).thenReturn(Optional.of(cuenta));
        when(calendarioRepo.findByCuentaIdAndSeleccionadoTrue(1L)).thenReturn(Optional.of(cal));
        when(citaRepo.findById(1L)).thenReturn(Optional.of(cita));
        when(citaRepo.save(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));

        servicio.onCitaCancelada(new CitaCanceladaEvent(1L));

        verify(googleService).eliminarEvento("tok", "primary", "evt-999");
        ArgumentCaptor<Cita> captor = ArgumentCaptor.forClass(Cita.class);
        verify(citaRepo).save(captor.capture());
        assertNull(captor.getValue().getGoogleEventId());
        assertEquals(SyncEstado.NO_SYNC, captor.getValue().getSyncStatus());
    }

    @Test
    void sincronizadorBulkCuentaPorEstado() {
        GoogleAccount cuenta = cuentaCon(1L);
        GoogleCalendario cal = calendario(1L, cuenta, "primary", true);
        when(accountRepo.findTopByOrderByIdAsc()).thenReturn(Optional.of(cuenta));
        when(calendarioRepo.findByCuentaIdAndSeleccionadoTrue(1L)).thenReturn(Optional.of(cal));
        when(googleService.crearEvento(anyString(), anyString(), any(GoogleService.EventoGoogle.class)))
                .thenReturn("evt-nuevo");

        Cita pendienteSinEvento = citaCompleta(1L, EstadoCita.PENDIENTE);
        Cita confirmadaConEvento = citaCompleta(2L, EstadoCita.CONFIRMADA);
        confirmadaConEvento.setGoogleEventId("evt-existente");
        Cita canceladaConEvento = citaCompleta(3L, EstadoCita.CANCELADA);
        canceladaConEvento.setGoogleEventId("evt-cancelar");
        when(citaRepo.findAll()).thenReturn(List.of(pendienteSinEvento, confirmadaConEvento, canceladaConEvento));
        when(citaRepo.save(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));

        GoogleSyncResponse resp = servicio.sincronizar();

        assertEquals(1, resp.creados());
        assertEquals(1, resp.actualizados());
        assertEquals(1, resp.cancelados());
        assertEquals(0, resp.errores());
        verify(googleService).eliminarEvento("tok", "primary", "evt-cancelar");
    }

    @Test
    void desconectarLimpiaTokensYCitas() {
        GoogleAccount cuenta = cuentaCon(1L);
        Cita cita = citaCompleta(1L, EstadoCita.PENDIENTE);
        cita.setGoogleEventId("evt-1");
        cita.setSyncStatus(SyncEstado.SYNCED);
        when(accountRepo.findTopByOrderByIdAsc()).thenReturn(Optional.of(cuenta));
        when(citaRepo.findAll()).thenReturn(List.of(cita));

        servicio.desconectar();

        verify(accountRepo).delete(cuenta);
        assertNull(cita.getGoogleEventId());
        assertEquals(SyncEstado.NO_SYNC, cita.getSyncStatus());
    }

    private GoogleAccount cuentaCon(Long id) {
        GoogleAccount a = new GoogleAccount();
        a.setId(id);
        a.setEmail("doctor@clinica.com");
        a.setAccessToken("tok");
        a.setRefreshToken("ref");
        a.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        return a;
    }

    private GoogleCalendario calendario(Long id, GoogleAccount cuenta, String calendarId, boolean seleccionado) {
        GoogleCalendario c = new GoogleCalendario();
        c.setId(id);
        c.setCuenta(cuenta);
        c.setCalendarId(calendarId);
        c.setSeleccionado(seleccionado);
        return c;
    }

    private Cita citaCompleta(Long id, EstadoCita estado) {
        Cita c = new Cita();
        c.setId(id);
        c.setEstado(estado);
        if (estado != EstadoCita.CANCELADA && estado != EstadoCita.NO_ASISTIO) {
            c.setSyncStatus(SyncEstado.NO_SYNC);
            c.setSyncAttempts(0);
        }
        org.dentalcrm.domain.paciente.Paciente p = new org.dentalcrm.domain.paciente.Paciente();
        p.setNombres("Juan");
        p.setApellidos("Pérez");
        p.setTelefono("+593999999");
        c.setPaciente(p);
        org.dentalcrm.domain.odontologo.Odontologo d = new org.dentalcrm.domain.odontologo.Odontologo();
        d.setNombres("María");
        d.setApellidos("López");
        c.setDoctor(d);
        org.dentalcrm.domain.servicio.Servicio s = new org.dentalcrm.domain.servicio.Servicio();
        s.setNombre("Limpieza");
        c.setServicio(s);
        c.setFecha(java.time.LocalDate.of(2026, 9, 16));
        c.setHoraInicio(java.time.LocalTime.of(10, 30));
        c.setHoraFin(java.time.LocalTime.of(11, 30));
        return c;
    }
}