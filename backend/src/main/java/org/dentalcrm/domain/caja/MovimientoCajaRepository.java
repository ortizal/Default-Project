package org.dentalcrm.domain.caja;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MovimientoCajaRepository extends JpaRepository<MovimientoCaja, Long> {

    List<MovimientoCaja> findByCajaIdOrderByFechaAscIdAsc(Long cajaId);

    Optional<MovimientoCaja> findByIdAndCajaId(Long id, Long cajaId);

    /** Vínculo con el movimiento de la cuenta del paciente (§3.3 ↔ §3.4). */
    Optional<MovimientoCaja> findFirstByCuentaDetalleId(Long cuentaDetalleId);
}
