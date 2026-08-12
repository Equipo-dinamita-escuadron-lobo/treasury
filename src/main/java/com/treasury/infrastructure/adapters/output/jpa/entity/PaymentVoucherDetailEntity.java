package com.treasury.infrastructure.adapters.output.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;

@Entity
@Table(name = "payment_voucher_details", uniqueConstraints =
        @UniqueConstraint(name = "uk_voucher_invoice", columnNames = {"voucher_id", "invoice_id"}))
@Getter @Setter @NoArgsConstructor
public class PaymentVoucherDetailEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JsonIgnore @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voucher_id", nullable = false)
    private PaymentVoucherEntity paymentVoucher;
    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;
    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;
    @Column(name = "invoice_reference", nullable = false, length = 80)
    private String invoiceReference;
    @Column(name = "payable_account_id", nullable = false)
    private Long payableAccountId;
    @Column(name = "payable_account_code", nullable = false, length = 30)
    private String payableAccountCode;
    @Column(name = "previous_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal previousBalance;
    @Column(name = "amount_paid", nullable = false, precision = 19, scale = 2)
    private BigDecimal amountPaid;
    @Column(name = "remaining_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal remainingBalance;
    @TenantId @Column(name = "tenant_id", nullable = false, length = 80)
    private String tenantId;
}
