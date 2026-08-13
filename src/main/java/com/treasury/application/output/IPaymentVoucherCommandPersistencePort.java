package com.treasury.application.output;

import com.treasury.domain.model.PaymentVoucher;

public interface IPaymentVoucherCommandPersistencePort {
    PaymentVoucher save(PaymentVoucher voucher);
    void delete(PaymentVoucher voucher);
}
