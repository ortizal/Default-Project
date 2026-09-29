package org.dentalcrm.domain.configuracion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConsultorioRepository extends JpaRepository<Consultorio, Long> {

    Optional<Consultorio> findFirstByOrderByIdAsc();
}