package org.dentalcrm.domain.plan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PresupuestoLineaRepository extends JpaRepository<PresupuestoLinea, Long> {

    List<PresupuestoLinea> findByPresupuestoIdOrderByNumeroAsc(Long presupuestoId);

    long countByPresupuestoId(Long presupuestoId);
}
