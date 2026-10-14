package org.dentalcrm.domain.paciente;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    @Query("""
            SELECT p FROM Paciente p
            WHERE (:q = '' OR lower(p.nombres) LIKE lower(concat('%', :q, '%'))
                         OR lower(p.apellidos) LIKE lower(concat('%', :q, '%'))
                         OR p.cedula LIKE concat('%', :q, '%')
                         OR p.telefono LIKE concat('%', :q, '%'))
              AND (:estado IS NULL OR p.estado = :estado)
            """)
    Page<Paciente> buscar(@Param("q") String q, @Param("estado") String estado, Pageable pageable);

    /** Identificadores que casan con el texto de la bandeja (§3.7). */
    @Query("""
            SELECT p.id FROM Paciente p
            WHERE lower(p.nombres) LIKE lower(concat('%', :q, '%'))
               OR lower(p.apellidos) LIKE lower(concat('%', :q, '%'))
               OR lower(coalesce(p.cedula, '')) LIKE lower(concat('%', :q, '%'))
            """)
    List<Long> idsPorTexto(@Param("q") String q);

    Optional<Paciente> findByCedulaIgnoreCase(String cedula);

    boolean existsByCedulaIgnoreCaseAndIdNot(String cedula, Long id);

    // La cédula es única a nivel nacional (y global en la BD), así que la
    // comprobación de duplicados debe cruzar tenants (consulta nativa).
    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM pacientes
                WHERE lower(trim(cedula)) = lower(trim(:cedula))
            )
            """, nativeQuery = true)
    boolean existeGlobalCedula(@Param("cedula") String cedula);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM pacientes
                WHERE lower(trim(cedula)) = lower(trim(:cedula))
                  AND id <> :id
            )
            """, nativeQuery = true)
    boolean existeGlobalCedulaExcepto(@Param("cedula") String cedula, @Param("id") Long id);

    @Query("SELECT p FROM Paciente p WHERE p.estado = 'ACTIVO' AND p.telefono IS NOT NULL")
    List<Paciente> activosConTelefono();

    @Query("SELECT COUNT(p) FROM Paciente p WHERE p.createdAt >= :desde")
    long countNuevosDesde(@Param("desde") java.time.Instant desde);

    /** Activos sin ninguna cita futura a partir de la fecha indicada (continuidad, §4.17). */
    @Query("""
            SELECT COUNT(p) FROM Paciente p
            WHERE p.estado = 'ACTIVO'
              AND NOT EXISTS (SELECT 1 FROM Cita c
                              WHERE c.paciente = p
                                AND c.fecha >= :hoy
                                AND c.estado IN (:estados))
            """)
    long countSinProximaCita(@Param("hoy") java.time.LocalDate hoy,
                             @Param("estados") java.util.Collection<org.dentalcrm.domain.cita.EstadoCita> estados);
}