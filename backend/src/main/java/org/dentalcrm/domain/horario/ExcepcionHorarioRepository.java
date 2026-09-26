package org.dentalcrm.domain.horario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExcepcionHorarioRepository extends JpaRepository<ExcepcionHorario, Long> {

    Optional<ExcepcionHorario> findByOdontologoIdAndFecha(Long odontologoId, LocalDate fecha);

    List<ExcepcionHorario> findByOdontologoIdAndFechaBetween(Long odontologoId, LocalDate desde, LocalDate hasta);

    List<ExcepcionHorario> findByFechaBetween(LocalDate desde, LocalDate hasta);
}
