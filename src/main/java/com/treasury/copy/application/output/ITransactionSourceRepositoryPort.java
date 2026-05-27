package com.treasury.copy.application.output;

import com.treasury.infrastructure.adapters.output.jpa.entity.TransactionEntity;

import java.time.Instant;
import java.util.List;

/**
 * Puerto de salida: lectura de transactions del tenant origen para copia.
 * REQ-TREASURY-01.
 */
public interface ITransactionSourceRepositoryPort {

    List<TransactionEntity> findByEntOrigenBeforeSnapshot(String entOrigen, Instant snapshotCorte);
}
