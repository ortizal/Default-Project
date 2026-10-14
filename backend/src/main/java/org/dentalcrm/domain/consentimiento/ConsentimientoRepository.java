package org.dentalcrm.domain.consentimiento;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsentimientoRepository extends JpaRepository<Consentimiento, Long> {

    List<Consentimiento> findByPacienteIdOrderByFechaDescIdDesc(Long pacienteId);

    long countByPacienteId(Long pacienteId);

    long countByPacienteIdAndEstado(Long pacienteId, EstadoConsentimiento estado);

    long countByFechaBetween(java.time.LocalDate desde, java.time.LocalDate hasta);
}
