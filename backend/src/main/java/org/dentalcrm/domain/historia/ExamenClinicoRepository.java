package org.dentalcrm.domain.historia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ExamenClinicoRepository extends JpaRepository<ExamenClinico, Long> {

    List<ExamenClinico> findByHistoriaIdOrderByFechaDesc(Long historiaId);

    long countByFechaBetween(LocalDate desde, LocalDate hasta);

    List<ExamenClinico> findByFechaBetween(LocalDate desde, LocalDate hasta);

    List<ExamenClinico> findByFechaGreaterThanEqual(LocalDate desde);
}
