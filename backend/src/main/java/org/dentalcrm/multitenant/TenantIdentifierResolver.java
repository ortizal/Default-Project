package org.dentalcrm.multitenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 * Resuelve el identificador de tenant para Hibernate cuando trabaja con
 * entidades anotadas con {@code @TenantId}. Se registra en application.yml
 * mediante {@code hibernate.tenant_identifier_resolver}.
 */
@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<Long> {

    /** Tenant por defecto (fila 'principal' sembrada en la migración V9). */
    public static final Long TENANT_DEFECTO = 1L;

    @Override
    public Long resolveCurrentTenantIdentifier() {
        return TenantContext.actualOrDefault();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}