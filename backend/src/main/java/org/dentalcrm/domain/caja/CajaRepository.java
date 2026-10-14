package org.dentalcrm.domain.caja;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CajaRepository extends JpaRepository<Caja, Long> {

    /** La única sesión abierta del tenant (uq_caja_abierta). */
    Optional<Caja> findTopByEstadoOrderByAbiertoEnDesc(EstadoCaja estado);

    List<Caja> findByFechaBetweenOrderByFechaDescIdDesc(LocalDate desde, LocalDate hasta);
}
