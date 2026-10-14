package org.dentalcrm.domain.historia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OdontogramaDienteRepository extends JpaRepository<OdontogramaDiente, Long> {

    List<OdontogramaDiente> findByOdontogramaIdOrderByDienteAsc(Long odontogramaId);

    void deleteByOdontogramaId(Long odontogramaId);

    long countByOdontogramaId(Long odontogramaId);
}
