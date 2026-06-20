package com.treasury.copy.application.output;

import com.treasury.infrastructure.adapters.output.jpa.entity.TreasuryEntity;

/**
 * Puerto de salida: escritura de treasury en el tenant destino durante copia.
 * REQ-TREASURY-01.
 */
public interface ITreasuryTargetRepositoryPort {

    TreasuryEntity guardar(TreasuryEntity entity);
}
