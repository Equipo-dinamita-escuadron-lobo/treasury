package com.treasury.domain.model;

import com.treasury.domain.exception.TreasuryException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor
public class SupplierInvoiceReplica {
    private Long id;
    private Long sourceInvoiceId;
    private String reference;
    private String enterpriseId;
    private Long supplierId;
    private BigDecimal originalAmount;
    private BigDecimal paidAmount = BigDecimal.ZERO;
    private BigDecimal pendingAmount;
    private BigDecimal reservedAmount = BigDecimal.ZERO;
    private LocalDate issueDate;
    private LocalDate originalDueDate;
    private LocalDate dueDate;
    private boolean dueDateOverridden;
    private Long payableAccountId;
    private String payableAccountCode;
    private boolean active = true;
    private String lastEventId;
    private Instant updatedAt;
    private long version;
    private String tenantId;

    public BigDecimal available() { return pendingAmount.subtract(reservedAmount); }

    public void reserve(BigDecimal amount) {
        positive(amount);
        if (amount.compareTo(available()) > 0) conflict("El pago supera el saldo disponible de " + reference);
        reservedAmount = reservedAmount.add(amount);
    }

    public void release(BigDecimal amount) { reservedAmount = reservedAmount.subtract(amount); }
    public void confirmPayment(BigDecimal amount) { release(amount); pendingAmount = pendingAmount.subtract(amount); paidAmount = paidAmount.add(amount); }
    public void reversePayment(BigDecimal amount) {
        positive(amount);
        if (paidAmount.compareTo(amount) < 0) {
            conflict("No hay saldo pagado suficiente para revertir en " + reference);
        }
        paidAmount = paidAmount.subtract(amount);
        pendingAmount = pendingAmount.add(amount);
    }
    public void confirmWriteOff(BigDecimal amount) { release(amount); pendingAmount = pendingAmount.subtract(amount); }
    public void reverseWriteOff(BigDecimal amount) { pendingAmount = pendingAmount.add(amount); }
    public void changeDueDate(LocalDate date) { dueDate = date; dueDateOverridden = true; }

    private void positive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw new TreasuryException(TreasuryException.Type.BAD_REQUEST, "El valor debe ser mayor que cero");
    }
    private void conflict(String message) { throw new TreasuryException(TreasuryException.Type.CONFLICT, message); }
}
