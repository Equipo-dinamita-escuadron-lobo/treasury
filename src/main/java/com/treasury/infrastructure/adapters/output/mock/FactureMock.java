package com.treasury.infrastructure.adapters.output.mock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Mock de factura para testing del módulo de tesorería
 * Basado en la estructura de Facture2 del skeleton de facture-management
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactureMock {
    
    private Long id;
    private String factureNumber;
    private Long supplierId;
    private String supplierName;
    private String supplierNit;
    private String enterpriseId;
    
    private LocalDate issueDate;
    private LocalDate dueDate;
    
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal pendingAmount;
    
    private FactureStatus status;
    private String description;
    private Long accountingAccount;
    
    /**
     * Calcula los días de vencimiento
     * @return días hasta vencimiento (negativo si está vencida)
     */
    public long getDaysToExpiration() {
        return LocalDate.now().until(dueDate).getDays();
    }
    
    /**
     * Verifica si la factura está vencida
     */
    public boolean isOverdue() {
        return dueDate.isBefore(LocalDate.now());
    }
    
    /**
     * Calcula el porcentaje pagado
     */
    public BigDecimal getPaymentPercentage() {
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return paidAmount.divide(totalAmount, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }
    
    /**
     * Obtiene el nombre del estado en español
     */
    public String getStatusDisplayName() {
        return switch (status) {
            case PENDING -> "Pendiente";
            case PARTIAL_PAID -> "Parcialmente Pagada";
            case PAID -> "Pagada";
            case OVERDUE -> "Vencida";
            case CANCELLED -> "Cancelada";
        };
    }
}
