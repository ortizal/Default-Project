package org.dentalcrm.domain.historia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface EvolucionRepository extends JpaRepository<Evolucion, Long> {

    List<Evolucion> findByHistoriaIdOrderByFechaDesc(Long historiaId);

    long countByFechaBetween(Instant desde, Instant hasta);

    List<Evolucion> findByFechaBetween(Instant desde, Instant hasta);

    List<Evolucion> findByFechaGreaterThanEqual(Instant desde);
}
