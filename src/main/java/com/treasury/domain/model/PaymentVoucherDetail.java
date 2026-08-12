package com.treasury.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor
public class PaymentVoucherDetail {
    private Long id;
    private Long supplierId;
    private Long invoiceId;
    private String invoiceReference;
    private Long payableAccountId;
    private String payableAccountCode;
    private BigDecimal previousBalance;
    private BigDecimal amountPaid;
    private BigDecimal remainingBalance;
    private String tenantId;
}
