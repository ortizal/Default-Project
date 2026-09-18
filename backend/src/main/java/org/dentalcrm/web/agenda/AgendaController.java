package org.dentalcrm.web.agenda;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.dentalcrm.web.agenda.dto.BloqueoRequest;
import org.dentalcrm.web.agenda.dto.BloqueoResponse;
import org.dentalcrm.web.agenda.dto.SlotResponse;
import org.dentalcrm.web.cita.dto.CitaResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/agenda")
@Tag(name = "Agenda", description = "Agenda del día, disponibilidad y bloqueos")
public class AgendaController {

    private final AgendaService agendaService;
    private final BloqueoService bloqueoService;

    public AgendaController(AgendaService agendaService, BloqueoService bloqueoService) {
        this.agendaService = agendaService;
        this.bloqueoService = bloqueoService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERMISO_AGENDA_READ')")
    @Operation(summary = "Citas del día para un odontólogo")
    public ResponseEntity<List<CitaResponse>> citasDelDia(
            @RequestParam Long odontologoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(agendaService.citasDelDia(odontologoId, fecha));
    }

    @GetMapping("/disponibilidad")
    @PreAuthorize("hasAuthority('PERMISO_AGENDA_READ')")
    @Operation(summary = "Slots disponibles para un odontólogo, fecha y servicio")
    public ResponseEntity<List<SlotResponse>> disponibilidad(
            @RequestParam Long odontologoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Long servicioId,
            @RequestParam(required = false) Integer duracion) {
        return ResponseEntity.ok(agendaService.disponibilidad(odontologoId, fecha, servicioId, duracion));
    }

    @GetMapping("/bloqueos")
    @PreAuthorize("hasAuthority('PERMISO_AGENDA_READ')")
    @Operation(summary = "Bloqueos por odontólogo (por fecha o rango)")
    public ResponseEntity<List<BloqueoResponse>> bloqueos(
            @RequestParam Long odontologoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        if (fecha != null) {
            return ResponseEntity.ok(bloqueoService.listarPorOdontologoYFecha(odontologoId, fecha));
        }
        LocalDate d = desde == null ? LocalDate.now() : desde;
        LocalDate h = hasta == null ? d.plusDays(30) : hasta;
        return ResponseEntity.ok(bloqueoService.listarPorRango(odontologoId, d, h));
    }

    @PostMapping("/bloqueos")
    @PreAuthorize("hasAuthority('PERMISO_AGENDA_WRITE')")
    @Operation(summary = "Crear bloqueo de agenda")
    public ResponseEntity<BloqueoResponse> crearBloqueo(@Valid @RequestBody BloqueoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bloqueoService.crear(request));
    }

    @DeleteMapping("/bloqueos/{id}")
    @PreAuthorize("hasAuthority('PERMISO_AGENDA_WRITE')")
    @Operation(summary = "Eliminar bloqueo de agenda")
    public ResponseEntity<Void> eliminarBloqueo(@PathVariable Long id) {
        bloqueoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}