package com.treasury.application.service;

import com.treasury.application.output.*;
import com.treasury.domain.model.*;
import com.treasury.domain.model.command.TreasuryCommands.AgingLine;
import com.treasury.domain.model.command.TreasuryCommands.PageResult;
import com.treasury.domain.model.command.TreasuryCommands.SupplierStatement;
import com.treasury.domain.model.command.TreasuryCommands.VoucherFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
class PayableQueryServiceTest {

    @Mock ISupplierInvoiceProviderPort invoices;
    @Mock IPaymentVoucherQueryPersistencePort vouchers;
    @Mock IPayableWriteOffPersistencePort writeOffs;
    @Mock ITreasuryAuditPersistencePort audit;
    @Mock IExecutionContextPort context;
    @Mock IAccountCodeResolverPort accountCodes;

    PayableQueryService service;

    @BeforeEach
    void setUp() {
        service = new PayableQueryService(invoices, vouchers, writeOffs, audit, context, null, accountCodes);
    }

    @Test
    void statementReconcilesOpeningInvoicesPaymentsWriteOffsAndClosingBalance() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);
        Long supplierId = 78L;
        String enterpriseId = "enterprise-a";

        SupplierInvoiceReplica openingInvoice = invoice(1L, supplierId, "FC-OLD", from.minusDays(10),
                new BigDecimal("100"), new BigDecimal("20"), new BigDecimal("80"));
        SupplierInvoiceReplica periodInvoice = invoice(2L, supplierId, "FC-NEW", from.plusDays(5),
                new BigDecimal("50"), new BigDecimal("30"), new BigDecimal("20"));

        when(invoices.findForStatement(enterpriseId, supplierId, null, to, null, null))
                .thenReturn(List.of(openingInvoice, periodInvoice));

        PaymentVoucherDetail paymentDetail = new PaymentVoucherDetail();
        paymentDetail.setSupplierId(supplierId);
        paymentDetail.setInvoiceId(2L);
        paymentDetail.setAmountPaid(new BigDecimal("30"));
        PaymentVoucher voucher = new PaymentVoucher();
        voucher.setStatus(PaymentVoucherStatus.POSTED);
        voucher.setDetails(List.of(paymentDetail));
        when(vouchers.search(any())).thenReturn(new com.treasury.domain.model.command.TreasuryCommands.PageResult<>(
                List.of(voucher), 1, 1, 0, 10000));

        PayableWriteOffDetail writeOffDetail = new PayableWriteOffDetail();
        writeOffDetail.setSupplierId(supplierId);
        writeOffDetail.setInvoiceId(1L);
        writeOffDetail.setAmount(new BigDecimal("5"));
        PayableWriteOff writeOff = new PayableWriteOff();
        writeOff.setStatus(WriteOffStatus.POSTED);
        writeOff.setCreatedAt(Instant.parse("2026-08-15T12:00:00Z"));
        writeOff.setDetails(List.of(writeOffDetail));
        when(writeOffs.findByEnterprise(enterpriseId)).thenReturn(List.of(writeOff));

        openingInvoice.setPendingAmount(new BigDecimal("75"));

        SupplierStatement statement = service.statement(enterpriseId, supplierId, from, to, null, null);

        assertEquals(new BigDecimal("80"), statement.openingBalance());
        assertEquals(new BigDecimal("50"), statement.invoiced());
        assertEquals(new BigDecimal("30"), statement.paid());
        assertEquals(new BigDecimal("5"), statement.writeOffTotal());
        assertEquals(new BigDecimal("95"), statement.pending());
        assertEquals(
                statement.openingBalance()
                        .add(statement.invoiced())
                        .subtract(statement.paid())
                        .subtract(statement.writeOffTotal()),
                statement.pending());
    }

    @Test
    void agingTotalMatchesPendingForSupplier() {
        LocalDate cutoff = LocalDate.of(2026, 8, 15);
        Long supplierId = 78L;
        String enterpriseId = "enterprise-a";

        SupplierInvoiceReplica first = invoice(1L, supplierId, "684925498", cutoff.minusDays(5),
                new BigDecimal("1500"), BigDecimal.ZERO, new BigDecimal("1500"));
        first.setEnterpriseId(enterpriseId);
        first.setPayableAccountId(1105L);
        first.setPayableAccountCode("1105");
        SupplierInvoiceReplica second = invoice(2L, supplierId, "684202749", cutoff.minusDays(3),
                new BigDecimal("2500"), BigDecimal.ZERO, new BigDecimal("2500"));
        second.setEnterpriseId(enterpriseId);
        second.setPayableAccountId(1105L);
        second.setPayableAccountCode("1105");

        when(invoices.findPending(enterpriseId, supplierId)).thenReturn(List.of(first, second));
        when(accountCodes.resolveCode(1105L, enterpriseId)).thenReturn(Optional.of("220501"));

        List<AgingLine> lines = service.aging(enterpriseId, cutoff, supplierId, null, null);
        BigDecimal agingTotal = lines.stream()
                .map(line -> line.current()
                        .add(line.days1to30())
                        .add(line.days31to60())
                        .add(line.days61to90())
                        .add(line.days91Plus()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        when(invoices.findForStatement(enterpriseId, supplierId, null, cutoff, null, null))
                .thenReturn(List.of(first, second));
        when(vouchers.search(any())).thenReturn(new com.treasury.domain.model.command.TreasuryCommands.PageResult<>(
                List.of(), 0, 0, 0, 10000));
        when(writeOffs.findByEnterprise(enterpriseId)).thenReturn(List.of());
        SupplierStatement statement = service.statement(enterpriseId, supplierId, null, cutoff, null, null);

        assertEquals(new BigDecimal("4000"), agingTotal);
        assertEquals(statement.pending(), agingTotal);
        assertEquals("220501", lines.get(0).accountCode());
    }

    @Test
    void statementExcludesVoidedVouchersFromPaid() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 15);
        Long supplierId = 78L;
        String enterpriseId = "enterprise-a";

        SupplierInvoiceReplica invoice = invoice(99L, supplierId, "662034699", from.plusDays(1),
                new BigDecimal("100533"), new BigDecimal("84854"), new BigDecimal("15679"));
        invoice.setEnterpriseId(enterpriseId);
        when(invoices.findForStatement(enterpriseId, supplierId, null, to, null, null)).thenReturn(List.of(invoice));

        PaymentVoucherDetail postedDetail = new PaymentVoucherDetail();
        postedDetail.setSupplierId(supplierId);
        postedDetail.setInvoiceId(99L);
        postedDetail.setAmountPaid(new BigDecimal("84854"));
        PaymentVoucher posted = new PaymentVoucher();
        posted.setStatus(PaymentVoucherStatus.POSTED);
        posted.setDetails(List.of(postedDetail));

        when(vouchers.search(any())).thenReturn(new com.treasury.domain.model.command.TreasuryCommands.PageResult<>(
                List.of(posted), 1, 1, 0, 10000));
        when(writeOffs.findByEnterprise(enterpriseId)).thenReturn(List.of());

        SupplierStatement statement = service.statement(enterpriseId, supplierId, from, to, null, null);

        assertEquals(new BigDecimal("84854"), statement.paid());
        assertEquals(new BigDecimal("15679"), statement.pending());
        assertEquals(
                statement.openingBalance().add(statement.invoiced()).subtract(statement.paid())
                        .subtract(statement.writeOffTotal()),
                statement.pending());
    }

    @Test
    void statementExcludesVoidedWriteOffsFromTotalsButKeepsTraceHistory() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);
        Long supplierId = 78L;
        String enterpriseId = "enterprise-a";

        SupplierInvoiceReplica invoice = invoice(10L, supplierId, "FC-357", from.plusDays(2),
                new BigDecimal("357000"), BigDecimal.ZERO, new BigDecimal("357000"));
        invoice.setEnterpriseId(enterpriseId);
        when(invoices.findForStatement(enterpriseId, supplierId, null, to, null, null)).thenReturn(List.of(invoice));
        when(vouchers.search(any())).thenReturn(new com.treasury.domain.model.command.TreasuryCommands.PageResult<>(
                List.of(), 0, 0, 0, 10000));

        PayableWriteOffDetail writeOffDetail = new PayableWriteOffDetail();
        writeOffDetail.setSupplierId(supplierId);
        writeOffDetail.setInvoiceId(10L);
        writeOffDetail.setAmount(new BigDecimal("100000"));
        PayableWriteOff voidedWriteOff = new PayableWriteOff();
        voidedWriteOff.setStatus(WriteOffStatus.VOIDED);
        voidedWriteOff.setCreatedAt(Instant.parse("2026-08-10T12:00:00Z"));
        voidedWriteOff.setUpdatedAt(Instant.parse("2026-08-12T15:00:00Z"));
        voidedWriteOff.setAccountingEntryId(99L);
        voidedWriteOff.setDetails(List.of(writeOffDetail));
        when(writeOffs.findByEnterprise(enterpriseId)).thenReturn(List.of(voidedWriteOff));

        SupplierStatement statement = service.statement(enterpriseId, supplierId, from, to, null, null);

        assertEquals(BigDecimal.ZERO, statement.writeOffTotal());
        assertEquals(new BigDecimal("357000"), statement.pending());
        assertEquals(1, statement.writeOffs().size());
        assertEquals(WriteOffStatus.VOIDED, statement.writeOffs().get(0).getStatus());
    }

    @Test
    void statementExcludesDraftDiscardedAndFailedWriteOffsFromTrace() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);
        Long supplierId = 78L;
        String enterpriseId = "enterprise-a";

        SupplierInvoiceReplica invoice = invoice(10L, supplierId, "FC-357", from.plusDays(2),
                new BigDecimal("357000"), BigDecimal.ZERO, new BigDecimal("357000"));
        when(invoices.findForStatement(enterpriseId, supplierId, null, to, null, null)).thenReturn(List.of(invoice));
        when(vouchers.search(any())).thenReturn(new com.treasury.domain.model.command.TreasuryCommands.PageResult<>(
                List.of(), 0, 0, 0, 10000));

        PayableWriteOffDetail detail = new PayableWriteOffDetail();
        detail.setSupplierId(supplierId);
        detail.setInvoiceId(10L);
        detail.setAmount(new BigDecimal("100000"));

        PayableWriteOff draft = new PayableWriteOff();
        draft.setStatus(WriteOffStatus.DRAFT);
        draft.setCreatedAt(Instant.parse("2026-08-10T12:00:00Z"));
        draft.setDetails(List.of(detail));

        PayableWriteOff discardedDraft = new PayableWriteOff();
        discardedDraft.setStatus(WriteOffStatus.VOIDED);
        discardedDraft.setCreatedAt(Instant.parse("2026-08-11T12:00:00Z"));
        discardedDraft.setUpdatedAt(Instant.parse("2026-08-11T12:00:00Z"));
        discardedDraft.setDetails(List.of(detail));

        PayableWriteOff failed = new PayableWriteOff();
        failed.setStatus(WriteOffStatus.FAILED);
        failed.setCreatedAt(Instant.parse("2026-08-12T12:00:00Z"));
        failed.setDetails(List.of(detail));

        when(writeOffs.findByEnterprise(enterpriseId)).thenReturn(List.of(draft, discardedDraft, failed));

        SupplierStatement statement = service.statement(enterpriseId, supplierId, from, to, null, null);

        assertEquals(BigDecimal.ZERO, statement.writeOffTotal());
        assertTrue(statement.writeOffs().isEmpty());
    }

    @Test
    void statementExcludesVoidedVouchersFromPaidButKeepsTraceHistory() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);
        Long supplierId = 78L;
        String enterpriseId = "enterprise-a";

        SupplierInvoiceReplica invoice = invoice(10L, supplierId, "FC-357", from.plusDays(2),
                new BigDecimal("357000"), BigDecimal.ZERO, new BigDecimal("357000"));
        invoice.setEnterpriseId(enterpriseId);
        when(invoices.findForStatement(enterpriseId, supplierId, null, to, null, null)).thenReturn(List.of(invoice));

        PaymentVoucherDetail voidedDetail = new PaymentVoucherDetail();
        voidedDetail.setSupplierId(supplierId);
        voidedDetail.setInvoiceId(10L);
        voidedDetail.setAmountPaid(new BigDecimal("100000"));
        PaymentVoucher voidedVoucher = new PaymentVoucher();
        voidedVoucher.setId(55L);
        voidedVoucher.setStatus(PaymentVoucherStatus.VOIDED);
        voidedVoucher.setIssueDate(from.plusDays(5));
        voidedVoucher.setUpdatedAt(Instant.parse("2026-08-12T15:00:00Z"));
        voidedVoucher.setAccountingEntryId(88L);
        voidedVoucher.setDetails(List.of(voidedDetail));

        when(vouchers.search(any())).thenAnswer(voucherSearchAnswer(List.of(), List.of(voidedVoucher)));
        when(writeOffs.findByEnterprise(enterpriseId)).thenReturn(List.of());

        SupplierStatement statement = service.statement(enterpriseId, supplierId, from, to, null, null);

        assertEquals(BigDecimal.ZERO, statement.paid());
        assertEquals(new BigDecimal("357000"), statement.pending());
        assertEquals(1, statement.vouchers().size());
        assertEquals(PaymentVoucherStatus.VOIDED, statement.vouchers().get(0).getStatus());
    }

    @Test
    void statementExcludesDraftDiscardedAndFailedVouchersFromTrace() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);
        Long supplierId = 78L;
        String enterpriseId = "enterprise-a";

        SupplierInvoiceReplica invoice = invoice(10L, supplierId, "FC-357", from.plusDays(2),
                new BigDecimal("357000"), BigDecimal.ZERO, new BigDecimal("357000"));
        when(invoices.findForStatement(enterpriseId, supplierId, null, to, null, null)).thenReturn(List.of(invoice));

        PaymentVoucherDetail detail = new PaymentVoucherDetail();
        detail.setSupplierId(supplierId);
        detail.setInvoiceId(10L);
        detail.setAmountPaid(new BigDecimal("100000"));

        PaymentVoucher draft = new PaymentVoucher();
        draft.setStatus(PaymentVoucherStatus.DRAFT);
        draft.setIssueDate(from.plusDays(3));
        draft.setDetails(List.of(detail));

        PaymentVoucher discardedDraft = new PaymentVoucher();
        discardedDraft.setStatus(PaymentVoucherStatus.VOIDED);
        discardedDraft.setIssueDate(from.plusDays(4));
        discardedDraft.setUpdatedAt(Instant.parse("2026-08-11T12:00:00Z"));
        discardedDraft.setDetails(List.of(detail));

        PaymentVoucher failed = new PaymentVoucher();
        failed.setStatus(PaymentVoucherStatus.FAILED);
        failed.setIssueDate(from.plusDays(5));
        failed.setDetails(List.of(detail));

        when(vouchers.search(any())).thenAnswer(voucherSearchAnswer(List.of(), List.of(draft, discardedDraft, failed)));
        when(writeOffs.findByEnterprise(enterpriseId)).thenReturn(List.of());

        SupplierStatement statement = service.statement(enterpriseId, supplierId, from, to, null, null);

        assertEquals(BigDecimal.ZERO, statement.paid());
        assertTrue(statement.vouchers().isEmpty());
    }

    @Test
    void activeDocumentFilterExcludesVoidedVoucherAndWriteOffHistory() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);
        Long supplierId = 78L;
        String enterpriseId = "enterprise-a";
        SupplierInvoiceReplica activeInvoice = invoice(10L, supplierId, "FC-ACTIVE", from.plusDays(1),
                new BigDecimal("100000"), BigDecimal.ZERO, new BigDecimal("100000"));
        when(invoices.findForStatement(enterpriseId, supplierId, null, to, null, true))
                .thenReturn(List.of(activeInvoice));

        PaymentVoucher postedVoucher = voucher(1L, PaymentVoucherStatus.POSTED, supplierId, 10L, "25000", from.plusDays(2));
        when(vouchers.search(any())).thenAnswer(voucherSearchAnswer(List.of(postedVoucher), List.of()));
        PayableWriteOff postedWriteOff = writeOff(WriteOffStatus.POSTED, supplierId, 10L, "10000");
        when(writeOffs.findByEnterprise(enterpriseId)).thenReturn(List.of(postedWriteOff,
                writeOff(WriteOffStatus.VOIDED, supplierId, 10L, "5000")));

        SupplierStatement statement = service.statement(enterpriseId, supplierId, from, to, null, true);

        assertEquals(1, statement.vouchers().size());
        assertEquals(PaymentVoucherStatus.POSTED, statement.vouchers().get(0).getStatus());
        assertEquals(1, statement.writeOffs().size());
        assertEquals(WriteOffStatus.POSTED, statement.writeOffs().get(0).getStatus());
    }

    @Test
    void voidedDocumentFilterExcludesPostedVoucherAndWriteOffs() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);
        Long supplierId = 78L;
        String enterpriseId = "enterprise-a";
        SupplierInvoiceReplica voidedInvoice = invoice(10L, supplierId, "FC-VOID", from.plusDays(1),
                new BigDecimal("100000"), BigDecimal.ZERO, new BigDecimal("100000"));
        voidedInvoice.setActive(false);
        when(invoices.findForStatement(enterpriseId, supplierId, null, to, null, false))
                .thenReturn(List.of(voidedInvoice));

        PaymentVoucher voidedVoucher = voucher(2L, PaymentVoucherStatus.VOIDED, supplierId, 10L, "25000", from.plusDays(2));
        voidedVoucher.setAccountingEntryId(88L);
        when(vouchers.search(any())).thenAnswer(voucherSearchAnswer(List.of(), List.of(voidedVoucher)));
        PayableWriteOff voidedWriteOff = writeOff(WriteOffStatus.VOIDED, supplierId, 10L, "10000");
        voidedWriteOff.setAccountingEntryId(99L);
        when(writeOffs.findByEnterprise(enterpriseId)).thenReturn(List.of(
                writeOff(WriteOffStatus.POSTED, supplierId, 10L, "5000"), voidedWriteOff));

        SupplierStatement statement = service.statement(enterpriseId, supplierId, from, to, null, false);

        assertEquals(BigDecimal.ZERO, statement.paid());
        assertEquals(BigDecimal.ZERO, statement.writeOffTotal());
        assertEquals(1, statement.vouchers().size());
        assertEquals(PaymentVoucherStatus.VOIDED, statement.vouchers().get(0).getStatus());
        assertEquals(1, statement.writeOffs().size());
        assertEquals(WriteOffStatus.VOIDED, statement.writeOffs().get(0).getStatus());
    }

    private static Answer<PageResult<PaymentVoucher>> voucherSearchAnswer(
            List<PaymentVoucher> posted, List<PaymentVoucher> voided) {
        return invocation -> {
            VoucherFilter filter = invocation.getArgument(0);
            if (filter.status() == PaymentVoucherStatus.VOIDED) {
                return new PageResult<>(voided, voided.size(), 1, 0, 10000);
            }
            return new PageResult<>(posted, posted.size(), 1, 0, 10000);
        };
    }

    private static PaymentVoucher voucher(Long id, PaymentVoucherStatus status, Long supplierId,
            Long invoiceId, String amount, LocalDate issueDate) {
        PaymentVoucherDetail detail = new PaymentVoucherDetail();
        detail.setSupplierId(supplierId);
        detail.setInvoiceId(invoiceId);
        detail.setAmountPaid(new BigDecimal(amount));
        PaymentVoucher voucher = new PaymentVoucher();
        voucher.setId(id);
        voucher.setStatus(status);
        voucher.setIssueDate(issueDate);
        voucher.setUpdatedAt(issueDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        voucher.setDetails(List.of(detail));
        return voucher;
    }

    private static PayableWriteOff writeOff(WriteOffStatus status, Long supplierId, Long invoiceId, String amount) {
        PayableWriteOffDetail detail = new PayableWriteOffDetail();
        detail.setSupplierId(supplierId);
        detail.setInvoiceId(invoiceId);
        detail.setAmount(new BigDecimal(amount));
        PayableWriteOff writeOff = new PayableWriteOff();
        writeOff.setStatus(status);
        writeOff.setCreatedAt(Instant.parse("2026-08-10T12:00:00Z"));
        writeOff.setUpdatedAt(Instant.parse("2026-08-12T12:00:00Z"));
        writeOff.setDetails(List.of(detail));
        return writeOff;
    }

    private static SupplierInvoiceReplica invoice(Long id, Long supplierId, String reference, LocalDate issueDate,
            BigDecimal original, BigDecimal paid, BigDecimal pending) {
        SupplierInvoiceReplica invoice = new SupplierInvoiceReplica();
        invoice.setId(id);
        invoice.setSupplierId(supplierId);
        invoice.setReference(reference);
        invoice.setIssueDate(issueDate);
        invoice.setDueDate(issueDate.plusDays(30));
        invoice.setOriginalAmount(original);
        invoice.setPaidAmount(paid);
        invoice.setPendingAmount(pending);
        invoice.setActive(true);
        return invoice;
    }
}
