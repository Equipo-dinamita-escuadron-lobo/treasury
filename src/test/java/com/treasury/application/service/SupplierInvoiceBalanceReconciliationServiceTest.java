package com.treasury.application.service;

import com.treasury.application.output.IAccountCodeResolverPort;
import com.treasury.application.output.IPayableWriteOffPersistencePort;
import com.treasury.application.output.IPaymentVoucherQueryPersistencePort;
import com.treasury.application.output.ISupplierInvoiceProviderPort;
import com.treasury.domain.model.PaymentVoucher;
import com.treasury.domain.model.PaymentVoucherDetail;
import com.treasury.domain.model.PaymentVoucherStatus;
import com.treasury.domain.model.SupplierInvoiceReplica;
import com.treasury.domain.model.command.TreasuryCommands.PageResult;
import com.treasury.domain.model.command.TreasuryCommands.VoucherFilter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierInvoiceBalanceReconciliationServiceTest {

    @Mock ISupplierInvoiceProviderPort invoices;
    @Mock IPaymentVoucherQueryPersistencePort vouchers;
    @Mock IPayableWriteOffPersistencePort writeOffs;
    @Mock IAccountCodeResolverPort accountCodes;

    SupplierInvoiceBalanceReconciliationService service;

    @BeforeEach
    void setUp() {
        service = new SupplierInvoiceBalanceReconciliationService(invoices, vouchers, writeOffs, accountCodes);
    }

    @Test
    void recalculatesPaidAndPendingFromPostedMovementsOnly() {
        SupplierInvoiceReplica invoice = invoice(10L, "100533", "5033", "95467");
        PaymentVoucher posted = voucher(PaymentVoucherStatus.POSTED, 10L, "84854");
        PaymentVoucher voided = voucher(PaymentVoucherStatus.VOIDED, 10L, "5033");

        when(invoices.findLocked(10L, "ent")).thenReturn(Optional.of(invoice));
        when(vouchers.search(any(VoucherFilter.class))).thenReturn(new PageResult<>(List.of(posted, voided), 2, 1, 0, 10000));
        when(writeOffs.findByEnterprise("ent")).thenReturn(List.of());
        when(invoices.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SupplierInvoiceReplica reconciled = service.reconcile(10L, "ent");

        assertThat(reconciled.getPaidAmount()).isEqualByComparingTo("84854");
        assertThat(reconciled.getPendingAmount()).isEqualByComparingTo("15679");
        verify(invoices).save(invoice);
    }

    @Test
    void normalizesPayableAccountCodeWhenStoredAsInternalId() {
        SupplierInvoiceReplica invoice = invoice(11L, "2323", "0", "2323");
        invoice.setPayableAccountId(1105L);
        invoice.setPayableAccountCode("1105");

        when(invoices.findLocked(11L, "ent")).thenReturn(Optional.of(invoice));
        when(vouchers.search(any())).thenReturn(new PageResult<>(List.of(), 0, 0, 0, 10000));
        when(writeOffs.findByEnterprise("ent")).thenReturn(List.of());
        when(accountCodes.resolveCode(1105L, "ent")).thenReturn(Optional.of("220501"));

        service.reconcile(11L, "ent");

        ArgumentCaptor<SupplierInvoiceReplica> saved = ArgumentCaptor.forClass(SupplierInvoiceReplica.class);
        verify(invoices).save(saved.capture());
        assertThat(saved.getValue().getPayableAccountCode()).isEqualTo("220501");
    }

    private static SupplierInvoiceReplica invoice(Long id, String original, String paid, String pending) {
        SupplierInvoiceReplica invoice = new SupplierInvoiceReplica();
        invoice.setId(id);
        invoice.setEnterpriseId("ent");
        invoice.setOriginalAmount(new BigDecimal(original));
        invoice.setPaidAmount(new BigDecimal(paid));
        invoice.setPendingAmount(new BigDecimal(pending));
        invoice.setReservedAmount(BigDecimal.ZERO);
        return invoice;
    }

    private static PaymentVoucher voucher(PaymentVoucherStatus status, Long invoiceId, String amount) {
        PaymentVoucherDetail detail = new PaymentVoucherDetail();
        detail.setInvoiceId(invoiceId);
        detail.setAmountPaid(new BigDecimal(amount));
        PaymentVoucher voucher = new PaymentVoucher();
        voucher.setStatus(status);
        voucher.setEnterpriseId("ent");
        voucher.setDetails(List.of(detail));
        return voucher;
    }
}
