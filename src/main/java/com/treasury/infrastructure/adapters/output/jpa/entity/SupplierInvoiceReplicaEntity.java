package com.treasury.infrastructure.adapters.output.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity @Table(name = "supplier_invoice_replicas", uniqueConstraints =
        @UniqueConstraint(name = "uk_supplier_invoice_enterprise", columnNames = {"source_invoice_id", "enterprise_id"}))
@Getter @Setter @NoArgsConstructor
public class SupplierInvoiceReplicaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="source_invoice_id", nullable=false) private Long sourceInvoiceId;
    @Column(name="reference", nullable=false, length=80) private String reference;
    @Column(name="enterprise_id", nullable=false, length=80) private String enterpriseId;
    @Column(name="supplier_id", nullable=false) private Long supplierId;
    @Column(name="original_amount", nullable=false, precision=19, scale=2) private BigDecimal originalAmount;
    @Column(name="paid_amount", nullable=false, precision=19, scale=2) private BigDecimal paidAmount = BigDecimal.ZERO;
    @Column(name="pending_amount", nullable=false, precision=19, scale=2) private BigDecimal pendingAmount;
    @Column(name="reserved_amount", nullable=false, precision=19, scale=2) private BigDecimal reservedAmount = BigDecimal.ZERO;
    @Column(name="issue_date", nullable=false) private LocalDate issueDate;
    @Column(name="original_due_date", nullable=false) private LocalDate originalDueDate;
    @Column(name="due_date", nullable=false) private LocalDate dueDate;
    @Column(name="due_date_overridden", nullable=false) private boolean dueDateOverridden;
    @Column(name="payable_account_id", nullable=false) private Long payableAccountId;
    @Column(name="payable_account_code", nullable=false, length=30) private String payableAccountCode;
    @Column(name="active", nullable=false) private boolean active = true;
    @Column(name="last_event_id", length=80) private String lastEventId;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;
    @Version private long version;
    @TenantId @Column(name="tenant_id", nullable=false, length=80) private String tenantId;
    @PrePersist @PreUpdate void timestamp(){ updatedAt = Instant.now(); }

    public BigDecimal available() { return pendingAmount.subtract(reservedAmount); }
}
