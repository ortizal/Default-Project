package org.dentalcrm.domain.plan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PresupuestoRepository extends JpaRepository<Presupuesto, Long> {

    List<Presupuesto> findByPlanIdOrderByNumeroDesc(Long planId);

    Optional<Presupuesto> findByIdAndPlanId(Long id, Long planId);

    Optional<Presupuesto> findTopByPlanIdAndVigenteTrueOrderByNumeroDesc(Long planId);

    Optional<Presupuesto> findTopByPlanIdOrderByNumeroDesc(Long planId);

    long countByPlanId(Long planId);
}
