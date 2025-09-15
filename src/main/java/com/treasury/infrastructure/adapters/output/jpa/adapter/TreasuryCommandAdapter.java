package com.treasury.infrastructure.adapters.output.jpa.adapter;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.treasury.domain.model.Treasury;
import com.treasury.domain.port.ITreasuryCommandRepositoryPort;
import com.treasury.infrastructure.adapters.output.jpa.entity.TreasuryEntity;
import com.treasury.infrastructure.adapters.output.jpa.mapper.TreasuryEntityMapper;
import com.treasury.infrastructure.adapters.output.jpa.repository.TreasuryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TreasuryCommandAdapter implements ITreasuryCommandRepositoryPort {

    private final TreasuryRepository treasuryRepository;
    private final TreasuryEntityMapper treasuryEntityMapper;

    @Override
    @Transactional
    public Treasury save(Treasury treasury) {
        TreasuryEntity treasuryEntity = treasuryEntityMapper.toEntity(treasury);
        TreasuryEntity savedEntity = treasuryRepository.save(treasuryEntity);
        return treasuryEntityMapper.toDomain(savedEntity);
    }

    @Override
    @Transactional
    public Treasury deposit(Treasury treasury) {
        TreasuryEntity treasuryEntity = treasuryEntityMapper.toEntity(treasury);
        TreasuryEntity savedEntity = treasuryRepository.save(treasuryEntity);
        return treasuryEntityMapper.toDomain(savedEntity);
    }

    @Override
    @Transactional
    public Treasury withdraw(Treasury treasury) {
        TreasuryEntity treasuryEntity = treasuryEntityMapper.toEntity(treasury);
        TreasuryEntity savedEntity = treasuryRepository.save(treasuryEntity);
        return treasuryEntityMapper.toDomain(savedEntity);
    }

    @Override
    @Transactional
    public Treasury transfer(Treasury sourceAccount, Treasury destinationAccount) {
        // Save both accounts in the transfer transaction
        TreasuryEntity sourceEntity = treasuryEntityMapper.toEntity(sourceAccount);
        TreasuryEntity destinationEntity = treasuryEntityMapper.toEntity(destinationAccount);

        treasuryRepository.save(sourceEntity);
        treasuryRepository.save(destinationEntity);

        // Return the updated source account
        return treasuryEntityMapper.toDomain(sourceEntity);
    }

    @Override
    @Transactional
    public void inactivate(Long id) {
        treasuryRepository.inactivateById(id);
    }

    @Override
    @Transactional
    public void activate(Long id) {
        treasuryRepository.activateById(id);
    }
}
