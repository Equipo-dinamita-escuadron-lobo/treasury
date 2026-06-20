package com.treasury.infrastructure.adapters.output.mock;

/**
 * Estados posibles de una factura en el sistema de tesorería
 */
public enum FactureStatus {
    PENDING("Pendiente"),
    PARTIAL_PAID("Parcialmente Pagada"),
    PAID("Pagada"),
    OVERDUE("Vencida"),
    CANCELLED("Cancelada");

    private final String displayName;

    FactureStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
