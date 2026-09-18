package org.dentalcrm.domain.auditoria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    @Query("""
            SELECT a FROM Auditoria a
            WHERE (:q = '' OR lower(a.accion) LIKE lower(concat('%', :q, '%'))
                         OR lower(a.modulo) LIKE lower(concat('%', :q, '%'))
                         OR lower(a.entidad) LIKE lower(concat('%', :q, '%')))
              AND (:modulo IS NULL OR a.modulo = :modulo)
            """)
    Page<Auditoria> buscar(@Param("q") String q, @Param("modulo") String modulo, Pageable pageable);
}
