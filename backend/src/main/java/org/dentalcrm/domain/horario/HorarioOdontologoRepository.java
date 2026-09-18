package org.dentalcrm.domain.horario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HorarioOdontologoRepository extends JpaRepository<HorarioOdontologo, Long> {

    List<HorarioOdontologo> findByOdontologoIdOrderByDiaSemanaAscHoraInicioAsc(Long odontologoId);

    List<HorarioOdontologo> findByOdontologoIdAndDiaSemanaAndEstado(Long odontologoId, Integer diaSemana, String estado);
}