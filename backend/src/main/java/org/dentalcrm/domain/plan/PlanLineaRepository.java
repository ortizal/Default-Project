package org.dentalcrm.domain.plan;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PlanLineaRepository extends JpaRepository<PlanLinea, Long> {

    List<PlanLinea> findByPlanIdOrderByNumeroAsc(Long planId);

    Optional<PlanLinea> findByIdAndPlanId(Long id, Long planId);

    long countByPlanId(Long planId);

    void deleteByPlanId(Long planId);

    /** Líneas sin terminar de planes vigentes, de más urgente a más antigua. */
    @Query("""
            SELECT l FROM PlanLinea l JOIN l.plan p JOIN p.paciente pa
            WHERE l.estado IN (:estados) AND p.estado <> :excluido
            ORDER BY CASE l.prioridad
                         WHEN org.dentalcrm.domain.plan.PrioridadLinea.URGENTE THEN 0
                         WHEN org.dentalcrm.domain.plan.PrioridadLinea.ALTA THEN 1
                         WHEN org.dentalcrm.domain.plan.PrioridadLinea.MEDIA THEN 2
                         ELSE 3
                     END,
                     p.fecha DESC, l.id DESC
            """)
    List<PlanLinea> buscarPendientes(@Param("estados") Collection<EstadoLineaPlan> estados,
                                     @Param("excluido") EstadoPlanTratamiento excluido,
                                     Pageable pageable);

    /** Producción clínica: líneas completadas agrupadas por procedimiento. */
    interface ProduccionFila {
        String getProcedimiento();

        long getCantidad();

        BigDecimal getTotal();
    }

    @Query("""
            SELECT l.procedimiento AS procedimiento,
                   COUNT(l) AS cantidad,
                   COALESCE(SUM(l.subtotal), 0) AS total
            FROM PlanLinea l JOIN l.plan p
            WHERE l.estado = org.dentalcrm.domain.plan.EstadoLineaPlan.COMPLETADO
              AND p.fecha BETWEEN :desde AND :hasta
            GROUP BY l.procedimiento
            ORDER BY COUNT(l) DESC, l.procedimiento ASC
            """)
    List<ProduccionFila> produccion(@Param("desde") LocalDate desde,
                                    @Param("hasta") LocalDate hasta,
                                    Pageable pageable);
}
