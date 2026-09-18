package org.dentalcrm.domain.google;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GoogleCalendarioRepository extends JpaRepository<GoogleCalendario, Long> {

    List<GoogleCalendario> findByCuentaIdOrderBySummaryAsc(Long cuentaId);

    List<GoogleCalendario> findByCuentaId(Long cuentaId);

    Optional<GoogleCalendario> findByCuentaIdAndSeleccionadoTrue(Long cuentaId);

    Optional<GoogleCalendario> findByCuentaIdAndCalendarId(Long cuentaId, String calendarId);

    long deleteAllByCuentaId(Long cuentaId);

    @Modifying
    @Query("update GoogleCalendario c set c.seleccionado = false where c.cuenta.id = :cuentaId")
    int desmarcarTodos(@Param("cuentaId") Long cuentaId);
}