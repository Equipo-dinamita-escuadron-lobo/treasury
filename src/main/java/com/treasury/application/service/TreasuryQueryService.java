package com.treasury.application.service;

import org.springframework.stereotype.Service;

import com.treasury.application.ports.input.ITreasuryQueryPort;
import com.treasury.domain.model.Treasury;
import com.treasury.domain.port.ITreasuryQueryRepositoryPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TreasuryQueryService implements ITreasuryQueryPort {

    private final ITreasuryQueryRepositoryPort treasuryQueryRepositoryPort;

    @Override
    public Treasury findById(Long id) {
        return treasuryQueryRepositoryPort.findById(id);
    }

    @Override
    public Treasury findByAccountNumber(String accountNumber) {
        return treasuryQueryRepositoryPort.findByAccountNumber(accountNumber);
    }

    @Override
    public boolean existsById(Long id) {
        return treasuryQueryRepositoryPort.existsById(id);
    }

    @Override
    public boolean existsByAccountNumber(String accountNumber) {
        return treasuryQueryRepositoryPort.existsByAccountNumber(accountNumber);
    }
}
