package com.treasury.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor
public class PayableWriteOffDetail {
    private Long id;
    private Long supplierId;
    private Long invoiceId;
    private Long payableAccountId;
    private String payableAccountCode;
    private BigDecimal amount;
    private String tenantId;
}
