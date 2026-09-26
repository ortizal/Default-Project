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

    /**
     * Conversaciones cuya última actividad cae dentro de la ventana indicada.
     * Son las candidatas a recuperación de chats sin respuesta tras un
     * reinicio: su último mensaje puede ser una salida fallida o una entrada
     * a la que el bot no contestó.
     *
     * <p>Se trae {@code sesion} y {@code paciente} con JOIN FETCH porque la
     * recuperación necesita comprobar que la sesión esté conectada antes de
     * reenviar y el agente necesita saber si hay paciente vinculado para
     * retomar, y el método se invoca fuera de una transacción (no habría
     * proxy para cargarlos).
     */
    @Query("""
            SELECT c FROM Conversacion c
            LEFT JOIN FETCH c.sesion
            LEFT JOIN FETCH c.paciente
            WHERE c.estado IN :estados
              AND c.ultimoMensajeAt >= :desde
            """)
    List<Conversacion> candidatasRecuperacion(@Param("desde") java.time.Instant desde,
                                              @Param("estados") List<EstadoConversacion> estados,
                                              org.springframework.data.domain.Pageable pageable);
}