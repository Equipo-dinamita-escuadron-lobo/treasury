package com.treasury.infrastructure.adapters.output.multitenancy.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TenantContext {
    private TenantContext() {
    }

    private static final InheritableThreadLocal<String> currentTenant = new InheritableThreadLocal<>();

    /**
     * Establece el identificador de inquilino en el contexto del hilo actual.
     * Esto se hace normalmente por el {@link TenantInterceptor} antes de que se
     * llame
     * al controlador.
     *
     * @param tenantId el identificador del inquilino
     */
    public static void setTenantId(String tenantId) {
        log.debug("Setting tenantId to " + tenantId);
        currentTenant.set(tenantId);
    }

    /**
     * Obtiene el identificador de inquilino del contexto del hilo actual.
     *
     * @return el identificador del inquilino, o null si no se ha establecido
     *         ninguno
     */
    public static String getTenantId() {
        return currentTenant.get();
    }

    /**
     * Borra el identificador de inquilino del contexto del hilo actual.
     * Normalmente se llama despu s de que se llama al controlador.
     */
    public static void clear() {
        currentTenant.remove();
    }
}
