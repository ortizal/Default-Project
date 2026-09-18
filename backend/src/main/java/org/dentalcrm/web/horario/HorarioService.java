package org.dentalcrm.web.horario;

import org.dentalcrm.domain.horario.HorarioOdontologo;
import org.dentalcrm.domain.horario.HorarioOdontologoRepository;
import org.dentalcrm.domain.odontologo.Odontologo;
import org.dentalcrm.domain.odontologo.OdontologoRepository;
import org.dentalcrm.exception.BusinessException;
import org.dentalcrm.service.AuditService;
import org.dentalcrm.web.horario.dto.HorarioRequest;
import org.dentalcrm.web.horario.dto.HorarioResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HorarioService {

    private static final String MODULO = "HORARIOS";

    private final HorarioOdontologoRepository horarioRepository;
    private final OdontologoRepository odontologoRepository;
    private final AuditService auditService;

    public HorarioService(HorarioOdontologoRepository horarioRepository,
                          OdontologoRepository odontologoRepository,
                          AuditService auditService) {
        this.horarioRepository = horarioRepository;
        this.odontologoRepository = odontologoRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<HorarioResponse> listarPorOdontologo(Long odontologoId) {
        return horarioRepository.findByOdontologoIdOrderByDiaSemanaAscHoraInicioAsc(odontologoId).stream()
                .map(HorarioResponse::from)
                .toList();
    }

    @Transactional
    public HorarioResponse crear(HorarioRequest request) {
        validar(request);
        HorarioOdontologo h = new HorarioOdontologo();
        aplicar(h, request);
        HorarioOdontologo creado = horarioRepository.save(h);
        auditService.registrar("CREAR_HORARIO", MODULO, "HORARIO_ODONTOLOGO", creado.getId(), null, HorarioResponse.from(creado));
        return HorarioResponse.from(creado);
    }

    @Transactional
    public HorarioResponse actualizar(Long id, HorarioRequest request) {
        validar(request);
        HorarioOdontologo h = buscarOArrojar(id);
        HorarioResponse anterior = HorarioResponse.from(h);
        aplicar(h, request);
        HorarioOdontologo actualizado = horarioRepository.save(h);
        auditService.registrar("MODIFICAR_HORARIO", MODULO, "HORARIO_ODONTOLOGO", id, anterior, HorarioResponse.from(actualizado));
        return HorarioResponse.from(actualizado);
    }

    @Transactional
    public void eliminar(Long id) {
        HorarioOdontologo h = buscarOArrojar(id);
        horarioRepository.delete(h);
        auditService.registrar("ELIMINAR_HORARIO", MODULO, "HORARIO_ODONTOLOGO", id, HorarioResponse.from(h), null);
    }

    private void validar(HorarioRequest r) {
        if (!r.horaFin().isAfter(r.horaInicio())) {
            throw new BusinessException("HORARIO_INVALIDO", "La hora de fin debe ser posterior a la de inicio");
        }
        if (!odontologoRepository.existsById(r.odontologoId())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Odontólogo no encontrado");
        }
    }

    private void aplicar(HorarioOdontologo h, HorarioRequest r) {
        if (h.getOdontologo() == null || !h.getOdontologo().getId().equals(r.odontologoId())) {
            Odontologo o = odontologoRepository.findById(r.odontologoId()).orElseThrow();
            h.setOdontologo(o);
        }
        h.setDiaSemana(r.diaSemana());
        h.setHoraInicio(r.horaInicio());
        h.setHoraFin(r.horaFin());
        h.setIntervaloMinutos(r.intervaloMinutos() == null ? 30 : r.intervaloMinutos());
        if (r.estado() != null) {
            h.setEstado(r.estado());
        }
    }

    private HorarioOdontologo buscarOArrojar(Long id) {
        return horarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "Horario no encontrado"));
    }
}