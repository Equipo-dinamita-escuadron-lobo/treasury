package com.treasury.domain.service;

import com.treasury.domain.model.PayableWriteOff;
import com.treasury.domain.model.PayableWriteOffDetail;
import com.treasury.domain.model.PaymentVoucher;
import com.treasury.domain.model.PaymentVoucherDetail;
import com.treasury.domain.model.PaymentVoucherStatus;
import com.treasury.domain.model.SupplierInvoiceReplica;
import com.treasury.domain.model.WriteOffStatus;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SupplierInvoiceBalanceCalculatorTest {

    @Test
    void countsOnlyPostedPaymentsAndWriteOffs() {
        SupplierInvoiceReplica invoice = invoice(1L, "100000");
        PaymentVoucher posted = voucher(PaymentVoucherStatus.POSTED, 1L, "60000");
        PaymentVoucher voided = voucher(PaymentVoucherStatus.VOIDED, 1L, "30000");
        PaymentVoucher posting = voucher(PaymentVoucherStatus.POSTING, 1L, "5000");
        PayableWriteOff writeOff = writeOff(WriteOffStatus.POSTED, 1L, "1000");

        var snapshot = SupplierInvoiceBalanceCalculator.calculate(invoice,
                List.of(posted, voided, posting),
                List.of(writeOff));

        assertThat(snapshot.paidAmount()).isEqualByComparingTo("60000");
        assertThat(snapshot.reservedAmount()).isEqualByComparingTo("5000");
        assertThat(snapshot.pendingAmount()).isEqualByComparingTo("39000");
    }

    @Test
    void voidedPaymentDoesNotReducePending() {
        SupplierInvoiceReplica invoice = invoice(1L, "100000");
        PaymentVoucher voidedOnly = voucher(PaymentVoucherStatus.VOIDED, 1L, "30000");

        var snapshot = SupplierInvoiceBalanceCalculator.calculate(invoice, List.of(voidedOnly), List.of());

        assertThat(snapshot.paidAmount()).isZero();
        assertThat(snapshot.pendingAmount()).isEqualByComparingTo("100000");
    }

    private static SupplierInvoiceReplica invoice(Long id, String original) {
        SupplierInvoiceReplica invoice = new SupplierInvoiceReplica();
        invoice.setId(id);
        invoice.setOriginalAmount(new BigDecimal(original));
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setPendingAmount(new BigDecimal(original));
        invoice.setReservedAmount(BigDecimal.ZERO);
        return invoice;
    }

    private static PaymentVoucher voucher(PaymentVoucherStatus status, Long invoiceId, String amount) {
        PaymentVoucherDetail detail = new PaymentVoucherDetail();
        detail.setInvoiceId(invoiceId);
        detail.setAmountPaid(new BigDecimal(amount));
        PaymentVoucher voucher = new PaymentVoucher();
        voucher.setStatus(status);
        voucher.setDetails(List.of(detail));
        return voucher;
    }

    private static PayableWriteOff writeOff(WriteOffStatus status, Long invoiceId, String amount) {
        PayableWriteOffDetail detail = new PayableWriteOffDetail();
        detail.setInvoiceId(invoiceId);
        detail.setAmount(new BigDecimal(amount));
        PayableWriteOff writeOff = new PayableWriteOff();
        writeOff.setStatus(status);
        writeOff.setDetails(List.of(detail));
        return writeOff;
    }
}
