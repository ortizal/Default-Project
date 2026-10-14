package org.dentalcrm.domain.comprobante;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnulacionComprobanteRepository extends JpaRepository<AnulacionComprobante, Long> {

    /** Trámites de un comprobante, del más reciente al más antiguo. */
    List<AnulacionComprobante> findByComprobanteIdOrderBySolicitadoEnDesc(Long comprobanteId);

    /** El trámite vigente (pendiente o aplicado) de un comprobante. */
    Optional<AnulacionComprobante> findFirstByComprobanteIdAndEstadoInOrderBySolicitadoEnDesc(
            Long comprobanteId, List<String> estados);
}
