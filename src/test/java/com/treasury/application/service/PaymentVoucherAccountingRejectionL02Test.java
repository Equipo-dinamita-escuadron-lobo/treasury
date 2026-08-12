package com.treasury.application.service;

import com.treasury.domain.model.command.TreasuryCommands.AccountingResult;
import com.treasury.application.output.*;
import com.treasury.domain.model.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L-02: rejected accounting results must leave vouchers in FAILED and allow redelivery
 * when the result arrives before the posting transaction commits.
 */
@ExtendWith(MockitoExtension.class)
class PaymentVoucherAccountingRejectionL02Test {
    @Mock IPaymentVoucherCommandPersistencePort voucherCommands;
    @Mock IPaymentVoucherQueryPersistencePort voucherQueries;
    @Mock ISupplierInvoiceProviderPort invoices;
    @Mock ITreasuryEventPublisher events;
    @Mock ITreasuryAuditPersistencePort audit;
    @Mock IExecutionContextPort context;
    @Mock IPaymentMethodProviderPort paymentMethods;

    PaymentVoucherService service;

    @BeforeEach
    void setUp() {
        service = new PaymentVoucherService(
                voucherCommands, voucherQueries, invoices, events, audit, context, paymentMethods);
        lenient().when(voucherCommands.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void rejectedAccountingResultMovesPostingVoucherToFailedAndReleasesReservation() {
        SupplierInvoiceReplica invoice = invoiceWithReservation("20");
        PaymentVoucher voucher = postingVoucher(invoice, "20");
        AccountingResult rejected = new AccountingResult(
                "result-1", "PAYMENT_VOUCHER", 5L, false, null,
                "Método de pago inactivo o sin equivalencia contable", "tenant");

        when(audit.wasProcessed("result-1")).thenReturn(false);
        when(voucherQueries.findById(5L)).thenReturn(Optional.of(voucher));
        when(invoices.findLocked(1L, "ent")).thenReturn(Optional.of(invoice));

        service.applyAccountingResult(rejected);

        assertThat(voucher.getStatus()).isEqualTo(PaymentVoucherStatus.FAILED);
        assertThat(voucher.getFailureReason()).contains("equivalencia contable");
        assertThat(invoice.getReservedAmount()).isEqualByComparingTo("0");
        verify(audit).markProcessed("result-1", "tenant");
        verify(events, never()).enqueue(any());
    }

    @Test
    void duplicateRejectedResultIsIgnoredAfterVoucherIsAlreadyFailed() {
        SupplierInvoiceReplica invoice = invoiceWithReservation("0");
        PaymentVoucher voucher = postingVoucher(invoice, "20");
        voucher.setStatus(PaymentVoucherStatus.FAILED);
        voucher.setFailureReason("Método de pago inactivo o sin equivalencia contable");
        AccountingResult rejected = new AccountingResult(
                "result-dup", "PAYMENT_VOUCHER", 5L, false, null, "rechazo", "tenant");

        when(audit.wasProcessed("result-dup")).thenReturn(false);
        when(voucherQueries.findById(5L)).thenReturn(Optional.of(voucher));

        service.applyAccountingResult(rejected);

        assertThat(voucher.getStatus()).isEqualTo(PaymentVoucherStatus.FAILED);
        verify(audit).markProcessed("result-dup", "tenant");
        verify(invoices, never()).findLocked(any(), any());
    }

    @Test
    void rejectedResultBeforePostingCommitIsNotMarkedProcessed() {
        SupplierInvoiceReplica invoice = invoiceWithReservation("0");
        PaymentVoucher voucher = postingVoucher(invoice, "20");
        voucher.setStatus(PaymentVoucherStatus.DRAFT);
        AccountingResult rejected = new AccountingResult(
                "result-early", "PAYMENT_VOUCHER", 5L, false, null, "rechazo", "tenant");

        when(audit.wasProcessed("result-early")).thenReturn(false);
        when(voucherQueries.findById(5L)).thenReturn(Optional.of(voucher));

        service.applyAccountingResult(rejected);

        assertThat(voucher.getStatus()).isEqualTo(PaymentVoucherStatus.DRAFT);
        verify(audit, never()).markProcessed(eq("result-early"), any());
    }

    private PaymentVoucher postingVoucher(SupplierInvoiceReplica invoice, String amount) {
        PaymentVoucherDetail detail = new PaymentVoucherDetail();
        detail.setInvoiceId(invoice.getId());
        detail.setSupplierId(invoice.getSupplierId());
        detail.setInvoiceReference(invoice.getReference());
        detail.setPayableAccountId(invoice.getPayableAccountId());
        detail.setPayableAccountCode(invoice.getPayableAccountCode());
        detail.setAmountPaid(new BigDecimal(amount));
        detail.setTenantId("tenant");
        PaymentVoucher voucher = new PaymentVoucher();
        voucher.setId(5L);
        voucher.setEnterpriseId("ent");
        voucher.setStatus(PaymentVoucherStatus.POSTING);
        voucher.setTenantId("tenant");
        voucher.setDetails(List.of(detail));
        return voucher;
    }

    private SupplierInvoiceReplica invoiceWithReservation(String reserved) {
        SupplierInvoiceReplica invoice = new SupplierInvoiceReplica();
        invoice.setId(1L);
        invoice.setEnterpriseId("ent");
        invoice.setSupplierId(10L);
        invoice.setReference("F-1");
        invoice.setPendingAmount(new BigDecimal("100"));
        invoice.setReservedAmount(new BigDecimal(reserved));
        invoice.setPayableAccountId(11L);
        invoice.setPayableAccountCode("2205");
        invoice.setActive(true);
        invoice.setTenantId("tenant");
        return invoice;
    }
}
