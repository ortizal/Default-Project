package org.dentalcrm.domain.consentimiento;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsentimientoPlantillaRepository extends JpaRepository<ConsentimientoPlantilla, Long> {

    List<ConsentimientoPlantilla> findAllByOrderByNombreAsc();

    Optional<ConsentimientoPlantilla> findByCodigo(String codigo);
}
