package com.treasury.copy.infrastructure.adapters.input.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de response para cancelar un proceso de copia.
 * REQ-TREASURY-03.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CopyCancelResponseDto {

    private String estado;
    private String mensaje;
}
