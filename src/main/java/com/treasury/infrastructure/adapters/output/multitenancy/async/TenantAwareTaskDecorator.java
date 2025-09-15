package com.treasury.infrastructure.adapters.output.multitenancy.async;

import com.treasury.infrastructure.adapters.output.multitenancy.utils.TenantContext;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;

public class TenantAwareTaskDecorator implements TaskDecorator {

/**
 * Decora un Runnable dado para asegurarse de que el contexto de inquilino actual se preserve
 * y se establezca antes de que el Runnable se ejecute y se borre después de la ejecución.
 * Esto asegura que los datos específicos del inquilino se manejen correctamente en tareas asíncronas.
 *
 * @param runnable el Runnable original a ser decorado
 * @return un nuevo Runnable que establece y borra el contexto de inquilino alrededor de la ejecución
 */
    @Override
    @NonNull
    public Runnable decorate(@NonNull Runnable runnable) {
        String tenantId = TenantContext.getTenantId();
        return () -> {
            try {
                TenantContext.setTenantId(tenantId);
                runnable.run();
            } finally {
                TenantContext.setTenantId(null);
            }
        };
    }
}
