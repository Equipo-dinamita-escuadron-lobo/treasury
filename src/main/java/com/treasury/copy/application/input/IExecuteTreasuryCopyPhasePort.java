package com.treasury.copy.application.input;

import com.treasury.copy.infrastructure.adapters.input.rest.dto.CopyPhaseRequestDto;
import com.treasury.copy.infrastructure.adapters.input.rest.dto.CopyPhaseResponseDto;

/**
 * Puerto de entrada: ejecuta una fase de copia de treasury.
 * REQ-TREASURY-01, ADR-38.
 */
public interface IExecuteTreasuryCopyPhasePort {

    CopyPhaseResponseDto ejecutar(CopyPhaseRequestDto request);
}
