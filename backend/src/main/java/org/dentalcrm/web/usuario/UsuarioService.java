package org.dentalcrm.web.usuario;

import org.dentalcrm.domain.usuario.Rol;
import org.dentalcrm.domain.usuario.RolRepository;
import org.dentalcrm.domain.usuario.Usuario;
import org.dentalcrm.domain.usuario.UsuarioRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.web.usuario.dto.UsuarioCreateRequest;
import org.dentalcrm.web.usuario.dto.UsuarioResponse;
import org.dentalcrm.web.usuario.dto.UsuarioUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class UsuarioService {

    private static final String MODULO = "USUARIOS";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          PasswordEncoder passwordEncoder,
                          AuditService auditService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(String q, int page, int size) {
        String query = q == null ? "" : q.trim();
        return usuarioRepository.buscar(query, PageRequest.of(page, Math.min(size, 100)))
                .map(UsuarioResponse::from);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return UsuarioResponse.from(buscarOArrojar(id));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioCreateRequest request) {
        if (usuarioRepository.existeGlobalUsername(request.username())) {
            throw new BusinessException("USERNAME_DUPLICADO", "El username ya existe");
        }
        if (usuarioRepository.existeGlobalEmail(request.email())) {
            throw new BusinessException("EMAIL_DUPLICADO", "El email ya existe");
        }

        Usuario u = new Usuario();
        u.setUsername(request.username().trim());
        u.setEmail(request.email().trim());
        u.setPassword(passwordEncoder.encode(request.password()));
        u.setNombres(request.nombres().trim());
        u.setApellidos(request.apellidos().trim());
        u.setTelefono(request.telefono());
        u.setEstado("ACTIVO");
        u.setRoles(resolverRoles(request.roles()));
        Usuario creado = usuarioRepository.save(u);
        auditService.registrar("CREAR_USUARIO", MODULO, "USUARIO", creado.getId(), null, UsuarioResponse.from(creado));
        return UsuarioResponse.from(creado);
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioUpdateRequest request) {
        Usuario u = buscarOArrojar(id);
        UsuarioResponse anterior = UsuarioResponse.from(u);

        if (request.email() != null && !request.email().isBlank()) {
            usuarioRepository.buscarGlobalPorEmail(request.email().trim())
                    .filter(existente -> !existente.getId().equals(id))
                    .ifPresent(existente -> { throw new BusinessException("EMAIL_DUPLICADO", "El email ya existe"); });
            u.setEmail(request.email().trim());
        }
        u.setNombres(request.nombres().trim());
        u.setApellidos(request.apellidos().trim());
        u.setTelefono(request.telefono());
        if (request.estado() != null) {
            u.setEstado(request.estado());
        }
        if (u.getPassword() != null && request.nuevaPassword() != null && !request.nuevaPassword().isBlank()) {
            u.setPassword(passwordEncoder.encode(request.nuevaPassword()));
        }
        if (request.roles() != null && !request.roles().isEmpty()) {
            u.setRoles(resolverRoles(request.roles()));
        }

        Usuario actualizado = usuarioRepository.save(u);
        auditService.registrar("MODIFICAR_USUARIO", MODULO, "USUARIO", id, anterior, UsuarioResponse.from(actualizado));
        return UsuarioResponse.from(actualizado);
    }

    @Transactional
    public UsuarioResponse cambiarEstado(Long id) {
        Usuario u = buscarOArrojar(id);
        String nuevo = "ACTIVO".equals(u.getEstado()) ? "INACTIVO" : "ACTIVO";
        UsuarioResponse anterior = UsuarioResponse.from(u);
        u.setEstado(nuevo);
        Usuario actualizado = usuarioRepository.save(u);
        auditService.registrar("CAMBIO_ESTADO_USUARIO", MODULO, "USUARIO", id, anterior, UsuarioResponse.from(actualizado));
        return UsuarioResponse.from(actualizado);
    }

    private Usuario buscarOArrojar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Usuario no encontrado"));
    }

    private Set<Rol> resolverRoles(Set<String> codigos) {
        Set<Rol> roles = new HashSet<>();
        for (String codigo : codigos) {
            Rol rol = rolRepository.findByCodigoIgnoreCase(codigo.trim())
                    .orElseThrow(() -> new BusinessException("ROL_NO_ENCONTRADO", "El rol " + codigo + " no existe"));
            roles.add(rol);
        }
        return roles;
    }
}