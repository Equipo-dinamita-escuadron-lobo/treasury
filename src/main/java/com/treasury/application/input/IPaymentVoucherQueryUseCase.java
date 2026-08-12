package com.treasury.application.input;

import com.treasury.domain.model.PaymentVoucher;
import com.treasury.domain.model.command.TreasuryCommands.PageResult;
import com.treasury.domain.model.command.TreasuryCommands.VoucherFilter;

public interface IPaymentVoucherQueryUseCase {
    PaymentVoucher find(Long id, String enterpriseId);
    PageResult<PaymentVoucher> search(VoucherFilter filter);
}
