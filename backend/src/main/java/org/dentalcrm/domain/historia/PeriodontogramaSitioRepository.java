package org.dentalcrm.domain.historia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PeriodontogramaSitioRepository extends JpaRepository<PeriodontogramaSitio, Long> {

    List<PeriodontogramaSitio> findByExamenIdOrderByDienteAsc(Long examenId);

    Optional<PeriodontogramaSitio> findByExamenIdAndDienteAndSitio(Long examenId, int diente,
                                                                   SitioPeriodontal sitio);

    void deleteByExamenIdAndDiente(Long examenId, int diente);

    long countByExamenId(Long examenId);
}
