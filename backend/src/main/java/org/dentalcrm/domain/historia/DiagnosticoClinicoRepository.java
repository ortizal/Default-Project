package org.dentalcrm.domain.historia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DiagnosticoClinicoRepository extends JpaRepository<DiagnosticoClinico, Long> {

    List<DiagnosticoClinico> findByHistoriaIdOrderByFechaDesc(Long historiaId);

    long countByFechaBetween(LocalDate desde, LocalDate hasta);

    /** Diagnóstico agrupado por nombre con nº de registros y de pacientes distintos. */
    interface DiagnosticoFrecuente {
        String getNombre();

        long getTotal();

        long getPacientes();
    }

    @Query("""
            SELECT d.nombre AS nombre,
                   COUNT(d) AS total,
                   COUNT(DISTINCT d.historia.paciente.id) AS pacientes
            FROM DiagnosticoClinico d
            WHERE d.fecha BETWEEN :desde AND :hasta
            GROUP BY d.nombre
            ORDER BY COUNT(d) DESC, d.nombre ASC
            """)
    List<DiagnosticoFrecuente> masFrecuentes(@Param("desde") LocalDate desde,
                                             @Param("hasta") LocalDate hasta,
                                             Pageable pageable);
}
