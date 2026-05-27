package com.treasury.copy.infrastructure.adapters.output.persistence.jpa;

import com.treasury.copy.application.output.ITransactionTargetRepositoryPort;
import com.treasury.infrastructure.adapters.output.jpa.entity.TransactionEntity;
import com.treasury.infrastructure.adapters.output.jpa.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JPA para ITransactionTargetRepositoryPort.
 * Reutiliza el repositorio existente del módulo principal.
 * REQ-TREASURY-01.
 */
@Component
@RequiredArgsConstructor
public class TransactionTargetRepositoryAdapter implements ITransactionTargetRepositoryPort {

    private final TransactionRepository jpaRepository;

    @Override
    @Transactional
    public TransactionEntity guardar(TransactionEntity entity) {
        return jpaRepository.save(entity);
    }
}
