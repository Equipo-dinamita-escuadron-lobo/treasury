package com.treasury.application.output;

import com.treasury.domain.model.PaymentMethodData;
import java.util.Optional;

public interface IPaymentMethodProviderPort {
    Optional<PaymentMethodData> findActive(Long paymentMethodId, String enterpriseId);
    boolean isActiveBankAccount(Long bankAccountId, String enterpriseId);
}
