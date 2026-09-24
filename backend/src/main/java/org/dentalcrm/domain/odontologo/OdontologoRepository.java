package org.dentalcrm.domain.odontologo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OdontologoRepository extends JpaRepository<Odontologo, Long> {

    @Query("""
            SELECT o FROM Odontologo o
            WHERE (:q = '' OR lower(o.nombres) LIKE lower(concat('%', :q, '%'))
                         OR lower(o.apellidos) LIKE lower(concat('%', :q, '%'))
                         OR lower(o.especialidad) LIKE lower(concat('%', :q, '%'))
                         OR lower(o.etiquetas) LIKE lower(concat('%', :q, '%')))
              AND (:estado IS NULL OR o.estado = :estado)
            """)
    Page<Odontologo> buscar(@Param("q") String q, @Param("estado") String estado, Pageable pageable);

    List<Odontologo> findTop5ByEstadoOrderByNombresAsc(String estado);

    List<Odontologo> findByEstadoOrderByNombresAsc(String estado);

    java.util.Optional<Odontologo> findByGoogleCalendarId(String googleCalendarId);
}