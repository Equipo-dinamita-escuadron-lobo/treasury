package com.treasury.copy.application.input;

import com.treasury.copy.infrastructure.adapters.input.rest.dto.CopyCancelResponseDto;

/**
 * Puerto de entrada: cancela un proceso de copia de treasury.
 * REQ-TREASURY-03.
 */
public interface ICancelTreasuryCopyPort {

    CopyCancelResponseDto cancelar(String idProceso);
}
