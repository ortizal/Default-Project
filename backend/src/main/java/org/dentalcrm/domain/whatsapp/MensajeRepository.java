package org.dentalcrm.domain.whatsapp;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    List<Mensaje> findByConversacion_IdOrderByReceivedAtAsc(Long conversacionId);

    @Query("SELECT m FROM Mensaje m WHERE m.conversacion.id = :cid ORDER BY m.receivedAt DESC, m.id DESC")
    List<Mensaje> ultimos(@Param("cid") Long conversacionId, Pageable pageable);

    long countByConversacion_Id(Long conversacionId);

    long countByEstado(EstadoMensaje estado);
}