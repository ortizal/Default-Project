package org.dentalcrm.domain.archivo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArchivoClinicoRepository extends JpaRepository<ArchivoClinico, Long> {

    List<ArchivoClinico> findByPacienteIdOrderByCreatedAtDescIdDesc(Long pacienteId);

    long countByCreatedAtBetween(java.time.Instant desde, java.time.Instant hasta);
}
