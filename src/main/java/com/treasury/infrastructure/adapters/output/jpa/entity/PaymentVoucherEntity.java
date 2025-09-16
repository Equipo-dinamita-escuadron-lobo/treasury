package com.treasury.infrastructure.adapters.output.jpa.entity;

import org.hibernate.annotations.TenantId;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "payment_vouchers")
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
public class PaymentVoucherEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date")
    private LocalDateTime date;

    @Column(name = "payee_id")
    private Long payeeId;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "method_id")
    private Long methodId;

    @Column(name = "bank_account_id")
    private Long bankAccountId;

    @Column(name = "description")
    private String description;

    @Column(name = "status")
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @TenantId
    private String tenantId;

    @OneToMany(mappedBy = "paymentVoucher", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PaymentVoucherDetailEntity> details;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
