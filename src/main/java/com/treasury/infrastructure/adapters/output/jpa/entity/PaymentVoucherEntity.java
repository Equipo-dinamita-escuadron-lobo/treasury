package com.treasury.infrastructure.adapters.output.jpa.entity;

import com.treasury.domain.model.PaymentVoucherStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payment_vouchers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_voucher_number_enterprise", columnNames = {"voucher_number", "enterprise_id"}),
        @UniqueConstraint(name = "uk_voucher_idempotency_enterprise", columnNames = {"idempotency_key", "enterprise_id"})})
@Getter @Setter @NoArgsConstructor
public class PaymentVoucherEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "voucher_number", nullable = false, length = 50)
    private String voucherNumber;
    @Column(name = "enterprise_id", nullable = false, length = 80)
    private String enterpriseId;
    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private PaymentVoucherStatus status = PaymentVoucherStatus.DRAFT;
    @Column(name = "payment_method_id", nullable = false)
    private Long paymentMethodId;
    @Column(name = "bank_account_id")
    private Long bankAccountId;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;
    @Column(length = 500)
    private String observations;
    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;
    @Column(name = "accounting_entry_id")
    private Long accountingEntryId;
    @Column(name = "failure_reason", length = 500)
    private String failureReason;
    @Column(name = "void_reason", length = 500)
    private String voidReason;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version private long version;
    @TenantId @Column(name = "tenant_id", nullable = false, length = 80)
    private String tenantId;
    @OneToMany(mappedBy = "paymentVoucher", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PaymentVoucherDetailEntity> details = new ArrayList<>();

    @PrePersist void createTimestamps() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void updateTimestamp() { updatedAt = Instant.now(); }
}
