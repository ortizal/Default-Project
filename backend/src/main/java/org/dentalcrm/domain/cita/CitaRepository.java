package org.dentalcrm.domain.cita;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, Long>, JpaSpecificationExecutor<Cita> {

    @Query("""
            SELECT c FROM Cita c
            WHERE c.doctor.id = :doctorId
              AND c.fecha = :fecha
              AND c.estado IN (org.dentalcrm.domain.cita.EstadoCita.PENDIENTE,
                               org.dentalcrm.domain.cita.EstadoCita.CONFIRMADA,
                               org.dentalcrm.domain.cita.EstadoCita.ATENDIDA)
              AND c.horaInicio < :horaFin
              AND c.horaFin > :horaInicio
            """)
    List<Cita> findOverlap(@Param("doctorId") Long doctorId,
                           @Param("fecha") LocalDate fecha,
                           @Param("horaInicio") LocalTime horaInicio,
                           @Param("horaFin") LocalTime horaFin);

    @EntityGraph(attributePaths = {"paciente", "doctor", "servicio"})
    List<Cita> findByFechaAndDoctorIdOrderByHoraInicioAsc(LocalDate fecha, Long doctorId);

    List<Cita> findTop10ByPacienteIdOrderByFechaDescHoraInicioDesc(Long pacienteId);

    long countBySyncStatus(SyncEstado syncStatus);

    @Query("""
            SELECT c FROM Cita c
            WHERE c.paciente.id = :pacienteId
              AND c.estado IN (org.dentalcrm.domain.cita.EstadoCita.PENDIENTE,
                               org.dentalcrm.domain.cita.EstadoCita.CONFIRMADA)
              AND c.fecha >= :hoy
            ORDER BY c.fecha ASC, c.horaInicio ASC
            """)
    List<Cita> proximasDelPaciente(@Param("pacienteId") Long pacienteId, @Param("hoy") LocalDate hoy);

    long countByFecha(LocalDate fecha);

    long countByFechaAndEstado(LocalDate fecha, EstadoCita estado);

    @Query("""
            SELECT c.fecha, COUNT(c)
            FROM Cita c
            WHERE c.fecha BETWEEN :desde AND :hasta
            GROUP BY c.fecha
            ORDER BY c.fecha ASC
            """)
    List<Object[]> citasPorDia(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query(value = """
            SELECT c.estado, COUNT(*)
            FROM citas c
            WHERE c.fecha BETWEEN :desde AND :hasta
              AND c.tenant_id = :tenantId
            GROUP BY c.estado
            ORDER BY COUNT(*) DESC
            """, nativeQuery = true)
    List<Object[]> citasPorEstado(@Param("desde") LocalDate desde,
                                  @Param("hasta") LocalDate hasta,
                                  @Param("tenantId") Long tenantId);

    @Query(value = """
            SELECT o.id, o.nombres || ' ' || o.apellidos, COUNT(c.id)
            FROM citas c
            JOIN odontologos o ON o.id = c.doctor_id
            WHERE c.fecha BETWEEN :desde AND :hasta
              AND c.tenant_id = :tenantId
            GROUP BY o.id, o.nombres, o.apellidos
            ORDER BY COUNT(c.id) DESC
            """, nativeQuery = true)
    List<Object[]> citasPorOdontologo(@Param("desde") LocalDate desde,
                                      @Param("hasta") LocalDate hasta,
                                      @Param("tenantId") Long tenantId);

    @Query(value = """
            SELECT s.id, s.nombre, COUNT(c.id)
            FROM citas c
            JOIN servicios s ON s.id = c.servicio_id
            WHERE c.fecha BETWEEN :desde AND :hasta
              AND c.tenant_id = :tenantId
            GROUP BY s.id, s.nombre
            ORDER BY COUNT(c.id) DESC
            LIMIT :limite
            """, nativeQuery = true)
    List<Object[]> serviciosMasSolicitados(@Param("desde") LocalDate desde,
                                           @Param("hasta") LocalDate hasta,
                                           @Param("limite") int limite,
                                           @Param("tenantId") Long tenantId);

    @Query("""
            SELECT c.fecha, COUNT(c)
            FROM Cita c
            WHERE c.confirmada = true
              AND c.fecha BETWEEN :desde AND :hasta
            GROUP BY c.fecha
            ORDER BY c.fecha ASC
            """)
    List<Object[]> confirmacionesPorDia(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query(value = """
            SELECT COALESCE(c.confirmation_source, 'PANEL'), COUNT(*)
            FROM citas c
            WHERE c.confirmada = true
              AND c.fecha BETWEEN :desde AND :hasta
              AND c.tenant_id = :tenantId
            GROUP BY c.confirmation_source
            ORDER BY COUNT(*) DESC
            """, nativeQuery = true)
    List<Object[]> confirmacionesPorFuente(@Param("desde") LocalDate desde,
                                           @Param("hasta") LocalDate hasta,
                                           @Param("tenantId") Long tenantId);

    @Query("""
            SELECT c.fecha, COUNT(c)
            FROM Cita c
            WHERE c.estado = org.dentalcrm.domain.cita.EstadoCita.CANCELADA
              AND c.fecha BETWEEN :desde AND :hasta
            GROUP BY c.fecha
            ORDER BY c.fecha ASC
            """)
    List<Object[]> cancelacionesPorDia(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("""
            SELECT c.fecha, COUNT(c)
            FROM Cita c
            WHERE c.estado = org.dentalcrm.domain.cita.EstadoCita.NO_ASISTIO
              AND c.fecha BETWEEN :desde AND :hasta
            GROUP BY c.fecha
            ORDER BY c.fecha ASC
            """)
    List<Object[]> noAsistioPorDia(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    List<Cita> findByEstadoAndFechaBetween(EstadoCita estado, LocalDate desde, LocalDate hasta);
}