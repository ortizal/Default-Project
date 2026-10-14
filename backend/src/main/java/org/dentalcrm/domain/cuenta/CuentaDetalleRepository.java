package org.dentalcrm.domain.cuenta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CuentaDetalleRepository extends JpaRepository<CuentaDetalle, Long> {

        @Query("""
                        select coalesce(sum(case
                                when d.tipo = org.dentalcrm.domain.cuenta.TipoMovimientoCuenta.ANTICIPO then d.total
                                when d.tipo = org.dentalcrm.domain.cuenta.TipoMovimientoCuenta.DEVOLUCION then -d.total
                                else 0.00
                        end), 0.00)
                        from CuentaDetalle d
                        where d.anulado = false
                        """)
        BigDecimal anticiposNetos();

    List<CuentaDetalle> findByCuentaIdOrderByFechaAscIdAsc(Long cuentaId);

    /** Cargo ya generado por un procedimiento (idempotencia). */
    List<CuentaDetalle> findByCuentaIdAndOrigenAndReferenciaId(
            Long cuentaId, OrigenMovimientoCuenta origen, Long referenciaId);

    Optional<CuentaDetalle> findByIdAndCuentaId(Long id, Long cuentaId);

    List<CuentaDetalle> findByAnuladoFalseAndFechaBetweenOrderByFechaDescIdDesc(
            LocalDate desde, LocalDate hasta);

    List<CuentaDetalle> findByAnuladoFalseAndFechaBetweenOrderByFechaAscIdAsc(
            LocalDate desde, LocalDate hasta);

    List<CuentaDetalle> findByAnuladoFalseAndFecha(LocalDate fecha);

    List<CuentaDetalle> findByAnuladoFalse();

    long countByCuentaIdAndAnuladoFalse(Long cuentaId);
}
