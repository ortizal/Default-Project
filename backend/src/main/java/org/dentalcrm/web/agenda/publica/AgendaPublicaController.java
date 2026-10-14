package org.dentalcrm.web.agenda.publica;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.dentalcrm.web.agenda.publica.dto.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Endpoints públicos con token temporal (§13); sin sesión ni JWT. */
@RestController
@RequestMapping("/api/v1/public/agenda")
@Tag(name = "Agenda pública", description = "Calendario de citas con enlace temporal")
public class AgendaPublicaController {

    private final AgendaPublicaService service;

    public AgendaPublicaController(AgendaPublicaService service) {
        this.service = service;
    }

    @Operation(summary = "Contexto del enlace (odontólogo y nombre del paciente)")
    @GetMapping("/context")
    public ResponseEntity<AgendaContextResponse> context(@RequestParam("t") String token) {
        return ResponseEntity.ok(service.contexto(token));
    }

    @Operation(summary = "Días del mes con estado de disponibilidad")
    @GetMapping("/dias")
    public ResponseEntity<List<AgendaDiaResponse>> dias(@RequestParam("t") String token,
                                                        @RequestParam int year,
                                                        @RequestParam int month) {
        return ResponseEntity.ok(service.dias(token, year, month));
    }

    @Operation(summary = "Cupos libres de un día")
    @GetMapping("/slots")
    public ResponseEntity<List<AgendaSlotResponse>> slots(@RequestParam("t") String token,
                                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(service.slots(token, fecha));
    }

    @Operation(summary = "Confirma el cupo elegido (con revalidación e idempotencia)")
    @PostMapping("/reservar")
    public ResponseEntity<AgendaReservarResponse> reservar(@RequestBody AgendaReservarRequest request) {
        return ResponseEntity.ok(service.reservar(request));
    }
}
