package com.treasury.copy.application.output;

import com.treasury.infrastructure.adapters.output.jpa.entity.SplitEntity;

import java.time.Instant;
import java.util.List;

/**
 * Puerto de salida: lectura de splits del tenant origen para copia.
 * REQ-TREASURY-01.
 */
public interface ISplitSourceRepositoryPort {

    List<SplitEntity> findByEntOrigenBeforeSnapshot(String entOrigen, Instant snapshotCorte);
}
