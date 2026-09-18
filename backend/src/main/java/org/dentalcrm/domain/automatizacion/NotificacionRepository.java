package org.dentalcrm.domain.automatizacion;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByEstadoAndProgramadaAtLessThanEqual(EstadoNotificacion estado, Instant tope, Pageable pageable);

    @Query("""
            SELECT n FROM Notificacion n
            WHERE n.estado = org.dentalcrm.domain.automatizacion.EstadoNotificacion.ENVIANDO
              AND n.updatedAt < :tope
            """)
    List<Notificacion> enviandoColgadas(@Param("tope") Instant tope, Pageable pageable);

    @Query("""
            SELECT n FROM Notificacion n
            WHERE n.cita.id = :citaId
              AND n.estado IN (org.dentalcrm.domain.automatizacion.EstadoNotificacion.PENDIENTE,
                               org.dentalcrm.domain.automatizacion.EstadoNotificacion.ENVIANDO)
            """)
    List<Notificacion> activasDeCita(@Param("citaId") Long citaId);

    @Query("""
            SELECT n FROM Notificacion n
            WHERE n.cita.id = :citaId
              AND n.automatizacion.evento = org.dentalcrm.domain.automatizacion.EventoAutomatizacion.CITA_PROXIMA
              AND n.estado = org.dentalcrm.domain.automatizacion.EstadoNotificacion.PENDIENTE
            """)
    List<Notificacion> recordatoriosPendientesDeCita(@Param("citaId") Long citaId);

    @EntityGraph(attributePaths = {"cita", "automatizacion.plantilla", "automatizacion", "plantilla", "paciente"})
    @Query("""
            SELECT n FROM Notificacion n
            ORDER BY n.createdAt DESC
            """)
    List<Notificacion> ultimas(Pageable pageable);

    long countByEstado(EstadoNotificacion estado);
}