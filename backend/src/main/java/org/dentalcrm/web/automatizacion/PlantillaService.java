package org.dentalcrm.web.automatizacion;

import org.dentalcrm.domain.automatizacion.PlantillaMensaje;
import org.dentalcrm.domain.automatizacion.PlantillaMensajeRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.web.automatizacion.dto.PlantillaRequest;
import org.dentalcrm.web.automatizacion.dto.PlantillaResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlantillaService {

    private static final String MODULO = "AUTOMATIZACIONES";

    private final PlantillaMensajeRepository plantillaRepository;
    private final AuditService auditService;

    public PlantillaService(PlantillaMensajeRepository plantillaRepository, AuditService auditService) {
        this.plantillaRepository = plantillaRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PlantillaResponse> listar() {
        return plantillaRepository.findAllByOrderByNombreAsc().stream()
                .map(PlantillaResponse::from).toList();
    }

    @Transactional
    public PlantillaResponse crear(PlantillaRequest request) {
        String nombre = request.nombre().trim();
        if (plantillaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new BusinessException("PLANTILLA_YA_EXISTE", "Ya existe una plantilla con el nombre '" + nombre + "'");
        }
        PlantillaMensaje p = new PlantillaMensaje();
        p.setNombre(nombre);
        p.setContenido(request.contenido());
        p.setActiva(request.activa() == null || request.activa());
        plantillaRepository.save(p);
        auditService.registrar("CREAR_PLANTILLA", MODULO, "PLANTILLA", p.getId());
        return PlantillaResponse.from(p);
    }

    @Transactional
    public PlantillaResponse actualizar(Long id, PlantillaRequest request) {
        PlantillaMensaje p = plantillaRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Plantilla no encontrada"));
        p.setNombre(request.nombre().trim());
        p.setContenido(request.contenido());
        p.setActiva(request.activa() == null || request.activa());
        plantillaRepository.save(p);
        auditService.registrar("ACTUALIZAR_PLANTILLA", MODULO, "PLANTILLA", id);
        return PlantillaResponse.from(p);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!plantillaRepository.existsById(id)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Plantilla no encontrada");
        }
        plantillaRepository.deleteById(id);
        auditService.registrar("ELIMINAR_PLANTILLA", MODULO, "PLANTILLA", id);
    }
}