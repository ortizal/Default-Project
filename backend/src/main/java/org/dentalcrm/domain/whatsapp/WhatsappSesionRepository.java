package org.dentalcrm.domain.whatsapp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WhatsappSesionRepository extends JpaRepository<WhatsappSesion, Long> {

    Optional<WhatsappSesion> findBySesionId(String sesionId);

    boolean existsBySesionId(String sesionId);

    List<WhatsappSesion> findAllByTenantIdOrderByCreatedAtAsc(Long tenantId);

    List<WhatsappSesion> findByEstado(EstadoSesionWhatsapp estado);
}