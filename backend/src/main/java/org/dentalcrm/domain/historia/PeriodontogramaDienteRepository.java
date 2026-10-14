package org.dentalcrm.domain.historia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PeriodontogramaDienteRepository extends JpaRepository<PeriodontogramaDiente, Long> {

    List<PeriodontogramaDiente> findByExamenIdOrderByDienteAsc(Long examenId);

    Optional<PeriodontogramaDiente> findByExamenIdAndDiente(Long examenId, int diente);

    void deleteByExamenIdAndDiente(Long examenId, int diente);

    long countByExamenId(Long examenId);
}
