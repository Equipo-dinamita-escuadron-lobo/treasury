package com.treasury.copy.infrastructure.adapters.input.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de response para consultar el estado de un proceso de copia.
 * REQ-TREASURY-03.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CopyStatusResponseDto {

    private Integer fase;
    private String estado;
    private Integer registrosProcesados;
    private Integer intentos;
    private String ultimoError;
}
