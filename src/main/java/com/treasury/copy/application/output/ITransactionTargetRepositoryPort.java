package com.treasury.copy.application.output;

import com.treasury.infrastructure.adapters.output.jpa.entity.TransactionEntity;

/**
 * Puerto de salida: escritura de transactions en el tenant destino durante copia.
 * REQ-TREASURY-01.
 */
public interface ITransactionTargetRepositoryPort {

    TransactionEntity guardar(TransactionEntity entity);
}
