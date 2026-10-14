package org.dentalcrm.domain.historia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvolucionAdendaRepository extends JpaRepository<EvolucionAdenda, Long> {

    List<EvolucionAdenda> findByEvolucionIdOrderByCreatedAtDesc(Long evolucionId);
}
