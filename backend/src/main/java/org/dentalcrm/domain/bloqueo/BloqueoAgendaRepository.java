package org.dentalcrm.domain.bloqueo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface BloqueoAgendaRepository extends JpaRepository<BloqueoAgenda, Long> {

    List<BloqueoAgenda> findByOdontologoIdAndFecha(Long odontologoId, LocalDate fecha);

    List<BloqueoAgenda> findByOdontologoIdAndFechaBetween(Long odontologoId, LocalDate desde, LocalDate hasta);

    @Query("""
            SELECT b FROM BloqueoAgenda b
            WHERE b.odontologo.id = :doctorId
              AND b.fecha = :fecha
              AND (:horaInicio IS NULL OR (b.horaInicio IS NULL OR b.horaInicio < :horaFin))
              AND (:horaFin IS NULL OR (b.horaFin IS NULL OR b.horaFin > :horaInicio))
            """)
    List<BloqueoAgenda> findOverlap(@Param("doctorId") Long doctorId,
                                    @Param("fecha") LocalDate fecha,
                                    @Param("horaInicio") LocalTime horaInicio,
                                    @Param("horaFin") LocalTime horaFin);
}