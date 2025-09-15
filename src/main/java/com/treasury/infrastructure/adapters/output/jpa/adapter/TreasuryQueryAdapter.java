package com.treasury.infrastructure.adapters.output.jpa.adapter;

import org.springframework.stereotype.Component;

import com.treasury.domain.model.Treasury;
import com.treasury.domain.port.ITreasuryQueryRepositoryPort;
import com.treasury.infrastructure.adapters.output.jpa.entity.TreasuryEntity;
import com.treasury.infrastructure.adapters.output.jpa.mapper.TreasuryEntityMapper;
import com.treasury.infrastructure.adapters.output.jpa.repository.TreasuryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TreasuryQueryAdapter implements ITreasuryQueryRepositoryPort {

    private final TreasuryRepository treasuryRepository;
    private final TreasuryEntityMapper treasuryEntityMapper;

    @Override
    public Treasury findById(Long id) {
        TreasuryEntity treasuryEntity = treasuryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Treasury account not found with id: " + id));
        return treasuryEntityMapper.toDomain(treasuryEntity);
    }

    @Override
    public Treasury findByAccountNumber(String accountNumber) {
        TreasuryEntity treasuryEntity = treasuryRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new RuntimeException("Treasury account not found with account number: " + accountNumber));
        return treasuryEntityMapper.toDomain(treasuryEntity);
    }

    @Override
    public boolean existsById(Long id) {
        return treasuryRepository.existsById(id);
    }

    @Override
    public boolean existsByAccountNumber(String accountNumber) {
        return treasuryRepository.existsByAccountNumber(accountNumber);
    }
}
