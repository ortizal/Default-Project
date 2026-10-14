package org.dentalcrm.domain.auditoria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    /**
     * Búsqueda libre de auditoría por acción, módulo o entidad más el
     * filtro exacto de fila ({@code entidad} + {@code entidadId}), que es
     * como la bandeja de comprobantes enlaza a su auditoría (plan §3.7).
     * Los JSON no se buscan: están mapeados como {@code jsonb} y no
     * admiten {@code lower()} en JPQL.
     */
    @Query("""
            SELECT a FROM Auditoria a
            WHERE (:q = '' OR lower(a.accion) LIKE lower(concat('%', :q, '%'))
                         OR lower(a.modulo) LIKE lower(concat('%', :q, '%'))
                         OR lower(a.entidad) LIKE lower(concat('%', :q, '%')))
              AND (:modulo IS NULL OR a.modulo = :modulo)
              AND (:entidad IS NULL OR a.entidad = :entidad)
              AND (:entidadId IS NULL OR a.entidadId = :entidadId)
            """)
    Page<Auditoria> buscar(@Param("q") String q,
                           @Param("modulo") String modulo,
                           @Param("entidad") String entidad,
                           @Param("entidadId") Long entidadId,
                           Pageable pageable);
}
