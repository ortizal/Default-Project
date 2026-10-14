package org.dentalcrm.domain.historia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OdontogramaRepository extends JpaRepository<Odontograma, Long> {

    List<Odontograma> findByHistoriaIdOrderByVersionDesc(Long historiaId);

    Optional<Odontograma> findFirstByHistoriaIdOrderByVersionDesc(Long historiaId);

    Optional<Odontograma> findByHistoriaIdAndVersion(Long historiaId, int version);

    long countByFechaBetween(java.time.LocalDate desde, java.time.LocalDate hasta);

    List<Odontograma> findByFechaBetween(java.time.LocalDate desde, java.time.LocalDate hasta);

    List<Odontograma> findByFechaGreaterThanEqual(java.time.LocalDate desde);
}
