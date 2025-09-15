package com.treasury.application.ports.input;

import com.treasury.domain.model.Treasury;

public interface ITreasuryCommandPort {
    Treasury createAccount(Treasury treasury);
    Treasury deposit(Treasury treasury);
    Treasury withdraw(Treasury treasury);
    Treasury transfer(Long sourceAccountId, Long destinationAccountId, Double amount);

    void inactivate(Long id);
    void activate(Long id);
}
