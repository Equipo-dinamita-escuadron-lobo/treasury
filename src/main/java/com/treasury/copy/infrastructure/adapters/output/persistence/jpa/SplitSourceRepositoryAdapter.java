package com.treasury.copy.infrastructure.adapters.output.persistence.jpa;

import com.treasury.copy.application.output.ISplitSourceRepositoryPort;
import com.treasury.infrastructure.adapters.output.jpa.entity.SplitEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Adaptador JPA para ISplitSourceRepositoryPort.
 * SplitEntity no tiene createdAt — el snapshotCorte se ignora (el corte temporal
 * lo aplica la copia de TransactionEntity, entidad padre).
 * REQ-TREASURY-01.
 */
@Component
@RequiredArgsConstructor
public class SplitSourceRepositoryAdapter implements ISplitSourceRepositoryPort {

    private final SplitCopySourceRepository jpaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SplitEntity> findByEntOrigenBeforeSnapshot(String entOrigen, Instant snapshotCorte) {
        return jpaRepository.findByEntOrigenBeforeSnapshot(entOrigen);
    }
}
