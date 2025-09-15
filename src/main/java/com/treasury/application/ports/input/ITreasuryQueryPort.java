package com.treasury.application.ports.input;

import com.treasury.domain.model.Treasury;

public interface ITreasuryQueryPort {
    Treasury findById(Long id);
    Treasury findByAccountNumber(String accountNumber);
    boolean existsById(Long id);
    boolean existsByAccountNumber(String accountNumber);
}
