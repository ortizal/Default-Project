package org.dentalcrm.multitenant;

/**
 * Contexto del tenant en el hilo de la petición.
 *
 * Lo establece {@link org.dentalcrm.security.JwtAuthenticationFilter} a partir
 * del claim {@code tenantId} del JWT, y el webhook de WhatsApp a partir de la
 * sesión que recibió el mensaje. Fuera de una petición (jobs programados,
 * pruebas) se resuelve el tenant por defecto (1).
 */
public final class TenantContext {

    private static final ThreadLocal<Long> ACTUAL = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Long tenantId) {
        ACTUAL.set(tenantId);
    }

    /** Devuelve el tenant del hilo actual, o {@code null} si no se ha fijado. */
    public static Long tenantId() {
        return ACTUAL.get();
    }

    /** Devuelve el tenant del hilo actual, o el tenant por defecto si no está fijado. */
    public static Long actualOrDefault() {
        Long tenantId = ACTUAL.get();
        return tenantId != null ? tenantId : TenantIdentifierResolver.TENANT_DEFECTO;
    }

    public static void clear() {
        ACTUAL.remove();
    }
}