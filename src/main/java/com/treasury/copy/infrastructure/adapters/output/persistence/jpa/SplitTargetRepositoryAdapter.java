package com.treasury.copy.infrastructure.adapters.output.persistence.jpa;

import com.treasury.copy.application.output.ISplitTargetRepositoryPort;
import com.treasury.infrastructure.adapters.output.jpa.entity.SplitEntity;
import com.treasury.infrastructure.adapters.output.jpa.repository.SplitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JPA para ISplitTargetRepositoryPort.
 * Reutiliza el repositorio existente del módulo principal.
 * REQ-TREASURY-01.
 */
@Component
@RequiredArgsConstructor
public class SplitTargetRepositoryAdapter implements ISplitTargetRepositoryPort {

    private final SplitRepository jpaRepository;

    @Override
    @Transactional
    public SplitEntity guardar(SplitEntity entity) {
        return jpaRepository.save(entity);
    }
}
