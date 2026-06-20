package com.treasury.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class Split {
    private Long id;
    private Long transactionId;
    private Long accountId;
    private BigDecimal amount; // Positivo = débito, negativo = crédito
    private String memo;

    public Split(Long transactionId, Long accountId, BigDecimal amount, String memo) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.amount = amount;
        this.memo = memo;
    }

    public boolean isDebit() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isCredit() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public BigDecimal getAbsoluteAmount() {
        return amount.abs();
    }

    public void validateAmount() {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Split amount cannot be zero");
        }
    }
}
