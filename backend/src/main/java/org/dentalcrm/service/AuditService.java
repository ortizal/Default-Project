package org.dentalcrm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dentalcrm.domain.auditoria.Auditoria;
import org.dentalcrm.domain.auditoria.AuditoriaRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditoriaRepository auditoriaRepository;
    private final ObjectMapper objectMapper;
    private final CurrentUserService currentUserService;

    public AuditService(AuditoriaRepository auditoriaRepository,
                        ObjectMapper objectMapper,
                        CurrentUserService currentUserService) {
        this.auditoriaRepository = auditoriaRepository;
        this.objectMapper = objectMapper;
        this.currentUserService = currentUserService;
    }

    public void registrar(String accion, String modulo, String entidad, Long entidadId,
                          Object datosAnteriores, Object datosNuevos) {
        Auditoria auditoria = new Auditoria();
        currentUserService.idUsuarioActual().ifPresent(auditoria::setUsuarioId);
        auditoria.setAccion(accion);
        auditoria.setModulo(modulo);
        auditoria.setEntidad(entidad);
        auditoria.setEntidadId(entidadId);
        auditoria.setDatosAnteriores(toJson(datosAnteriores));
        auditoria.setDatosNuevos(toJson(datosNuevos));
        auditoriaRepository.save(auditoria);
    }

    public void registrar(String accion, String modulo, String entidad, Long entidadId) {
        registrar(accion, modulo, entidad, entidadId, null, null);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}