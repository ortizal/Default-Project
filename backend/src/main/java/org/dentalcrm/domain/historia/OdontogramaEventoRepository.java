package org.dentalcrm.domain.historia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OdontogramaEventoRepository extends JpaRepository<OdontogramaEvento, Long> {

    List<OdontogramaEvento> findTop100ByOdontogramaIdOrderByFechaDesc(Long odontogramaId);
}
