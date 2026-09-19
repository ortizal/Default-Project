package org.dentalcrm.domain.whatsapp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversacionRepository extends JpaRepository<Conversacion, Long> {

    Optional<Conversacion> findBySesionIdAndTelefono(Long sesionId, String telefono);

    Optional<Conversacion> findByTelefono(String telefono);

    @Query("""
            SELECT c FROM Conversacion c
            WHERE (:q = '' OR lower(c.nombreContacto) LIKE lower(concat('%', :q, '%'))
                         OR c.telefono LIKE concat('%', :q, '%'))
            ORDER BY c.ultimoMensajeAt DESC NULLS LAST, c.id DESC
            """)
    List<Conversacion> buscar(@Param("q") String q);

    @Query("SELECT COUNT(c) FROM Conversacion c WHERE c.estado IN :estados")
    long countByEstadoIn(@Param("estados") List<EstadoConversacion> estados);
}