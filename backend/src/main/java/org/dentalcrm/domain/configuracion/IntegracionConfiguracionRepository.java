package org.dentalcrm.domain.configuracion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IntegracionConfiguracionRepository extends JpaRepository<IntegracionConfiguracion, Long> {
    Optional<IntegracionConfiguracion> findByTenantId(Long tenantId);
}