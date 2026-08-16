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

public final class SupplierInvoiceBalanceCalculator {
    private SupplierInvoiceBalanceCalculator() {
    }

    public record BalanceSnapshot(BigDecimal paidAmount, BigDecimal pendingAmount, BigDecimal reservedAmount) {
    }

    public static BalanceSnapshot calculate(SupplierInvoiceReplica invoice, List<PaymentVoucher> vouchers,
            List<PayableWriteOff> writeOffs) {
        Long invoiceId = invoice.getId();
        BigDecimal original = invoice.getOriginalAmount() == null ? BigDecimal.ZERO : invoice.getOriginalAmount();
        BigDecimal paid = BigDecimal.ZERO;
        BigDecimal writeOffTotal = BigDecimal.ZERO;
        BigDecimal reserved = BigDecimal.ZERO;

        for (PaymentVoucher voucher : vouchers) {
            for (PaymentVoucherDetail detail : voucher.getDetails()) {
                if (!invoiceId.equals(detail.getInvoiceId())) {
                    continue;
                }
                BigDecimal amount = detail.getAmountPaid() == null ? BigDecimal.ZERO : detail.getAmountPaid();
                if (voucher.getStatus() == PaymentVoucherStatus.POSTED) {
                    paid = paid.add(amount);
                } else if (voucher.getStatus() == PaymentVoucherStatus.POSTING) {
                    reserved = reserved.add(amount);
                }
            }
        }

        for (PayableWriteOff writeOff : writeOffs) {
            for (PayableWriteOffDetail detail : writeOff.getDetails()) {
                if (!invoiceId.equals(detail.getInvoiceId())) {
                    continue;
                }
                BigDecimal amount = detail.getAmount() == null ? BigDecimal.ZERO : detail.getAmount();
                if (writeOff.getStatus() == WriteOffStatus.POSTED) {
                    writeOffTotal = writeOffTotal.add(amount);
                } else if (writeOff.getStatus() == WriteOffStatus.POSTING) {
                    reserved = reserved.add(amount);
                }
            }
        }

        BigDecimal pending = original.subtract(paid).subtract(writeOffTotal);
        return new BalanceSnapshot(paid, pending, reserved);
    }
}
