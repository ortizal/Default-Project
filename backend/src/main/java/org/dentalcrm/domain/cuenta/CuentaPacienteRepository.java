package org.dentalcrm.domain.cuenta;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CuentaPacienteRepository extends JpaRepository<CuentaPaciente, Long> {

    interface ResumenSaldosPendientes {
        BigDecimal getTotal();

        long getCantidad();
    }

    @Query("""
            select coalesce(sum(c.saldo), 0.00) as total, count(c) as cantidad
            from CuentaPaciente c
            where c.saldo > 0
            """)
    ResumenSaldosPendientes resumirSaldosPendientes();

    /** Una sola cuenta por paciente dentro del tenant (uq_cuenta_paciente). */
    Optional<CuentaPaciente> findByPacienteId(Long pacienteId);

    /**
     * Cuenta con bloqueo de escritura: la emisión del comprobante la toma
     * dentro de su transacción para que dos peticiones simultáneas no
     * facturen la misma cuenta.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CuentaPaciente c where c.id = :id")
    Optional<CuentaPaciente> buscarParaFacturar(@Param("id") Long id);

    List<CuentaPaciente> findByEstadoOrderByUpdatedAtDesc(EstadoCuenta estado);

    List<CuentaPaciente> findAllByOrderByUpdatedAtDesc();
}
