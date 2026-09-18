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
}