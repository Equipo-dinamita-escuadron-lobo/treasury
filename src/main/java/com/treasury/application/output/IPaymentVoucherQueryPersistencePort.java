package com.treasury.application.output;

import com.treasury.domain.model.PaymentVoucher;
import com.treasury.domain.model.command.TreasuryCommands.PageResult;
import com.treasury.domain.model.command.TreasuryCommands.VoucherFilter;
import java.util.Optional;

public interface IPaymentVoucherQueryPersistencePort {
    Optional<PaymentVoucher> find(Long id, String enterpriseId);
    Optional<PaymentVoucher> findById(Long id);
    Optional<PaymentVoucher> findByIdempotencyKey(String key, String enterpriseId);
    PageResult<PaymentVoucher> search(VoucherFilter filter);
}
