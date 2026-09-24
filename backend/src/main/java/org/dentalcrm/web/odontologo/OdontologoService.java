package org.dentalcrm.web.odontologo;

import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.odontologo.OdontologoRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.web.odontologo.dto.OdontologoRequest;
import org.dentalcrm.web.odontologo.dto.OdontologoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OdontologoService {

    private static final String MODULO = "ODONTOLOGOS";

    private final OdontologoRepository odontologoRepository;
    private final AuditService auditService;

    public OdontologoService(OdontologoRepository odontologoRepository, AuditService auditService) {
        this.odontologoRepository = odontologoRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<OdontologoResponse> listar(String q, String estado, int page, int size) {
        String query = q == null ? "" : q.trim();
        return odontologoRepository.buscar(query, estado, PageRequest.of(page, Math.min(size, 100)))
                .map(OdontologoResponse::from);
    }

    @Transactional(readOnly = true)
    public OdontologoResponse obtener(Long id) {
        return OdontologoResponse.from(buscarOArrojar(id));
    }

    @Transactional(readOnly = true)
    public List<OdontologoResponse> activos() {
        return odontologoRepository.findByEstadoOrderByNombresAsc("ACTIVO").stream()
                .map(OdontologoResponse::from)
                .toList();
    }

    @Transactional
    public OdontologoResponse crear(OdontologoRequest request) {
        Odontologo o = new Odontologo();
        aplicar(o, request);
        Odontologo creado = odontologoRepository.save(o);
        auditService.registrar("CREAR_ODONTOLOGO", MODULO, "ODONTOLOGO", creado.getId(), null, OdontologoResponse.from(creado));
        return OdontologoResponse.from(creado);
    }

    @Transactional
    public OdontologoResponse actualizar(Long id, OdontologoRequest request) {
        Odontologo o = buscarOArrojar(id);
        OdontologoResponse anterior = OdontologoResponse.from(o);
        aplicar(o, request);
        Odontologo actualizado = odontologoRepository.save(o);
        auditService.registrar("MODIFICAR_ODONTOLOGO", MODULO, "ODONTOLOGO", id, anterior, OdontologoResponse.from(actualizado));
        return OdontologoResponse.from(actualizado);
    }

    @Transactional
    public void desactivar(Long id) {
        Odontologo o = buscarOArrojar(id);
        if ("INACTIVO".equals(o.getEstado())) {
            throw new BusinessException("ODONTOLOGO_INACTIVO", "El odontólogo ya está inactivo");
        }
        OdontologoResponse anterior = OdontologoResponse.from(o);
        o.setEstado("INACTIVO");
        odontologoRepository.save(o);
        auditService.registrar("DESACTIVAR_ODONTOLOGO", MODULO, "ODONTOLOGO", id, anterior, OdontologoResponse.from(o));
    }

    private Odontologo buscarOArrojar(Long id) {
        return odontologoRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Odontólogo no encontrado"));
    }

    private void aplicar(Odontologo o, OdontologoRequest r) {
        o.setNombres(r.nombres().trim());
        o.setApellidos(r.apellidos().trim());
        o.setEspecialidad(r.especialidad());
        o.setTelefono(r.telefono() == null ? null : r.telefono().trim());
        o.setEmail(r.email() == null ? null : r.email().trim());
        o.setEtiquetas(r.etiquetas() == null ? null : r.etiquetas().trim());
        o.setGoogleCalendarId(r.googleCalendarId() == null ? null : r.googleCalendarId());
        if (r.estado() != null) {
            o.setEstado(r.estado());
        }
    }
}