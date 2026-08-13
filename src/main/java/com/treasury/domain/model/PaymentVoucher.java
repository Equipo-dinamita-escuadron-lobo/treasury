package com.treasury.domain.model;

import com.treasury.domain.exception.TreasuryException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor
public class PaymentVoucher {
    private Long id;
    private String voucherNumber;
    private String enterpriseId;
    private LocalDate issueDate;
    private PaymentVoucherStatus status = PaymentVoucherStatus.DRAFT;
    private Long paymentMethodId;
    private Long bankAccountId;
    private BigDecimal total = BigDecimal.ZERO;
    private String observations;
    private String idempotencyKey;
    private Long accountingEntryId;
    private String failureReason;
    private String voidReason;
    private Instant createdAt;
    private Instant updatedAt;
    private long version;
    private String tenantId;
    private List<PaymentVoucherDetail> details = new ArrayList<>();

    public void replaceDetails(List<PaymentVoucherDetail> newDetails) {
        if (status != PaymentVoucherStatus.DRAFT && status != PaymentVoucherStatus.FAILED)
            conflict("Solo se pueden editar comprobantes en borrador o fallidos");
        details = new ArrayList<>(newDetails);
        total = details.stream().map(PaymentVoucherDetail::getAmountPaid).reduce(BigDecimal.ZERO, BigDecimal::add);
        status = PaymentVoucherStatus.DRAFT;
        failureReason = null;
    }

    public void startPosting(String key) {
        if (status != PaymentVoucherStatus.DRAFT && status != PaymentVoucherStatus.FAILED)
            conflict("El comprobante no se puede contabilizar desde su estado actual");
        idempotencyKey = key; status = PaymentVoucherStatus.POSTING; failureReason = null;
    }

    public void applyAccountingResult(boolean accepted, Long entryId, String reason) {
        if (status != PaymentVoucherStatus.POSTING) return;
        status = accepted ? PaymentVoucherStatus.POSTED : PaymentVoucherStatus.FAILED;
        accountingEntryId = entryId; failureReason = reason;
    }

    public void voidWithReason(String reason) {
        if (status != PaymentVoucherStatus.POSTED && status != PaymentVoucherStatus.VOID_FAILED)
            conflict("Solo se anulan comprobantes contabilizados o con anulaciÃ³n fallida");
        if (reason == null || reason.isBlank()) conflict("El motivo de anulaciÃ³n es obligatorio");
        status = PaymentVoucherStatus.VOIDING; voidReason = reason; failureReason = null;
    }

    public void applyVoidAccountingResult(boolean accepted, String reason) {
        if (status != PaymentVoucherStatus.VOIDING) return;
        status = accepted ? PaymentVoucherStatus.VOIDED : PaymentVoucherStatus.VOID_FAILED;
        failureReason = accepted ? null : reason;
    }

    private void conflict(String message) { throw new TreasuryException(TreasuryException.Type.CONFLICT, message); }
}
