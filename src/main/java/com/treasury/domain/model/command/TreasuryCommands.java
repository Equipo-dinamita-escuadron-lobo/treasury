package com.treasury.domain.model.command;

import com.treasury.domain.model.PaymentScheduleStatus;
import com.treasury.domain.model.PaymentVoucher;
import com.treasury.domain.model.PaymentVoucherStatus;
import com.treasury.domain.model.SupplierInvoiceReplica;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class TreasuryCommands {
    private TreasuryCommands() {
    }

    public record Detail(Long supplierId, Long invoiceId, BigDecimal amount) {}
    public record Voucher(String enterpriseId, LocalDate issueDate, Long paymentMethodId,
                          Long bankAccountId, String observations, List<Detail> details) {}
    public record Schedule(String enterpriseId, LocalDate executionDate, Long paymentMethodId,
                           Long bankAccountId, String observations, List<Detail> details) {}
    public record WriteOff(String enterpriseId, String reason, Long counterpartAccountId,
                           String counterpartAccountCode, List<Detail> details) {}
    public record DueDate(LocalDate dueDate, String reason) {}
    public record AccountingResult(String eventId, String sourceEventId, String operation,
                                   String documentType, Long documentId, boolean accepted,
                                   Long accountingEntryId, String reason, String tenantId) {
        public AccountingResult(String eventId, String documentType, Long documentId,
                                boolean accepted, Long accountingEntryId, String reason, String tenantId) {
            this(eventId, null, "CREATE", documentType, documentId, accepted,
                    accountingEntryId, reason, tenantId);
        }
        public boolean isVoid() { return "VOID".equalsIgnoreCase(operation); }
    }
    public record PurchaseInvoiceEvent(String eventId, String eventType, Long invoiceId, String reference,
                                       String enterpriseId, Long supplierId, BigDecimal originalAmount,
                                       BigDecimal paidAmount, BigDecimal pendingAmount, LocalDate issueDate,
                                       LocalDate dueDate, Long payableAccountId, String payableAccountCode,
                                       boolean active, String tenantId) {}
    public record VoucherFilter(String enterpriseId, String voucherNumber, PaymentVoucherStatus status, LocalDate from, LocalDate to,
                                Long supplierId, Long invoiceId, Long paymentMethodId, Long bankAccountId,
                                BigDecimal min, BigDecimal max, int page, int size, String sort) {}
    public record ScheduleFilter(String enterpriseId, PaymentScheduleStatus status, LocalDate from, LocalDate to) {}
    public record PageResult<T>(List<T> content, long totalElements, int totalPages, int number, int size) {}
    public record AgingLine(Long supplierId, Long invoiceId, String reference, String accountCode,
                            LocalDate dueDate, long daysOverdue, BigDecimal current, BigDecimal days1to30,
                            BigDecimal days31to60, BigDecimal days61to90, BigDecimal days91Plus) {}
    public record SupplierStatement(Long supplierId, BigDecimal invoiced, BigDecimal paid,
                                    BigDecimal pending, List<SupplierInvoiceReplica> invoices,
                                    List<PaymentVoucher> vouchers) {}
}
