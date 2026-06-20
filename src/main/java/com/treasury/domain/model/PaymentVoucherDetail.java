package com.treasury.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class PaymentVoucherDetail {
    private Long id;
    private Long voucherId;
    private Long factureId;
    private BigDecimal amount;

    public PaymentVoucherDetail(Long voucherId, Long factureId, BigDecimal amount) {
        this.voucherId = voucherId;
        this.factureId = factureId;
        this.amount = amount;
    }

    public void validateAmount() {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
}
