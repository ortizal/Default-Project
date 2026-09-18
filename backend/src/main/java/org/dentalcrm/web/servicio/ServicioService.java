package org.dentalcrm.web.servicio;

import org.dentalcrm.domain.servicio.Servicio;
import org.dentalcrm.domain.servicio.ServicioRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.web.servicio.dto.ServicioRequest;
import org.dentalcrm.web.servicio.dto.ServicioResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServicioService {

    private static final String MODULO = "SERVICIOS";

    private final ServicioRepository servicioRepository;
    private final AuditService auditService;

    public ServicioService(ServicioRepository servicioRepository, AuditService auditService) {
        this.servicioRepository = servicioRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<ServicioResponse> listar(String q, String estado, int page, int size) {
        String query = q == null ? "" : q.trim();
        return servicioRepository.buscar(query, estado, PageRequest.of(page, Math.min(size, 100)))
                .map(ServicioResponse::from);
    }

    @Transactional(readOnly = true)
    public ServicioResponse obtener(Long id) {
        return ServicioResponse.from(buscarOArrojar(id));
    }

    @Transactional(readOnly = true)
    public List<ServicioResponse> activos() {
        return servicioRepository.findByEstadoOrderByNombreAsc("ACTIVO").stream()
                .map(ServicioResponse::from)
                .toList();
    }

    @Transactional
    public ServicioResponse crear(ServicioRequest request) {
        validarNombreUnico(request.nombre(), null);
        Servicio s = new Servicio();
        aplicar(s, request);
        Servicio creado = servicioRepository.save(s);
        auditService.registrar("CREAR_SERVICIO", MODULO, "SERVICIO", creado.getId(), null, ServicioResponse.from(creado));
        return ServicioResponse.from(creado);
    }

    @Transactional
    public ServicioResponse actualizar(Long id, ServicioRequest request) {
        Servicio s = buscarOArrojar(id);
        validarNombreUnico(request.nombre(), id);
        ServicioResponse anterior = ServicioResponse.from(s);
        aplicar(s, request);
        Servicio actualizado = servicioRepository.save(s);
        auditService.registrar("MODIFICAR_SERVICIO", MODULO, "SERVICIO", id, anterior, ServicioResponse.from(actualizado));
        return ServicioResponse.from(actualizado);
    }

    @Transactional
    public void desactivar(Long id) {
        Servicio s = buscarOArrojar(id);
        if ("INACTIVO".equals(s.getEstado())) {
            throw new BusinessException("SERVICIO_INACTIVO", "El servicio ya está inactivo");
        }
        ServicioResponse anterior = ServicioResponse.from(s);
        s.setEstado("INACTIVO");
        servicioRepository.save(s);
        auditService.registrar("DESACTIVAR_SERVICIO", MODULO, "SERVICIO", id, anterior, ServicioResponse.from(s));
    }

    private Servicio buscarOArrojar(Long id) {
        return servicioRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Servicio no encontrado"));
    }

    private void validarNombreUnico(String nombre, Long idActual) {
        if (nombre == null || nombre.isBlank()) {
            return;
        }
        String n = nombre.trim();
        boolean existe = idActual == null
                ? servicioRepository.findByNombreIgnoreCase(n).isPresent()
                : servicioRepository.existsByNombreIgnoreCaseAndIdNot(n, idActual);
        if (existe) {
            throw new BusinessException("NOMBRE_DUPLICADO", "Ya existe un servicio con el nombre " + n);
        }
    }

    private void aplicar(Servicio s, ServicioRequest r) {
        s.setNombre(r.nombre().trim());
        s.setDescripcion(r.descripcion());
        s.setDuracionMinutos(r.duracionMinutos());
        s.setPrecio(r.precio());
        if (r.estado() != null) {
            s.setEstado(r.estado());
        }
    }
}