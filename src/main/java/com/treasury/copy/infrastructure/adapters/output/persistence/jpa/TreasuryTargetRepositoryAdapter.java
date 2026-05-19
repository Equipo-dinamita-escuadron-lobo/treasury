package com.treasury.copy.infrastructure.adapters.output.persistence.jpa;

import com.treasury.copy.application.output.ITreasuryTargetRepositoryPort;
import com.treasury.infrastructure.adapters.output.jpa.entity.TreasuryEntity;
import com.treasury.infrastructure.adapters.output.jpa.repository.TreasuryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JPA para ITreasuryTargetRepositoryPort.
 * Reutiliza el repositorio existente del módulo principal.
 * REQ-TREASURY-01.
 */
@Component
@RequiredArgsConstructor
public class TreasuryTargetRepositoryAdapter implements ITreasuryTargetRepositoryPort {

    private final TreasuryRepository jpaRepository;

    @Override
    @Transactional
    public TreasuryEntity guardar(TreasuryEntity entity) {
        return jpaRepository.save(entity);
    }
}
