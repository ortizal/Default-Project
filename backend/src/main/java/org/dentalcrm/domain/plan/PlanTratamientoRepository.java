package org.dentalcrm.domain.plan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanTratamientoRepository extends JpaRepository<PlanTratamiento, Long> {

    List<PlanTratamiento> findByPacienteIdOrderByFechaDescIdDesc(Long pacienteId);

    Optional<PlanTratamiento> findByIdAndPacienteId(Long id, Long pacienteId);

    long countByPacienteId(Long pacienteId);

    long countByFechaBetween(java.time.LocalDate desde, java.time.LocalDate hasta);
}
