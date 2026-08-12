package com.treasury.infrastructure.adapters.input.rest.dto;

import com.treasury.domain.model.PaymentScheduleStatus;
import com.treasury.domain.model.PaymentVoucherStatus;
import com.treasury.domain.model.WriteOffStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class TreasuryDtos {
    private TreasuryDtos() {}

    public record VoucherDetailRequest(@NotNull Long supplierId, @NotNull Long invoiceId,
                                       @NotNull @DecimalMin(value="0.01") BigDecimal amount) {}
    public record VoucherRequest(@NotBlank String enterpriseId, @NotNull LocalDate issueDate,
                                 @NotNull Long paymentMethodId, Long bankAccountId,
                                 @Size(max=500) String observations,
                                 @NotEmpty List<@Valid VoucherDetailRequest> details) {}
    public record VoidRequest(@NotBlank @Size(max=500) String reason) {}
    public record DueDateRequest(@NotNull LocalDate dueDate, @NotBlank @Size(max=500) String reason) {}
    public record ScheduleRequest(@NotBlank String enterpriseId, @NotNull @FutureOrPresent LocalDate executionDate,
                                  @NotNull Long paymentMethodId, Long bankAccountId,
                                  @Size(max=500) String observations,
                                  @NotEmpty List<@Valid VoucherDetailRequest> details) {}
    public record WriteOffRequest(@NotBlank String enterpriseId, @NotBlank @Size(max=500) String reason,
                                  @NotNull Long counterpartAccountId, @NotBlank String counterpartAccountCode,
                                  @NotEmpty List<@Valid VoucherDetailRequest> details) {}

    public record VoucherDetailResponse(Long id, Long supplierId, Long invoiceId, String invoiceReference,
                                         Long payableAccountId, String payableAccountCode,
                                         BigDecimal previousBalance, BigDecimal amountPaid,
                                         BigDecimal remainingBalance) {}
    public record VoucherResponse(Long id, String voucherNumber, String enterpriseId, LocalDate issueDate,
                                  PaymentVoucherStatus status, Long paymentMethodId, Long bankAccountId,
                                  BigDecimal total, String observations, Long accountingEntryId,
                                  String failureReason, String voidReason, long version,
                                  List<VoucherDetailResponse> details) {}
    public record PayableResponse(Long id, Long sourceInvoiceId, String reference, String enterpriseId,
                                  Long supplierId, BigDecimal originalAmount, BigDecimal paidAmount,
                                  BigDecimal pendingAmount, BigDecimal reservedAmount, BigDecimal availableAmount,
                                  LocalDate issueDate, LocalDate originalDueDate, LocalDate dueDate,
                                  Long payableAccountId, String payableAccountCode, boolean active, long version) {}
    public record ScheduleDetailResponse(Long id, Long supplierId, Long invoiceId, BigDecimal amount) {}
    public record ScheduleResponse(Long id, String enterpriseId, LocalDate executionDate,
                                   Long paymentMethodId, Long bankAccountId, String observations,
                                   PaymentScheduleStatus status, Long voucherId, int retryCount,
                                   String failureReason, long version,
                                   List<ScheduleDetailResponse> details) {}
    public record WriteOffDetailResponse(Long id, Long supplierId, Long invoiceId,
                                         Long payableAccountId, String payableAccountCode,
                                         BigDecimal amount) {}
    public record WriteOffResponse(Long id, String enterpriseId, String reason,
                                   Long counterpartAccountId, String counterpartAccountCode,
                                   BigDecimal total, WriteOffStatus status,
                                   Long accountingEntryId, long version,
                                   List<WriteOffDetailResponse> details) {}
    public record AgingLine(Long supplierId, Long invoiceId, String reference, String accountCode,
                            LocalDate dueDate, long daysOverdue, BigDecimal current, BigDecimal days1to30,
                            BigDecimal days31to60, BigDecimal days61to90, BigDecimal days91Plus) {}
    public record SupplierStatement(Long supplierId, BigDecimal invoiced, BigDecimal paid,
                                    BigDecimal pending, List<PayableResponse> invoices,
                                    List<VoucherResponse> vouchers) {}
}
