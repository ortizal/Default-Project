package org.dentalcrm.web.auditoria;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dentalcrm.domain.auditoria.Auditoria;
import org.dentalcrm.domain.auditoria.AuditoriaRepository;
import org.dentalcrm.web.auditoria.dto.AuditoriaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
    private final ObjectMapper objectMapper;

    public AuditoriaService(AuditoriaRepository auditoriaRepository, ObjectMapper objectMapper) {
        this.auditoriaRepository = auditoriaRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Page<AuditoriaResponse> listar(String q, String modulo, int page, int size) {
        String query = q == null ? "" : q.trim();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return auditoriaRepository.buscar(query, modulo, pageable).map(this::toResponse);
    }

    private AuditoriaResponse toResponse(Auditoria a) {
        return new AuditoriaResponse(
                a.getId(),
                a.getUsuarioId(),
                a.getAccion(),
                a.getModulo(),
                a.getEntidad(),
                a.getEntidadId(),
                parse(a.getDatosAnteriores()),
                parse(a.getDatosNuevos()),
                a.getIp(),
                a.getCreatedAt());
    }

    private JsonNode parse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return objectMapper.getNodeFactory().textNode(json);
        }
    }
}
