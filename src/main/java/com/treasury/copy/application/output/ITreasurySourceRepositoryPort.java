package com.treasury.copy.application.output;

import com.treasury.infrastructure.adapters.output.jpa.entity.TreasuryEntity;

import java.time.Instant;
import java.util.List;

/**
 * Puerto de salida: lectura de treasury del tenant origen para copia.
 * REQ-TREASURY-01.
 */
public interface ITreasurySourceRepositoryPort {

    List<TreasuryEntity> findByEntOrigenBeforeSnapshot(String entOrigen, Instant snapshotCorte);
}
