package org.dentalcrm.domain.historia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamenPeriodontalRepository extends JpaRepository<ExamenPeriodontal, Long> {

    Optional<ExamenPeriodontal> findFirstByHistoriaIdOrderByVersionDesc(Long historiaId);

    List<ExamenPeriodontal> findByHistoriaIdOrderByVersionDesc(Long historiaId);

    Optional<ExamenPeriodontal> findByHistoriaIdAndVersion(Long historiaId, int version);

    long countByFechaBetween(java.time.LocalDate desde, java.time.LocalDate hasta);
}
