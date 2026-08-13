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
public class PaymentSchedule {
    private Long id;
    private String enterpriseId;
    private LocalDate executionDate;
    private Long paymentMethodId;
    private Long bankAccountId;
    private String observations;
    private PaymentScheduleStatus status = PaymentScheduleStatus.SCHEDULED;
    private Long voucherId;
    private int retryCount;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;
    private long version;
    private String tenantId;
    private List<PaymentScheduleDetail> details = new ArrayList<>();

    public BigDecimal getTotal() { return details.stream().map(PaymentScheduleDetail::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add); }
    public void ensureEditable() { if (status != PaymentScheduleStatus.SCHEDULED && status != PaymentScheduleStatus.FAILED) conflict("La programación no puede modificarse"); }
    public void cancel() { ensureEditable(); status = PaymentScheduleStatus.CANCELED; }
    public void start() { ensureEditable(); status = PaymentScheduleStatus.PROCESSING; failureReason = null; }
    public void waitingAccounting() { if (status == PaymentScheduleStatus.PROCESSING) status = PaymentScheduleStatus.WAITING_ACCOUNTING; }
    public void accountingResult(boolean accepted, String reason) {
        if (status != PaymentScheduleStatus.WAITING_ACCOUNTING) return;
        if (accepted) { status = PaymentScheduleStatus.EXECUTED; failureReason = null; }
        else failed(reason);
    }
    public void failed(String reason) { retryCount++; failureReason = reason; status = PaymentScheduleStatus.FAILED; }
    private void conflict(String message) { throw new TreasuryException(TreasuryException.Type.CONFLICT, message); }
}
