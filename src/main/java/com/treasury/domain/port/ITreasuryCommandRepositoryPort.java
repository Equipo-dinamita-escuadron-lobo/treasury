package com.treasury.domain.port;

import com.treasury.domain.model.Treasury;

public interface ITreasuryCommandRepositoryPort {
    Treasury save(Treasury treasury);
    Treasury deposit(Treasury treasury);
    Treasury withdraw(Treasury treasury);
    Treasury transfer(Treasury sourceAccount, Treasury destinationAccount);

    void inactivate(Long id);
    void activate(Long id);
}
