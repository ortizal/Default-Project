package org.dentalcrm.domain.receta;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecetaRepository extends JpaRepository<Receta, Long> {

    List<Receta> findByPacienteIdOrderByFechaDescIdDesc(Long pacienteId);

    long countByPacienteId(Long pacienteId);

    long countByPacienteIdAndEstado(Long pacienteId, EstadoReceta estado);

    long countByFechaBetween(java.time.LocalDate desde, java.time.LocalDate hasta);

    List<Receta> findByFechaBetween(java.time.LocalDate desde, java.time.LocalDate hasta);

    List<Receta> findByFechaGreaterThanEqual(java.time.LocalDate desde);
}
