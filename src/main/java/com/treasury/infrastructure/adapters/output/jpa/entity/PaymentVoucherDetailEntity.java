package com.treasury.infrastructure.adapters.output.jpa.entity;

import org.hibernate.annotations.TenantId;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "payment_voucher_details")
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
public class PaymentVoucherDetailEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "voucher_id", insertable = false, updatable = false)
    private Long voucherId;

    @Column(name = "facture_id")
    private Long factureId;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @TenantId
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", referencedColumnName = "id")
    private PaymentVoucherEntity paymentVoucher;
}
