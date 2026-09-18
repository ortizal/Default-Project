package org.dentalcrm.domain.servicio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServicioRepository extends JpaRepository<Servicio, Long> {

    @Query("""
            SELECT s FROM Servicio s
            WHERE (:q = '' OR lower(s.nombre) LIKE lower(concat('%', :q, '%')))
              AND (:estado IS NULL OR s.estado = :estado)
            """)
    Page<Servicio> buscar(@Param("q") String q, @Param("estado") String estado, Pageable pageable);

    Optional<Servicio> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    List<Servicio> findByEstadoOrderByNombreAsc(String estado);
}