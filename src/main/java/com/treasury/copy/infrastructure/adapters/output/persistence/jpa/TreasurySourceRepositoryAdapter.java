package com.treasury.copy.infrastructure.adapters.output.persistence.jpa;

import com.treasury.copy.application.output.ITreasurySourceRepositoryPort;
import com.treasury.infrastructure.adapters.output.jpa.entity.TreasuryEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Adaptador JPA para ITreasurySourceRepositoryPort.
 * REQ-TREASURY-01.
 */
@Component
@RequiredArgsConstructor
public class TreasurySourceRepositoryAdapter implements ITreasurySourceRepositoryPort {

    private final TreasuryCopySourceRepository jpaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TreasuryEntity> findByEntOrigenBeforeSnapshot(String entOrigen, Instant snapshotCorte) {
        return jpaRepository.findByEntOrigenBeforeSnapshot(entOrigen, snapshotCorte);
    }
}
