package com.treasury.application.input;

import com.treasury.domain.model.PaymentVoucher;
import com.treasury.domain.model.command.TreasuryCommands.AccountingResult;
import com.treasury.domain.model.command.TreasuryCommands.Voucher;

public interface IPaymentVoucherCommandUseCase {
    PaymentVoucher create(Voucher command);
    PaymentVoucher update(Long id, Voucher command);
    void delete(Long id, String enterpriseId);
    PaymentVoucher post(Long id, String enterpriseId, String idempotencyKey);
    PaymentVoucher voidVoucher(Long id, String enterpriseId, String reason);
    void applyAccountingResult(AccountingResult result);
}
