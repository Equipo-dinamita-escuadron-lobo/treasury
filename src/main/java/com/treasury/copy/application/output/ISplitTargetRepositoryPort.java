package com.treasury.copy.application.output;

import com.treasury.infrastructure.adapters.output.jpa.entity.SplitEntity;

/**
 * Puerto de salida: escritura de splits en el tenant destino durante copia.
 * REQ-TREASURY-01.
 */
public interface ISplitTargetRepositoryPort {

    SplitEntity guardar(SplitEntity entity);
}
