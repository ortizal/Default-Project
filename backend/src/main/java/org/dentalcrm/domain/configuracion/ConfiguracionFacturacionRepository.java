package org.dentalcrm.domain.configuracion;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConfiguracionFacturacionRepository extends JpaRepository<ConfiguracionFacturacion, Long> {

    Optional<ConfiguracionFacturacion> findByTenantId(Long tenantId);

    /**
     * Misma fila con bloqueo de escritura: quien reserve el secuencial del
     * siguiente comprobante (Fase G.4) espera aquí a que termine el que
     * está emitiendo a la vez, para que dos facturas no usen el mismo número.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ConfiguracionFacturacion c where c.tenantId = :tenantId")
    Optional<ConfiguracionFacturacion> buscarParaSecuencial(@Param("tenantId") Long tenantId);
}
