package org.dentalcrm.domain.historia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HistoriaClinicaRepository extends JpaRepository<HistoriaClinica, Long> {

    Optional<HistoriaClinica> findByPacienteId(Long pacienteId);
}
