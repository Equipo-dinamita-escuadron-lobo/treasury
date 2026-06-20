package com.treasury.copy.application.input;

import com.treasury.copy.infrastructure.adapters.input.rest.dto.CopyStatusResponseDto;

/**
 * Puerto de entrada: consulta el estado de un proceso de copia de treasury.
 * REQ-TREASURY-03.
 */
public interface IGetTreasuryCopyStatusPort {

    CopyStatusResponseDto obtenerEstado(String idProceso);
}
