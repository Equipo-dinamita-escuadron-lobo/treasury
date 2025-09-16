package com.treasury.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PaymentVoucher {
    private Long id;
    private LocalDateTime date;
    private Long payeeId;
    private BigDecimal amount;
    private Long methodId;
    private Long bankAccountId;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PaymentVoucherDetail> details;

    public PaymentVoucher(LocalDateTime date, Long payeeId, BigDecimal amount,
                         Long methodId, Long bankAccountId, String description) {
        this.date = date;
        this.payeeId = payeeId;
        this.amount = amount;
        this.methodId = methodId;
        this.bankAccountId = bankAccountId;
        this.description = description;
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void approve() {
        if (!"PENDING".equals(this.status)) {
            throw new IllegalStateException("Only pending vouchers can be approved");
        }
        this.status = "APPROVED";
        this.updatedAt = LocalDateTime.now();
    }

    public void reject() {
        if (!"PENDING".equals(this.status)) {
            throw new IllegalStateException("Only pending vouchers can be rejected");
        }
        this.status = "REJECTED";
        this.updatedAt = LocalDateTime.now();
    }

    public void pay() {
        if (!"APPROVED".equals(this.status)) {
            throw new IllegalStateException("Only approved vouchers can be paid");
        }
        this.status = "PAID";
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isPending() {
        return "PENDING".equals(this.status);
    }

    public boolean isApproved() {
        return "APPROVED".equals(this.status);
    }

    public boolean isPaid() {
        return "PAID".equals(this.status);
    }
}
