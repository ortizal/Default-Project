package org.dentalcrm.domain.reserva;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReservaWebIdempotenciaRepository extends JpaRepository<ReservaWebIdempotencia, Long> {

    /** La cita ya creada para esa clave, si el envío anterior terminó bien. */
    Optional<ReservaWebIdempotencia> findByTenantIdAndIdempotencyKey(Long tenantId, String idempotencyKey);
}
