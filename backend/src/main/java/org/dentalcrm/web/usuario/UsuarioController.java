package org.dentalcrm.web.usuario;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.dentalcrm.web.usuario.dto.UsuarioCreateRequest;
import org.dentalcrm.web.usuario.dto.UsuarioResponse;
import org.dentalcrm.web.usuario.dto.UsuarioUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuarios", description = "Gestión de usuarios del sistema")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERMISO_USUARIOS_READ')")
    @Operation(summary = "Listar usuarios")
    public ResponseEntity<Page<UsuarioResponse>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(usuarioService.listar(q, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISO_USUARIOS_READ')")
    @Operation(summary = "Obtener usuario por id")
    public ResponseEntity<UsuarioResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERMISO_USUARIOS_WRITE')")
    @Operation(summary = "Crear usuario")
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISO_USUARIOS_WRITE')")
    @Operation(summary = "Editar usuario")
    public ResponseEntity<UsuarioResponse> actualizar(@PathVariable Long id,
                                                      @Valid @RequestBody UsuarioUpdateRequest request) {
        return ResponseEntity.ok(usuarioService.actualizar(id, request));
    }

    @PostMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('PERMISO_USUARIOS_WRITE')")
    @Operation(summary = "Cambiar estado del usuario (activar/desactivar)")
    public ResponseEntity<UsuarioResponse> cambiarEstado(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.cambiarEstado(id));
    }
}