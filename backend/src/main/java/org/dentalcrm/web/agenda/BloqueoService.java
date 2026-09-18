package org.dentalcrm.web.agenda;

import org.dentalcrm.domain.bloqueo.BloqueoAgenda;
import org.dentalcrm.domain.bloqueo.BloqueoAgendaRepository;
import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.odontologo.OdontologoRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.service.CurrentUserService;
import org.dentalcrm.web.agenda.dto.BloqueoRequest;
import org.dentalcrm.web.agenda.dto.BloqueoResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class BloqueoService {

    private static final String MODULO = "AGENDA";

    private final BloqueoAgendaRepository bloqueoRepository;
    private final OdontologoRepository odontologoRepository;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    public BloqueoService(BloqueoAgendaRepository bloqueoRepository,
                          OdontologoRepository odontologoRepository,
                          AuditService auditService,
                          CurrentUserService currentUserService) {
        this.bloqueoRepository = bloqueoRepository;
        this.odontologoRepository = odontologoRepository;
        this.auditService = auditService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<BloqueoResponse> listarPorOdontologoYFecha(Long odontologoId, LocalDate fecha) {
        return bloqueoRepository.findByOdontologoIdAndFecha(odontologoId, fecha).stream()
                .map(BloqueoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BloqueoResponse> listarPorRango(Long odontologoId, LocalDate desde, LocalDate hasta) {
        return bloqueoRepository.findByOdontologoIdAndFechaBetween(odontologoId, desde, hasta).stream()
                .map(BloqueoResponse::from)
                .toList();
    }

    @Transactional
    public BloqueoResponse crear(BloqueoRequest request) {
        Odontologo o = odontologoRepository.findById(request.odontologoId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Odontólogo no encontrado"));
        if (!request.esDiaCompleto() && !request.horaFin().isAfter(request.horaInicio())) {
            throw new BusinessException("BLOQUEO_INVALIDO", "La hora de fin debe ser posterior a la de inicio");
        }

        BloqueoAgenda b = new BloqueoAgenda();
        b.setOdontologo(o);
        b.setFecha(request.fecha());
        b.setHoraInicio(request.horaInicio());
        b.setHoraFin(request.horaFin());
        b.setMotivo(request.motivo());
        currentUserService.idUsuarioActual().ifPresent(b::setCreatedBy);
        BloqueoAgenda creado = bloqueoRepository.save(b);
        auditService.registrar("CREAR_BLOQUEO", MODULO, "BLOQUEO_AGENDA", creado.getId(), null, BloqueoResponse.from(creado));
        return BloqueoResponse.from(creado);
    }

    @Transactional
    public void eliminar(Long id) {
        BloqueoAgenda b = bloqueoRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Bloqueo no encontrado"));
        bloqueoRepository.delete(b);
        auditService.registrar("ELIMINAR_BLOQUEO", MODULO, "BLOQUEO_AGENDA", id, BloqueoResponse.from(b), null);
    }
}