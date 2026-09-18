package org.dentalcrm.domain.automatizacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AutomatizacionRepository extends JpaRepository<Automatizacion, Long> {

    List<Automatizacion> findAllByOrderByEventoAscNombreAsc();

    List<Automatizacion> findByActivaTrueAndEvento(EventoAutomatizacion evento);
}