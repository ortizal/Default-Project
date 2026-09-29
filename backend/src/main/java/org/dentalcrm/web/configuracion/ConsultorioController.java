package org.dentalcrm.web.configuracion;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.dentalcrm.web.configuracion.dto.ConsultorioRequest;
import org.dentalcrm.web.configuracion.dto.ConsultorioResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/configuracion/consultorio")
@Tag(name = "Configuración del consultorio")
@PreAuthorize("hasAuthority('PERMISO_CONFIGURACION')")
public class ConsultorioController {

    private final ConsultorioService consultorioService;

    public ConsultorioController(ConsultorioService consultorioService) {
        this.consultorioService = consultorioService;
    }

    @GetMapping
    public ResponseEntity<ConsultorioResponse> obtener() {
        return ResponseEntity.ok(consultorioService.obtener());
    }

    @PutMapping
    public ResponseEntity<ConsultorioResponse> actualizar(@Valid @RequestBody ConsultorioRequest request) {
        return ResponseEntity.ok(consultorioService.actualizar(request));
    }
}