package com.treasury.copy.infrastructure.adapters.input.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de response para la ejecución de una fase de copia.
 * Contrato uniforme REQ-TREASURY-02.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CopyPhaseResponseDto {

    private String estado;
    private int registrosProcesados;
    private List<CopyEquivalenciaDto> equivalenciasGeneradas;
    private String mensaje;
    private List<String> advertencias;
}
