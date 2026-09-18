package org.dentalcrm.domain.automatizacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlantillaMensajeRepository extends JpaRepository<PlantillaMensaje, Long> {

    Optional<PlantillaMensaje> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    List<PlantillaMensaje> findAllByOrderByNombreAsc();
}