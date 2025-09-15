package com.treasury.domain.port;

import com.treasury.domain.model.Treasury;

public interface ITreasuryQueryRepositoryPort {
    Treasury findById(Long id);
    Treasury findByAccountNumber(String accountNumber);
    boolean existsById(Long id);
    boolean existsByAccountNumber(String accountNumber);
}
